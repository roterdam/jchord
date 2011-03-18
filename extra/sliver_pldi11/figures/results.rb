#!/usr/bin/ruby

require 'myutils'
require 'rfig/FigureSet'
require 'sequel'
require 'utils'

############################################################

initFigureSet(:outPrefix => '.', :defaultFont => 'times',
              :latexHeader => IO.readlines('../std-macros.tex') +
                              IO.readlines('../macros.tex'),
              :outputStrings => false,
              :lazy => true)

def C(x)
  return nil unless x
  x = x.to_i
  return "#{x/1000},#{sprintf("%03d", x%1000)}" if x >= 1000
  return x
end
def T(x)
  return nil unless x
  x = x.to_i
  return x if x < 10**3
  return "#{sprintf("%.1f", x/10.0**3)}K" if x < 10*10**3
  return "#{round(x/10.0**3,0)}K" if x < 10**6
  return "#{sprintf("%.1f", x/10.0**6)}M" if x < 10*10**6
  return "#{round(x/10.0**6,0)}M"
end
def S(x); x && "\\#{x}" end 
def F(x); x && sprintf("%.2f", x) end

$clients = ['downcast', 'monosite', 'race']
$benchmarks = ['elevator', 'hedc', 'weblech', 'lusearch', 'avrora']
$methods = ['full', 'site', 'pr:none', 'pr:is', 'pr:has', 'pr:is,has']
def genMethodNames(abs)
  ['$\Full(_)$', '$\Site(_)$', '$\PR(_)$', '$\PR(_,\hclassIs)$', '$\PR(_,\hclassHas)$', '$\PR(_,\hclassIsHas)$'].map { |x|
    x.sub(/_/, abs)
  }
end
$methodNames = {
  false => genMethodNames('\Klimabs'),
  true => genMethodNames('\Klimdabs')
}

$absSizeLabel = '$|A_t\'|$'

#def CB(client, benchmark) "[#{S(client)},#{S(benchmark)}]" end
def CB(client, benchmark) "#{S(client)}/#{S(benchmark)}" end

DB = Sequel.connect('sqlite://sliver.db')
if DB.tables.size == 0
  puts "Populating database..."
  keys = {}
  records = []
  IO.foreach('sliver.results').each { |line|
    record = {}
    line.split(/\t/).each { |pair|
      key, value = pair.split(/:/, 2)
      key, value = key.to_sym, value.chomp
      keys[key] = true
      record[key] = value
    }
    records << record
  }
  DB.create_table :records do
    keys.keys.each { |key|
      String key.to_sym
    }
  end
  records.each { |r| DB[:records].insert(r) }
end
$R = DB[:records]

# Print out basic statistics
if true
  descriptions = {
    'elevator' => 'discrete event simulation program',
    'hedc' => 'web crawler',
    'weblech' => 'website downloading and mirroring tool',
    'lusearch' => 'text search tool',
    'avrora' => 'simulation and analysis framework for AVR microcontrollers'
  }

  #colLabels = ['\\# classes', '\\# methods', '\\# bytecodes', '$|\H|$']
  colLabels = ['description', '\\# classes', '\\# methods', '\\# bytecodes', '$|\H|$']
  rowLabels = []
  contents = []
  $benchmarks.each { |benchmark|
    $R.filter(:benchmark => benchmark).filter{numClasses}.each { |r|
      rowLabels << S(r[:benchmark])
      contents << [descriptions[r[:benchmark]], C(r[:numClasses]), C(r[:numMethods]), T(r[:numBytecodes]), C(r[:numH])]
      break
    }
  }
  stats = DataTable.new(:contents => contents, :rowLabels => rowLabels, :colLabels => colLabels)
  IO.writelines('stats.tex', [latexTable(stats).lines(false).justify('lrrrr').render])
end

