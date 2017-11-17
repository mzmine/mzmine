package kendrickMassPlots;

import java.awt.Window;
import java.util.Arrays;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.visualization.intensityplot.YAxisValueSource;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakSelectionParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class KendrickMassPlotParameters extends SimpleParameterSet {
	public static final String rawDataFilesOption = "Raw data file";

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);

    public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
            "Raw data files", "Raw data files to display", new RawDataFile[0]);

    public static final ComboParameter<Object> xAxisValueSource = new ComboParameter<Object>(
            "X axis value", "X axis value",
            new Object[] { rawDataFilesOption });

    public static final ComboParameter<YAxisValueSource> yAxisValueSource = new ComboParameter<YAxisValueSource>(
            "Y axis value", "Y axis value", YAxisValueSource.values());

    public static final PeakSelectionParameter selectedRows = new PeakSelectionParameter();

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public KendrickMassPlotParameters() {
        super(new Parameter[] { peakList, dataFiles, xAxisValueSource,
                yAxisValueSource, selectedRows, windowSettings });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        PeakList selectedPeakLists[] = getParameter(peakList).getValue()
                .getMatchingPeakLists();
        if (selectedPeakLists.length > 0) {
            RawDataFile plDataFiles[] = selectedPeakLists[0].getRawDataFiles();
            PeakListRow plRows[] = selectedPeakLists[0].getRows();
            Arrays.sort(plRows, new PeakListRowSorter(SortingProperty.MZ,
                    SortingDirection.Ascending));
            getParameter(dataFiles).setChoices(plDataFiles);
            getParameter(dataFiles).setValue(plDataFiles);
        }

        return super.showSetupDialog(parent, valueCheckRequired);
    }

}
