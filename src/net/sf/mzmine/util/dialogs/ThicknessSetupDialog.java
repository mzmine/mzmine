/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerParameters;

public class ThicknessSetupDialog extends JDialog implements ActionListener {

	private JFormattedTextField fieldThickness;
	private JButton btnOK, btnCancel;
	private SpectraPlot plot;

	public ThicknessSetupDialog(SpectraPlot plot) {
		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), true);

		this.plot = plot;
		double oldThickness = SpectraVisualizerParameters.getBarThickness();
		DecimalFormat defaultFormatter = new DecimalFormat("#.######");
		JLabel label = new JLabel("Thickness ");
		fieldThickness = new JFormattedTextField(defaultFormatter);
		fieldThickness.setValue(oldThickness);

		// Create a panel for labels and fields
		JPanel pnlLabelsAndFields = new JPanel(new GridLayout(0, 2));
		pnlLabelsAndFields.add(label);
		pnlLabelsAndFields.add(fieldThickness);

		// Create buttons
		JPanel pnlButtons = new JPanel();
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);

		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		// Put everything into a main panel
		JPanel pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(pnlAll);

		pnlAll.add(pnlLabelsAndFields, BorderLayout.CENTER);
		pnlAll.add(pnlButtons, BorderLayout.SOUTH);

		pack();

		setTitle("Please set value of thickness");
		setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == btnOK) {
			Number numberValue = (Number) fieldThickness.getValue();
			double newThickness = numberValue.doubleValue();
			SpectraVisualizerParameters.setBarThickness(newThickness);
			plot.thicknessUpdated();
		}

		dispose();
	}

}
