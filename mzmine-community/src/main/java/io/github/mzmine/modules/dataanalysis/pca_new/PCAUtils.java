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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.statistics.DataTableUtils;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunction;
import io.github.mzmine.modules.dataanalysis.utils.scaling.ScalingFunction;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.jetbrains.annotations.Nullable;

public class PCAUtils {

  private static final Logger logger = Logger.getLogger(PCAUtils.class.getName());

  /**
   * Calculates the PCA of a matrix by singular value decomposition (svd).
   * https://stats.stackexchange.com/questions/134282/relationship-between-svd-and-pca-how-to-use-svd-to-perform-pca
   *
   * @param scaledCenteredData the data. The imputed data  see {@link ImputationFunction} that is
   *                           already scaled and centered 0 and scaled according to the scaling
   *                           function.
   * @return A pca result.
   */
  public static PCAResult quickPCA(RealMatrix scaledCenteredData) {
    return quickPCA(scaledCenteredData, null);
  }

  /**
   * Calculates the PCA of a matrix by singular value decomposition (svd).
   * https://stats.stackexchange.com/questions/134282/relationship-between-svd-and-pca-how-to-use-svd-to-perform-pca
   *
   * @param data            the data. The imputed data  see {@link ImputationFunction} either
   *                        already scaled and centered or otherwise provide a scaling funtion.
   * @param scalingFunction if null then data needs to be scaled and centered before. If not null
   *                        then we apply center(scale(data))
   * @return A pca result.
   */
  public static PCAResult quickPCA(RealMatrix data, @Nullable ScalingFunction scalingFunction) {

    // scaling might have been performed already on the FeatureDataTable
    if (scalingFunction != null) {
      logger.finest(() -> "Performing scaling and centering");
      data = StatisticUtils.scaleAndCenter(data, scalingFunction, false);
    }

    logger.finest(() -> "Performing singular value decomposition. This may take a while");
    SingularValueDecomposition svd = new SingularValueDecomposition(data);
    // https://stats.stackexchange.com/questions/134282/relationship-between-svd-and-pca-how-to-use-svd-to-perform-pca

    return new PCAResult(svd);
  }

  /**
   * @param dataTable already sorted rows, filtered for sample type, and prepared by scaling and
   *                  centering
   * @return the results or null if conditions are not met
   */
  public static PCARowsResult performPCAOnDataTable(FeaturesDataTable dataTable) {
    final List<RawDataFile> files = dataTable.getRawDataFiles();
    if (files.isEmpty()) {
      return null;
    }

    // missing values are already imputed and scaling and centering are also already applied
    final RealMatrix data = DataTableUtils.createRealMatrix(dataTable);

    final PCAResult pcaResult = quickPCA(data);
    return new PCARowsResult(pcaResult, dataTable.getFeatureListRows(), files);
  }
}
