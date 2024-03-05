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

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.jetbrains.annotations.NotNull;

public record PCAResult(RealMatrix data, RealMatrix dataMeanCentered,
                        SingularValueDecomposition svd) {

  public RealMatrix getFirstNComponents(int numComponents) {
    return svd.getU().getSubMatrix(0, svd.getU().getRowDimension() - 1, 0,
        numComponents - 1);
  }

  public RealMatrix getPrincipalComponents() {
    return svd.getU();
  }

  public RealMatrix projectDataToScores(int numComponents) {
    final RealMatrix firstNComponents = getFirstNComponents(numComponents);
    final RealMatrix subMatrixS = svd.getS()
        .getSubMatrix(0, numComponents - 1, 0, numComponents - 1);
    final RealMatrix projectedData = firstNComponents.multiply(subMatrixS);
    return projectedData;
  }

  public RealMatrix projectDataToScores(int domainColIndex, int rangeColIndex) {
    final RealMatrix pcMatrix = getPCMatrix(domainColIndex, rangeColIndex);
    final RealMatrix projected = pcMatrix.multiply(svd.getS().getSubMatrix(0, 1, 0, 1));
    return projected;
  }

  @NotNull
  private RealMatrix getPCMatrix(int domainColIndex, int rangeColIndex) {
    final RealMatrix pcs = svd.getU();
    final RealVector domainVector = pcs.getColumnVector(domainColIndex);
    final RealVector rangeVector = pcs.getColumnVector(rangeColIndex);
    RealMatrix pcMatrix = new Array2DRowRealMatrix(pcs.getRowDimension(), 2);
    pcMatrix.setColumnVector(0, domainVector);
    pcMatrix.setColumnVector(1, rangeVector);

    return pcMatrix;
  }

  public RealMatrix getLoadingsMatrix() {
    final RealMatrix transpose = svd.getV().transpose();
    return transpose;
  }
}
