package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.Color;
import java.awt.Component;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class CVPaintScaleSetupDialogTableCellRenderer extends
		DefaultTableCellRenderer {

	private TreeMap<Double, Color> lookupTable;
	
	public CVPaintScaleSetupDialogTableCellRenderer(TreeMap<Double, Color> lookupTable) {
		this.lookupTable = lookupTable;
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value,
	  boolean isSelected, boolean hasFocus, int row, int column)
	{
		if (value==null) 
			this.setText("");
		else 
			this.setText(String.valueOf(value));
		
		if (lookupTable==null) {
			return this;
		}
		if (lookupTable.size()<row) {
			return this;
		}
		
		Double key = lookupTable.keySet().toArray(new Double[0])[row];
		Color color = lookupTable.get(key);
		
		if (column==0) this.setBackground(table.getBackground());
		if (column==1) this.setBackground(color);
		
		return this;		
	}
}