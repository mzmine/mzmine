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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JFrame;

import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;




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

		JoinAlignerParameterSetupDialog jaPSD = new JoinAlignerParameterSetupDialog((JFrame)(MainWindow.getInstance()), new String("Please give parameter values"), currentParameters);
		jaPSD.setVisible(true);

		// Check if user pressed cancel
		if (jaPSD.getExitCode()==-1) {
			return false;
		}

		return true;
	}

	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {
	}


}
