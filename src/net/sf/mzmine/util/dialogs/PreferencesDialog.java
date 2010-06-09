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

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZminePreferences;
import net.sf.mzmine.util.GUIUtils;

public class PreferencesDialog extends JDialog implements ActionListener {

	public static final int TEXTFIELD_COLUMNS = 3;

	// Dialog controls
	private JButton btnOK, btnCancel;
	private JTextField manualNumberField;
	private JRadioButton setAutoButton, setManuallyButton;
	private JTextField proxyAddressField, proxyPortField;
	private JCheckBox proxyBox;

	/**
	 * Constructor
	 */
	public PreferencesDialog() {

		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), "Preferences", true);

		JLabel mainLabel = new JLabel("Number of concurrently running tasks");

		setAutoButton = new JRadioButton("Set to the number of CPUs ("
				+ Runtime.getRuntime().availableProcessors() + ")");
		setAutoButton.addActionListener(this);

		setManuallyButton = new JRadioButton("Set manually: ");
		setManuallyButton.addActionListener(this);

		ButtonGroup radioButtons = new ButtonGroup();
		radioButtons.add(setAutoButton);
		radioButtons.add(setManuallyButton);

		JPanel setAutoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		setAutoPanel.add(setAutoButton);

		JPanel setManuallyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		setManuallyPanel.add(setManuallyButton);
		manualNumberField = new JTextField(TEXTFIELD_COLUMNS);
		setManuallyPanel.add(manualNumberField);

		// Create panel with controls
		JPanel pnlLabelsAndFields = new JPanel(new GridLayout(3, 1));
		pnlLabelsAndFields.add(mainLabel);
		pnlLabelsAndFields.add(setAutoPanel);
		pnlLabelsAndFields.add(setManuallyPanel);


		// Proxy options 
		JLabel proxyLabel = new JLabel("Internet connection settings");
		proxyBox = new JCheckBox("Connection through proxy");
		proxyBox.addActionListener(this);

		JPanel proxyAddressPanel = new JPanel(new GridLayout(2, 2));
		JLabel proxyAddressLabel = new JLabel("Proxy address:");
		proxyAddressField = new JTextField();
		JLabel proxyPortLabel = new JLabel("Proxy port:");
		proxyPortField = new JTextField();
		proxyAddressPanel.add(proxyAddressLabel);
		proxyAddressPanel.add(proxyAddressField);		
		proxyAddressPanel.add(proxyPortLabel);
		proxyAddressPanel.add(proxyPortField);

		JPanel proxyPanel = new JPanel(new GridLayout(3, 1));
		proxyPanel.add(proxyLabel);
		proxyPanel.add(proxyBox);
		proxyPanel.add(proxyAddressPanel);
		

		// Create buttons
		JPanel pnlButtons = new JPanel();
		btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
		btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);

		// Put everything into a main panel
		JPanel pnlAll = new JPanel(new BorderLayout());
		GUIUtils.addMargin(pnlAll, 10);
		pnlAll.add(pnlLabelsAndFields, BorderLayout.NORTH);
		pnlAll.add(proxyPanel, BorderLayout.CENTER);
		pnlAll.add(pnlButtons, BorderLayout.SOUTH);
		add(pnlAll);

		// Set values
		MZminePreferences preferences = MZmineCore.getPreferences();
		manualNumberField.setText(String.valueOf(preferences
				.getManualNumberOfThreads()));
		if (preferences.isAutoNumberOfThreads()) {
			setAutoButton.setSelected(true);
			manualNumberField.setEnabled(false);
		} else {
			setManuallyButton.setSelected(true);
		}

		if(preferences.isProxy()){
			proxyBox.setSelected(true);
		}
		proxyAddressField.setText(preferences.getProxyAddress());
		proxyPortField.setText(preferences.getProxyPort());
		
		proxyAddressField.setEnabled(proxyBox.isSelected());
		proxyPortField.setEnabled(proxyBox.isSelected());

		pack();

		setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == setAutoButton) {
			manualNumberField.setEnabled(false);
		}

		if (src == setManuallyButton) {
			manualNumberField.setEnabled(true);
		}

		if (src == btnOK) {

			MZminePreferences preferences = MZmineCore.getPreferences();

			preferences.setAutoNumberOfThreads(setAutoButton.isSelected());
			int manualNumberOfThreads = Integer.parseInt(manualNumberField
					.getText());
			preferences.setManualNumberOfThreads(manualNumberOfThreads);

			preferences.setProxy(proxyBox.isSelected());
			preferences.setProxyAddress(proxyAddressField.getText());
			preferences.setProxyPort(proxyPortField.getText());

			dispose();
		}

		if (src == btnCancel) {
			dispose();
		}

		if (src == proxyBox){
			proxyAddressField.setEnabled(proxyBox.isSelected());
			proxyPortField.setEnabled(proxyBox.isSelected());
		}

	}

}
