package routing

import izumi.reflect.Tag
import izumi.reflect.macrortti._

package object bench extends LazyListCompat {
  val route1 = routing.Method.GET / "route1"
  val route2 = routing.Method.GET / "route2" / pathVar[String]("param")
  val route3 = routing.Method.POST / "route3" / pathVar[Int]("param")
  val route4 = routing.Method.GET / "route4" / "part" :? queryParam[Boolean]("enabled")
  val route5 = routing.Method.POST / pathVar[String]("param") / "route5" :? queryParam[Int]("id")

  def mkRequests[R](f: Route[_ <: Method, _] => R): Iterator[R] =
    LazyList.continually(LazyList(f(route1), f(route2), f(route3), f(route4), f(route5))).flatten.iterator

  @annotation.nowarn("msg=match may not be exhaustive")
  def fakeParam(t: Tag[_]): Any = t.tag match {
    case t if t =:= LTT[Int] => 1
    case t if t =:= LTT[String] => "test"
    case t if t =:= LTT[Boolean] => true
  }

  def testParams(r: Route[_, _]): r.Params = (r.paramTpes match {
    case Vector() => ()
    case Vector(t) => fakeParam(t)
    case ts =>
      Class.forName(s"scala.Tuple${ts.size}")
        .getConstructor(List.fill(ts.size)(classOf[AnyRef]):_*)
        .newInstance(ts.map(fakeParam(_).asInstanceOf[AnyRef]):_*)
  }).asInstanceOf[r.Params]
}
