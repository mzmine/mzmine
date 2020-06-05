package io.github.mzmine.modules.visualization.ims;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class ImsVisualizerParameters extends SimpleParameterSet {
    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(1, 1);
    public static final ScanSelectionParameter scanSelection =
            new ScanSelectionParameter(new ScanSelection(1));
    public static final MZRangeParameter mzRange = new MZRangeParameter();

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();
    public ImsVisualizerParameters() {
        super(new Parameter[] {dataFiles, scanSelection, mzRange, windowSettings});
    }

}
