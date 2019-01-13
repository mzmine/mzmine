package net.sf.mzmine.modules.visualization.productionfilter;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 * Product Ion visualizer's toolbar class
 */
class ProductIonFilterToolBar extends JToolBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static final Icon dataPointsIcon = new ImageIcon("icons/datapointsicon.png");

	ProductIonFilterToolBar(ProductIonFilterVisualizerWindow masterFrame) {

		super(JToolBar.VERTICAL);

		setFloatable(false);
		setFocusable(false);
		setMargin(new Insets(5, 5, 5, 5));
		setBackground(Color.white);

		GUIUtils.addButton(this, null, dataPointsIcon, masterFrame, "HIGHLIGHT",
				"Highlight selected precursor mass range");

	}

}