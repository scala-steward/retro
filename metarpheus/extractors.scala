package morpheus

import scala.meta._
import scala.meta.dialects.Scala211

package object extractors {

  def parse(file: java.io.File): scala.meta.Source = file.parse[Source]

  /**
   * Extract all terms from a sequence of applications of an infix operator
   * (which translates to nested `ApplyInfix`es).
   * e.g. getAllInfix(t1 + t2 + t3 + t4, "+") results in List(t1, t2, t3, t4)
   */
  def getAllInfix(ainfix: internal.ast.Term, op: String): List[internal.ast.Term] = {
    import scala.meta.internal.ast._
    ainfix match {
      case Term.ApplyInfix(subinfix: Term.ApplyInfix, Term.Name(`op`), Nil, List(term : Term)) =>
        getAllInfix(subinfix, `op`) :+ term
      case Term.ApplyInfix(term1: Term, Term.Name(`op`), Nil, List(term2 : Term)) =>
        term1 :: term2 :: Nil
      case term: Term => term :: Nil
    }
  }

  /*
   * Convert a scala-meta representation of a type to a metarpheus
   * intermediate representation
   */
  def tpeToIntermediate(tpe: internal.ast.Type): intermediate.Type = tpe match {
    case name: scala.meta.internal.ast.Type.Name =>
      intermediate.Type.Name(name.value)
    case scala.meta.internal.ast.Type.Apply(name: scala.meta.internal.ast.Type.Name, args) =>
      intermediate.Type.Apply(name.value, args.map(tpeToIntermediate))
  }

  private val emptyTokens = Set(" ", "\\n", "comment")

  private[extractors] def stripCommentMarkers(s: String) =
    s.stripPrefix("/")
      .dropWhile(_ == '*')
      .reverse
      .stripPrefix("/")
      .dropWhile(_ == '*')
      .reverse

  /*
   * Search for the comment associated with this definition
   */
  private[extractors] def findRelatedComment(source: scala.meta.Source, t: scala.meta.internal.ast.Defn): Option[scala.meta.Token] = {
    val tokenIdx = source.tokens.indexOf(t.tokens(0))
    source.tokens.take(tokenIdx).reverse
      .takeWhile(c => emptyTokens.contains(c.name))
      .find(_.name == "comment")
  }

  private[extractors] sealed trait Tag
  private[extractors] case class ParamDesc(name: String, desc: String) extends Tag

  /**
   * Extract route description and tags (such as @param) from route comment
   */
  private[extractors] def extractDescAndTagsFromComment(
    token: Option[scala.meta.Token]): (Option[String], List[Tag]) =

    token.map { c =>
      val cleanLines = stripCommentMarkers(c.code)
        .split("\n").map(_.trim.stripPrefix("*").trim)
        .filter(_ != "").toList

      val TagRegex = """@([^\s]+) (.*)""".r
      val ParamRegex = """@param ([^\s]+) (.*)""".r

      val (desc, tagLines) = cleanLines.span(_ match {
        case TagRegex(_, _) => false
        case _ => true
      })

      @annotation.tailrec
      def getTags(acc: List[Tag], lines: List[String]): List[Tag] = lines match {
        case Nil => acc
        case l :: ls => {
          val (tagls, rest) = ls.span(_ match {
            case TagRegex(tag, rest) => false
            case _ => true
          })
          val next = l match {
            case ParamRegex(name, l1) => ParamDesc(name, (l1 :: tagls).mkString(" "))
          }
          getTags(acc :+ next, rest)
        }
      }

      (Some(desc.mkString(" ")), getTags(Nil, tagLines))
    }.getOrElse((None, List()))
}
