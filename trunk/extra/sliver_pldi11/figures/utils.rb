def arrangeGraphs(graphs, numPerRow=3)
  captionRow, objRow = [], []
  rows = []
  n = 0
  numPerRow = [numPerRow]*100 unless numPerRow.is_a?(Array)
  100.times { numPerRow << numPerRow[-1] } # HACK
  partialSums = [numPerRow[0]]
  (1...numPerRow.size).each { |i| partialSums[i] = partialSums[i-1] + numPerRow[i] }
  graphs = graphs.compact
  graphs.each_with_index { |captionObj,i|
    caption, obj = captionObj
    if caption
      captionRow << _("(#{(?a+n).chr}) #{caption}").scale(0.8)
      n += 1
    else
      captionRow << ''
    end
    objRow << obj
    if i+1 == partialSums[rows.size] || i == graphs.size-1 then
      rows << table(objRow, captionRow).center.cmargin(u(0.3))
      captionRow, objRow = [], []
    end
  }
  rtable(*rows).center.rmargin(u(0.3))
end
