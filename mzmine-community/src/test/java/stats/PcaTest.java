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

package stats;


import io.github.mzmine.modules.dataanalysis.pca_new.PCACalculator;
import java.util.logging.Logger;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Test;

public class PcaTest {

  private static final Logger logger = Logger.getLogger(PcaTest.class.getName());

  @Test
  void pcaTest() {
    double[][] data = new double[][]{{5.1, 3.5, 1.4, 0.2, 1}, {4.9, 3.0, 1.4, 0.2, 1},
        {4.7, 3.2, 1.3, 0.2, 1}, {4.6, 3.1, 1.5, 0.2, 1}, {5.0, 3.6, 1.4, 0.2, 1}};

    RealMatrix matrix = new Array2DRowRealMatrix(data);

    final RealMatrix centered = PCACalculator.performMeanCenter(matrix, false);
    logger.info(centered.toString());

    PCACalculator calc = new PCACalculator(matrix);

    calc.performPCA(2);

    final RealMatrix principalComponentMatrix = calc.getPrincipalComponentMatrix();
    logger.info(() -> STR."Scores: \{principalComponentMatrix.toString()}");
    logger.info(() -> STR."Loadings: \{calc.getLoadings().toString()}");

  }

}
