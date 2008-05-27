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

package net.sf.mzmine.modules.peakpicking.twostep;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetectorSetupDialog;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 
 */
class TwoStepPickerSetupDialog extends JDialog implements ActionListener {

	private TwoStepPickerParameters parameters;
	private ExitCode exitCode = ExitCode.UNKNOWN;

	// Buttons
	protected JButton btnOK, btnCancel, btnSetMass, btnSetPeak;

	// Panels
	private JPanel pnlCombo, pnlButtons;

	// Combo Box
	JComboBox comboMassDetectors, comboPeaksConstructors;

	// Text Fields
	JTextField txtField;

	// Derived classed may add their components to this panel
	protected JPanel pnlAll;

	public TwoStepPickerSetupDialog(String title,
			TwoStepPickerParameters parameters) {

		super(MZmineCore.getDesktop().getMainFrame(),
				"Please select mass detector  & peak builder", true);

		this.parameters = parameters;

		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JLabel lblLabel = new JLabel("Filename suffix ");
		lblLabel.setSize(200, 28);
		panel1.add(lblLabel);
		panel1.add(Box.createRigidArea(new Dimension(10, 10)));
		txtField = new JTextField();
		txtField.setText(parameters.getSuffix());
		txtField.selectAll();
		txtField.setMaximumSize(new Dimension(250, 30));
		panel1.add(txtField);
		panel1.add(Box.createRigidArea(new Dimension(10, 10)));

		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
		panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JLabel lblMassDetectors = new JLabel("Mass Detector");
		lblMassDetectors.setSize(200, 28);
		panel2.add(lblMassDetectors);
		panel2.add(Box.createRigidArea(new Dimension(10, 10)));
		comboMassDetectors = new JComboBox(
				TwoStepPickerParameters.massDetectorNames);
		comboMassDetectors.setSelectedIndex(parameters
				.getMassDetectorTypeNumber());
		comboMassDetectors.addActionListener(this);
		comboMassDetectors.setMaximumSize(new Dimension(200, 30));
		panel2.add(comboMassDetectors);
		panel2.add(Box.createRigidArea(new Dimension(10, 10)));
		btnSetMass = new JButton("Set parameters");
		btnSetMass.addActionListener(this);
		panel2.add(btnSetMass);
		panel2.add(Box.createRigidArea(new Dimension(10, 10)));

		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));
		panel3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JLabel lblPeakBuilder = new JLabel("Peak Builder");
		lblPeakBuilder.setMaximumSize(new Dimension(200, 30));
		panel3.add(lblPeakBuilder);
		panel3.add(Box.createRigidArea(new Dimension(10, 10)));
		comboPeaksConstructors = new JComboBox(
				TwoStepPickerParameters.peakBuilderNames);
		comboPeaksConstructors.setSelectedIndex(parameters
				.getPeakBuilderTypeNumber());
		comboPeaksConstructors.addActionListener(this);
		comboPeaksConstructors.setMaximumSize(new Dimension(200, 28));
		panel3.add(comboPeaksConstructors);
		panel3.add(Box.createRigidArea(new Dimension(10, 10)));
		btnSetPeak = new JButton("Set parameters");
		btnSetPeak.addActionListener(this);
		panel3.add(btnSetPeak);
		panel3.add(Box.createRigidArea(new Dimension(10, 10)));

		pnlCombo = new JPanel(new BorderLayout());
		pnlCombo.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		pnlCombo.add(panel1, BorderLayout.NORTH);
		pnlCombo.add(panel2, BorderLayout.CENTER);
		pnlCombo.add(panel3, BorderLayout.SOUTH);

		// Buttons
		pnlButtons = new JPanel();
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		// Panel where everything is collected
		pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(pnlAll);

		// Leave the BorderLayout.CENTER area empty, so that derived dialogs can
		// put their own controls in there
		// pnlAll.add(pnlSuffix, BorderLayout.NORTH);
		pnlAll.add(pnlCombo, BorderLayout.CENTER);
		pnlAll.add(pnlButtons, BorderLayout.SOUTH);

		pack();
		setTitle(title);
		setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	}

	public ExitCode getExitCode() {
		return exitCode;
	}

	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == btnSetMass) {
			int ind = comboMassDetectors.getSelectedIndex();

			MassDetectorSetupDialog dialog = new MassDetectorSetupDialog(
					parameters, ind);
			dialog.setVisible(true);

		}

		if (src == btnSetPeak) {
			int ind = comboPeaksConstructors.getSelectedIndex();
			ParameterSetupDialog dialog = new ParameterSetupDialog(
					"Please set parameter values for " + toString(), parameters
							.getPeakBuilderParameters(ind));
			dialog.setVisible(true);
		}

		if (src == btnOK) {
			parameters.setTypeNumber(comboMassDetectors.getSelectedIndex(),
					comboPeaksConstructors.getSelectedIndex());
			parameters.setSuffix(txtField.getText());
			exitCode = ExitCode.OK;
			dispose();
		}

		if (src == btnCancel) {
			exitCode = ExitCode.CANCEL;
			dispose();
		}

	}

}
