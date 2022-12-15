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

package io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.maths.Weighting;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import org.jetbrains.annotations.NotNull;

public class MobilityScanMergerParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final DoubleParameter noiseLevel = new DoubleParameter("Frame noise level",
      "Noise level for the merged frame. Merged signals below this threshold will be ignored.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E1, 0d, 1E12);

  public static final ComboParameter<IntensityMergingType> mergingType = new ComboParameter<>(
      "Merging type", "Spectra merging algorithm", IntensityMergingType.values(),
      IntensityMergingType.SUMMED);

  public static final ComboParameter<Weighting> weightingType = new ComboParameter<>(
      "m/z weighting", "Weights m/z values by their intensities with the given function.",
      Weighting.values(), Weighting.LINEAR);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "", 0.003, 15, false);

  public MobilityScanMergerParameters() {
    super(new Parameter[]{rawDataFiles, noiseLevel, mergingType, weightingType, scanSelection,
            mzTolerance},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_file_merging/mobility-scan-merging.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    MobilityScanMergerSetupDialog dialog = new MobilityScanMergerSetupDialog(valueCheckRequired,
        this);
    dialog.showAndWait();
    ExitCode code = dialog.getExitCode();
    return code;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
