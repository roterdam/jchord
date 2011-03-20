#!/usr/bin/ruby

require 'rfig/FigureSet'

initFigureSet(:outPrefix => '.', :defaultFont => 'times',
              :latexHeader => IO.readlines('../std-macros.tex') +
                              IO.readlines('../macros.tex'),
              :outputStrings => false,
              :lazy => true)

def vec(x); x.to_s.size == 1 ? "\\vec{#{x}}" : "\\vek{#{x}}" end
def V(x); "\\{#{vec(x)}\\}" end
def Vs(x); "#{vec(x)}*" end

############################################################
def graphExample
  def make(g, edgeIndices)
    nodes = (0...4).map { |i| dnode(i).size(u(0.4)) }
    edges = edgeIndices.map { |i,j| dedge(nodes[i], nodes[j]).arrow.arrowSize(7) }
    struct = overlay(
      table(
        [nodes[0], nodes[1]],
        [nodes[2], nodes[3]],
      nil).margin(u(0.4), u(0.4)),
    *edges)
    tuples = edgeIndices.map { |i,j| "$\\edge(#{g},#{i},#{j})$" }
    [struct, tuples]
  end
  struct1, tuples1 = make('\Ga', [[0,1], [0,2], [1,3]])
  struct2, tuples2 = make('\Gb', [[0,3]])
  
  ctable(
    table(
      [struct1, struct2],
      ['$\Ga$', '$\Gb$'],
    nil).center.border(1).cmargin(u(0.4)).ospace.scale(0.8),
    scale(0.7),
    rtable(
      '{\bf Input tuples}:',
      ind(rtable(*(tuples1+tuples2+['$\head(\vec{0}, 0)$', '$\ext(1, \vec{0}, \vek{10})$', '$\dots$']))),
    nil),
    rtable(
      '{\bf Derived tuples}:',
      ind(rtable(
        '$\path(\Ga,\vec{0})$',
        '$\path(\Ga,\vek{10})$',
        '$\path(\Ga,\vek{20})$',
        '$\path(\Ga,\vek{310})$',
        '$\path(\Gb,\vek{0})$',
        '$\path(\Gb,\vek{30})$',
      nil)),
    nil),
  nil).cmargin(u(0.2))
end
printObj(:obj => graphExample.signature(96), :outPrefix => 'graphExample')

def N(x); ctable(x).ospace(u(0.04)) end
def a(i); N("$A_#{i}$") end
def b(i); N("$B_#{i}$") end
def ta(i); N("$\\tilde A_#{i}$") end
def tb(i); N("$\\tilde B_#{i}$") end
def pa(i); N("$A_#{i}'$") end
def alpha(i); "$\\alpha_#{i}$" end
def beta(i); "$\\beta_{#{i}}$" end

def edge1(x,y); N("$\\edge(\\Ga,#{x},#{y})$") end
def Edge1(x,y); N("$\\greenfour{\\edge(\\Ga,#{x},#{y})}$") end
def pedge1(x,y); N("$\\red{\\edge(\\Ga,#{x},#{y})}$") end
def path1(c); N("$\\path(\\Ga,#{c})$") end
def edge2(x,y); N("$\\edge(\\Gb,#{x},#{y})$") end
def Edge2(x,y); N("$\\greenfour{\\edge(\\Gb,#{x},#{y})}$") end
def path2(c); N("$\\path(\\Gb,#{c})$") end
def common; N("$\\common(\\Ga,\\Gb,3)$") end
def ext(j,c,cc); N("$\\ext(#{j},#{c},#{cc})$") end
def Ext(j,c,cc); N("$\\greenfour{\\ext(#{j},#{c},#{cc})}$") end
def pext(j,c,cc); N("$\\red{\\ext(#{j},#{c},#{cc})}$") end

############################################################
def graphDerivation
  tab = table(
    [n2=pedge1(0,2), n1=pext(2,Vs(0),Vs(2)), n3=path1(Vs(0)), n4=Ext(1,Vs(0),Vs(1)), n5=Edge1(0,1)        , ''           , ''          , ''          , ''                   , ''],
    [''            , n6=path1(Vs(2))       , ''             , n7=path1(Vs(1))      , n8=Ext(3,Vs(1),Vs(3)), n9=Edge1(1,3), ''          , n10=path2(0), n11=Ext(3,Vs(0),Vs(3)), n12=Edge2(0,3)],
    [''            , ''                    , ''             , ''                   , n13=path1(Vs(3))     , ''           , ''          , ''          , n14=path2(Vs(3))     , ''],
    [''            , ''                    , ''             , ''                   , ''                   , ''           , n15=common().border(1), ''          , ''                   , ''],
  nil).center.margin(u(0.5), u(0.2))
  edges = [
    [n1,n6],[n2,n6],[n3,n6],
    [n3,n7],[n4,n7],[n5,n7],
    [n7,n13],[n8,n13],[n9,n13],
    [n10,n14],[n11,n14],[n12,n14],
    [n13,n15],[n14,n15],
  nil].compact.map { |na,nb| clippedpath(na,nb).arrow.arrowSize(6).thickness(2) }
  overlay(tab, *edges)
end
printObj(:obj => graphDerivation.signature(109), :outPrefix => 'graphDerivation')

