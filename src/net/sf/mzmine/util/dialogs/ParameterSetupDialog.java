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

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.components.DragOrderedJList;
import net.sf.mzmine.util.components.ExtendedCheckBox;
import net.sf.mzmine.util.components.HelpButton;

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
 * RANGE - JPanel containing a JFormattedTextField (min), JLabel and
 * JFormattedTextField (max)
 * 
 * BOOLEAN - JCheckBox
 * 
 * MULTIPLE_SELECTION - JScrollPane with JViewPort containing a JPanel
 * containing multiple ExtendedCheckBoxes
 * 
 * FILE_NAME - JPanel containing JTextField, space and a JButton
 * 
 * ORDERED_LIST - DragOrderedJList
 * 
 */
public class ParameterSetupDialog extends JDialog implements ActionListener {

	static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

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
	 * Derived classed may add their components to these panels. Both panels use
	 * BorderLayout. mainPanel containts componentPanel in the WEST position and
	 * nothing else. componentsPanel contains parameter components in the NORTH
	 * position and buttons in the SOUTH position. Other positions are free to
	 * use by derived (specialized) dialogs.
	 */
	protected JPanel componentsPanel, mainPanel;

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

		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
	 * Constructs all components of the dialog
	 */
	private void addDialogComponents() {

		JComponent components[] = new JComponent[parameterSet.getParameters().length * 3];
		int componentCounter = 0;

		// Create labels and components for each parameter
		for (Parameter p : parameterSet.getParameters()) {

			// create labels
			JLabel label = new JLabel(p.getName());
			components[componentCounter++] = label;

			JComponent comp = createComponentForParameter(p);

			comp.setToolTipText(p.getDescription());
			label.setLabelFor(comp);

			parametersAndComponents.put(p, comp);

			components[componentCounter++] = comp;

			String unitStr = "";
			if (p.getUnits() != null)
				unitStr = p.getUnits();

			components[componentCounter++] = new JLabel(unitStr);

		}

		// Buttons
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

		// Panel collecting all labels, fields and units
		JPanel labelsAndFields = GUIUtils.makeTablePanel(parameterSet
				.getParameters().length, 3, 1, components);

		// Load the values into the components
		updateComponentsFromParameterSet();

		// Panel where components are in the NORTH part and derived classes can
		// add their own components around
		componentsPanel = new JPanel(new BorderLayout());
		componentsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,
				10));
		componentsPanel.add(labelsAndFields, BorderLayout.NORTH);
		componentsPanel.add(pnlButtons, BorderLayout.SOUTH);

		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(componentsPanel, BorderLayout.WEST);

		add(mainPanel);

	}

	/**
	 * Creates a dialog component to control given parameter.
	 */
	protected JComponent createComponentForParameter(Parameter p) {

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

			JFormattedTextField minTxtField = new JFormattedTextField(format);
			JFormattedTextField maxTxtField = new JFormattedTextField(format);
			minTxtField.setColumns(TEXTFIELD_COLUMNS);
			maxTxtField.setColumns(TEXTFIELD_COLUMNS);
			JPanel panel = new JPanel();
			panel.add(minTxtField);
			GUIUtils.addLabel(panel, " - ");
			panel.add(maxTxtField);
			comp = panel;
			break;

		case BOOLEAN:
			JCheckBox checkBox = new JCheckBox();
			comp = checkBox;
			break;

		case MULTIPLE_SELECTION:
			JPanel checkBoxesPanel = new JPanel();
			checkBoxesPanel.setBackground(Color.white);
			checkBoxesPanel.setLayout(new BoxLayout(checkBoxesPanel,
					BoxLayout.Y_AXIS));

			int vertSize = 0,
			numCheckBoxes = 0;
			ExtendedCheckBox<Object> ecb = null;
			Object multipleValues[] = parameterSet.getMultipleSelection(p);
			if (multipleValues == null)
				multipleValues = p.getPossibleValues();
			if (multipleValues == null)
				multipleValues = new Object[0];

			for (Object genericObject : multipleValues) {

				ecb = new ExtendedCheckBox<Object>(genericObject, false);
				ecb.setAlignmentX(Component.LEFT_ALIGNMENT);
				checkBoxesPanel.add(ecb);

				if (numCheckBoxes < 7)
					vertSize += (int) ecb.getPreferredSize().getHeight() + 2;

				numCheckBoxes++;
			}

			if (numCheckBoxes < 3)
				vertSize += 30;

			JScrollPane peakPanelScroll = new JScrollPane(checkBoxesPanel,
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			peakPanelScroll.setPreferredSize(new Dimension(0, vertSize));
			comp = peakPanelScroll;
			break;

		case FILE_NAME:
			final JTextField txtFilename = new JTextField();
			txtFilename.setColumns(TEXTFIELD_COLUMNS);
			txtFilename.setFont(smallFont);
			JButton btnFileBrowser = new JButton("...");
			final Parameter fileParameter = p;
			btnFileBrowser.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser();
					File currentFile = new File(txtFilename.getText());
					File currentDir = currentFile.getParentFile();
					if (currentDir != null && currentDir.exists())
						fileChooser.setCurrentDirectory(currentDir);
					fileChooser.setMultiSelectionEnabled(false);
					int returnVal = fileChooser.showDialog(MZmineCore
							.getDesktop().getMainFrame(), "Select");
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						setComponentValue(fileParameter, fileChooser
								.getSelectedFile().getAbsolutePath());
					}

				}
			});
			JPanel panelFilename = new JPanel();
			panelFilename.setLayout(new BoxLayout(panelFilename,
					BoxLayout.X_AXIS));
			panelFilename.add(txtFilename);
			panelFilename.add(Box.createRigidArea(new Dimension(10, 1)));
			panelFilename.add(btnFileBrowser);
			comp = panelFilename;
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
			JPanel panel = (JPanel) parametersAndComponents.get(p);
			JFormattedTextField minField = (JFormattedTextField) panel
					.getComponent(0);
			JFormattedTextField maxField = (JFormattedTextField) panel
					.getComponent(2);
			double minValue = ((Number) minField.getValue()).doubleValue();
			double maxValue = ((Number) maxField.getValue()).doubleValue();
			Range rangeValue = new Range(minValue, maxValue);
			return rangeValue;

		case STRING:
			JTextField stringField = (JTextField) parametersAndComponents
					.get(p);
			return stringField.getText();

		case BOOLEAN:
			JCheckBox checkBox = (JCheckBox) parametersAndComponents.get(p);
			return checkBox.isSelected();

		case MULTIPLE_SELECTION:
			JScrollPane scrollPanel = (JScrollPane) parametersAndComponents
					.get(p);
			if (scrollPanel == null)
				return null;

			JPanel checkBoxPanel = (JPanel) scrollPanel.getViewport()
					.getComponent(0);

			Vector<Object> selectedGenericObject = new Vector<Object>();
			Component checkBoxes[] = checkBoxPanel.getComponents();

			for (Component comp : checkBoxes) {
				ExtendedCheckBox<?> box = (ExtendedCheckBox<?>) comp;
				if (box.isSelected()) {
					Object genericObject = box.getObject();
					selectedGenericObject.add(genericObject);
				}
			}
			return selectedGenericObject.toArray();

		case FILE_NAME:
			JPanel fileNamePanel = (JPanel) parametersAndComponents.get(p);
			JTextField txtFilename = (JTextField) fileNamePanel.getComponent(0);
			return txtFilename.getText();

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
			Range valueRange = (Range) value;
			JPanel panel = (JPanel) component;
			JFormattedTextField minField = (JFormattedTextField) panel
					.getComponent(0);
			minField.setValue(valueRange.getMin());
			JFormattedTextField maxField = (JFormattedTextField) panel
					.getComponent(2);
			maxField.setValue(valueRange.getMax());
			break;

		case BOOLEAN:
			JCheckBox checkBox = (JCheckBox) component;
			Boolean selected = (Boolean) value;
			checkBox.setSelected(selected);
			break;

		case FILE_NAME:
			JPanel fileNamePanel = (JPanel) component;
			JTextField txtFilename = (JTextField) fileNamePanel.getComponent(0);
			txtFilename.setText(value.toString());
			break;

		case MULTIPLE_SELECTION:
			Object multipleValues[] = (Object[]) value;
			JScrollPane scrollPanel = (JScrollPane) component;
			JPanel multiplePanel = (JPanel) scrollPanel.getViewport()
					.getComponent(0);
			Component checkBoxes[] = multiplePanel.getComponents();
			for (Component comp : checkBoxes) {
				ExtendedCheckBox<?> box = (ExtendedCheckBox<?>) comp;
				boolean isSelected = false;
				for (Object v : multipleValues) {
					if (v == box.getObject())
						isSelected = true;
				}
				box.setSelected(isSelected);
			}
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
	public void updateParameterSetFromComponents()
			throws IllegalArgumentException {
		for (Parameter p : parameterSet.getParameters()) {
			Object value = getComponentValue(p);
			if (value != null) {
				try {
					parameterSet.setParameterValue(p, value);
				} catch (IllegalArgumentException e) {
					// ignore
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

	}

	/**
	 * Method for reading exit code
	 */
	public ExitCode getExitCode() {
		return exitCode;
	}

}
