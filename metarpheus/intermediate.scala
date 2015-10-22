package morpheus
package intermediate

sealed trait Type
object Type {
  case class Name(name: String) extends Type
  case class Apply(name: String, args: Seq[Type]) extends Type
}

case class CaseClass(
  name: String, members: List[CaseClass.Member], desc: Option[String])
object CaseClass {
  case class Member(
    name: String, tpe: Type, desc: Option[String])
}

case class RouteParam(
  name: Option[String],
  tpe: Type,
  required: Boolean,
  desc: Option[String])

sealed trait RouteSegment
case object RouteSegment {
  case class Param(routeParam: RouteParam) extends RouteSegment
  case class String(str: java.lang.String) extends RouteSegment
}

case class Route(
  method: String,
  route: List[RouteSegment],
  params: List[RouteParam],
  authenticated: Boolean,
  returns: Type,
  body: Option[Route.Body],
  ctrl: List[String],
  desc: Option[String],
  name: List[String])

object Route {
  case class Body(tpe: Type, desc: Option[String])
}

case class API(
  models: List[CaseClass],
  routes: List[Route]) {

  def stripUnusedModels: API = {
    val modelsInUse: List[intermediate.Type] = {
      routes.flatMap { route =>
        route.route.collect {
          case RouteSegment.Param(routeParam) => routeParam.tpe
        } ++
        route.params.map(_.tpe) ++
        List(route.returns) ++
        route.body.map(b => List(b.tpe)).getOrElse(Nil)
      }
    }

    val inUseConcreteTypeNames: Set[String] = {
      def recurse(t: intermediate.Type): List[intermediate.Type.Name] = t match {
        case name: intermediate.Type.Name => List(name)
        case intermediate.Type.Apply(_, args) => args.flatMap(recurse).toList
      }
      modelsInUse.flatMap(recurse)
    }.map(_.name).toSet

    this.copy(models = models.filter(m => inUseConcreteTypeNames.contains(m.name)))
  }
}
