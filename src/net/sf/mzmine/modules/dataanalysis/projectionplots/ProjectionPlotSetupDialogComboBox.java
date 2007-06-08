package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class ProjectionPlotSetupDialogComboBox extends JComboBox {


	public ProjectionPlotSetupDialogComboBox(Color[] colors) {
		super(colors);
		this.setRenderer(new Renderer());
	}
	
	private class Renderer extends
			BasicComboBoxRenderer {
	
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			// TODO Auto-generated method stub
			Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Color) { 
				comp.setBackground((Color)value);
				((JLabel)comp).setText("");
			}
			return comp;
		}
	
	}

}