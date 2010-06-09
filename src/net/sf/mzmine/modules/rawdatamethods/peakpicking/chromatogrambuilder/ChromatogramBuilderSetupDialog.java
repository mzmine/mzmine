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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
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
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massconnection.MassConnector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massdetection.MassDetectorSetupDialog;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massfilters.MassFilter;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massfilters.MassFilterSetupDialog;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 
 */
class ChromatogramBuilderSetupDialog extends JDialog implements ActionListener {

	final String helpID = this.getClass().getPackage().getName().replace('.',
			'/')
			+ "/help/ChromatogramBuilder.html";

	private ChromatogramBuilderParameters parameters;
	private ExitCode exitCode = ExitCode.UNKNOWN;
	private String title;

	// Dialog components
	private JButton btnOK, btnCancel, btnHelp, btnSetMass, btnSetFilter,
			btnSetConnector;
	private JComboBox comboMassDetectors, comboMassFilters,
			comboMassConnectors;
	private JTextField txtField;

	private MassDetector massDetectors[];
	private MassFilter massFilters[];
	private MassConnector massConnectors[];

	public ChromatogramBuilderSetupDialog(String title,
			ChromatogramBuilderParameters parameters) {

		super(MZmineCore.getDesktop().getMainFrame(),
				"Please select mass detector & connector", true);

		this.parameters = parameters;
		this.title = title;

		this.massDetectors = parameters.getMassDetectors();
		this.massFilters = parameters.getMassFilters();
		this.massConnectors = parameters.getMassConnectors();

		addComponentsToDialog();
		this.setResizable(false);
	}

