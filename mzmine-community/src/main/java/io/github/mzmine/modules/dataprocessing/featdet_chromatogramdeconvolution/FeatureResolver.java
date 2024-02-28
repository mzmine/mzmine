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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import io.github.mzmine.util.maths.CenterFunction;

public interface FeatureResolver extends MZmineModule {

  /**
   * Gets if resolver requires R, if applicable
   */
  public boolean getRequiresR();

  /**
   * Gets R required packages for the resolver's method, if applicable
   */
  public String[] getRequiredRPackages();

  /**
   * Gets R required packages versions for the resolver's method, if applicable
   */
  public String[] getRequiredRPackagesVersions();

  /**
   * Gets R engine type, if applicable
   */
  public REngineType getREngineType(final ParameterSet parameters);

  /**
   * Resolve a peaks found within given chromatogram. For easy use, three arrays (scanNumbers,
   * retentionTimes and intensities) are provided, although the contents of these arrays can also be
   * obtained from the chromatogram itself. The size of these arrays must be same, and must be equal
   * to the number of scans covered by given chromatogram.
   * 
   * @param mzCenterFunction
   * 
   * @param rTRangeMSMS
   * @param msmsRange
   * @param rTRangeMSMS
   * @param msmsRange
   * @param rTRangeMSMS
   * @param msmsRange
   * 
   * @throws RSessionWrapperException
   */
  public ResolvedPeak[] resolvePeaks(Feature chromatogram, ParameterSet parameters,
      RSessionWrapper rSession, CenterFunction mzCenterFunction, double msmsRange,
      float rTRangeMSMS) throws RSessionWrapperException;

  public Class<? extends MZmineProcessingModule> getModuleClass();
}
