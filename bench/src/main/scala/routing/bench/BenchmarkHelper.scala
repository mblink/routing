package routing
package bench

import routing.extractor.ExtractRequest

abstract class BenchmarkHelper[Req, Res, Rtr](implicit R: ExtractRequest[Req]) {
  def actionRes(s: String): Res

  lazy val route1Res = () => actionRes("route 1")
  lazy val route2Res = (p: String) => actionRes(s"route 2: $p")
  lazy val route3Res = (i: Int) => actionRes(s"route 3: $i")
  lazy val route4Res = (b: Boolean) => actionRes(s"route 4: $b")
  lazy val route5Res = (s: String, i: Int) => actionRes(s"route 5: $s, $i")

  def router(handlers: Handled[Res]*): Rtr
  def manualRouter(pf: PartialFunction[Req, Res]): Rtr

  lazy val routingService = router(
    route1.handle(_ => route1Res()),
    route2.handle(x => route2Res(x)),
    route3.handle(x => route3Res(x)),
    route4.handle(x => route4Res(x)),
    route5.handle { case (s, i) => route5Res(s, i) }
  )

  lazy val routingManualService = manualRouter {
    case route1(_) => route1Res()
    case route2(param) => route2Res(param)
    case route3(param) => route3Res(param)
    case route4(enabled) => route4Res(enabled)
    case route5(param, id) => route5Res(param, id)
  }

  def request(route: Route[_ <: Method, _]): Req

  val reqs = mkRequests(request)

  def runReq(request: Req, router: Rtr): String

  @inline def run(router: Rtr): String = runReq(reqs.next(), router)
}
