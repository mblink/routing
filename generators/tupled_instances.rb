require_relative './util'

def mk_tuple(ts, unit)
  ('(' * ts.length) + "#{unit}, " + ts.map { |t| t + ')' }.join(', ')
end

def tuple_val(ts, i)
  't' + ('._1' * (ts.length - (i + 1))) + '._2'
end

def tupled_n(i)
  ts = (1..i).to_a.map { |j| "A#{j}" }
  ps = ts.map(&:downcase)
  tsb = ts + ['B']
  psb = ps + ['b']
  [
    "implicit def tupled#{ts.length}[#{join_tpes(ts, false)}, B]: Tupled[#{join_tpes(ts, true)}, B, #{join_tpes(tsb, true)}] =",
    "  inst(",
    "    { case (#{join_tpes(ps, true)}, b) => #{join_tpes(psb, true)} },",
    "    { case (#{join_tpes(psb, true)}) => (#{join_tpes(ps, true)}, b) })",
    ""
  ]
end

def join_lines(ls)
  ls.join("\n").rstrip
end

puts <<-EOT
package routing
package util

trait TupledInstancesLP1 {
  def inst[A, B, O](abo: (A, B) => O, oab: O => (A, B)): Tupled[A, B, O] =
    new Tupled[A, B, O] {
      def tuple(a: A, b: B): O = abo(a, b)
      def untuple(o: O): (A, B) = oab(o)
    }

  #{join_lines(tupled_n(1))}
}

trait TupledInstancesLP0 extends TupledInstancesLP1 {

#{join_lines((2..21).flat_map { |i| tupled_n(i) }.map { |l| "  #{l}" })}
}

trait TupledInstances extends TupledInstancesLP0 {
  implicit val tupledUnitUnitUnit: Tupled[Unit, Unit, Unit] =
    inst((_, _) => (), _ => ((), ()))

  implicit def tupledUnitAA[A]: Tupled[Unit, A, A] =
    inst((_, a) => a, ((), _))

  implicit def tupledAUnitA[A]: Tupled[A, Unit, A] =
    inst((a, _) => a, (_, ()))
}
EOT
