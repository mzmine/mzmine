package net.sf.mzmine.modules.visualization.scatterplot.plottooltip;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;

public interface CustomToolTipProvider {
	
	public JComponent getCustomToolTipComponent(MouseEvent event);

}
