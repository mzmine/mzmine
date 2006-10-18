package net.sf.mzmine.userinterface.dialogs;

import java.io.File;

import javax.swing.JFileChooser;

import sunutils.ExampleFileFilter;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.util.AlignmentResultExporter;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelectionAcceptor;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelectionDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

public class AlignmentResultExportDialog implements AlignmentResultColumnSelectionAcceptor {

	private AlignmentResult[] alignmentResults;
	
	public AlignmentResultExportDialog(AlignmentResult[] alignmentResults) {
		
		this.alignmentResults = alignmentResults;
	
		// Show column selection dialog
		AlignmentResultColumnSelection columnSelection = new AlignmentResultColumnSelection();
		columnSelection.setAllColumns();
		AlignmentResultColumnSelectionDialog dialog = new AlignmentResultColumnSelectionDialog(columnSelection, this);
		MainWindow.getInstance().addInternalFrame(dialog);
		dialog.setVisible(true);
	}
	
	/*
	 * This is the call-back method for column selection dialog
	 */
	public void setColumnSelection(AlignmentResultColumnSelection columnSelection) {
		
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
