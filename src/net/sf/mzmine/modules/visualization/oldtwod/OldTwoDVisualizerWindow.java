/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.RawDataVisualizer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CursorPosition;

/**
 * 2D visualizer using JFreeChart library
 */
public class OldTwoDVisualizerWindow extends JInternalFrame implements
        RawDataVisualizer, ActionListener, TaskListener {

    private OldTwoDToolBar toolBar;
    private OldTwoDPlot twoDPlot;
    private OldTwoDXAxis twoDXAxis;
    private OldTwoDYAxis twoDYAxis;
    
    private JCheckBox resampleCheckBox;

    private OldTwoDDataSet dataset;

    private RawDataFile dataFile;
    private int msLevel;

    private Desktop desktop;

    public OldTwoDVisualizerWindow(RawDataFile dataFile) {

        super(dataFile.toString(), true, true, true, true);

        this.desktop = MZmineCore.getDesktop();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.dataFile = dataFile;
        this.msLevel = msLevel;

        dataset = new OldTwoDDataSet(dataFile, this);
        

		JPanel topPnl = new JPanel();
		topPnl.setMinimumSize(new Dimension(getWidth(),5));
		topPnl.setPreferredSize(new Dimension(getWidth(),5));
		topPnl.setBackground(Color.white);
		getContentPane().add(topPnl, java.awt.BorderLayout.NORTH);
		
        twoDXAxis = new OldTwoDXAxis(dataset);
        twoDXAxis.setMinimumSize(new Dimension(getWidth(),25));
        twoDXAxis.setPreferredSize(new Dimension(getWidth(),25));
        twoDXAxis.setBackground(Color.white);
		add(twoDXAxis, BorderLayout.SOUTH);

		twoDYAxis = new OldTwoDYAxis(dataset);
		twoDYAxis.setMinimumSize(new Dimension(100, getHeight()));
		twoDYAxis.setPreferredSize(new Dimension(100, getHeight()));
		twoDYAxis.setBackground(Color.white);
		getContentPane().add(twoDYAxis, java.awt.BorderLayout.WEST);
		
        toolBar = new OldTwoDToolBar(this);
        add(toolBar, BorderLayout.EAST);
        
        twoDPlot = new OldTwoDPlot(this, dataset);
        add(twoDPlot, BorderLayout.CENTER);
        
/*
        resampleCheckBox = new JCheckBox("Resample when zooming", true);
        resampleCheckBox.setBackground(Color.white);
        resampleCheckBox.setFont(resampleCheckBox.getFont().deriveFont(10f));
        resampleCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
        resampleCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(resampleCheckBox, BorderLayout.SOUTH);
*/
        updateTitle();
        
        pack();
        
        setSize(600,400);

    }

    public void datasetUpdating() {
    	if (twoDPlot!=null)
    		twoDPlot.datasetUpdating();
    }
    
    public void datasetUpdateReady() {
    	if (twoDPlot!=null)
    		twoDPlot.datasetUpdateReady();
    	
    }

    
    public void switchCentroidContinousMode() {
    	boolean interpolate = dataset.isInterpolated();

    	dataset.resampleIntensityMatrix(!interpolate);
    	   	
    }
    
    public void setFullZoom(int msLevel) {
    	
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
        
        // Determine maximum zoom settings
    	float rtMin = dataFile.getDataMinRT(msLevel);
    	float rtMax = dataFile.getDataMaxRT(msLevel);
    	float mzMin = dataFile.getDataMinMZ(msLevel);
    	float mzMax = dataFile.getDataMaxMZ(msLevel);

        // Determine x resolution (rt resolution)
        int xResolution = twoDPlot.getWidth();
        int[] scanNumbers = dataFile.getScanNumbers(msLevel, rtMin, rtMax);
        if (scanNumbers.length < xResolution) xResolution = scanNumbers.length;
        
        // Use previous interpolate setting (default to no interpolation)
        boolean interpolate = false;
        if (dataset!=null) interpolate = dataset.isInterpolated();

    	
    	dataset.resampleIntensityMatrix(msLevel, rtMin, rtMax, mzMin, mzMax, xResolution, twoDPlot.getHeight(), interpolate);
    }
    
    public void setZoomRange(int msLevel, float rtMin, float rtMax, float mzMin, float mzMax) {
    	
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
      
        // Determine x resolution (rt resolution)
        int xResolution = twoDPlot.getWidth();
        int[] scanNumbers = dataFile.getScanNumbers(msLevel, rtMin, rtMax);
        if (scanNumbers.length < xResolution) xResolution = scanNumbers.length;

        // Use previous interpolate setting (default to no interpolation)
        boolean interpolate = false;
        if (dataset!=null) interpolate = dataset.isInterpolated();
        
        dataset.resampleIntensityMatrix(msLevel, rtMin, rtMax, mzMin, mzMax, xResolution, twoDPlot.getHeight(), interpolate);
    	
    }
    
    void updateTitle() {

        StringBuffer title = new StringBuffer();
        title.append("[");
        title.append(dataFile.toString());
        title.append("]: 2D view");

        title.append(", MS");
        title.append(msLevel);     
        
        setTitle(title.toString());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("TOGGLE_PLOT_MODE")) {
        	switchCentroidContinousMode();
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage("Error while updating 2D visualizer: "
                    + task.getErrorMessage());
        }
        
        repaint();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {

    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#getCursorPosition()
     */
    public CursorPosition getCursorPosition() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setCursorPosition(net.sf.mzmine.util.CursorPosition)
     */
    public void setCursorPosition(CursorPosition newPosition) {
        // TODO Auto-generated method stub

    }

}
