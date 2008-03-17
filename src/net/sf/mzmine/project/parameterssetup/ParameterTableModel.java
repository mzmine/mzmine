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

package net.sf.mzmine.project.parameterssetup;

import java.util.Hashtable;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.io.RawDataFile;

public class ParameterTableModel extends
		AbstractTableModel {

	private RawDataFile[] files;
	private Hashtable<Parameter, Object[]> parameterValues;
	private Parameter[] parameters;
	
	public ParameterTableModel(RawDataFile[] files, Hashtable<Parameter, Object[]> parameterValues) {
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
		return files.length;
	}

	public Object getValueAt(int row, int col) {
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