# Graph abstraction size (A') or time across iterations
def absIterGraph(metric,logScale)
  nameGraphs = []
  $clients.each { |client|
    $benchmarks.each { |benchmark|
      #puts "#{client} #{benchmark}"
      empty = false
      if [['downcast','elevator']].index([client,benchmark])
        #nameGraphs << ['','']
        #next
        empty = true
      end
      contents = []
      sel = [0,1,2,5] # Only select pr:is,has
      $methods.values_at(*sel).each { |method|
        contents += $R.filter(:client => client, :benchmark => benchmark, :method => method, :disallowRepeats => 'false').map { |r|
          case metric
          when 'Ap' then
            v = r[:Ap]; next unless v; v = Integer(v)
            v = Math.log(v) if logScale
          when 'time' then # the value consists of two values [client time only, total time]
            client_v = r[:clientTime]
            next unless client_v
            client_v = Integer(client_v) / 60.0
            total_v = client_v + [r[:relevantTime], r[:preClientTime], r[:preRelevantTime]].compact.map{|x| Integer(x)}.sum / 60.0
            client_v = Math.log(client_v) if logScale
            total_v = Math.log(total_v) if logScale
            if client_v == total_v
              v = client_v
            else
              v = [client_v, total_v]
            end
          end
          #puts v.inspect
          [Integer(r[:iteration]), v]
        }.compact.transpose
      }
      numIters = contents.map { |a| a.size }.max
      errorBars = contents.map { |a| a.map { |x| x.is_a?(Array) ? x : nil } }
      contents = contents.map { |a| a.map { |x| x.is_a?(Array) ? x[1] : x } } # Take the
      #puts contents.inspect
      cellName = {'Ap' => $absSizeLabel, 'time' => 'time'}[metric]
      dataTable = DataTable.new(
        :rowLabels => $methodNames[false].values_at(*sel).map{|x|[nil,x]}.flatten,
        :colName => '$t$', :cellName => cellName,
        :contents => contents, :errorBars => errorBars)
      g = lineGraph(dataTable).useAdjRowPairs.ylabelBold(false)
      empty ? g.legendPosition(0,0) : g.legendPosition(nil)
      g.hortErrorBarPostFunc{nil}
      g.vertErrorBarPostFunc { |e| e.dashed('evenly') }
      g.yrotateAxisLabel(false)
      g.ysciNotation.xrange(0) if metric == 'Ap'
      g.yexpValue.ytickIncrValue(Math.log(10)) if logScale
      g.xroundPlaces(0).xrange(1).xtickIncrValue([numIters/4,1].max)
      g.xlength(u(2.5)).ylength(u(2))
      g.colors(red, magenta, green, blue)
      nameGraphs << [CB(client, benchmark), g]
    }
  }
  #nameGraphs[0][1] = nameGraphs[1][1].getLegend
  arrangeGraphs(nameGraphs, $benchmarks.size)
end
printObj(:obj => absIterGraph('Ap',false).signature(147), :outPrefix => 'absSizeGraph')
#printObj(:obj => absIterGraph('Ap',true).signature(135), :outPrefix => 'absSizeGraph-log')
#printObj(:obj => absIterGraph('time',false).signature(138), :outPrefix => 'timeGraph')
#printObj(:obj => absIterGraph('time',true).signature(136), :outPrefix => 'timeGraph-log')

# Print out number of queries proven
N = 5
if true
  rowLabels = []
  contents = []
  $clients.each { |client|
    $benchmarks.each { |benchmark|
      rowLabels << CB(client, benchmark)
      #puts rowLabels.last
      numUnproven = []
      ['full', 'pr:none'].each { |method|
        $R.filter(:client => client, :benchmark => benchmark, :method => method, :disallowRepeats => 'false').each { |r|
          iter = Integer(r[:iteration])
          n = r[:numUnproven]; next unless n
          n = C(n)
          numUnproven[iter] = method == 'pr:none' && (not numUnproven[iter]) ? "{\\bf #{n}}" : n
        }
      }
      #puts numUnproven.inspect
      contents << (1..N).map { |i| numUnproven[i] || '-' }
    }
  }
  tab = latexTable(DataTable.new(:contents => contents, :rowLabels => rowLabels, :cellName => '$k$',
                                 :colLabels => (1..N).to_a)).lines(false).justify('r'*N)
  #puts tab.render
  IO.writelines("numUnproven.tex", [tab.render])
end

