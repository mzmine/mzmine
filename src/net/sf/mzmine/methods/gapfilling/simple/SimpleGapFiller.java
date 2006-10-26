/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.methods.gapfilling.simple;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

// TODO: Code for this method must be rewritten

public class SimpleGapFiller implements Method,
ListSelectionListener, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private SimpleGapFillerParameters parameters;
	
    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;
	
	
	public boolean askParameters() {

		parameters = new SimpleGapFillerParameters();
		
        ParameterSetupDialog dialog = new ParameterSetupDialog(		
				MainWindow.getInstance(),
				"Please check parameter values for " + toString(),
				parameters
		);
        dialog.setVisible(true);

		if (dialog.getExitCode()==-1) return false;

		return true;

	}
	
	public String toString() {
		return "Simple Gap filler";
	}

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters, OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

        logger.info("Running " + toString() + " on " + alignmentResults.length + " alignment results.");

        SimpleGapFillerMain ruler = new SimpleGapFillerMain(taskController, alignmentResults[0], (SimpleGapFillerParameters) parameters);
        
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();
        
        myMenuItem = desktop.addMenuItem(MZmineMenu.ALIGNMENT,
                "Peak list aligner", this, null, KeyEvent.VK_A,
                false, false);

        desktop.addSelectionListener(this);

        
    }

	public void valueChanged(ListSelectionEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}