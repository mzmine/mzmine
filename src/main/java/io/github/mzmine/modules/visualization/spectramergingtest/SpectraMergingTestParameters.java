package io.github.mzmine.modules.visualization.spectramergingtest;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;

public class SpectraMergingTestParameters extends SimpleParameterSet {

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level", "Noise level",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final ComboParameter<MergingType> mergingType = new ComboParameter<MergingType>(
      "Merging type", "merging type", MergingType.values(), MergingType.MAXIMUM);

  public SpectraMergingTestParameters() {
    super(new Parameter[]{noiseLevel, mzTolerance, mergingType});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    ParameterSetupDialogWithPreview dialog = new SpectraMergingPane(this);
    dialog.showAndWait();
    ExitCode code = dialog.getExitCode();

    return code;
  }
}
