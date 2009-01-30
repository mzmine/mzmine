/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.main.mzmineviewer;

public enum MZviewerModuleName {
	
    PEAKLISTLOADER ("net.sf.mzmine.modules.io.xmlimport.XMLImporter"),
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
