package wiro
package client.akkaHttp

import akka.actor.ActorSystem

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers.RawHeader

import akka.stream.ActorMaterializer

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

import scala.concurrent.{ ExecutionContext, Future }

trait RPCClientContext[T] extends MetaDataMacro with PathMacro {
  def methodsMetaData: Map[String, MethodMetaData]
  def tp: Seq[String]
  def path: String = tp.last
}

class RPCClient(
  config: Config,
  ctx: RPCClientContext[_]
)(implicit
  system: ActorSystem,
  materializer: ActorMaterializer,
  executionContext: ExecutionContext
) extends autowire.Client[Json, Decoder, Encoder] {
  def write[Result: Encoder](r: Result): Json = r.asJson

  def read[Result: Decoder](p: Json): Result = {
    //This trick is required to match the result type of autowire
    val right = Json.obj("Right" -> Json.obj("b" -> p))
    val left = Json.obj("Left" -> Json.obj("a" -> p))
    (left.as[Result], right.as[Result]) match {
      case (_, Right(result)) => result
      case (Right(result), _) => result
      case (Left(error1), Left(error2))  =>
        throw new Exception(error1.getMessage + error2.getMessage)
    }
  }

  private[this] def splitTokenArgs(args: Map[String, Json]): (List[String], Map[String, Json]) = {
    val tokenCandidates = args.map { case (_, v) => v.as[wiro.Auth] }.collect { case Right(result) => result.token }.toList
    val nonTokenArgs = args.filter { case (_, v) => v.as[wiro.Auth].isLeft }
    (tokenCandidates, nonTokenArgs)
  }

  private[this] def handlingToken(
    autowireRequest: Request
  )(
    httpRequest: (Map[String, Json]) => HttpRequest
  ): HttpRequest = {
    val (tokenCandidates, nonTokenArgs) = splitTokenArgs(autowireRequest.args)
    val maybeToken =
      if (tokenCandidates.length > 1) throw new Exception("Only one parameter of wiro.Auth type should be provided")
      else tokenCandidates.headOption

    maybeToken match {
      //TODO replace with (Authorization(OAuth2BearerToken(token))))
      case Some(token) => httpRequest(nonTokenArgs).addHeader(RawHeader("Authorization", s"Token token=$token"))
      case None => httpRequest(nonTokenArgs)
    }
  }

  private[this] def commandHttpRequest(request: Request, uri: String): HttpRequest =
    handlingToken(request) { args =>
      HttpRequest(
        uri = uri,
        method = HttpMethods.POST,
        entity = HttpEntity(
          contentType = ContentTypes.`application/json`,
          string = args.asJson.noSpaces
        )
      )
    }

  private[this] def queryHttpRequest(request: Request, uri: String): HttpRequest =
    handlingToken(request) { nonTokenArgs =>
      val args = nonTokenArgs.map { case (name, value) => s"$name=${value.noSpaces}" }.mkString("&")
      val completeUri = s"$uri?$args"
      val method = HttpMethods.GET

      HttpRequest(
        uri = completeUri,
        method = method
      )
    }

  override def doCall(autowireRequest: Request): Future[Json] = {
    val completePath = autowireRequest.path.mkString(".")
    //we're trying to match here the paths generated by two different macros
    //if it fails at runtime it means something is wrong in the implementation
    val methodMetaData = ctx.methodsMetaData
      .getOrElse(completePath, throw new Exception(s"Couldn't find metadata about method $completePath"))
    val operationName = methodMetaData.operationType.name.getOrElse(autowireRequest.path.last)
    val uri = s"http://${config.host}:${config.port}/${ctx.path}/$operationName"

    val httpRequest = methodMetaData.operationType match {
      case OperationType.Command(_) => commandHttpRequest(autowireRequest, uri)
      case OperationType.Query(_) => queryHttpRequest(autowireRequest, uri)
    }

    Http().singleRequest(httpRequest)
      .flatMap{ response => Unmarshal(response.entity).to[Json] }
  }
}
