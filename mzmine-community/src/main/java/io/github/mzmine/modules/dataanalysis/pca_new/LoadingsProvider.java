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
import org.apache.commons.math3.linear.RealMatrix;

public class LoadingsProvider extends SimpleXYProvider {

  private final PCARowsResult result;
  private final int loadingsY;
  private final int loadingsX;

  /**
   * @param loadingsX index of the principal component used for domain axis, subtract 1 from the
   *                  number since the pc matrix starts at 0.
   * @param loadingsY index of the principal component used for range axis, subtract 1 from the
   *                  number since the pc matrix starts at 0.
   */
  public LoadingsProvider(PCARowsResult result, String seriesKey, Color awt, int loadingsX,
      int loadingsY) {
    super(seriesKey, awt);
    this.result = result;
    this.loadingsX = loadingsX;
    this.loadingsY = loadingsY;
  }

  public LoadingsProvider(PCARowsResult result, String seriesKey, Color awt) {
    this(result, seriesKey, awt, 0, 1);
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    final PCAResult pcaResult = result.pcaResult();

    final RealMatrix loadingsMatrix = pcaResult.getLoadingsMatrix();

    double[] domainData = new double[loadingsMatrix.getColumnDimension()];
    double[] rangeData = new double[loadingsMatrix.getColumnDimension()];
    for (int i = 0; i < loadingsMatrix.getColumnDimension(); i++) {
      domainData[i] = loadingsMatrix.getEntry(loadingsX, i);
      rangeData[i] = loadingsMatrix.getEntry(loadingsY, i);
    }

    setxValues(domainData);
    setyValues(rangeData);
  }
}
