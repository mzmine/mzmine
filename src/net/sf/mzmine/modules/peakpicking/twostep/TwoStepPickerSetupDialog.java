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
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.Desktop;
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
	private String title;

	// Dialog components
	private JButton btnOK, btnCancel, btnSetMass, btnSetPeak;
	private JComboBox comboMassDetectors, comboPeaksConstructors;
	private JTextField txtField;

	public TwoStepPickerSetupDialog(String title,
			TwoStepPickerParameters parameters) {

		super(MZmineCore.getDesktop().getMainFrame(),
				"Please select mass detector  & peak builder", true);

		this.parameters = parameters;
		this.title = title;

		addComponentsToDialog();
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
			inform();
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

	/**
	 * This function add all components for this dialog
	 * 
	 */
	private void addComponentsToDialog() {
		// Elements of panel1
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel lblLabel = new JLabel("Filename suffix ");
		lblLabel.setSize(200, 28);
		txtField = new JTextField();
		txtField.setText(parameters.getSuffix());
		txtField.selectAll();
		txtField.setMaximumSize(new Dimension(250, 30));

		panel1.add(lblLabel);
		panel1.add(Box.createRigidArea(new Dimension(10, 10)));
		panel1.add(txtField);
		panel1.add(Box.createRigidArea(new Dimension(10, 10)));

		// Elements of panel2
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
		panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel lblMassDetectors = new JLabel("Mass Detector");
		lblMassDetectors.setSize(200, 28);
		comboMassDetectors = new JComboBox(
				TwoStepPickerParameters.massDetectorNames);
		comboMassDetectors.setSelectedIndex(parameters
				.getMassDetectorTypeNumber());
		comboMassDetectors.addActionListener(this);
		comboMassDetectors.setMaximumSize(new Dimension(200, 30));
		btnSetMass = new JButton("Set parameters");
		btnSetMass.addActionListener(this);

		panel2.add(lblMassDetectors);
		panel2.add(Box.createRigidArea(new Dimension(10, 10)));
		panel2.add(comboMassDetectors);
		panel2.add(Box.createRigidArea(new Dimension(10, 10)));
		panel2.add(btnSetMass);
		panel2.add(Box.createRigidArea(new Dimension(10, 10)));

		// Elements of panel2
		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));
		panel3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel lblPeakBuilder = new JLabel("Peak Builder");
		lblPeakBuilder.setMaximumSize(new Dimension(200, 30));
		comboPeaksConstructors = new JComboBox(
				TwoStepPickerParameters.peakBuilderNames);
		comboPeaksConstructors.setSelectedIndex(parameters
				.getPeakBuilderTypeNumber());
		comboPeaksConstructors.addActionListener(this);
		comboPeaksConstructors.setMaximumSize(new Dimension(200, 28));
		btnSetPeak = new JButton("Set parameters");
		btnSetPeak.addActionListener(this);

		panel3.add(lblPeakBuilder);
		panel3.add(Box.createRigidArea(new Dimension(10, 10)));
		panel3.add(comboPeaksConstructors);
		panel3.add(Box.createRigidArea(new Dimension(10, 10)));
		panel3.add(btnSetPeak);
		panel3.add(Box.createRigidArea(new Dimension(10, 10)));

		// Elements of pnlCombo
		JPanel pnlCombo = new JPanel(new BorderLayout());
		pnlCombo.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		pnlCombo.add(panel1, BorderLayout.NORTH);
		pnlCombo.add(panel2, BorderLayout.CENTER);
		pnlCombo.add(panel3, BorderLayout.SOUTH);

		// Elements of pnlButtons
		JPanel pnlButtons = new JPanel();

		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);

		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		// Panel where everything is collected
		JPanel pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnlAll.add(pnlCombo, BorderLayout.CENTER);
		pnlAll.add(pnlButtons, BorderLayout.SOUTH);
		add(pnlAll);

		pack();
		setTitle(title);
		//setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
		
	}
	
	/**
	 * 
	 */
	private void inform() {

		Desktop desktop = MZmineCore.getDesktop();
		RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
		int massDetectorNumber = comboMassDetectors.getSelectedIndex();
		String massDetectorName = TwoStepPickerParameters.massDetectorNames[massDetectorNumber];
		boolean centroid = false;
		boolean notMsLevelOne = false;

		if (dataFiles.length != 0) {
			for (int i = 0; i < dataFiles.length; i++) {
				
				int msLevels[] = dataFiles[i].getMSLevels();
				Arrays.sort(msLevels);
				
				if (msLevels[0] != 1){
					notMsLevelOne = true;
					break;
				}
				
				int index = dataFiles[i].getScanNumbers(1)[0];
				Scan scan = dataFiles[i].getScan(index);
				
				if (scan.isCentroided()){
					centroid = true;
					break;
				}
			}

			if (notMsLevelOne) {
				desktop
						.displayMessage(" One or more selected files does not contain spectrum of MS level \"1\".\n"
								+ " The actual mass detector only works over spectrum of this level.");
			}

			if ((centroid) && (!massDetectorName.equals("Centroid"))) {
				desktop
						.displayMessage(" One or more selected files contains centroided data points.\n"
								+ " The actual mass detector could give an unexpected result ");
			}

			if ((!centroid) && (massDetectorName.equals("Centroid"))) {
				desktop
						.displayMessage(" Neither one of the selected files contains centroided data points.\n"
								+ " The actual mass detector could give an unexpected result ");
			}
		}
	}

}
