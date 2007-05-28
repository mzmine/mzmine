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

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.ColumnSelectionDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.ExitCode;
import sunutils.ExampleFileFilter;

public class AlignmentResultExportDialog {

	public AlignmentResultExportDialog(AlignmentResult[] alignmentResults) {
		
		// Show column selection dialog
		AlignmentResultExportColumns columnSelection = new AlignmentResultExportColumns();

		ColumnSelectionDialog dialog = new ColumnSelectionDialog(MainWindow.getInstance(), columnSelection);
		dialog.setVisible(true);
        
        if (dialog.getExitCode() == ExitCode.CANCEL) return;
		
		// Ask file names for all alignment result(s)
		File[] alignmentResultFiles = new File[alignmentResults.length];
		int i = 0;
		for (AlignmentResult alignmentResult : alignmentResults) {
			File file = askFileName(alignmentResult);
			if (file==null) return; // Cancel?
			
			alignmentResultFiles[i] = file;
			i++;
		}
		
		// Export alignment result(s)
		AlignmentResultExporter exporter = new AlignmentResultExporter();
		i=0;
		for (AlignmentResult alignmentResult : alignmentResults) {
			exporter.exportToFile(alignmentResult, alignmentResultFiles[i], columnSelection);
			i++;
		}
	}
	
	private File askFileName(AlignmentResult alignmentResult) {
		
		JFileChooser chooser = new JFileChooser();
	    // Note: source for ExampleFileFilter can be found in FileChooserDemo,
	    // under the demo/jfc directory in the JDK.
	    ExampleFileFilter filter = new ExampleFileFilter();
	    filter.addExtension("txt");
	    filter.setDescription("Tab-delimitted text file");
	    chooser.setFileFilter(filter);
	    chooser.setDialogTitle("Please give file name for \"" + alignmentResult.toString() + "\"");
	    int returnVal = chooser.showSaveDialog(MainWindow.getInstance().getMainFrame());
	    if(returnVal == JFileChooser.APPROVE_OPTION)
	       return chooser.getSelectedFile();
	    
	    return null;
		
	}	

	
}
