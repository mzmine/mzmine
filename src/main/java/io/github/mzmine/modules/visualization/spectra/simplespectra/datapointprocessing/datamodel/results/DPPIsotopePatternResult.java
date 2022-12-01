/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import org.jetbrains.annotations.Nullable;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;

/**
 * Used to store a detected isotope pattern in a
 * {@link io.github.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint}.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPIsotopePatternResult extends DPPResult<IsotopePattern> {

  private ProcessedDataPoint[] linkedDataPoints;
  private final int charge;

  public DPPIsotopePatternResult(IsotopePattern value, ProcessedDataPoint[] linkedDataPoints,
      int charge) {
    super(value);

    if (value.getNumberOfDataPoints() == linkedDataPoints.length) {
      this.linkedDataPoints = linkedDataPoints;
    }
    this.charge = charge;
  }

  public DPPIsotopePatternResult(IsotopePattern value, int charge) {
    super(value);

    this.charge = charge;
  }

  public @Nullable ProcessedDataPoint[] getLinkedDataPoints() {
    return linkedDataPoints;
  }

  public void setLinkedDataPoints(@Nullable ProcessedDataPoint[] linkedDataPoints) {
    this.linkedDataPoints = linkedDataPoints;
  }

  public ProcessedDataPoint getLinkedDataPoint(int i) {
    if (linkedDataPoints != null)
      if (i < linkedDataPoints.length)
        return linkedDataPoints[i];
    return null;
  }

  public int getCharge() {
    return charge;
  }

  @Override
  public String toString() {
    return "Isotope pattern (" + getValue().getNumberOfDataPoints() + ")";
  }

  @Override
  public ResultType getResultType() {
    return ResultType.ISOTOPEPATTERN;
  }
}
