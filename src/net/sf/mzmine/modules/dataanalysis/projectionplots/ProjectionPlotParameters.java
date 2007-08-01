package net.sf.mzmine.modules.dataanalysis.projectionplots;

import org.dom4j.Element;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.io.RawDataFile;

public class ProjectionPlotParameters implements StorableParameterSet {

    private PeakList sourcePeakList;

	
	// XML elements
	private static final String coloringModeElement = "coloringmode";
	private static final String peakMeasuringModeElement = "peakmeasuringmode";
	
	// Exported/imported parameters and their possible values
	private Object coloringMode;
	public static final String ColoringByFileOption = "Color by file";
	public static final String ColoringByParameterValueOption = "Color by parameter value";
	
	private Object peakMeasuringMode;
    public static final String PeakHeightOption = "Peak height";
    public static final String PeakAreaOption = "Peak area";
	
    // Not exported parameter values
    private Parameter selectedParameter;	// Parameter used when coloring by parameter value
    private RawDataFile[] selectedDataFiles;
    private PeakListRow[] selectedRows;
	
		
    
    
	public ProjectionPlotParameters(PeakList sourcePeakList) {
		
		this.sourcePeakList = sourcePeakList;
        this.selectedDataFiles = sourcePeakList.getRawDataFiles();
        this.selectedRows = sourcePeakList.getRows();		
			
		coloringMode = ColoringByFileOption;
		peakMeasuringMode = PeakAreaOption;
		selectedParameter = null;
	}
	
	private ProjectionPlotParameters(PeakList sourcePeakList, Object coloringMode, Object peakMeasuringMode, Parameter selectedParameter, RawDataFile[] selectedDataFiles, PeakListRow[] selectedRows) {
		this.coloringMode = coloringMode;
		this.peakMeasuringMode = peakMeasuringMode;
		this.selectedParameter = selectedParameter;
		this.selectedDataFiles = selectedDataFiles;
		this.selectedRows = selectedRows;
	}
	
    /**
     * Represent method's parameters and their values in human-readable format
     */
    public String toString() {
        return "Coloring mode: " + coloringMode
                + ", peak measuring mode: " + peakMeasuringMode
                + ", selected parameter: "
                + selectedParameter;
    }	
	
    public ProjectionPlotParameters clone() {
        return new ProjectionPlotParameters(sourcePeakList, coloringMode, peakMeasuringMode, selectedParameter, selectedDataFiles, selectedRows);
    }
	
	public void exportValuesToXML(Element element) {
		element.addElement(coloringModeElement).setText(coloringMode.toString());
		element.addElement(peakMeasuringModeElement).setText(peakMeasuringMode.toString());
	}

	public void importValuesFromXML(Element element) {
        String coloringModeString = element.elementText(coloringModeElement);
        if (ColoringByFileOption.equals(coloringModeString)) coloringMode = ColoringByFileOption;
        if (ColoringByParameterValueOption.equals(coloringModeString)) coloringMode = ColoringByParameterValueOption;
        
        String peakMeasuringModeString = element.elementText(peakMeasuringModeElement);
        if (PeakHeightOption.equals(peakMeasuringModeString)) peakMeasuringMode = PeakHeightOption;
        if (PeakAreaOption.equals(peakMeasuringModeString)) peakMeasuringMode = PeakAreaOption;
	}

	public Object getColoringMode() {
		return coloringMode;
	}

	public void setColoringMode(Object coloringMode) {
		this.coloringMode = coloringMode;
	}

	public Object getPeakMeasuringMode() {
		return peakMeasuringMode;
	}

	public void setPeakMeasuringMode(Object peakMeasuringMode) {
		this.peakMeasuringMode = peakMeasuringMode;
	}

	public RawDataFile[] getSelectedDataFiles() {
		return selectedDataFiles;
	}

	public void setSelectedDataFiles(RawDataFile[] selectedDataFiles) {
		this.selectedDataFiles = selectedDataFiles;
	}

	public Parameter getSelectedParameter() {
		return selectedParameter;
	}

	public void setSelectedParameter(Parameter selectedParameter) {
		this.selectedParameter = selectedParameter;
	}

	public PeakListRow[] getSelectedRows() {
		return selectedRows;
	}

	public void setSelectedRows(PeakListRow[] selectedRows) {
		this.selectedRows = selectedRows;
	}

	public PeakList getSourcePeakList() {
		return sourcePeakList;
	}

	public void setSourcePeakList(PeakList sourcePeakList) {
		this.sourcePeakList = sourcePeakList;
	}

	
	
}
