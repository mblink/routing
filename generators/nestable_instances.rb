require_relative './util'

def mk_tuple(ts, unit)
  ('(' * ts.length) + "#{unit}, " + ts.map { |t| t + ')' }.join(', ')
end

def tuple_val(ts, i)
  't' + ('._1' * (ts.length - (i + 1))) + '._2'
end

def nestable_n(i)
  ts = (1..i).to_a.map { |j| "A#{j}" }
  [
    "implicit def nestable#{ts.length}[#{join_tpes(ts, false)}]: Nestable[#{join_tpes(ts, true)}, #{mk_tuple(ts, 'Unit')}] =",
    "  inst(",
    "    t => #{mk_tuple(ts.map.with_index { |_, i| (ts.length == 1) ? 't' : "t._#{i + 1}" }, '()')},",
    "    t => #{join_tpes(ts.map.with_index { |_, i| tuple_val(ts, i) }, true)})",
    ""
  ]
end

puts <<-EOT
package routing
package util

trait NestableInstances {
  def inst[Flat, Nested](f: Flat => Nested, g: Nested => Flat): Nestable[Flat, Nested] =
    new Nestable[Flat, Nested](f, g) {}

  implicit val nestable0: Nestable[Unit, Unit] =
    inst(_ => (), _ => ())

#{(1..22).flat_map { |i| nestable_n(i) }.map { |l| "  #{l}" }.join("\n").rstrip}
}
EOT
