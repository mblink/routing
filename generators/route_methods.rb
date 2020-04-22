require_relative './util'

def gen_method(meth_name, ret_tpe)
  lines = [
    "def #{meth_name}()(implicit ev: Unit <:< P): #{ret_tpe} =",
    "  self.#{meth_name}Raw(ev(()))",
    ""
  ]
  (1..22).each do |i|
    ts = (1..i).to_a.map { |j| "A#{j}" }
    lines += [
      "def #{meth_name}[#{join_tpes(ts, false)}](#{ts.map { |t| "#{t.downcase}: #{t}" }.join(', ')})(implicit N: Nestable[#{join_tpes(ts, true)}, P]): #{ret_tpe} =",
      "  self.#{meth_name}Raw(N.nest(#{join_tpes(ts, true).downcase}))",
      ""
    ]
  end

  lines
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
