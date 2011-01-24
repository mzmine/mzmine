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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.dialogs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Hashtable;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.components.DragOrderedJList;
import net.sf.mzmine.util.components.FileNameComponent;
import net.sf.mzmine.util.components.GridBagPanel;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.components.MultipleSelectionComponent;
import net.sf.mzmine.util.components.RangeComponent;

/**
 * This class represents the parameter setup dialog to set the values of
 * SimpleParameterSet. Each Parameter is represented by a component. The
 * component can be obtained by calling getComponentForParameter(). Type of
 * component depends on parameter type:
 * 
 * STRING - JTextField
 * 
 * INTEGER, DOUBLE - JFormattedTextField
 * 
 * RANGE - RangeComponent
 * 
 * BOOLEAN - JCheckBox
 * 
 * MULTIPLE_SELECTION - MultipleSelectionComponent
 * 
 * FILE_NAME - FileNameComponent
 * 
 * ORDERED_LIST - DragOrderedJList
 * 
 */
public class ParameterSetupDialog extends JDialog implements ActionListener,
		PropertyChangeListener {

	public static final int TEXTFIELD_COLUMNS = 10;

	private ExitCode exitCode = ExitCode.UNKNOWN;

	private String helpID;

	// Parameters and their representation in the dialog
	private SimpleParameterSet parameterSet;
	protected Hashtable<Parameter, JComponent> parametersAndComponents;
	private Hashtable<Parameter, Object> autoValues;

	// Buttons
	private JButton btnOK, btnCancel, btnAuto, btnHelp;

	/**
	 * This single panel contains a grid of all the components of this dialog
	 * (see GridBagPanel). First three columns of the grid are title (JLabel),
	 * parameter component (JFormattedTextField or other) and units (JLabel),
	 * one row for each parameter. Row number 100 contains all the buttons of
	 * the dialog. Derived classes may add their own components such as previews
	 * to the unused cells of the grid.
	 */
	protected GridBagPanel mainPanel;

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters) {
		this(title, parameters, null, null);
	}

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters,
			String helpID) {
		this(title, parameters, null, helpID);
	}

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters,
			Hashtable<Parameter, Object> autoValues) {
		this(title, parameters, autoValues, null);
	}

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters,
			Hashtable<Parameter, Object> autoValues, String helpID) {

		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), title, true);

		this.parameterSet = parameters;
		this.autoValues = autoValues;
		this.helpID = helpID;

		parametersAndComponents = new Hashtable<Parameter, JComponent>();

		addDialogComponents();

		updateMinimumSize();
		pack();

		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
	 * This method must be called each time when a component is added to
	 * mainPanel. It will ensure the minimal size of the dialog is set to the
	 * minimum size of the mainPanel plus a little extra, so user cannot resize
	 * the dialog window smaller.
	 */
	protected void updateMinimumSize() {
		Dimension panelSize = mainPanel.getMinimumSize();
		Dimension minimumSize = new Dimension(panelSize.width + 50,
				panelSize.height + 50);
		setMinimumSize(minimumSize);
	}

	/**
	 * Constructs all components of the dialog
	 */
	private void addDialogComponents() {

		// Main panel which holds all the components in a grid
		mainPanel = new GridBagPanel();

		int rowCounter = 0;

		// Create labels and components for each parameter
		for (Parameter p : parameterSet.getParameters()) {

			// create labels
			JLabel label = new JLabel(p.getName());
			mainPanel.add(label, 0, rowCounter);

			JComponent comp = createComponentForParameter(p);

			comp.setToolTipText(p.getDescription());
			label.setLabelFor(comp);

			parametersAndComponents.put(p, comp);

			// Multiple selection will be expandable, other components not
			int verticalWeight = comp instanceof MultipleSelectionComponent ? 1 : 0;
			mainPanel.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight);

			String unitStr = p.getUnits();
			if (unitStr != null) {
				JLabel unitLabel = new JLabel(unitStr);
				mainPanel.add(unitLabel, 2, rowCounter);
			}

			rowCounter++;

		}

		// Add a single empty cell to the 99th row. This cell is expandable
		// (weightY is 1), therefore the other components will be
		// aligned to the top, which is what we want
		JPanel emptySpace = new JPanel();
		mainPanel.add(emptySpace, 0, 99, 3, 1, 0, 1);

		// Create a separate panel for the buttons
		JPanel pnlButtons = new JPanel();

		btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
		btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);

		if (autoValues != null) {
			btnAuto = GUIUtils.addButton(pnlButtons, "Set automatically", null,
					this);
		}

		if (helpID != null) {
			btnHelp = new HelpButton(helpID);
			pnlButtons.add(btnHelp);
		}

		/*
		 * Last row in the table will be occupied by the buttons. We set the row
		 * number to 100 and width to 3, spanning the 3 component columns
		 * defined above.
		 */
		mainPanel.add(pnlButtons, 0, 100, 3, 1);

		// Add some space around the widgets
		GUIUtils.addMargin(mainPanel, 10);

		// Add the main panel as the only component of this dialog
		add(mainPanel);

		pack();

		// Load the values into the components
		updateComponentsFromParameterSet();

	}

	/**
	 * Creates a dialog component to control given parameter.
	 */
	private JComponent createComponentForParameter(Parameter p) {

		Object[] possibleValues = p.getPossibleValues();
		if ((possibleValues != null)
				&& (p.getType() != ParameterType.MULTIPLE_SELECTION)
				&& (p.getType() != ParameterType.ORDERED_LIST)) {
			JComboBox combo = new JComboBox();
			for (Object value : possibleValues) {
				combo.addItem(value);
				if (value == parameterSet.getParameterValue(p))
					combo.setSelectedItem(value);
			}
			combo.addActionListener(this);
			combo.setToolTipText(p.getDescription());
			return combo;

		}

		JComponent comp = null;

		NumberFormat format = p.getNumberFormat();
		if (format == null)
			format = NumberFormat.getNumberInstance();

		switch (p.getType()) {

		case STRING:
			JTextField txtField = new JTextField();
			txtField.setColumns(TEXTFIELD_COLUMNS);
			comp = txtField;
			break;

		case INTEGER:
		case DOUBLE:
			JFormattedTextField fmtField = new JFormattedTextField(format);
			fmtField.setColumns(TEXTFIELD_COLUMNS);
			comp = fmtField;
			break;

		case RANGE:
			comp = new RangeComponent(format);
			break;

		case BOOLEAN:
			JCheckBox checkBox = new JCheckBox();
			checkBox.addActionListener(this);
			comp = checkBox;
			break;

		case MULTIPLE_SELECTION:
			Object multipleValues[] = parameterSet.getMultipleSelection(p);
			if (multipleValues == null)
				multipleValues = (Object[]) p.getPossibleValues();
			if (multipleValues == null)
				multipleValues = new Object[0];
			MultipleSelectionComponent msc = new MultipleSelectionComponent(
					multipleValues);
			msc.addActionListener(this);
			comp = msc;
			break;

		case FILE_NAME:
			comp = new FileNameComponent();
			break;

		case ORDERED_LIST:
			DefaultListModel fieldOrderModel = new DefaultListModel();
			for (Object item : p.getPossibleValues())
				fieldOrderModel.addElement(item);
			DragOrderedJList fieldOrderList = new DragOrderedJList(
					fieldOrderModel);
			fieldOrderList
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fieldOrderList.setBorder(new EtchedBorder(1));
			comp = fieldOrderList;
			break;

		}

		// This only applies to text boxes, but we can add it to all components
		comp.addPropertyChangeListener("value", this);

		// By calling this we make sure the components will never be resized
		// smaller than their optimal size
		comp.setMinimumSize(comp.getPreferredSize());

		return comp;

	}

	/**
	 * Returns current value of the component controlling given parameter.
	 */
	protected Object getComponentValue(Parameter p) {

		Object[] possibleValues = p.getPossibleValues();
		if ((possibleValues != null)
				&& (p.getType() != ParameterType.MULTIPLE_SELECTION)
				&& (p.getType() != ParameterType.ORDERED_LIST)) {
			JComboBox combo = (JComboBox) parametersAndComponents.get(p);
			int selectedIndex = combo.getSelectedIndex();
			if (selectedIndex < 0)
				selectedIndex = 0;
			return possibleValues[selectedIndex];
		}

		switch (p.getType()) {

		case INTEGER:
			JFormattedTextField intField = (JFormattedTextField) parametersAndComponents
					.get(p);
			Number intNumberValue = (Number) intField.getValue();
			if (intNumberValue == null)
				return null;
			return intNumberValue.intValue();

		case DOUBLE:
			JFormattedTextField doubleField = (JFormattedTextField) parametersAndComponents
					.get(p);
			Number doubleNumberValue = (Number) doubleField.getValue();
			if (doubleNumberValue == null)
				return null;
			return doubleNumberValue.doubleValue();

		case RANGE:
			RangeComponent rangeComp = (RangeComponent) parametersAndComponents
					.get(p);
			return rangeComp.getRangeValue();

		case STRING:
			JTextField stringField = (JTextField) parametersAndComponents
					.get(p);
			return stringField.getText();

		case BOOLEAN:
			JCheckBox checkBox = (JCheckBox) parametersAndComponents.get(p);
			return checkBox.isSelected();

		case MULTIPLE_SELECTION:
			MultipleSelectionComponent msc = (MultipleSelectionComponent) parametersAndComponents
					.get(p);
			return msc.getSelectedValues();

		case FILE_NAME:
			FileNameComponent fileNameField = (FileNameComponent) parametersAndComponents
					.get(p);
			return fileNameField.getFilePath();

		case ORDERED_LIST:
			JList list = (JList) parametersAndComponents.get(p);
			DefaultListModel fieldOrderModel = (DefaultListModel) list
					.getModel();
			return fieldOrderModel.toArray();
		}

		return null;

	}

	/**
	 * Sets the value of a component which is controlling given parameter to
	 * given value.
	 */
	protected void setComponentValue(Parameter p, Object value) {

		JComponent component = parametersAndComponents.get(p);
		if ((component == null) || (value == null))
			return;

		if (component instanceof JComboBox) {
			JComboBox combo = (JComboBox) component;
			combo.setSelectedItem(value);
			return;
		}

		switch (p.getType()) {

		case STRING:
			JTextField strField = (JTextField) component;
			String strValue = (String) value;
			strField.setText(strValue);
			break;

		case INTEGER:
		case DOUBLE:
			JFormattedTextField txtField = (JFormattedTextField) component;
			txtField.setValue(value);
			break;

		case RANGE:
			RangeComponent rangeComp = (RangeComponent) component;
			rangeComp.setRangeValue((Range) value);
			break;

		case BOOLEAN:
			JCheckBox checkBox = (JCheckBox) component;
			Boolean selected = (Boolean) value;
			checkBox.setSelected(selected);
			break;

		case FILE_NAME:
			FileNameComponent fileNameField = (FileNameComponent) component;
			fileNameField.setFilePath(value.toString());
			break;

		case MULTIPLE_SELECTION:
			MultipleSelectionComponent msc = (MultipleSelectionComponent) parametersAndComponents
					.get(p);
			msc.setSelectedValues((Object[]) value);
			break;

		case ORDERED_LIST:
			JList list = (JList) component;
			DefaultListModel newModel = new DefaultListModel();
			Object values[] = (Object[]) value;
			for (Object v : values)
				newModel.addElement(v);
			list.setModel(newModel);
			break;

		}
	}

	/**
	 * This function sets the values of all components according to the values
	 * in the ParameterSet.
	 */
	protected void updateComponentsFromParameterSet() {
		for (Parameter p : parameterSet.getParameters()) {
			setComponentValue(p, parameterSet.getParameterValue(p));
		}
	}

	/**
	 * This function collects all the information from the form components and
	 * set the ParameterSet values accordingly.
	 */
	protected void updateParameterSetFromComponents()
			throws IllegalArgumentException {
		for (Parameter p : parameterSet.getParameters()) {
			Object value = getComponentValue(p);
			if (value != null) {
				try {
					parameterSet.setParameterValue(p, value);
				} catch (IllegalArgumentException e) {
					// ignore invalid value
				}
			}
		}

	}

	/**
	 * Return a component which is controlling given parameter.
	 */
	protected JComponent getComponentForParameter(Parameter p) {
		return parametersAndComponents.get(p);
	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == btnOK) {
			try {
				updateParameterSetFromComponents();
			} catch (Exception invalidValueException) {
				MZmineCore.getDesktop().displayErrorMessage(
						invalidValueException.getMessage());
				return;
			}

			exitCode = ExitCode.OK;
			dispose();
		}

		if (src == btnCancel) {
			exitCode = ExitCode.CANCEL;
			dispose();
		}

		if (src == btnAuto) {
			for (Parameter p : autoValues.keySet()) {
				setComponentValue(p, autoValues.get(p));
			}
		}

		if ((src instanceof JCheckBox) || (src instanceof JComboBox)) {
			parametersChanged();
		}

	}

	/**
	 * Method for reading exit code
	 */
	public ExitCode getExitCode() {
		return exitCode;
	}

	public void propertyChange(PropertyChangeEvent event) {
		parametersChanged();
	}

	/**
	 * This method does nothing, but it is called whenever user changes the
	 * parameters. It can be overridden in extending classes to update some
	 * preview components, for example.
	 */
	protected void parametersChanged() {

	}

}
