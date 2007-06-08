package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Component;
import java.util.TreeMap;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class ProjectionPlotSetupDialogTableCellRenderer extends
		DefaultTableCellRenderer {

	public Component getTableCellRendererComponent(JTable table, Object value,
	  boolean isSelected, boolean hasFocus, int row, int column)
	{
		if (value instanceof Color) 
			this.setText("");
		else 
			this.setText(String.valueOf(value));
				
		if (column==0) this.setBackground(table.getBackground());
		if (column==1) this.setBackground((Color)value);
		
		return this;		
	}
}