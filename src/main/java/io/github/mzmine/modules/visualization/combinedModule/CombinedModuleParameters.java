package io.github.mzmine.modules.visualization.combinedModule;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class CombinedModuleParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();
  public static final RTRangeParameter retentionTimeRange = new RTRangeParameter();
  public static final MZRangeParameter mzRange =
      new MZRangeParameter("Precursor m/z", "Range of precursor m/z values");

  public static final String[] xAxisTypes = {"Retention time", "Precursor ion m/z"};
  public static final ComboParameter<String> xAxisType =
      new ComboParameter<String>("X axis", "X axis type", xAxisTypes);

  public static final String[] yAxisTypes = {"Product ion m/z", "Neutral loss"};
  public static final ComboParameter<String> yAxisType =
      new ComboParameter<String>("Y axis", "Y axis type", yAxisTypes);
  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Intensities less than this value are interpreted as noise.",
      MZmineCore.getConfiguration().getIntensityFormat());
  public static final ComboParameter<String> colorScale =
      new ComboParameter<String>("Color Scale", "Color Scale",
          new String[]{"Precursor ion intensity", "Product ion intensity"});

  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public CombinedModuleParameters() {
    super(new Parameter[]{dataFiles, xAxisType, yAxisType, retentionTimeRange, mzRange, colorScale,
        noiseLevel, windowSettings});
  }
}
