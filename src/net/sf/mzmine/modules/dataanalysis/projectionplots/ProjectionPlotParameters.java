package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.util.Hashtable;

import org.dom4j.Element;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;

public class ProjectionPlotParameters implements StorableParameterSet {

	private static final Color[] colors = { Color.red, Color.green, Color.blue, Color.magenta, Color.lightGray, Color.orange};
	
    private PeakList sourcePeakList;
	
	// XML elements
	private static final String coloringModeElement = "coloringmode";
	private static final String peakMeasuringModeElement = "peakmeasuringmode";
	
	// Exported/imported parameters and their possible values
	private Object coloringMode;
	public static final String ColoringSingleOption = "No coloring";
	public static final String ColoringByParameterValueOption = "Color by parameter value";
	public static final String ColoringByFileOption = "Color by file";	
	
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
        if (ColoringSingleOption.equals(coloringModeString)) coloringMode = ColoringByFileOption;
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
	
	public Hashtable<RawDataFile, Color> getColorsForSelectedFiles() {
		
		/* 
		 * Projection plot colors are automatically picked here according to the current coloring mode. 
		 */
				
		Hashtable<RawDataFile, Color> colorsForSelectedFiles = new Hashtable<RawDataFile, Color>();
		
		if (coloringMode == this.ColoringSingleOption) {
			for (RawDataFile rawDataFile : getSelectedDataFiles()) {
				colorsForSelectedFiles.put(rawDataFile, colors[0]);
			}
		}
		
		if (coloringMode == ColoringByFileOption) {
			int colorInd = 0;
			for (RawDataFile rawDataFile : getSelectedDataFiles()) {
				colorsForSelectedFiles.put(rawDataFile, colors[colorInd % colors.length]);
				colorInd++;
			}
		}
		if (coloringMode == ColoringByParameterValueOption) {
			// Collect all parameter values
			MZmineProject currentProject = MZmineCore.getCurrentProject();
			Hashtable<Object, Color> colorsForParameterValues = new Hashtable<Object, Color>();
			int colorInd = 0;
			for (RawDataFile rawDataFile : getSelectedDataFiles()) {
				Object paramValue = currentProject.getParameterValue(selectedParameter, rawDataFile);
				if (!colorsForParameterValues.containsKey(rawDataFile)) {
					colorsForParameterValues.put(paramValue, colors[colorInd % colors.length]);
					colorInd++;
				}
				Color color = colorsForParameterValues.get(paramValue);
				colorsForSelectedFiles.put(rawDataFile, color);
			}
			
			
		}
		
		return colorsForSelectedFiles;
		
	}

	
	
}
