/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.scores.CvType;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunction;
import io.github.mzmine.modules.dataanalysis.utils.imputation.OneFifthOfMinimumImputer;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelection;
import io.github.mzmine.util.MathUtils;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

record CvFilter(double maxCvPercent, AbundanceMeasure abundanceMeasure, List<RawDataFile> cvFiles,
                boolean keepUndetected) {

  public CvFilter(MetadataGroupSelection metadataGrouping, double maxCv,
      AbundanceMeasure abundanceMeasure, FeatureList flist, boolean keepUndetected) {
    this(maxCv, abundanceMeasure, metadataGrouping.getMatchingFiles().stream()
        .filter(file -> flist.getRawDataFiles().contains(file)).toList(), keepUndetected);
  }

  public static CvFilter of(CVFilterParameters parameters, FeatureList flist) {
    return new CvFilter(parameters.getValue(CVFilterParameters.grouping),
        parameters.getValue(CVFilterParameters.maxCv),
        parameters.getValue(CVFilterParameters.abundanceMeasure), flist,
        parameters.getValue(CVFilterParameters.keepUndetected));
  }

  /**
   * @return True if the row passes the filter and is thereby below the set {@link #maxCvPercent}.
   */
  public boolean matches(final FeatureListRow row) {
    final RealVector abundances = new ArrayRealVector(cvFiles.size());
    final ImputationFunction imputer = new OneFifthOfMinimumImputer();

    for (int i = 0; i < cvFiles.size(); i++) {
      final RawDataFile qcFile = cvFiles.get(i);
      abundances.setEntry(i, abundanceMeasure.getOrNaN((ModularFeature) row.getFeature(qcFile)));
    }

    final boolean allNaN = IntStream.range(0, abundances.getDimension())
        .allMatch(i -> Double.isNaN(abundances.getEntry(i)));
    if (allNaN && !keepUndetected) {
      // feature not detected in QCs, will not filter it out
      return false;
    }

    final double imputation = imputer.apply(abundances);
    abundances.mapToSelf(v -> Double.isNaN(v) ? imputation : v);

    final double[] abundancesArray = abundances.toArray();
    final double avg = MathUtils.calcAvg(abundancesArray);
    final double sd = MathUtils.calcStd(abundancesArray);
    final double rsd = sd / avg;

    row.set(CvType.class, (float) rsd);

    return rsd <= maxCvPercent;
  }
}
