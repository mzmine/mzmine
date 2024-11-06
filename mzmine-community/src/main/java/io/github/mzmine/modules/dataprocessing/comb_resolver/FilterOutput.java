package io.github.mzmine.modules.dataprocessing.comb_resolver;

import java.util.List;

import com.google.common.collect.Range;

public record FilterOutput(List<Range<Double>> onlyFirstResolverRanges,List<Range<Double>> onlySecondResolverRanges, List<Range<Double>> bothResolverRanges){}
