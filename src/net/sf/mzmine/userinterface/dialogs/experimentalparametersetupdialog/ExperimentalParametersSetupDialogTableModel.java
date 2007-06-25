package net.sf.mzmine.userinterface.dialogs.experimentalparametersetupdialog;

import java.util.Hashtable;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.io.RawDataFile;

public class ExperimentalParametersSetupDialogTableModel extends
		AbstractTableModel {

	private RawDataFile[] files;
	private Hashtable<Parameter, Object[]> parameterValues;
	private Parameter[] parameters;
	
	public ExperimentalParametersSetupDialogTableModel(RawDataFile[] files, Hashtable<Parameter, Object[]> parameterValues) {
		System.out.println("Initializing new table model with " + files.length  + " files and " + parameterValues.size() + " parameters");
		this.files = files;
		parameters = parameterValues.keySet().toArray(new Parameter[0]);
		this.parameterValues = parameterValues;
	}
	
	public int getColumnCount() {
		return 1 + parameterValues.size();
	}

	public String getColumnName(int col) {
		if (col==0) return "Raw data";
		if (col>0) {
			Parameter p = parameters[col-1];
			return p.toString();
		}
		return null;
	}	
	
	public int getRowCount() {
		System.out.println("getRowCount() is going to return " + files.length);
		return files.length;
	}

	public Object getValueAt(int row, int col) {
		System.out.println("Get value at row,col " + row + ", " + col);
		if (col==0) return files[row].toString();
		if (col>0) {
			Parameter p = parameters[col-1];
			return parameterValues.get(p)[row];
		}
		return null;
	}
	

	
	@Override
	public void setValueAt(Object value, int row, int col) {
		// TODO Auto-generated method stub
		if (col==0) return;
		Parameter p = parameters[col-1];
		Object[] values = parameterValues.get(p);
		values[row] = value;

	}

	public Parameter getParameter(int column) {
		if (column==0) return null;
		if ( (parameters==null) || (parameters.length==0) ) return null;
		
		return parameters[column-1];
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		if (col==0) return false;
		return true;
	}
	
	
	
}
