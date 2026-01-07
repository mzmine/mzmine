/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

/**
 * A pca based on singular value decomposition. The data matrix X is decomposed into X = U*S*V.
 * Columns of U contrain the principal components, S are the singular values, which can be projected
 * into the PC space using U and a submatrix of S, which creates the scores plot. Loadings are the
 * transpose of V.
 * <p>
 * https://stats.stackexchange.com/questions/134282/relationship-between-svd-and-pca-how-to-use-svd-to-perform-pca
 */
public record PCAResult(SingularValueDecomposition svd) {

  /**
   * @param numComponents
   * @return Returns a sub-matrix the first n principal components of the decomposition.
   */
  public RealMatrix firstNComponents(int numComponents) {
    return svd.getU().getSubMatrix(0, svd.getU().getRowDimension() - 1, 0, numComponents - 1);
  }

  public RealMatrix principalComponentsMatrix() {
    // the u matrix of an svd contains the principal components.
    return svd.getU();
  }

  /**
   * Projects the data matrix onto the principal components. The result is n dimensional and is
   * called "scores" matrix and is used for the scores plot.
   *
   * @param numComponents the number of components n.
   */
  public RealMatrix projectDataToScores(int numComponents) {
    final RealMatrix firstNComponents = firstNComponents(numComponents);
    final RealMatrix subMatrixS = svd.getS()
        .getSubMatrix(0, numComponents - 1, 0, numComponents - 1);
    final RealMatrix projectedData = firstNComponents.multiply(subMatrixS);
    return projectedData;
  }

  /**
   * Projects the data matrix onto the selected principal components. The result is 2 dimensional
   * and is called "scores" matrix and is used for the scores plot.
   *
   * @see #projectDataToScores(int)
   */
  public RealMatrix projectDataToScores(int domainColIndex, int rangeColIndex) {
    final RealMatrix pcMatrix = pcMatrix(domainColIndex, rangeColIndex);
    final RealMatrix projected = pcMatrix.multiply(svd.getS().getSubMatrix(0, 1, 0, 1));
    return projected;
  }

  /**
   * Retrieves two specific PCs from the PC matrix.
   */
  @NotNull
  private RealMatrix pcMatrix(int domainColIndex, int rangeColIndex) {
    final RealMatrix pcs = svd.getU();
    // the vectors are the respective components.
    final RealVector domainVector = pcs.getColumnVector(domainColIndex);
    final RealVector rangeVector = pcs.getColumnVector(rangeColIndex);
    RealMatrix pcMatrix = new Array2DRowRealMatrix(pcs.getRowDimension(), 2);
    pcMatrix.setColumnVector(0, domainVector);
    pcMatrix.setColumnVector(1, rangeVector);

    return pcMatrix;
  }

  /**
   * Retrieves the loadings (importance of each observed feature) from the pca. After svd the
   * loadings are the transpose of the v matrix.
   */
  public RealMatrix getLoadingsMatrix() {
    final RealMatrix transpose = svd.getV().transpose();
    return transpose;
  }

  /**
   * The contributions of the principle components
   *
   * @param components number of components
   * @return length of returned array is limited by the components argument and the actual number of
   * components available. PC1 will be first element [0].
   */
  public float[] getComponentContributions(int components) {
    double[] singularValues = svd.getSingularValues();
    // Calculate total variance - singularValues are related to standard deviation
    double totalVariance = 0;
    for (double value : singularValues) {
      totalVariance += value * value;
    }

    components = Math.min(components, singularValues.length);
    // Calculate variance explained by PC1 and PC2
    float[] contributions = new float[components];
    for (int i = 0; i < components; i++) {
      contributions[i] = (float) ((singularValues[i] * singularValues[i]) / totalVariance);
    }
    return contributions;
  }

  public int componentCount() {
    return principalComponentsMatrix().getRowDimension();
  }
}
