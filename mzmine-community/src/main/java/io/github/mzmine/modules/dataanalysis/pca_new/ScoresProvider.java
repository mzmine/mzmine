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

import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import javafx.beans.property.Property;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class ScoresProvider extends SimpleXYProvider {

  private final PCARowsResult result;
  private final int pcX;
  private final int pcY;

  /**
   * @param pcX index of the principal component used for domain axis, subtract 1 from the number
   *            since the pc matrix starts at 0.
   * @param pcY index of the principal component used for range axis, subtract 1 from the number
   *            since the pc matrix starts at 0.
   */
  public ScoresProvider(PCARowsResult result, String seriesKey, Color awt, int pcX, int pcY) {
    super(seriesKey, awt);
    this.result = result;
    this.pcX = pcX;
    this.pcY = pcY;
  }

  public ScoresProvider(PCARowsResult result, String seriesKey, Color awt) {
    this(result, seriesKey, awt, 0, 1);
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    final PCAResult pcaResult = result.pcaResult();

    final RealVector pcOne = pcaResult.principalComponents().getColumnVector(pcX);
    final RealVector pcTwo = pcaResult.principalComponents().getColumnVector(pcY);

    final Array2DRowRealMatrix pcMatrix = new Array2DRowRealMatrix(2, pcOne.getDimension());
    final RealMatrix scores = pcaResult.data().multiply(pcMatrix);

    double[] domainData = new double[scores.getColumnDimension()];
    double[] rangeData = new double[scores.getColumnDimension()];
    for (int i = 0; i < scores.getColumnDimension(); i++) {
      domainData[i] = scores.getEntry(i, 0);
      rangeData[i] = scores.getEntry(i, 1);
    }

    setxValues(domainData);
    setyValues(rangeData);
  }
}
