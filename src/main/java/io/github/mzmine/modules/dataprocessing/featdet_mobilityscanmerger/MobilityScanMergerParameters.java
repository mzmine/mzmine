/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import org.jetbrains.annotations.NotNull;

public class MobilityScanMergerParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Data points below this threshold will be ignored.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E1, 0d, 1E12);

  public static final ComboParameter<MergingType> mergingType = new ComboParameter<>("Merging type",
      "merging type", MergingType.values(), MergingType.SUMMED);

  public static final ComboParameter<Weighting> weightingType = new ComboParameter<>(
      "m/z weighting", "Weights m/z values by their intensities with the given function.",
      Weighting.values(), Weighting.LINEAR);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "", 0.0001, 2, false);

  public MobilityScanMergerParameters() {
    super(new Parameter[]{rawDataFiles, noiseLevel, mergingType, weightingType, scanSelection,
            mzTolerance},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_mobility_scan_merging/mobility-scan-merging.html");
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
