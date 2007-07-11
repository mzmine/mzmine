package net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale;

import java.awt.Color;
import java.util.TreeMap;
import javax.swing.table.AbstractTableModel;

public class InterpolatingLookupPaintScaleSetupDialogTableModel extends AbstractTableModel {

	private static String[] columnNames = {"Value", "Color"};
	
	private TreeMap<Double, Color> lookupTable;
	
	public InterpolatingLookupPaintScaleSetupDialogTableModel(TreeMap<Double, Color> lookupTable) {
		this.lookupTable = lookupTable;
	}
	
	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return lookupTable.size();
	}

	public String getColumnName(int column) {
		return columnNames[column];
	}
	
	public Object getValueAt(int row, int column) {
		if (column==0)
			return lookupTable.keySet().toArray(new Double[0])[row];
		return null;
	}
	
}
