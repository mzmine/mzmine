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
package net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.filtering.datasetfilters.preview.RawDataFilterSetupDialog;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 
 */
class RawDataFilteringSetupDialog extends JDialog implements ActionListener {

	private RawDataFilteringParameters parameters;
	private ExitCode exitCode = ExitCode.UNKNOWN;
	private String title;

	// Dialog components
	private JButton btnOK,  btnCancel,  btnHelp,  btnSetFilter;
	private JComboBox comboRawDataFilters;
	private JTextField txtField;
	private JCheckBox checkBoxAutoRemove;

	public RawDataFilteringSetupDialog(String title,
			RawDataFilteringParameters parameters) {

		super(MZmineCore.getDesktop().getMainFrame(),
				"Please select raw data filter", true);

		this.parameters = parameters;
		this.title = title;

		addComponentsToDialog();
		this.setResizable(false);
	}

	public ExitCode getExitCode() {
		return exitCode;
	}

	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == btnSetFilter) {
			int ind = comboRawDataFilters.getSelectedIndex();            
			RawDataFilterSetupDialog dialog = new RawDataFilterSetupDialog(
					parameters, ind);
			dialog.setVisible(true);

		}
		if (src == btnOK) {
			inform();
			parameters.setTypeNumber(comboRawDataFilters.getSelectedIndex());
			parameters.setSuffix(txtField.getText());
			exitCode = ExitCode.OK;
			dispose();
		}

		if (src == btnCancel) {
			exitCode = ExitCode.CANCEL;
			dispose();
		}

		if (src == checkBoxAutoRemove) {
			parameters.setAutoRemove(checkBoxAutoRemove.isSelected());
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

		// Elements of Raw Data Filter
		comboRawDataFilters = new JComboBox(
				RawDataFilteringParameters.rawDataFilterNames);
		comboRawDataFilters.setSelectedIndex(parameters.getRawDataFilterTypeNumber());
		comboRawDataFilters.addActionListener(this);
		comboRawDataFilters.setMaximumSize(new Dimension(200, 30));
		btnSetFilter = new JButton("Set parameters");
		btnSetFilter.addActionListener(this);

		// Elements of buttons
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		btnHelp = new HelpButton(
				"net/sf/mzmine/modules/peakpicking/threestep/help/ThreeStepsDetector.html");

		// Elements of autoRemove checkBox
		checkBoxAutoRemove = new JCheckBox();
		checkBoxAutoRemove.setSelected(parameters.getAutoRemove());
		checkBoxAutoRemove.addActionListener(this);

		JPanel pnlCombo = new JPanel();
		pnlCombo.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 10.0;
		c.weightx = 10.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.gridx = 0;
		c.gridy = 0;
		pnlCombo.add(new JLabel("Filename suffix"), c);
		c.gridwidth = 4;
		c.gridx = 1;
		pnlCombo.add(txtField, c);

		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		pnlCombo.add(new JLabel("Scan by Scan Filter"), c);
		c.gridwidth = 3;
		c.gridx = 1;
		pnlCombo.add(comboRawDataFilters, c);
		c.gridwidth = 1;
		c.gridx = 4;
		pnlCombo.add(btnSetFilter, c);

		c.gridx = 0;
		c.gridy = 2;
		pnlCombo.add(new JLabel("Remove original file"), c);
		c.gridwidth = 3;
		c.gridx = 1;
		pnlCombo.add(checkBoxAutoRemove, c);
		c.gridwidth = 1;
		c.gridx = 4;

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
		boolean notMsLevelOne = false;

		if (dataFiles.length != 0) {
			for (int i = 0; i < dataFiles.length; i++) {

				int msLevels[] = dataFiles[i].getMSLevels();

				if (msLevels[0] != 1) {
					notMsLevelOne = true;
					break;
				}
			}

			if (notMsLevelOne) {
				desktop.displayMessage(" One or more selected files does not contain spectrum of MS level 1." + " The actual raw data filter only works over spectrum of this level.");
			}
		}
	}
}
