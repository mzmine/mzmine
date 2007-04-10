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
package net.sf.mzmine.methods.deisotoping.incompletefilter;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.taskcontrol.TaskSequenceListener;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


// TODO: Implement this method!

public class IncompleteIsotopePatternFilter implements Method {

	private IncompleteIsotopePatternFilterParameters parameters;

	
	public String toString() {
		return new String("Incomplete isotope pattern filter");
	}

	/**
	 * Method asks parameter values from user
	 */
	public boolean askParameters() {

        parameters = new IncompleteIsotopePatternFilterParameters();

        ParameterSetupDialog dialog = new ParameterSetupDialog(		
        				MainWindow.getInstance(),
        				"Please check parameter values for " + toString(),
        				parameters
        		);
        dialog.setVisible(true);
        
		//if (dialog.getExitCode()==-1) return false;

		return true;
	}
	
	public void setParameters(SimpleParameterSet parameters) {
		this.parameters = (IncompleteIsotopePatternFilterParameters)parameters;
	}


    


    /**
     * @see net.sf.mzmine.methods.Method#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ParameterSet setupParameters(ParameterSet current) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[], net.sf.mzmine.data.ParameterSet, net.sf.mzmine.taskcontrol.TaskSequenceListener)
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, ParameterSet parameters, TaskSequenceListener methodListener) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        // TODO Auto-generated method stub
        
    }    




}

