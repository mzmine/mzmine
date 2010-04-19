/*
 * Copyright 2006-2010 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.threed;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import visad.ScalarMap;
import visad.util.ColorMapWidget;
import visad.util.GMCWidget;

/**
 * 3D visualizer properties dialog
 */
class ThreeDPropertiesDialog extends JDialog implements ActionListener {

	private static final String title = "3D visualizer properties";

	private ThreeDDisplay display;
	private GMCWidget gmcWidget;
	private ColorMapWidget colorWidget;
	private JFormattedTextField normalizeIntensityField;
	private JButton normalizeButton, okButton;

	ThreeDPropertiesDialog(ThreeDDisplay display) {

		super(MZmineCore.getDesktop().getMainFrame(), title, false);

		this.display = display;

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		ScalarMap colorMap = (ScalarMap) display.getMapVector().get(4);

		GUIUtils.addLabel(this, "Color mapping");

		try {
			colorWidget = new ColorMapWidget(colorMap);
			add(colorWidget);
		} catch (Exception e) {
			e.printStackTrace();
		}

		GUIUtils.addLabel(this, "Graphics mode control");

		gmcWidget = new GMCWidget(display.getGraphicsModeControl());
		add(gmcWidget);

		GUIUtils.addLabel(this, "Normalize Z axis");

		JPanel normalizePanel = new JPanel();
		GUIUtils.addLabel(normalizePanel, "Normalize to: ",
				SwingConstants.CENTER);
		normalizeIntensityField = new JFormattedTextField(MZmineCore
				.getIntensityFormat());
		normalizeIntensityField.setColumns(10);
		normalizePanel.add(normalizeIntensityField);
		normalizeButton = GUIUtils.addButton(normalizePanel, "Normalize", null,
				this);
		add(normalizePanel);
		
		GUIUtils.addSeparator(this);

		okButton = GUIUtils.addButton(this, "OK", null, this);

		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {
		Object src = event.getSource();

		if (src == okButton) {
			dispose();
		}

		if (src == normalizeButton) {
			Number normalizeFieldValue = (Number) normalizeIntensityField
					.getValue();
			if (normalizeFieldValue == null)
				return;
			double normalizeValue = normalizeFieldValue.doubleValue();
			display.normalizeIntensityAxis(normalizeValue);
		}

	}

}
