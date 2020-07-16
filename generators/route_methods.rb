require_relative './util'

def gen_method(meth_name, ret_tpe)
  (1..22).reduce([
    "def #{meth_name}()(implicit ev: Unit <:< P): #{ret_tpe} =",
    "  self.#{meth_name}Raw(ev(()))",
    ""
  ]) do |lines, i|
    ts = (1..i).to_a.map { |j| "A#{j}" }
    tpe = ->(u, x) { ts[1..-1].reduce("(#{u}, #{x.(ts[0])})") { |acc, t| "(#{acc}, #{x.(t)})" } }
    lines + [
      "def #{meth_name}[#{join_tpes(ts, false)}](#{ts.map { |t| "#{t.downcase}: #{t}" }.join(', ')})(",
      "  implicit ev: #{tpe.('Unit', ->(t) { t })} <:< P",
      "): #{ret_tpe} =",
      "  self.#{meth_name}Raw(#{tpe.('()', ->(t) { t.downcase })})",
      ""
    ]
  end
end

puts <<-EOT
package routing
package util

trait RouteMethods[M <: Method, P] { self: Route[M, P] =>
#{(gen_method('path', 'ReversePath') +
   gen_method('query', 'ReverseQuery') +
   gen_method('uri', 'ReverseUri') +
   gen_method('url', 'ReverseUri') +
   gen_method('call', 'Call') +
   gen_method('apply', 'Call')).map { |l| "  #{l}" }.join("\n").rstrip}
}
EOT
