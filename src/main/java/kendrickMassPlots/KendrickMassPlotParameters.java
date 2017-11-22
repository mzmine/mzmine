package kendrickMassPlots;

import java.awt.Window;
import java.util.Arrays;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.intensityplot.YAxisValueSource;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakSelectionParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class KendrickMassPlotParameters extends SimpleParameterSet {
	public static final String rawDataFilesOption = "Raw data file";

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);
    
    public static final RawDataFilesParameter rawFile = new RawDataFilesParameter(); 
    
   // public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
   //         "Raw data files", "Raw data files to display", new RawDataFile[0]);
    
    public static final PeakSelectionParameter selectedRows = new PeakSelectionParameter();
    
    public static final ComboParameter<String> yAxisValues = new ComboParameter<>("Y-Axis", "Select the kendrick mass defect base", new String[] {"KMD (CH2)", "KMD (H)"}); 
    
    public static final ComboParameter<String> xAxisValues = new ComboParameter<>("X-Axis", "Select a second kendrick mass defect base, kendrick masse (KM) or m/z", new String[] {"m/z", "KM", "KMD (CH2)", "KMD (H)"}); 
    
    public static final ComboParameter<String> zAxisValues = new ComboParameter<>("Z-Axis", "Select a parameter for a third dimension, displayed as a heatmap or select none", new String[] {"none", "rt", "intensity","area"});
    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public KendrickMassPlotParameters() {
        super(new Parameter[] { peakList, rawFile, selectedRows, yAxisValues, xAxisValues, windowSettings });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        PeakList selectedPeakLists[] = getParameter(peakList).getValue()
                .getMatchingPeakLists();
        if (selectedPeakLists.length > 0) {
            PeakListRow plRows[] = selectedPeakLists[0].getRows();
            Arrays.sort(plRows, new PeakListRowSorter(SortingProperty.MZ,
                    SortingDirection.Ascending));
        }

        return super.showSetupDialog(parent, valueCheckRequired);
    }

}
