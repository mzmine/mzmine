/*
 * Copyright 2006-2008 The MZmine Development Team
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


package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;

public class ThicknessSetupDialog extends JDialog implements ActionListener {

	private JTextField fieldThickness;
	private JButton btnOK, btnApply, btnCancel;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private SpectraPlot plot;
	private double oldThickness;
	private static double minThickness = 0.000010f;

	public ThicknessSetupDialog(SpectraPlot plot) {
		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), true);

		this.plot = plot;
		oldThickness = plot.getThicknessBar();
		// NumberFormat defaultFormatter = NumberFormat.getNumberInstance();
		JLabel label = new JLabel("Thickness ");
		fieldThickness = new JTextField(Double.toString(oldThickness));

		// Create a panel for labels and fields
		JPanel pnlLabelsAndFields = new JPanel(new GridLayout(0, 2));
		pnlLabelsAndFields.add(label);
		pnlLabelsAndFields.add(fieldThickness);

		// Create buttons
		JPanel pnlButtons = new JPanel();
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnApply = new JButton("Apply");
		btnApply.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);

		pnlButtons.add(btnOK);
		pnlButtons.add(btnApply);
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
			if (setValuesToPlot()) {
				dispose();
			}
		}

		if (src == btnApply) {
			setValuesToPlot();
		}

		if (src == btnCancel) {
			plot.setThicknessBar(oldThickness);
			dispose();
		}
	}

	private boolean setValuesToPlot() {

		double thickness = Double.parseDouble(fieldThickness.getText());

		if (thickness > minThickness) {
			plot.setThicknessBar(thickness);
		} else {
			displayMessage("Invalid value for thickness ");
			return false;
		}

		return true;
	}

	private void displayMessage(String msg) {
		try {
			logger.info(msg);
			JOptionPane.showMessageDialog(this, msg, "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (Exception exce) {
		}
	}

}
