package net.sf.mzmine.modules.lightviewer;

public enum MZviewerModuleName {
	
    PEAKLISTLOADER ("net.sf.mzmine.modules.io.peaklistsaveload.load.PeakListLoader"),
    SCATTERPLOTVISUALIZER ("net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotVisualizer"),
    HISTOGRAMVISUALIZER ("net.sf.mzmine.modules.visualization.histogram.HistogramVisualizer"),
    INFOVISUALIZER ("net.sf.mzmine.modules.visualization.infovisualizer.InfoVisualizer"),
    PEAKLISTTABLEVISUALIZER ("net.sf.mzmine.modules.visualization.peaklist.PeakListTableVisualizer"),
    INTENSITYPLOT ("net.sf.mzmine.modules.visualization.intensityplot.IntensityPlot");
    
    private String className;
    
    MZviewerModuleName (String className){
    	this.className = className;
    }
    
    public String getClassName(){
    	return this.className;
    }

}