	public ExitCode getExitCode() {
		return exitCode;
	}

	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == btnSetMass) {
			MassDetector detector = (MassDetector) comboMassDetectors
					.getSelectedItem();
			if (detector == null)
				return;
			MassDetectorSetupDialog dialog = new MassDetectorSetupDialog(
					detector);
			dialog.setVisible(true);
		}

		if (src == btnSetFilter) {
			MassDetector detector = (MassDetector) comboMassDetectors
					.getSelectedItem();
			if (detector == null)
				return;
			int ind = comboMassFilters.getSelectedIndex() - 1;
			if (ind < 0)
				return;
			MassFilterSetupDialog dialog = new MassFilterSetupDialog(detector,
					massFilters[ind]);
			dialog.setVisible(true);

		}

		if (src == btnSetConnector) {
			MassConnector connector = (MassConnector) comboMassConnectors
					.getSelectedItem();
			if (connector == null)
				return;
			ParameterSetupDialog dialog = new ParameterSetupDialog(connector
					.getName()
					+ "'s parameter setup dialog ", connector.getParameters());

			dialog.setVisible(true);
		}

		if (src == btnOK) {
			inform();
			parameters.setTypeNumber(comboMassDetectors.getSelectedIndex(),
					comboMassFilters.getSelectedIndex() - 1,
					comboMassConnectors.getSelectedIndex());
			parameters.setSuffix(txtField.getText());
			exitCode = ExitCode.OK;
			dispose();
		}

		if (src == btnCancel) {
			exitCode = ExitCode.CANCEL;
			dispose();
		}

		if (src == comboMassFilters) {
			int ind = comboMassFilters.getSelectedIndex();
			btnSetFilter.setEnabled(ind > 0);
		}

	}

	/**
	 * This function add all components for this dialog
	 * 
	 */
	private void addComponentsToDialog() {

		// Elements of suffix
		txtField = new JTextField();
		txtField.setText(parameters.getSuffix());
		txtField.selectAll();
		txtField.setMaximumSize(new Dimension(250, 30));

		// Elements of Mass detector
		comboMassDetectors = new JComboBox(massDetectors);
		comboMassDetectors.setSelectedItem(parameters.getMassDetector());
		comboMassDetectors.addActionListener(this);
		comboMassDetectors.setMaximumSize(new Dimension(200, 30));
		btnSetMass = new JButton("Set parameters");
		btnSetMass.addActionListener(this);

		// Elements of mass connector
		comboMassFilters = new JComboBox(massFilters);
		comboMassFilters.insertItemAt("None", 0);
		MassFilter selectedFilter = parameters.getMassFilter();
		if (selectedFilter != null) {
			comboMassFilters.setSelectedItem(selectedFilter);
		} else {
			comboMassFilters.setSelectedIndex(0);
		}
		comboMassFilters.addActionListener(this);
		comboMassFilters.setMaximumSize(new Dimension(200, 30));
		btnSetFilter = new JButton("Set parameters");
		btnSetFilter.addActionListener(this);
		btnSetFilter.setEnabled(selectedFilter != null);

		// Elements of mass connector
		comboMassConnectors = new JComboBox(massConnectors);
		comboMassConnectors.setSelectedItem(parameters.getMassConnector());
		comboMassConnectors.addActionListener(this);
		comboMassConnectors.setMaximumSize(new Dimension(200, 30));
		btnSetConnector = new JButton("Set parameters");
		btnSetConnector.addActionListener(this);

		// Elements of buttons
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		btnHelp = new HelpButton(helpID);

		JPanel pnlCombo = new JPanel();
		pnlCombo.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 10.0;
		c.weightx = 10.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.gridx = 0;
		c.gridy = 0;
		pnlCombo.add(new JLabel("Filename suffix "), c);
		c.gridwidth = 4;
		c.gridx = 1;
		pnlCombo.add(txtField, c);

		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		pnlCombo.add(new JLabel("Mass detection"), c);
		c.gridwidth = 3;
		c.gridx = 1;
		pnlCombo.add(comboMassDetectors, c);
		c.gridwidth = 1;
		c.gridx = 4;
		pnlCombo.add(btnSetMass, c);

		c.gridx = 0;
		c.gridy = 2;
		pnlCombo.add(new JLabel("Filtering"), c);
		c.gridwidth = 3;
		c.gridx = 1;
		pnlCombo.add(comboMassFilters, c);
		c.gridwidth = 1;
		c.gridx = 4;
		pnlCombo.add(btnSetFilter, c);

		c.gridx = 0;
		c.gridy = 3;
		pnlCombo
				.add(new JLabel("<HTML>Chromatogram<BR>construction</HTML>"), c);
		c.gridwidth = 3;
		c.gridx = 1;
		pnlCombo.add(comboMassConnectors, c);
		c.gridwidth = 1;
		c.gridx = 4;
		pnlCombo.add(btnSetConnector, c);

		c.gridx = 1;
		c.gridy = 4;
		pnlCombo.add(btnOK, c);
		c.gridx = 2;
		pnlCombo.add(btnCancel, c);
		c.gridx = 3;
		pnlCombo.add(btnHelp, c);

		// Panel where everything is collected
		JPanel pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnlAll.add(pnlCombo, BorderLayout.CENTER);
		add(pnlAll);

		pack();
		setTitle(title);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
     * 
     */
	private void inform() {

		Desktop desktop = MZmineCore.getDesktop();
		RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
		MassDetector md = (MassDetector) comboMassDetectors.getSelectedItem();
		String massDetectorName = md.getName();
		boolean centroid = false;
		boolean notMsLevelOne = false;

		if (dataFiles.length != 0) {
			for (int i = 0; i < dataFiles.length; i++) {

				int msLevels[] = dataFiles[i].getMSLevels();

				if (msLevels[0] != 1) {
					notMsLevelOne = true;
					break;
				}

				Scan scan;
				int[] indexArray = dataFiles[i].getScanNumbers(1);
				int increment = indexArray.length / 10;

				// Verify if the current DataFile contains centroided scans
				for (int j = 0; j < indexArray.length; j += increment) {
					scan = dataFiles[i].getScan(indexArray[j]);
					if (scan.isCentroided()) {
						centroid = true;
						break;
					}
				}
			}

			if (notMsLevelOne) {
				desktop
						.displayMessage("One or more selected files does not contain spectrum of MS level 1."
								+ " The actual mass detector only works over spectrum of this level.");
			}

			if ((centroid) && (!massDetectorName.startsWith("Centroid"))) {
				desktop
						.displayMessage("One or more selected files contains centroided data points."
								+ " The actual mass detector could give an unexpected result");
			}

			if ((!centroid) && (massDetectorName.startsWith("Centroid"))) {
				desktop
						.displayMessage("Neither one of the selected files contains centroided data points."
								+ " The actual mass detector could give an unexpected result");
			}
		}
	}

}
