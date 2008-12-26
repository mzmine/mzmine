/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;

public class ScatterPlotWindow extends JInternalFrame implements
TaskListener, ActionListener {
	
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ScatterPlotPanel scatterPlotPanel;
    private ScatterPlotSearchPanel searchPanel;
    
    private Desktop desktop;
	
    public ScatterPlotWindow(PeakList peakList, String title) {

        super(title, true, true, true, true);
        this.desktop = MZmineCore.getDesktop();


        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        try{

        logger.finest("Creates scatterPlotSearchPanel");
        searchPanel = new ScatterPlotSearchPanel(this);
        add(searchPanel, BorderLayout.SOUTH);
        
        logger.finest("Creates scatterPlotPanel");
        scatterPlotPanel = new ScatterPlotPanel(this);
        add(scatterPlotPanel, BorderLayout.CENTER);
        
        logger.finest("Creates ScatterPlotDataSet");
		if (peakList != null){
			scatterPlotPanel.setPeakList(peakList, searchPanel.getSelectionList());
			searchPanel.activeButtons();
		}
		
        }
        catch (Exception e){
        	e.printStackTrace();
        }

        pack();

    }
    
	public ScatterPlot getPlotChart(){
		return scatterPlotPanel.getPlot();
	}
	
	public ScatterPlotSearchPanel getBotomPanel(){
		return searchPanel;
	}
	
	
	public void actionPerformed(ActionEvent e) {
	}

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage("Error while updating scatter plot: "
                    + task.getErrorMessage());
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
    }

}
