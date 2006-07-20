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

package net.sf.mzmine.methods.alignment.join;

import java.util.logging.Logger;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;




/**
 *
 */
public class JoinAligner implements Method {



	public String getMethodDescription() {
		return new String("Join Aligner");
	}

	/**
	 * This method asks user to define which raw data files should be aligned and also check parameter values
	 */
	public boolean askParameters(MethodParameters parameters) {

		if (parameters==null) return false;
		JoinAlignerParameters currentParameters = (JoinAlignerParameters)parameters;

		//JoinAlignerParameterSetupDialog jaPSD = new JoinAlignerParameterSetupDialog((JFrame)(MainWindow.getInstance()), new String("Please give parameter values"), currentParameters);
		//jaPSD.setVisible(true);

		// Check if user pressed cancel
		//if (jaPSD.getExitCode()==-1) {
		//	return false;
		//}

		return true;
	}

	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {
	}

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.taskcontrol.TaskController, net.sf.mzmine.userinterface.Desktop, java.util.logging.Logger)
     */
    public void initModule(TaskController taskController, Desktop desktop, Logger logger) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters, OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {
        // TODO Auto-generated method stub
        
    }


}
