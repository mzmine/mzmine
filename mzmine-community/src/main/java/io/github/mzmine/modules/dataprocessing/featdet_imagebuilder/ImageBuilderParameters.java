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

package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * These parameters must have the same name as the ones in
 * {@link
 * io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters}
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageBuilderParameters extends SimpleParameterSet {

  public static final IntegerParameter minTotalSignals = new IntegerParameter(
      "Minimum total signals", "Minimum number of signals (data points) to form an image", 50, true,
      1, null);

  public static final IntegerParameter minimumConsecutiveScans = new IntegerParameter(
      "Minimum consecutive scans",
      "Minimum number of consecutive signals (data points) to form an image", 5, true, 1, null);

  public static final DoubleParameter minHighest = new DoubleParameter("Minimum absolute height",
      "Minimum intensity of an m/z to be considered as an image.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);


  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "images");

  public ImageBuilderParameters() {
    super(ADAPChromatogramBuilderParameters.dataFiles,
        ADAPChromatogramBuilderParameters.scanSelection,
        ADAPChromatogramBuilderParameters.mzTolerance,
        ADAPChromatogramBuilderParameters.minHighestPoint,
        minTotalSignals,
        minimumConsecutiveScans,
        suffix);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("Min group size in # of scans", minimumConsecutiveScans);
    nameParameterMap.put("Min highest intensity", minHighest);
    nameParameterMap.put("Scan to scan accuracy (m/z)",
        ADAPChromatogramBuilderParameters.mzTolerance);
    return nameParameterMap;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }
}
