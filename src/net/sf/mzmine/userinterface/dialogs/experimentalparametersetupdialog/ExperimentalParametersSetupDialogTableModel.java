package net.sf.mzmine.userinterface.dialogs.experimentalparametersetupdialog;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.io.OpenedRawDataFile;

public class ExperimentalParametersSetupDialogTableModel extends
		AbstractTableModel {

	private OpenedRawDataFile[] files;
	private Vector<Parameter> experimentalParameters;
	
	public ExperimentalParametersSetupDialogTableModel(OpenedRawDataFile[] files, Vector<Parameter> experimentalParameters) {
		this.files = files;
		this.experimentalParameters = experimentalParameters;
	}
	
	public int getColumnCount() {
		return 1 + experimentalParameters.size();
	}

	public String getColumnName(int col) {
		if (col==0) return "Raw data";
		if (col>0) {
			Parameter p = experimentalParameters.elementAt(col-1);
			return p.toString();
		}
		return null;
	}	
	
	public int getRowCount() {
		return files.length;
	}

	public Object getValueAt(int row, int col) {
		if (col==0) return files[row].toString();
		if (col>0) {
			Parameter p = experimentalParameters.elementAt(col-1);
			// TODO: Retrieve value for p from files[row]
			return "N/A";
		}
		return null;
	}

}