# Show fraction change in abstraction size
if true
  colLabels = [
    '$\frac{|B_t|}{|A_t|}$', # Ratio of projection
    '$\frac{|\tilde B_t|}{|B_t|}$', # of projected pruning
    '$\frac{|A_t\'|}{|A_t|}$', # Effect of prepruning
    '$\frac{|\tilde A_t|}{|A_t|}$', # Total effect of pruning
    '$\frac{|A_{t+1}|}{|A_t|}$', # Ratio of refinement
  nil].compact
  rowLabels = []
  contents = []
  [$clients, [nil]][0].each { |client|
    [$benchmarks, [nil]][0].each { |benchmark|
      [(0...30), [nil]][1].each { |iteration|
        query = {:method => 'pr:is,has', :disallowRepeats => 'false'}
        query[:client] = client if client
        query[:benchmark] = benchmark if benchmark
        query[:iteration] = iteration if iteration
        label = [S(client), S(benchmark), iteration].compact.join('/')
        data = []
        $R.filter(query).each { |r|
          next unless r[:A] && r[:tA] && r[:nextA]
          a = Float(r[:A])
          b = Float(r[:B])
          tb = Float(r[:tB])
          ap = Float(r[:Ap])
          ta = Float(r[:tA])
          aa = Float(r[:nextA])
          #puts [label, a, ap, ta, aa].inspect
          data << [1.0*b/a, 1.0*tb/b, 1.0*ap/a, 1.0*ta/a, 1.0*aa/a]
        }
        next unless data.size > 0
        rowLabels << label
        contents << data.transpose.map { |a| a.mean }
      }
    }
  }
  averages = contents.transpose.map { |a| a.mean }
  rowLabels << nil
  contents << nil
  rowLabels << 'Average'
  contents << averages
  contents = contents.map { |a| a && a.map {|x| F(x)} }
  #contents.each { |x| puts x.inspect }
  tab = latexTable(DataTable.new(:contents => contents, :colLabels => colLabels, :rowLabels => rowLabels)).lines(false).justify('ccccc')
  IO.writelines("sizeRatio.tex", [tab.render])
end

# Show effect of barely-repeating
def barelyRepeatingEffect(logScale)
  nameGraphs = []
  "downcast/hedc downcast/avrora downcast/lusearch monosite/elevator race/elevator".split.map { |x| x.split(/\//) }.each { |client,benchmark|
    contents = []
    ['false','true'].each { |disallowRepeats|
      data = []
      $R.filter(:client => client, :benchmark => benchmark, :disallowRepeats => disallowRepeats, :method => 'pr:is,has').each { |r|
        v = r[:A]
        next unless v
        v = Integer(v)
        v = Math.log(v) if logScale
        data << [Integer(r[:iteration]), v]
      }
      contents += data.transpose if data.size > 0
    }
    numIters = contents.map { |a| a.size }.max
    #puts contents.map{|x|x.join(' ')}
    g = lineGraph(DataTable.new(:contents => contents, :rowLabels => [nil, '$\Klimabs$', nil, '$\Klimdabs$'], :colName => 'iteration $t$', :cellName => $absSizeLabel))
    g.useAdjRowPairs
    g.ysciNotation.xrange(0)
    g.yexpValue.ytickIncrValue(Math.log(10)) if logScale
    g.ylabelBold(false)
    g.xroundPlaces(0).xrange(1).xtickIncrValue([numIters/4,1].max)
    g.xlength(u(2.5)).ylength(u(2))
    g.colors(red, blue)
    g.legendPosition(-1, 1)
    g.yrotateAxisLabel(false)
    nameGraphs << [CB(client,benchmark), g]
  }
  arrangeGraphs(nameGraphs, 5)
end
printObj(:obj => barelyRepeatingEffect(false).signature(144), :outPrefix => 'barelyRepeatingEffect')
printObj(:obj => barelyRepeatingEffect(true).signature(138), :outPrefix => 'barelyRepeatingEffect-log')

# Show effect of class-based
def classEffect(logScale)
  nameGraphs = []
  $clients.each { |client|
    $benchmarks.each { |benchmark|
      contents = []
      ['pr:none','pr:is', 'pr:has', 'pr:is,has'].each { |method|
        data = []
        $R.filter(:client => client, :benchmark => benchmark, :disallowRepeats => 'false', :method => method).each { |r|
          v = r[:Ap]
          next unless v
          v = Integer(v)
          v = Math.log(v) if logScale
          data << [Integer(r[:iteration]), v]
        }
        contents += data.transpose if data.size > 0
      }
      #puts CB(client, benchmark)
      #puts contents.map{|x|x.join(' ')}
      rowLabels = $methodNames[false].values_at(2,3,4,5).map{|x|[nil,x]}.flatten
      g = lineGraph(DataTable.new(:contents => contents, :rowLabels => rowLabels, :colName => 'iteration $t$', :cellName => $absSizeLabel))
      g.useAdjRowPairs
      g.ysciNotation.xrange(0)
      g.yexpValue.ytickIncrValue(Math.log(10)) if logScale
      g.xroundPlaces(0).xrange(1).xtickIncrValue(1)
      nameGraphs << [CB(client,benchmark), g]
    }
  }
  arrangeGraphs(nameGraphs)
end
printObj(:obj => classEffect(false).signature(139), :outPrefix => 'classEffect')
printObj(:obj => classEffect(true).signature(139), :outPrefix => 'classEffect-log')

finishFigureSet
