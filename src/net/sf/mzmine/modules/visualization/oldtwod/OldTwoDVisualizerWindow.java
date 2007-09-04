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
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;
import net.sf.mzmine.util.CursorPosition;

/**
 * 2D visualizer using JFreeChart library
 */
public class OldTwoDVisualizerWindow extends JInternalFrame implements
        RawDataVisualizer, ActionListener, TaskListener {

	private OldTwoDTitlePanel twoDTitle;
	private OldTwoDToolBar toolBar;
    private OldTwoDPlot twoDPlot;
    private OldTwoDXAxis twoDXAxis;
    private OldTwoDYAxis twoDYAxis;
    
    private JCheckBox resampleCheckBox;

    private OldTwoDDataSet dataset;

    private RawDataFile dataFile;
    private int msLevel;

    private Desktop desktop;
    
    private CursorPosition cursorPosition;
    private CursorPosition rangeCursorPosition;
    
    
    private static final int PALETTE_GRAY20 = 1;
    private static final int PALETTE_GRAY5 = 2;
    private static final int PALETTE_GRAY1 = 3;
    private static final int PALETTE_RAINBOW = 4;
    private int currentPaletteType = PALETTE_GRAY20;
    private float maximumIntensity = 0.0f;

    public OldTwoDVisualizerWindow(RawDataFile dataFile) {

        super(dataFile.toString(), true, true, true, true);

        this.desktop = MZmineCore.getDesktop();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.dataFile = dataFile;
        this.msLevel = msLevel;

        dataset = new OldTwoDDataSet(dataFile, this);
        
        twoDTitle = new OldTwoDTitlePanel(this); 
        twoDTitle.setBackground(Color.white);
		getContentPane().add(twoDTitle, java.awt.BorderLayout.NORTH);
		
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
        toolBar.setMinimumSize(new Dimension(50, getHeight()));
        toolBar.setPreferredSize(new Dimension(50, getHeight()));
        add(toolBar, BorderLayout.EAST);
        
        
        Object[] tmp = createPaintScale(currentPaletteType, maximumIntensity);
        InterpolatingLookupPaintScale paintScale =  (InterpolatingLookupPaintScale)tmp[0];
        
        twoDPlot = new OldTwoDPlot(this, dataset, paintScale);
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
    	
    	
    	float newMax = dataset.getMaxIntensity();
    	
    	if (newMax>maximumIntensity) {
    		maximumIntensity = newMax;
    		Object[] tmp = createPaintScale(currentPaletteType, maximumIntensity);
    		twoDPlot.setPaintScale((InterpolatingLookupPaintScale)tmp[0], (Color)tmp[1], (Color)tmp[2], (Color)tmp[3]);
    	}
    		   	
    	if (twoDPlot!=null)
    		twoDPlot.datasetUpdateReady();
    	
    }
    
    private Object[] createPaintScale(int paletteMode, float maxIntensity) {
    	
    	Object[] res = new Object[4];
   	
    	if (paletteMode == PALETTE_GRAY20) {
    		InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
	    	paintScale.add(0.0, new Color(255,255,255));
	    	paintScale.add(0.2*maxIntensity, new Color(0,0,0));

	    	res[0] = paintScale;
			res[1] = new Color(0.5f, 0.5f, 1.0f);
			res[2] = new Color(0.5f, 0.5f, 0.75f);
			res[3] = new Color(0.25f, 1.0f, 0.25f);
    	}
    	
    	if (paletteMode == PALETTE_GRAY5) {
    		InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
	    	paintScale.add(0.0, new Color(255,255,255));
	    	paintScale.add(0.05*maxIntensity, new Color(0,0,0));

	    	res[0] = paintScale;
			res[1] = new Color(0.5f, 0.5f, 1.0f);
			res[2] = new Color(0.5f, 0.5f, 0.75f);
			res[3] = new Color(0.25f, 1.0f, 0.25f);    	
    	}

    	if (paletteMode == PALETTE_GRAY1) {
    		InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
	    	paintScale.add(0.0, new Color(255,255,255));
	    	paintScale.add(0.01*maxIntensity, new Color(0,0,0));

	    	res[0] = paintScale;
			res[1] = new Color(0.5f, 0.5f, 1.0f);
			res[2] = new Color(0.5f, 0.5f, 0.75f);
			res[3] = new Color(0.25f, 1.0f, 0.25f);	    	
    	}    	
    	
    	if (paletteMode == PALETTE_RAINBOW) {
    		InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
    		paintScale.add(000.0f/256.0f * maxIntensity, new Color(255,255,255));
    		paintScale.add(002.0f/256.0f * maxIntensity, new Color(255,000,000));
    		paintScale.add(016.0f/256.0f * maxIntensity, new Color(255,253,000));
    		paintScale.add(032.0f/256.0f * maxIntensity, new Color(000,192,000));
    		paintScale.add(064.0f/256.0f * maxIntensity, new Color(000,128,188));
    		paintScale.add(128.0f/256.0f * maxIntensity, new Color(000,000,250));
    		paintScale.add(256.0f/256.0f * maxIntensity, new Color(000,000,000));

    		res[0] = paintScale;
			res[1] = new Color(0.25f, 0.25f, 0.25f);
			res[2] = new Color(0.25f, 0.25f, 0.25f);
			res[3] = new Color(0.0f, 0.0f, 0.0f);    		
    	}
    	
    	return res;
    	
    }

    
    public void switchCentroidContinousMode() {
    	boolean interpolate = dataset.isInterpolated();

    	dataset.resampleIntensityMatrix(!interpolate);
    	   	
    }
    
    public void setFullZoom(int msLevel) {
    	
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
        
        // Use maximum zoom settings
    	float rtMin = dataFile.getDataMinRT(msLevel);
    	float rtMax = dataFile.getDataMaxRT(msLevel);
    	float mzMin = dataFile.getDataMinMZ(msLevel);
    	float mzMax = dataFile.getDataMaxMZ(msLevel);
       
        // Use previous interpolate setting (default to no interpolation)
        boolean interpolate = false;
        if (dataset!=null) interpolate = dataset.isInterpolated();
    	
    	dataset.resampleIntensityMatrix(msLevel, rtMin, rtMax, mzMin, mzMax, twoDPlot.getWidth(), twoDPlot.getHeight(), interpolate);

    }
    
    public void setZoomRange(int msLevel, float rtMin, float rtMax, float mzMin, float mzMax) {
    	
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
      
        // Use previous interpolate setting (default to no interpolation)
        boolean interpolate = false;
        if (dataset!=null) interpolate = dataset.isInterpolated();
        
        dataset.resampleIntensityMatrix(msLevel, rtMin, rtMax, mzMin, mzMax, twoDPlot.getWidth(), twoDPlot.getHeight(), interpolate);
    	
    }
    
    public void setCursorPosition(CursorPosition cursorPosition) {
    	this.cursorPosition = cursorPosition;
    	twoDTitle.updateTitle();
    }
    
    public CursorPosition getCursorPosition() {
    	return cursorPosition;
    }
    
    public void setRangeCursorPosition(CursorPosition cursorPosition) {
    	this.rangeCursorPosition = cursorPosition;
    	twoDTitle.updateTitle();
    }
    
    public CursorPosition getRangeCursorPosition() {
    	return rangeCursorPosition;
    }
    
    
    void updateTitle() {

        StringBuffer title = new StringBuffer();
        title.append("[");
        title.append(dataFile.toString());
        title.append("]: 2D view");

        title.append(", MS");
        title.append(msLevel+1);     
        
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
        
        if (command.equals("SWITCH_PALETTE")) {
        	switch (currentPaletteType) {
	        	case PALETTE_GRAY20:
	        		currentPaletteType = PALETTE_GRAY5; 
	        		break;
	        	case PALETTE_GRAY5:
	        		currentPaletteType = PALETTE_GRAY1; 
	        		break;
	        	case PALETTE_GRAY1:
	        		currentPaletteType = PALETTE_RAINBOW; 
	        		break;
	        	case PALETTE_RAINBOW:
	        		currentPaletteType = PALETTE_GRAY20;
	        		break;
        	}

    		Object[] tmp = createPaintScale(currentPaletteType, maximumIntensity);
    		twoDPlot.setPaintScale((InterpolatingLookupPaintScale)tmp[0], (Color)tmp[1], (Color)tmp[2], (Color)tmp[3]);    	
        	
        	twoDPlot.datasetUpdateReady();
        	repaint();
        	
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



}
