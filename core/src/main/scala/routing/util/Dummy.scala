package routing
package util

private[routing] object dummy {
  sealed trait Dummy1
  object Dummy1 { implicit val inst: Dummy1 = new Dummy1 {} }
  sealed trait Dummy2
  object Dummy2 { implicit val inst: Dummy2 = new Dummy2 {} }
  sealed trait Dummy3
  object Dummy3 { implicit val inst: Dummy3 = new Dummy3 {} }
  sealed trait Dummy4
  object Dummy4 { implicit val inst: Dummy4 = new Dummy4 {} }
  sealed trait Dummy5
  object Dummy5 { implicit val inst: Dummy5 = new Dummy5 {} }
  sealed trait Dummy6
  object Dummy6 { implicit val inst: Dummy6 = new Dummy6 {} }
  sealed trait Dummy7
  object Dummy7 { implicit val inst: Dummy7 = new Dummy7 {} }
  sealed trait Dummy8
  object Dummy8 { implicit val inst: Dummy8 = new Dummy8 {} }
  sealed trait Dummy9
  object Dummy9 { implicit val inst: Dummy9 = new Dummy9 {} }
  sealed trait Dummy10
  object Dummy10 { implicit val inst: Dummy10 = new Dummy10 {} }
}
