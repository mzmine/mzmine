/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.NumberFormatter.FormatterType;
import net.sf.mzmine.util.components.HelpButton;

public class FormatSetupDialog extends JDialog implements ActionListener {

	public static final int TEXTFIELD_COLUMNS = 12;
	
	final String helpID = GUIUtils.generateHelpID(this);

	// NumberFormatter instances
	private NumberFormatter mzFormat, rtFormat, intensityFormat;

	// Dialog controls
	private JButton btnOK, btnCancel, btnHelp;
	private JTextField mzFormatField, rtFormatField, intensityFormatField;
	private JRadioButton timeRadioButton, numberRadioButton;

	/**
	 * Constructor
	 */
	public FormatSetupDialog() {

		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), "Format setting", true);

		this.mzFormat = MZmineCore.getMZFormat();
		this.rtFormat = MZmineCore.getRTFormat();
		this.intensityFormat = MZmineCore.getIntensityFormat();

		JLabel mzFormatLabel = new JLabel("m/z format");
		JLabel rtFormatLabel = new JLabel("Retention time format");
		JLabel intensityFormatLabel = new JLabel("Intensity format");

		mzFormatField = new JTextField(TEXTFIELD_COLUMNS);
		JPanel mzFormatFieldPanel = new JPanel();
		mzFormatFieldPanel.add(mzFormatField);
		rtFormatField = new JTextField(TEXTFIELD_COLUMNS);
		JPanel rtFormatFieldPanel = new JPanel();
		rtFormatFieldPanel.add(rtFormatField);
		intensityFormatField = new JTextField(TEXTFIELD_COLUMNS);
		JPanel intensityFormatFieldPanel = new JPanel();
		intensityFormatFieldPanel.add(intensityFormatField);

		ButtonGroup radioButtons = new ButtonGroup();
		timeRadioButton = new JRadioButton("Time");
		numberRadioButton = new JRadioButton("Number");
		radioButtons.add(timeRadioButton);
		radioButtons.add(numberRadioButton);
		JPanel radioButtonsPanel = new JPanel();
		radioButtonsPanel.add(timeRadioButton);
		radioButtonsPanel.add(numberRadioButton);

		// Create panel with controls
		JPanel pnlLabelsAndFields = new JPanel(new GridLayout(4, 2));
		pnlLabelsAndFields.add(mzFormatLabel);
		pnlLabelsAndFields.add(mzFormatFieldPanel);
		pnlLabelsAndFields.add(rtFormatLabel);
		pnlLabelsAndFields.add(radioButtonsPanel);
		pnlLabelsAndFields.add(new JLabel());
		pnlLabelsAndFields.add(rtFormatFieldPanel);
		pnlLabelsAndFields.add(intensityFormatLabel);
		pnlLabelsAndFields.add(intensityFormatFieldPanel);

		// Create buttons
		JPanel pnlButtons = new JPanel();
		btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
		btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);
		btnHelp = new HelpButton(helpID);
		pnlButtons.add(btnHelp);

		// Put everything into a main panel
		JPanel pnlAll = new JPanel(new BorderLayout());
		GUIUtils.addMargin(pnlAll, 10);
		pnlAll.add(pnlLabelsAndFields, BorderLayout.CENTER);
		pnlAll.add(pnlButtons, BorderLayout.SOUTH);
		add(pnlAll);

		// Set values
		mzFormatField.setText(mzFormat.getPattern());
		rtFormatField.setText(rtFormat.getPattern());
		intensityFormatField.setText(intensityFormat.getPattern());
		if (rtFormat.getType() == FormatterType.TIME)
			timeRadioButton.setSelected(true);
		else
			numberRadioButton.setSelected(true);

		pack();

		setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();
		Desktop desktop = MZmineCore.getDesktop();

		if (src == btnOK) {

			try {
				mzFormat.setFormat(FormatterType.NUMBER, mzFormatField
						.getText());
			} catch (IllegalArgumentException e) {
				desktop.displayErrorMessage("Error in m/z format string: "
						+ e.getMessage());
				return;
			}

			try {
				if (timeRadioButton.isSelected()) {
					rtFormat.setFormat(FormatterType.TIME, rtFormatField
							.getText());
				} else {
					rtFormat.setFormat(FormatterType.NUMBER, rtFormatField
							.getText());
				}
			} catch (IllegalArgumentException e) {
				desktop
						.displayErrorMessage("Error in retention time format string: "
								+ e.getMessage());
				return;
			}

			try {
				intensityFormat.setFormat(FormatterType.NUMBER,
						intensityFormatField.getText());
			} catch (IllegalArgumentException e) {
				desktop
						.displayErrorMessage("Error in intensity format string: "
								+ e.getMessage());
				return;
			}

			dispose();

			// repaint to update all formatted numbers
			desktop.getMainFrame().repaint();

		}

		if (src == btnCancel) {
			dispose();
		}

	}

}
