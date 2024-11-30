package io.github.mzmine.modules.dataprocessing.comb_resolver;

import com.google.common.collect.Range;
import java.util.List;

public record FilterOutput(List<Range<Double>> onlyFirstResolverRanges,
                           List<Range<Double>> onlySecondResolverRanges,
                           List<Range<Double>> bothResolverRanges) {

}
