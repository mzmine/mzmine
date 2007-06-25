/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.io.export;

import java.io.File;

import javax.swing.JFileChooser;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

import com.sun.java.ExampleFileFilter;

public class PeakListExportDialog {

	public PeakListExportDialog(PeakList[] peakLists) {
		
		// Show column selection dialog
		PeakListExportColumns columnSelection = new PeakListExportColumns();

		ColumnSelectionDialog dialog = new ColumnSelectionDialog(MZmineCore.getDesktop().getMainFrame(), columnSelection);
		dialog.setVisible(true);
        
        if (dialog.getExitCode() == ExitCode.CANCEL) return;
		
		// Ask file names for all alignment result(s)
		File[] peakListFiles = new File[peakLists.length];
		int i = 0;
		for (PeakList peakList : peakLists) {
			File file = askFileName(peakList);
			if (file==null) return; // Cancel?
			
			peakListFiles[i] = file;
			i++;
		}
		
		// Export alignment result(s)
		PeakListExporter exporter = new PeakListExporter();
		i=0;
		for (PeakList peakList : peakLists) {
			exporter.exportToFile(peakList, peakListFiles[i], columnSelection);
			i++;
		}
	}
	
	private File askFileName(PeakList peakList) {
		
		JFileChooser chooser = new JFileChooser();
	    // Note: source for ExampleFileFilter can be found in FileChooserDemo,
	    // under the demo/jfc directory in the JDK.
	    ExampleFileFilter filter = new ExampleFileFilter();
	    filter.addExtension("txt");
	    filter.setDescription("Tab-delimitted text file");
	    chooser.setFileFilter(filter);
	    chooser.setDialogTitle("Please give file name for \"" + peakList.toString() + "\"");
	    int returnVal = chooser.showSaveDialog(MZmineCore.getDesktop().getMainFrame());
	    if(returnVal == JFileChooser.APPROVE_OPTION)
	       return chooser.getSelectedFile();
	    
	    return null;
		
	}	

	
}
