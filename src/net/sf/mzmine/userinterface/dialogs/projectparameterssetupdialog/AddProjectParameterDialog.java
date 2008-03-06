/*
 * Copyright 2006-2007 The MZmine Development Team
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
package net.sf.mzmine.userinterface.dialogs.projectparameterssetupdialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;

public class AddProjectParameterDialog extends JDialog implements
		ActionListener {

	private JPanel panelAddNewParameter;
	private JLabel labelAddNewParameter;
	private JPanel panelParameterInformation;
	private JPanel panelName;
	private JLabel labelName;
	private JTextField fieldName;
	private JPanel panelFields;
	private ButtonGroup buttongroupType;
	private JRadioButton radiobuttonNumerical;
	private JRadioButton radiobuttonFreeText;
	private JRadioButton radiobuttonCategorical;
	private JPanel panelNumericalAndFreeText;
	private JPanel panelNumerical;
	private JPanel panelNumericalFields;
	private JLabel labelNumericalMinValue;
	private JFormattedTextField fieldNumericalMinValue;
	private JLabel labelNumericalDefaultValue;
	private JFormattedTextField fieldNumericalDefaultValue;
	private JLabel labelNumericalMaxValue;
	private JFormattedTextField fieldNumericalMaxValue;
	private JPanel panelFreeText;
	private JPanel panelFreeTextFields;
	private JLabel labelFreeTextDefaultValue;
	private JTextField fieldFreeTextDefaultValue;
	private JPanel panelCategorical;
	private JPanel panelCategoricalFields;
	private JScrollPane scrollCategories;
	private JList listCategories;
	private JPanel panelAddCategoryButtons;
	private JButton buttonAddCategory;
	private JButton buttonRemoveCategory;
	private JPanel panelAddCancelButtons;
	private JButton buttonAddParameter;
	private JButton buttonCancel;

	private ProjectParametersSetupDialog mainDialog;

	private DefaultListModel categories;

	public AddProjectParameterDialog(
			ProjectParametersSetupDialog mainDialog) {

		super(mainDialog, true);

		setTitle("Define new project parameter");

		this.mainDialog = mainDialog;

		categories = new DefaultListModel();
		initComponents();

		radiobuttonNumerical.setSelected(true);
		switchNumericalFields(true);
		switchFreeTextFields(false);
		switchCategoricalFields(false);

		pack();

		setLocationRelativeTo(mainDialog);

	}

	public void initComponents() {
		panelAddNewParameter = new JPanel(new BorderLayout());

		panelParameterInformation = new JPanel(new BorderLayout());

		labelAddNewParameter = new JLabel("Add experimental parameter");

		panelName = new JPanel(new FlowLayout(FlowLayout.LEFT));
		labelName = new JLabel("Name");
		fieldName = new JTextField(25);
		panelName.add(labelName);
		panelName.add(fieldName);

		buttongroupType = new ButtonGroup();
		radiobuttonNumerical = new JRadioButton("Numerical values");
		radiobuttonFreeText = new JRadioButton("Free text");
		radiobuttonCategorical = new JRadioButton("Set of values");
		radiobuttonNumerical.addActionListener(this);
		radiobuttonFreeText.addActionListener(this);
		radiobuttonCategorical.addActionListener(this);
		buttongroupType.add(radiobuttonNumerical);
		buttongroupType.add(radiobuttonFreeText);
		buttongroupType.add(radiobuttonCategorical);

		// Fields for different types of parameters
		panelFields = new JPanel(new GridLayout(1, 2));

		panelNumericalAndFreeText = new JPanel(new BorderLayout());

		// Min, default and max for numerical
		panelNumerical = new JPanel(new BorderLayout());

		panelNumericalFields = new JPanel(new GridLayout(3, 2, 5, 2));
		labelNumericalMinValue = new JLabel("Minimum value");
		fieldNumericalMinValue = new JFormattedTextField(NumberFormat
				.getNumberInstance());
		labelNumericalDefaultValue = new JLabel("Default value");
		fieldNumericalDefaultValue = new JFormattedTextField(NumberFormat
				.getNumberInstance());
		labelNumericalMaxValue = new JLabel("Maximum value");
		fieldNumericalMaxValue = new JFormattedTextField(NumberFormat
				.getNumberInstance());
		panelNumericalFields.add(labelNumericalMinValue);
		panelNumericalFields.add(fieldNumericalMinValue);
		panelNumericalFields.add(labelNumericalDefaultValue);
		panelNumericalFields.add(fieldNumericalDefaultValue);
		panelNumericalFields.add(labelNumericalMaxValue);
		panelNumericalFields.add(fieldNumericalMaxValue);
		panelNumericalFields.setBorder(BorderFactory.createEmptyBorder(5, 5, 5,
				5));

		panelNumerical.add(radiobuttonNumerical, BorderLayout.NORTH);
		panelNumerical.add(panelNumericalFields, BorderLayout.CENTER);
		panelNumerical.setBorder(BorderFactory.createEtchedBorder());

		panelFreeText = new JPanel(new BorderLayout());
		panelFreeText.setPreferredSize(panelNumerical.getPreferredSize());
		panelFreeText.setBorder(BorderFactory.createEtchedBorder());

		panelFreeTextFields = new JPanel(new GridLayout(3, 2, 5, 2));
		labelFreeTextDefaultValue = new JLabel("Default value");
		fieldFreeTextDefaultValue = new JTextField();
		panelFreeTextFields.add(labelFreeTextDefaultValue);
		panelFreeTextFields.add(fieldFreeTextDefaultValue);
		panelFreeTextFields.add(new JPanel());
		panelFreeTextFields.add(new JPanel());
		panelFreeTextFields.add(new JPanel());
		panelFreeTextFields.add(new JPanel());
		panelFreeTextFields.setBorder(BorderFactory.createEmptyBorder(5, 5, 5,
				5));

		panelFreeText.add(radiobuttonFreeText, BorderLayout.NORTH);
		panelFreeText.add(panelFreeTextFields, BorderLayout.CENTER);

		panelNumericalAndFreeText.add(panelNumerical, BorderLayout.NORTH);
		panelNumericalAndFreeText.add(panelFreeText, BorderLayout.SOUTH);

		panelCategorical = new JPanel(new BorderLayout());

		// List of values for categorical
		panelCategoricalFields = new JPanel(new BorderLayout());
		scrollCategories = new JScrollPane();
		listCategories = new JList(categories);
		scrollCategories.setViewportView(listCategories);
		panelAddCategoryButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonAddCategory = new JButton("Add value");
		buttonAddCategory.addActionListener(this);
		buttonRemoveCategory = new JButton("Remove value");
		buttonRemoveCategory.addActionListener(this);
		panelAddCategoryButtons.add(buttonAddCategory);
		panelAddCategoryButtons.add(buttonRemoveCategory);
		panelCategoricalFields.add(scrollCategories, BorderLayout.CENTER);
		panelCategoricalFields.add(panelAddCategoryButtons, BorderLayout.SOUTH);
		panelCategoricalFields.setBorder(BorderFactory.createEmptyBorder(5, 5,
				5, 5));

		panelCategorical.add(radiobuttonCategorical, BorderLayout.NORTH);
		panelCategorical.add(panelCategoricalFields, BorderLayout.CENTER);
		panelCategorical.setBorder(BorderFactory.createEtchedBorder());

		panelFields.add(panelNumericalAndFreeText);
		panelFields.add(panelCategorical);

		panelParameterInformation.add(panelName, BorderLayout.NORTH);
		panelParameterInformation.add(panelFields, BorderLayout.CENTER);

		panelAddCancelButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonAddParameter = new JButton("Add parameter");
		buttonAddParameter.addActionListener(this);
		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);
		panelAddCancelButtons.add(buttonAddParameter);
		panelAddCancelButtons.add(buttonCancel);

		panelAddNewParameter.add(labelAddNewParameter, BorderLayout.NORTH);
		panelAddNewParameter
				.add(panelParameterInformation, BorderLayout.CENTER);
		panelAddNewParameter.add(panelAddCancelButtons, BorderLayout.SOUTH);
		panelAddNewParameter.setBorder(BorderFactory.createEmptyBorder(5, 5,
				25, 5));

		add(panelAddNewParameter, BorderLayout.CENTER);

	}

	private void switchNumericalFields(boolean enabled) {
		labelNumericalMinValue.setEnabled(enabled);
		fieldNumericalMinValue.setEnabled(enabled);
		labelNumericalDefaultValue.setEnabled(enabled);
		fieldNumericalDefaultValue.setEnabled(enabled);
		labelNumericalMaxValue.setEnabled(enabled);
		fieldNumericalMaxValue.setEnabled(enabled);
	}

	private void switchCategoricalFields(boolean enabled) {
		listCategories.setEnabled(enabled);
		buttonAddCategory.setEnabled(enabled);
		buttonRemoveCategory.setEnabled(enabled);
	}

	private void switchFreeTextFields(boolean enabled) {
		labelFreeTextDefaultValue.setEnabled(enabled);
		fieldFreeTextDefaultValue.setEnabled(enabled);
	}

	@Override
	public void actionPerformed(ActionEvent actionEvent) {

		Object src = actionEvent.getSource();

		Desktop desktop = MZmineCore.getDesktop();

		if (src == buttonAddParameter) {
			if (fieldName.getText().length() == 0) {
				desktop
						.displayErrorMessage("Give a name for the parameter first.");
				return;
			}
			String paramName = fieldName.getText();

			SimpleParameter parameter = null;

			if (radiobuttonNumerical.isSelected()) {
				ParameterType paramType = ParameterType.FLOAT;
				Double minValue = Double.NEGATIVE_INFINITY;
				if (fieldNumericalMinValue.getValue() != null)
					minValue = ((Number) fieldNumericalMinValue.getValue())
							.doubleValue();

				Double defaultValue = 0.0;
				if (fieldNumericalDefaultValue.getValue() != null) {
					defaultValue = ((Number) fieldNumericalDefaultValue
							.getValue()).doubleValue();
				}

				Double maxValue = Double.POSITIVE_INFINITY;
				if (fieldNumericalMaxValue.getValue() != null)
					maxValue = ((Number) fieldNumericalMaxValue.getValue())
							.doubleValue();

				parameter = new SimpleParameter(paramType, paramName, null,
						null, defaultValue, minValue, maxValue);
			}

			if (radiobuttonFreeText.isSelected()) {
				ParameterType paramType = ParameterType.STRING;
				String defaultValue = "";
				if (fieldFreeTextDefaultValue.getText() != null)
					defaultValue = fieldFreeTextDefaultValue.getText();
				parameter = new SimpleParameter(paramType, paramName, null,
						(Object) defaultValue);
			}

			if (radiobuttonCategorical.isSelected()) {
				ParameterType paramType = ParameterType.STRING;
				String[] possibleValues = new String[categories.size()];
				if (possibleValues.length == 0) {
					desktop
							.displayErrorMessage("Give at least a single parameter value.");
					return;
				}
				for (int valueIndex = 0; valueIndex < categories.size(); valueIndex++)
					possibleValues[valueIndex] = (String) categories
							.get(valueIndex);
				parameter = new SimpleParameter(paramType, paramName, null,
						possibleValues[0], possibleValues);
			}

			mainDialog.addParameter(parameter);

			dispose();
		}

		if (src == buttonCancel) {
			dispose();
		}

		if ((src == radiobuttonNumerical) || (src == radiobuttonCategorical)
				|| (src == radiobuttonFreeText)) {
			if (radiobuttonNumerical.isSelected()) {
				switchNumericalFields(true);
				switchCategoricalFields(false);
				switchFreeTextFields(false);
			}
			if (radiobuttonFreeText.isSelected()) {
				switchNumericalFields(false);
				switchCategoricalFields(false);
				switchFreeTextFields(true);
			}
			if (radiobuttonCategorical.isSelected()) {
				switchNumericalFields(false);
				switchCategoricalFields(true);
				switchFreeTextFields(false);
			}
		}

		if (src == buttonAddCategory) {
			String inputValue = JOptionPane
					.showInputDialog("Please input a new value");
			if (((DefaultListModel) listCategories.getModel())
					.contains(inputValue)) {
				desktop.displayErrorMessage("Value already exists.");
				return;
			}
			((DefaultListModel) listCategories.getModel())
					.addElement(inputValue);
		}

		if (src == buttonRemoveCategory) {

			int[] selectedIndices = listCategories.getSelectedIndices();
			if ((selectedIndices == null) || (selectedIndices.length == 0)) {
				desktop.displayErrorMessage("Select at least one value first.");
			}

			for (int selectedIndex : selectedIndices) {
				((DefaultListModel) listCategories.getModel())
						.removeElementAt(selectedIndex);
			}

		}

	}

}
