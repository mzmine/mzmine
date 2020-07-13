package io.github.mzmine.modules.visualization.chromatogramandspectra;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ChromatogramAndSpectraVisualizerParameters extends SimpleParameterSet {

  public static final MZToleranceParameter chromMzTolerance = new MZToleranceParameter(
      "EIC tolerance",
      "m/z tolerance of the chromatogram builder for extracted ion chromatograms (EICs)", 0.001,
      10);

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      "Chromatogram scan selection",
      "Parameters for scan selection the chromatogram will be build on.",
      new ScanSelection(null, null, null, null, null, 1, null));

  public ChromatogramAndSpectraVisualizerParameters() {
    super(new Parameter[]{chromMzTolerance, scanSelection});
  }
}
