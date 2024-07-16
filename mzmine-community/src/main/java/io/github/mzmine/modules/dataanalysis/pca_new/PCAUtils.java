/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunction;
import io.github.mzmine.modules.dataanalysis.utils.scaling.ScalingFunction;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class PCAUtils {

  private static final Logger logger = Logger.getLogger(PCAUtils.class.getName());

  /**
   * Calculates the PCA of a matrix by singular value decomposition (svd).
   * https://stats.stackexchange.com/questions/134282/relationship-between-svd-and-pca-how-to-use-svd-to-perform-pca
   *
   * @param data the data. The imputed data  see {@link ImputationFunction}. Will be centered around
   *             0 and scaled according to the scaling function.
   * @return A pca result.
   */
  public static PCAResult quickPCA(RealMatrix data, ScalingFunction scalingFunction) {

    logger.finest(() -> "Performing scaling and centering");
    final RealMatrix centeredMatrix = StatisticUtils.centerAndScale(data, scalingFunction, false);

    logger.finest(() -> "Performing singular value decomposition. This may take a while");
    SingularValueDecomposition svd = new SingularValueDecomposition(centeredMatrix);
    // https://stats.stackexchange.com/questions/134282/relationship-between-svd-and-pca-how-to-use-svd-to-perform-pca

    return new PCAResult(svd);
  }

  /**
   * @param originalData the imputed data. see {@link ImputationFunction}
   */
  public static PCAResult performPCA(RealMatrix originalData, RealMatrix pretreatedData) {
    logger.finest(() -> "Performing singular value decomposition. This may take a while");
    SingularValueDecomposition svd = new SingularValueDecomposition(pretreatedData);
    // https://stats.stackexchange.com/questions/134282/relationship-between-svd-and-pca-how-to-use-svd-to-perform-pca

    return new PCAResult(svd);
  }

  /**
   * Performs a PCA on a list of feature list rows. Imputes missing values as 0s.
   *
   * @param rows               The rows.
   * @param measure            The abundance to use.
   * @return A pca result that can be mapped to the used rows.
   */
  public static PCARowsResult performPCAOnRows(List<FeatureListRow> rows, AbundanceMeasure measure,
      ScalingFunction scalingFunction, ImputationFunction imputationFunction) {
    return performPCAOnRows(rows, measure, scalingFunction, imputationFunction,
        SampleTypeFilter.all());
  }

  /**
   * Performs a PCA on a list of feature list rows. Imputes missing values as 0s.
   *
   * @param rows    The rows.
   * @param measure The abundance to use.
   * @return A pca result that can be mapped to the used rows.
   */
  public static PCARowsResult performPCAOnRows(List<FeatureListRow> rows, AbundanceMeasure measure,
      ScalingFunction scalingFunction, ImputationFunction imputationFunction,
      SampleTypeFilter sampleTypeFilter) {
    final List<RawDataFile> files = rows.stream().flatMap(row -> row.getRawDataFiles().stream())
        .distinct().filter(sampleTypeFilter::matches).toList();
    final RealMatrix data = StatisticUtils.createDatasetFromRows(rows, files, measure);
    StatisticUtils.imputeMissingValues(data, true, imputationFunction);
    final PCAResult pcaResult = quickPCA(data, scalingFunction);
    return new PCARowsResult(pcaResult, rows, files);
  }
}
