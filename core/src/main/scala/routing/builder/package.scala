package routing

package object builder {
  type NextPath[A, V, PI, PO] = NextRoute[part.PathPart, A, V, PI, PO]
  type NextQuery[A, V, PI, PO] = NextRoute[part.QueryPart, A, V, PI, PO]
}
