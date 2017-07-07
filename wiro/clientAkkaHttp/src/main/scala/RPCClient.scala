package wiro
package client.akkaHttp

import akka.actor.ActorSystem

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal

import akka.stream.ActorMaterializer

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._

import io.circe._
import io.circe.syntax._

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

  private[this] def commandHttpRequest(request: Request, uri: String): HttpRequest =
    HttpRequest(
      uri = uri,
      method = HttpMethods.POST,
      entity = HttpEntity(
        contentType = ContentTypes.`application/json`,
        string = request.args.asJson.noSpaces
      )
    )


  private[this] def queryHttpRequqest(request: Request, uri: String): HttpRequest = {
    val args = request.args.map { case (name, value) => s"$name=${value.noSpaces}" }.mkString("&")
    val completeUri = s"$uri?$args"
    HttpRequest(
      uri = completeUri,
      method = HttpMethods.GET
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
      case OperationType.Query(_) => queryHttpRequqest(autowireRequest, uri)
    }

    Http().singleRequest(httpRequest)
      .flatMap{ response => Unmarshal(response.entity).to[Json] }
  }
}
