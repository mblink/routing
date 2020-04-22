def join_tpes(ts, ps)
  parens = ps && ts.length != 1
  "#{parens ? '(' : ''}#{ts.join(', ')}#{parens ? ')' : ''}"
end
