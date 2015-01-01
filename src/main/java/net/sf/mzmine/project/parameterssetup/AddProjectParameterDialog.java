/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.project.parameterssetup;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class AddProjectParameterDialog extends JDialog implements
	ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JPanel panelAddNewParameter;
    private JLabel labelAddNewParameter;
    private JPanel panelParameterInformation;
    private JPanel panelName;
    private JLabel labelName;
    private JTextField fieldName;
    private ButtonGroup buttongroupType;
    private JRadioButton radiobuttonNumerical;
    private JRadioButton radiobuttonFreeText;
    private JRadioButton radiobuttonCategorical;
    private JPanel panelNumericalAndFreeText;
    private JPanel panelNumerical;
    private JPanel panelFreeText;
    private JPanel panelCategorical;
    private JPanel panelCategoricalFields;
    private JScrollPane scrollCategories;
    private JList<String> listCategories;
    private JPanel panelAddCategoryButtons;
    private JButton buttonAddCategory;
    private JButton buttonRemoveCategory;
    private JPanel panelAddCancelButtons;
    private JButton buttonAddParameter;
    private JButton buttonCancel;

    private ProjectParametersSetupDialog mainDialog;

    private DefaultListModel<String> categories;

    public AddProjectParameterDialog(ProjectParametersSetupDialog mainDialog) {

	super(mainDialog, true);

	setTitle("Define new project parameter");

	this.mainDialog = mainDialog;

	categories = new DefaultListModel<String>();
	initComponents();

	radiobuttonNumerical.setSelected(true);
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
	panelNumericalAndFreeText = new JPanel(new BorderLayout());

	// Min, default and max for numerical
	panelNumerical = new JPanel(new BorderLayout());

	panelNumerical.add(radiobuttonNumerical, BorderLayout.NORTH);
	panelNumerical.setBorder(BorderFactory.createEtchedBorder());

	panelFreeText = new JPanel(new BorderLayout());
	panelFreeText.setPreferredSize(panelNumerical.getPreferredSize());
	panelFreeText.setBorder(BorderFactory.createEtchedBorder());

	panelFreeText.add(radiobuttonFreeText, BorderLayout.NORTH);

	panelNumericalAndFreeText.add(panelNumerical, BorderLayout.NORTH);
	panelNumericalAndFreeText.add(panelFreeText, BorderLayout.SOUTH);

	panelCategorical = new JPanel(new BorderLayout());

	// List of values for categorical
	panelCategoricalFields = new JPanel(new BorderLayout());
	scrollCategories = new JScrollPane();
	listCategories = new JList<String>(categories);
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

	panelNumericalAndFreeText.add(panelCategorical, BorderLayout.CENTER);

	panelParameterInformation.add(panelName, BorderLayout.NORTH);
	panelParameterInformation.add(panelNumericalAndFreeText,
		BorderLayout.CENTER);

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

    private void switchCategoricalFields(boolean enabled) {
	listCategories.setEnabled(enabled);
	buttonAddCategory.setEnabled(enabled);
	buttonRemoveCategory.setEnabled(enabled);
    }

    public void actionPerformed(ActionEvent actionEvent) {

	Object src = actionEvent.getSource();

	Desktop desktop = MZmineCore.getDesktop();

	if (src == buttonAddParameter) {
	    if (fieldName.getText().length() == 0) {
		desktop.displayErrorMessage(this,
			"Give a name for the parameter first.");
		return;
	    }
	    String paramName = fieldName.getText();

	    UserParameter<?, ?> parameter = null;

	    if (radiobuttonNumerical.isSelected()) {

		parameter = new DoubleParameter(paramName, null);
	    }

	    if (radiobuttonFreeText.isSelected()) {
		parameter = new StringParameter(paramName, null);
	    }

	    if (radiobuttonCategorical.isSelected()) {
		String[] possibleValues = new String[categories.size()];
		if (possibleValues.length == 0) {
		    desktop.displayErrorMessage(this,
			    "Give at least a single parameter value.");
		    return;
		}
		for (int valueIndex = 0; valueIndex < categories.size(); valueIndex++)
		    possibleValues[valueIndex] = (String) categories
			    .get(valueIndex);
		parameter = new ComboParameter<String>(paramName, null,
			possibleValues);
	    }

	    mainDialog.addParameter(parameter);

	    dispose();
	}

	if (src == buttonCancel) {
	    dispose();
	}

	if ((src == radiobuttonNumerical) || (src == radiobuttonCategorical)
		|| (src == radiobuttonFreeText)) {
	    if (radiobuttonCategorical.isSelected()) {
		switchCategoricalFields(true);
	    } else {
		switchCategoricalFields(false);
	    }
	}

	if (src == buttonAddCategory) {
	    String inputValue = JOptionPane
		    .showInputDialog("Please input a new value");
	    if ((inputValue == null) || (inputValue.trim().length() == 0))
		return;
	    if (((DefaultListModel<String>) listCategories.getModel())
		    .contains(inputValue)) {
		desktop.displayErrorMessage(this, "Value already exists.");
		return;
	    }
	    ((DefaultListModel<String>) listCategories.getModel())
		    .addElement(inputValue);
	}

	if (src == buttonRemoveCategory) {

	    int[] selectedIndices = listCategories.getSelectedIndices();
	    if ((selectedIndices == null) || (selectedIndices.length == 0)) {
		desktop.displayErrorMessage(this,
			"Select at least one value first.");
		return;
	    }

	    for (int selectedIndex : selectedIndices) {
		((DefaultListModel<String>) listCategories.getModel())
			.removeElementAt(selectedIndex);
	    }

	}

    }

}
