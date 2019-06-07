package io.buildo.tapiro

import io.buildo.metarpheus.core.intermediate.{Route, RouteParam, Type => MetarpheusType}
import scala.meta._

object Meta {
  val http4sClass = (
    `package`: Term.Name,
    controllerName: Type.Name,
    endpointsName: Type.Name,
    implicits: List[Term.Param],
    http4sEndpoints: List[Defn.Val],
    app: Defn.Val,
  ) => {
    val tapirEndpoints = q"private[this] val endpoints = new $endpointsName()"
    val httpsEndpointsName = Type.Name(s"${controllerName.syntax}Http4sEndpoints")
    q"""package ${`package`} {
  import cats.effect._
  import cats.implicits._
  import cats.data.NonEmptyList

  import org.http4s._
  import org.http4s.implicits._

  import tapir.server.http4s._
  import tapir.Codec.JsonCodec

  class $httpsEndpointsName(controller: $controllerName[IO])(..$implicits) {
    ..${tapirEndpoints +: http4sEndpoints :+ app}
  }
}
"""
  }

  val httpApp = (head: Route, tail: List[Route]) => {
    val first = Term.Name(head.name.last)
    val rest = tail.map(a => Term.Name(a.name.last))
    q"val app: HttpApp[IO] = NonEmptyList($first, List(..$rest)).reduceK.orNotFound"
  }

  val http4sEndpoints = (routes: List[Route]) =>
    routes.flatMap { route =>
      val name = Term.Name(route.name.last)
      val endpointsName = Term.Select(Term.Name("endpoints"), name)
      val controllersName = Term.Select(Term.Name("controller"), name)
      val controllerContent =
        if (route.method == "get") Some(Term.Select(Term.Eta(controllersName), Term.Name("tupled")))
        else if (route.method == "post") Some(controllersName)
        else None
      controllerContent.map { content =>
        val toRoutes = Term.Apply(Term.Select(endpointsName, Term.Name("toRoutes")), List(content))
        q"private[this] val ${Pat.Var(name)}: HttpRoutes[IO] = $toRoutes"
      }
    }

  val tapirClass = (
    `package`: Term.Name,
    name: Type.Name,
    implicits: List[Term.Param],
    objects: List[Defn.Val],
  ) => {
    q"""package ${`package`} {
  import tapir._
  import tapir.Codec.JsonCodec

  class $name(..$implicits) {
   ..$objects
  }
}
""",
  }

  val codecsImplicits = (routes: List[Route]) => {
    routes.map { route =>
      List(route.returns) ++ route.body.map(_.tpe)
    }.flatten.distinct.map(typeToImplicitParam),
  }

  val routeToTapirEndpoint = (route: Route) =>
    q"val ${Pat.Var(Term.Name(route.name.tail.mkString))}: ${endpointType(route)} = ${endpointImpl(route)}"

  private[this] val typeToImplicitParam = (tpe: MetarpheusType) => {
    val name = typeNameString(tpe)
    val paramName = Term.Name(s"${name.head.toLower}${name.tail}")
    param"implicit ${paramName}: JsonCodec[${typeName(tpe)}]"
  }

  private[this] val typeName = (`type`: MetarpheusType) => Type.Name(typeNameString(`type`))

  private[this] val typeNameString = (`type`: MetarpheusType) =>
    `type` match {
      case MetarpheusType.Apply(name, _) => name
      case MetarpheusType.Name(name)     => name
    }

  private[this] val endpointType = (route: Route) => {
    val returnType = typeName(route.returns)
    val argsList = route.params.map(p => typeName(p.tpe)) ++
      route.body.map(b => typeName(b.tpe))
    val argsType = argsList match {
      case Nil         => Type.Name("Unit")
      case head :: Nil => head
      case l           => Type.Tuple(l)
    }
    t"Endpoint[$argsType, String, $returnType, Nothing]"
  }

  private[this] val endpointImpl = (route: Route) => {
    val basicEndpoint = Term.Apply(
      Term.Select(Term.Select(Term.Name("endpoint"), Term.Name(route.method)), Term.Name("in")),
      List(Lit.String(route.name.tail.mkString)),
    )
    withOutput(
      withError(
        route.method match {
          case "get" =>
            route.params.foldLeft(basicEndpoint) { (acc, param) =>
              withParam(acc, param)
            }
          case "post" =>
            route.params.foldLeft(basicEndpoint) { (acc, param) =>
              withBody(acc, param.tpe)
            }
          case _ => throw new Exception("method not supported")
        },
      ),
      route.returns,
    )
  }

  private[this] val withBody = (endpoint: meta.Term, tpe: MetarpheusType) => {
    Term.Apply(
      Term.Select(endpoint, Term.Name("in")),
      List(Term.ApplyType(Term.Name("jsonBody"), List(Type.Name(typeNameString(tpe))))),
    ),
  }

  private[this] val withError = (endpoints: meta.Term) =>
    Term.Apply(Term.Select(endpoints, Term.Name("errorOut")), List(Term.Name("stringBody")))

  private[this] val withOutput = (endpoint: meta.Term, returnType: MetarpheusType) =>
    Term.Apply(
      Term.Select(endpoint, Term.Name("out")),
      List(
        Term.ApplyType(
          Term.Name("jsonBody"),
          List(typeName(returnType)),
        ),
      ),
    )

  private[this] val withParam = (endpoint: meta.Term, param: RouteParam) => {
    val noDesc =
      Term.Apply(
        Term.Select(endpoint, Term.Name("in")),
        List(
          Term.Apply(
            Term.ApplyType(Term.Name("query"), List(Type.Name(typeNameString(param.tpe)))),
            List(Lit.String(param.name.getOrElse(typeNameString(param.tpe)))),
          ),
        ),
      )

    param.desc match {
      case None => noDesc
      case Some(desc) =>
        Term.Apply(Term.Select(noDesc, Term.Name("description")), List(Lit.String(desc)))
    }
  }
}
