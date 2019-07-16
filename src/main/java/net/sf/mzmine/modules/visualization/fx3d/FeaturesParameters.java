package net.sf.mzmine.modules.visualization.fx3d;

import java.util.HashMap;
import java.util.Map;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class FeaturesParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter DATA_FILES = new RawDataFilesParameter();

    public static final MultiChoiceParameter<Feature> PEAKS = new MultiChoiceParameter<Feature>(
            "Features", "Please choose peaks to visualize", new Feature[0],
            null, 0);

    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    public static final DoubleRangeParameter MZ_RANGE = new MZRangeParameter();

    public static final IntegerParameter rtResolution = new IntegerParameter(
            "Retention time resolution",
            "Number of data points on retention time axis", 500);

    public static final IntegerParameter mzResolution = new IntegerParameter(
            "m/z resolution", "Number of data points on m/z axis", 500);

    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    private Map<Feature, String> peakLabelMap;

    public FeaturesParameters() {
        super(new Parameter[] { DATA_FILES, PEAKS, scanSelection, MZ_RANGE,
                rtResolution, mzResolution, windowSettings });
        peakLabelMap = null;
    }

    public Map<Feature, String> getPeakLabelMap() {

        return peakLabelMap == null ? null
                : new HashMap<Feature, String>(peakLabelMap);
    }

    public void setPeakLabelMap(final Map<Feature, String> map) {

        peakLabelMap = map == null ? null : new HashMap<Feature, String>(map);
    }

}