############################################################
def graphIters
  contents = [
    ['$A_0$',
      #'$\edge(\Ga,0,1)$',
      #'$\edge(\Ga,0,2)$',
      #'$\edge(\Ga,1,3)$',
      #'$\edge(\Gb,0,3)$',
      '$\ext(1,\vec{0}*,\vec{1}*)$',
      '$\ext(2,\vec{0}*,\vec{2}*)$',
      '$\ext(3,\vec{0}*,\vec{3}*)$',
      '$\ext(3,\vec{1}*,\vec{3}*)$'],
    ['$\tilde A_0$',
      '$\ext(1,\vec{0}*,\vec{1}*)$',
      '$\ext(3,\vec{0}*,\vec{3}*)$',
      '$\ext(3,\vec{1}*,\vec{3}*)$',
      ''],
    ['$A_1$',
      '$\ext(1,\{\vec{0}\},\vec{10}*)$',
      '$\ext(3,\{\vec{0}\},\vec{30}*)$',
      '$\ext(3,\{\vec{1}\},\vec{31}*)$',
      '$\ext(3,\vec{10}*,\vec{31}*)$'],
    ['$\tilde A_1$', '\it (none)', '', '', '']
  ]
  table(*contents.transpose).cmargin(u(0.5)).center
  #table(
  #  ['$\bC(A_0)$',
  #   '$\bC(\tilde A_0)$',
  #   '$\bC(A_1)$',
  #   '$\bC(\tilde A_1)$'],
  #  [Vs(0),    Vs(0), V(0),   '\it (none)'],
  #  [Vs(1),    Vs(1), Vs('10'), ''],
  #  [Vs(2),    Vs(3), Vs('30'), ''],
  #  [Vs(3),    ''   , Vs('31'), ''],
  #nil).cmargin(u(0.5)).center
end
printObj(:obj => graphIters.signature(94), :outPrefix => 'graphIters')

############################################################
def algorithm(complex)
  def R; '$\bP$' end
  def E(a,b,label,description=nil)
    e = clippedpath(a, b).arrow.arrowSize(6)
    stuff = [e]
    stuff << overlay(ctable(label).opaque).scale(0.7).shift(tcenter(e)).center if label
    stuff << overlay(ctable(description).opaque).pivot(0,+1).shift(tcenter(e).add(upair(0, -0.1))) if description
    overlay(*stuff)
  end
  def mod(p1,p2,label)
    b = rect(u(1.7), u(1.2)).dashed('evenly')
    t = rtable(b, label).center.rNumGhosts(0,1).rmargin(u(0.1))
    overlay(t).pivot(0,-1).shift(tmidpoint(tdown(p1).sub(upair(0,0.1)),tdown(p2).sub(upair(0,0.1))))
  end

  def desc(x); _("(#{x})").scale(0.5) end

  init = desc('init')
  refine = desc('refine')
  prune = desc('prune')
  preprune = desc('pre-prune')

  alg1 = overlay(
    table(
      [x = N('$X$'), ''     , ta0=ta(0), ''     , ta1=ta(1), ''     , ''],
      [''          , a0=a(0), ''       , a1=a(1), ''       , a2=a(2), '$\cdots$'],
    nil).center.margin(u(0.8), u(0.5)),
    E(x,a0,alpha(0), init),
    E(a0,ta0,R(), prune),
    E(ta0,a1,alpha(1), refine),
    E(a1,ta1,R(), prune),
    E(ta1,a2,alpha(2), refine),
  nil)
  alg2 = overlay(
    table(
      #[x = N('$X$'), ''     , '', ''       , ta0=ta(0), ''     , '', ''       , ta1=ta(1), ''     , ''],
      #[''          , a0=a(0), '', pa0=pa(0), ''       , a1=a(1), '', pa1=pa(1), ''       , a2=a(2), ''],
      #[''          , b0=b(0), '', tb0=tb(0), ''       , b1=b(1), '', tb1=tb(1), ''       , ''     , ''],
      [''     , '', ''       , ta0=ta(0), ''     , '', ''       , ta1=ta(1), ''     , ''],
      [a0=a(0), '', pa0=pa(0), ''       , a1=a(1), '', pa1=pa(1), ''       , a2=a(2), ''],
      [b0=b(0), '', tb0=tb(0), ''       , b1=b(1), '', tb1=tb(1), ''       , ''     , ''],
    nil).center.margin(u(0.8), u(0.3)),
    #E(x,a0,alpha(0)),
    E(a0,b0,beta(0)), # BEGIN
    E(b0,tb0,R()),
    E(tb0,pa0,alpha(0)),
    #E(a0,pa0,nil),
    E(a0,pa0,'$\cap$'),
    E(pa0,ta0,R(),prune),
    E(ta0,a1,alpha(1),refine),
    E(a1,b1,beta(1)), # BEGIN
    E(b1,tb1,R()),
    E(tb1,pa1,alpha(1)),
    E(a1,pa1,'$\cap$'),
    E(pa1,ta1,R(),prune),
    E(ta1,a2,alpha(2),refine),
    mod(b0,tb0,preprune),
    mod(b1,tb1,preprune),
    overlay('$\cdots$').pivot(-1,0).shift(tright(a2)),
  nil)

  complex ? alg2 : alg1
  #rtable(alg1, alg2).rmargin(u(0.5))
end
printObj(:obj => algorithm(true).signature(117), :outPrefix => 'algorithm')

finishFigureSet
