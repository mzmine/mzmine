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

package net.sf.mzmine.userinterface.dialogs.experimentalparametersetupdialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class ExperimentalParametersSetupDialog extends JDialog implements ActionListener {

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
		private JPanel panelAddParameterButton;
			private JButton buttonAddParameter;
			
	private JPanel panelParameterValues;
		private JLabel labelParameterValues;
		private JScrollPane scrollParameterValues;
		private JTable tableParameterValues;
		private ExperimentalParametersSetupDialogTableModel tablemodelParameterValues;
		private JPanel panelRemoveParameterButton;
		private JButton buttonRemoveParameter;
	private JPanel panelOKCancelButtons;
		private JButton buttonOK;
		private JButton buttonCancel;
	
	private ExitCode exitCode = ExitCode.UNKNOWN;
	
	private DefaultListModel categories;
	private RawDataFile[] dataFiles;
	private Hashtable<Parameter, Object[]> parameterValues;
	
    private Desktop desktop;
    
	public ExperimentalParametersSetupDialog() {
		super(MZmineCore.getDesktop().getMainFrame(), true);
		
		categories = new DefaultListModel();
		parameterValues = new Hashtable<Parameter, Object[]>();
		
		this.dataFiles = MZmineCore.getCurrentProject().getDataFiles();
		this.desktop = MZmineCore.getDesktop();
        
		setTitle("Setup experimental parameters and values");
		initComponents();
		
		copyParameterValuesFromRawDataFiles();
		
		setupTableModel();

		radiobuttonNumerical.setSelected(true);
		switchNumericalFields(true);
		switchFreeTextFields(false);
		switchCategoricalFields(false);
		
		setLocationRelativeTo(desktop.getMainFrame());
		
	}

	public ExitCode getExitCode() {
		return exitCode;
	}
	
	public void actionPerformed(ActionEvent actionEvent) {

		Object src = actionEvent.getSource();
		if (src == buttonOK) {
			
			// Validate parameter values  
			if (!validateParameterValues()) return;
			
			// Copy paramter values to raw data files
			copyParameterValuesToRawDataFiles();
			
			exitCode = ExitCode.OK;
			dispose();
		}
		
		if (src == buttonCancel) {
			exitCode = ExitCode.CANCEL;
			dispose();
		}
		
		if (src == buttonAddParameter) {
			if (fieldName.getText().length()==0) {
				desktop.displayErrorMessage("Give a name for the parameter first.");
				return;
			}
			String paramName = fieldName.getText();
			
			SimpleParameter parameter = null;

			if (radiobuttonNumerical.isSelected()) {
				ParameterType paramType = ParameterType.FLOAT;
				Double minValue = Double.NEGATIVE_INFINITY;
				if (fieldNumericalMinValue.getValue()!=null)
					minValue = ((Number)fieldNumericalMinValue.getValue()).doubleValue();
				
				Double defaultValue = 0.0;
				if (fieldNumericalDefaultValue.getValue()!=null) {
					defaultValue = ((Number)fieldNumericalDefaultValue.getValue()).doubleValue();
				} 
				
				Double maxValue = Double.POSITIVE_INFINITY;
				if (fieldNumericalMaxValue.getValue()!=null)
					maxValue = ((Number)fieldNumericalMaxValue.getValue()).doubleValue();
				
				parameter = new SimpleParameter(paramType, paramName, null, null, defaultValue, minValue, maxValue);
			}
			
			if (radiobuttonFreeText.isSelected()) {
				ParameterType paramType = ParameterType.STRING;
				String defaultValue = "";
				if (fieldFreeTextDefaultValue.getText()!=null)
					defaultValue = fieldFreeTextDefaultValue.getText();
				parameter = new SimpleParameter(paramType, paramName, null, (Object)defaultValue);
			}
			
			if (radiobuttonCategorical.isSelected()) {
				ParameterType paramType = ParameterType.STRING;
				String[] possibleValues = new String[categories.size()];
				if (possibleValues.length==0) {
					desktop.displayErrorMessage("Give at least a single parameter value.");
					return;
				}
				for (int valueIndex=0; valueIndex<categories.size(); valueIndex++)
					possibleValues[valueIndex] = (String)categories.get(valueIndex);
				parameter = new SimpleParameter(paramType, paramName, null, possibleValues[0], possibleValues);
			}
			
			// Initialize with default value
			Object[] values = new Object[dataFiles.length];
			for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) 
				values[dataFileIndex] = parameter.getDefaultValue();
			
			// Add this newly created parameter to hashtable
			parameterValues.put(parameter, values);
			setupTableModel();
			
			
		}
		
		if (src == buttonRemoveParameter) {
			int selectedColumn = tableParameterValues.getSelectedColumn();
			Parameter parameter = tablemodelParameterValues.getParameter(selectedColumn);
			if (parameter==null) {
				desktop.displayErrorMessage("Select a parameter column from the table first.");
				return;
			}
			parameterValues.remove(parameter);
			setupTableModel();
			
		}
		
		if ( (src == radiobuttonNumerical) ||
			 (src == radiobuttonCategorical) ||
			 (src == radiobuttonFreeText) ) {
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
			String inputValue = JOptionPane.showInputDialog("Please input a new value");
			if (	((DefaultListModel)listCategories.getModel()).contains(inputValue)	) {
				desktop.displayErrorMessage("Value already exists.");
				return;
			}
			((DefaultListModel)listCategories.getModel()).addElement(inputValue);
		}
		
		if (src == buttonRemoveCategory) {
			
			int[] selectedIndices = listCategories.getSelectedIndices();
			if ((selectedIndices==null) || (selectedIndices.length==0)) {
				desktop.displayErrorMessage("Select at least one value first.");	
			}
			
			for (int selectedIndex : selectedIndices) {
				((DefaultListModel)listCategories.getModel()).removeElementAt(selectedIndex);
			}
			
		}
		
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
	
	
	private boolean validateParameterValues() {
		// Create new parameters and set values
		for (int columnIndex=0; columnIndex<parameterValues.keySet().size(); columnIndex++) {
			Parameter parameter = tablemodelParameterValues.getParameter(columnIndex+1);
			
			if (parameter.getType()==ParameterType.FLOAT) {
				Double minValue = null;
				Double maxValue = null;
				if (parameter.getMinimumValue()!=null)
					minValue = (Double)(parameter.getMinimumValue());
				if (parameter.getMaximumValue()!=null)
					maxValue = (Double)(parameter.getMaximumValue());
	
				for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) {
					Object objValue = tablemodelParameterValues.getValueAt(dataFileIndex, columnIndex+1);
					Double value = null;
					if (objValue instanceof Double) value = (Double)objValue;
					if (objValue instanceof String) {
						try {
							value = Double.parseDouble((String)objValue);
						} catch (NumberFormatException ex) {
							desktop.displayErrorMessage("Incorrect value (" + (String)objValue + ") for parameter " + parameter.getName() + " in data file " + dataFiles[dataFileIndex].toString() + ".");
							return false;
						}
					}
					if ((minValue!=null) && (minValue>value)) {
						desktop.displayErrorMessage("Too small value (" + value + ") for parameter " + parameter.getName() + " in data file " + dataFiles[dataFileIndex].toString() + ".");
						return false;
					}
					if ((maxValue!=null) && (maxValue<value)) {
						desktop.displayErrorMessage("Too big value (" + value + ") for parameter " + parameter.getName() + " in data file " + dataFiles[dataFileIndex].toString() + ".");
						return false;
					}				
				}
			}
		}
		
		return true;
		
	}
	
	private void copyParameterValuesToRawDataFiles() {
		
        MZmineProject currentProject = MZmineCore.getCurrentProject();

        // Remove all previous experimental parameters from project
        Parameter[] parameters = currentProject.getParameters();
        for (Parameter parameter : parameters) {
        	currentProject.removeParameter(parameter);
        }
        
        // Add new experimental parameters
		parameters = parameterValues.keySet().toArray(new Parameter[0]);
		for (Parameter parameter : parameters) {
			currentProject.addParameter(parameter);
		}		
        
		// Set values for new parameters
		for (int columnIndex=0; columnIndex<parameterValues.keySet().size(); columnIndex++) {
			Parameter parameter = tablemodelParameterValues.getParameter(columnIndex+1);
			
			for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) {
				RawDataFile file = dataFiles[dataFileIndex];
				
				Object value = tablemodelParameterValues.getValueAt(dataFileIndex, columnIndex+1);
				if (parameter.getType()==ParameterType.FLOAT) {
					Double doubleValue=null;
					if (value instanceof Double)
						doubleValue = (Double)value;
					if (value instanceof String) 
						doubleValue = Double.parseDouble((String)value);
                    currentProject.setParameterValue(parameter, file, doubleValue);
				}
				if (parameter.getType()==ParameterType.STRING) {
					if (value==null) value = "";
					currentProject.setParameterValue(parameter, file, (String)value);
				}
				
			}

		}
		
	}
	
	private void copyParameterValuesFromRawDataFiles() {

		
		for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) {
			
			MZmineProject currentProject = MZmineCore.getCurrentProject();
			
			RawDataFile file = dataFiles[dataFileIndex];
			Parameter[] parameters = currentProject.getParameters();
			
			// Loop through all experimental paramters defined for this file
			for (Parameter p : parameters) {
				
				// Check if this parameter has been seen before?
				Object[] values;
				if (!(parameterValues.containsKey(p))) {
					// No, initialize a new array of values for this parameter
					values = new Object[dataFiles.length];
					for (int i=0; i<values.length; i++) 
						values[i] = p.getDefaultValue();
					parameterValues.put(p, values);
				} else {
					values = parameterValues.get(p);
				}
				
				// Set value of this parameter for the current raw data file 
				values[dataFileIndex] = currentProject.getParameterValue(p, file); 
				
			}
			
		}
		
	}
	
	
	private void setupTableModel() {
		
		tablemodelParameterValues = new ExperimentalParametersSetupDialogTableModel(dataFiles, parameterValues);
		tableParameterValues.setModel(tablemodelParameterValues);
		
		for (int columnIndex=0; columnIndex<(tablemodelParameterValues.getColumnCount()-1); columnIndex++) {
			Parameter parameter = tablemodelParameterValues.getParameter(columnIndex+1);
			if (parameter.getPossibleValues()!=null)
				tableParameterValues.getColumnModel().getColumn(columnIndex+1).setCellEditor(new DefaultCellEditor(new JComboBox(parameter.getPossibleValues())));
		}

		
	}
		
	private void initComponents() {
		
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
				panelFields = new JPanel(new GridLayout(1,2));
					
					panelNumericalAndFreeText = new JPanel(new BorderLayout());
				
						// Min, default and max for numerical				
						panelNumerical = new JPanel(new BorderLayout());				
	
							panelNumericalFields = new JPanel(new GridLayout(3,2,5,2));
								labelNumericalMinValue = new JLabel("Minimum value");
								fieldNumericalMinValue = new JFormattedTextField(NumberFormat.getNumberInstance());
								labelNumericalDefaultValue = new JLabel("Default value");
								fieldNumericalDefaultValue = new JFormattedTextField(NumberFormat.getNumberInstance());
								labelNumericalMaxValue = new JLabel("Maximum value");
								fieldNumericalMaxValue = new JFormattedTextField(NumberFormat.getNumberInstance());
								panelNumericalFields.add(labelNumericalMinValue);
								panelNumericalFields.add(fieldNumericalMinValue);
								panelNumericalFields.add(labelNumericalDefaultValue);
								panelNumericalFields.add(fieldNumericalDefaultValue);
								panelNumericalFields.add(labelNumericalMaxValue);
								panelNumericalFields.add(fieldNumericalMaxValue);
								panelNumericalFields.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
							
						
							panelNumerical.add(radiobuttonNumerical, BorderLayout.NORTH);
							panelNumerical.add(panelNumericalFields, BorderLayout.CENTER);
							panelNumerical.setBorder(BorderFactory.createEtchedBorder());
							
						panelFreeText = new JPanel(new BorderLayout());
						panelFreeText.setPreferredSize(panelNumerical.getPreferredSize());				
						panelFreeText.setBorder(BorderFactory.createEtchedBorder());

							panelFreeTextFields = new JPanel(new GridLayout(3,2,5,2));
							labelFreeTextDefaultValue = new JLabel("Default value");
							fieldFreeTextDefaultValue = new JTextField();
							panelFreeTextFields.add(labelFreeTextDefaultValue);
							panelFreeTextFields.add(fieldFreeTextDefaultValue);
							panelFreeTextFields.add(new JPanel());
							panelFreeTextFields.add(new JPanel());
							panelFreeTextFields.add(new JPanel());
							panelFreeTextFields.add(new JPanel());
							panelFreeTextFields.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
							
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
							panelCategoricalFields.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

						panelCategorical.add(radiobuttonCategorical, BorderLayout.NORTH);
						panelCategorical.add(panelCategoricalFields, BorderLayout.CENTER);
						panelCategorical.setBorder(BorderFactory.createEtchedBorder());
					
					panelFields.add(panelNumericalAndFreeText);
					panelFields.add(panelCategorical);
					
			panelParameterInformation.add(panelName, BorderLayout.NORTH);
			panelParameterInformation.add(panelFields, BorderLayout.CENTER);
							
			panelAddParameterButton = new JPanel(new FlowLayout(FlowLayout.LEFT));
				buttonAddParameter = new JButton("Add parameter");
				buttonAddParameter.addActionListener(this);
				panelAddParameterButton.add(buttonAddParameter);
				
			panelAddNewParameter.add(labelAddNewParameter, BorderLayout.NORTH);
			panelAddNewParameter.add(panelParameterInformation, BorderLayout.CENTER);
			panelAddNewParameter.add(panelAddParameterButton, BorderLayout.SOUTH);
			panelAddNewParameter.setBorder(BorderFactory.createEmptyBorder(5, 5, 25, 5));
			
		panelParameterValues = new JPanel(new BorderLayout());
			labelParameterValues = new JLabel("Values for experimental parameters");
			scrollParameterValues = new JScrollPane();
			tablemodelParameterValues = new ExperimentalParametersSetupDialogTableModel(new RawDataFile[0], new Hashtable<Parameter, Object[]>());
			tableParameterValues = new JTable(tablemodelParameterValues);
			tableParameterValues.setColumnSelectionAllowed(true);
			tableParameterValues.setRowSelectionAllowed(false);
			scrollParameterValues.setViewportView(tableParameterValues);
			scrollParameterValues.setMinimumSize(new Dimension(100,100));
			scrollParameterValues.setPreferredSize(new Dimension(100,100));
			panelRemoveParameterButton = new JPanel(new FlowLayout(FlowLayout.LEFT));
				buttonRemoveParameter = new JButton("Remove parameter");
				buttonRemoveParameter.addActionListener(this);
				panelRemoveParameterButton.add(buttonRemoveParameter);
			
			panelParameterValues.add(labelParameterValues, BorderLayout.NORTH);
			panelParameterValues.add(scrollParameterValues, BorderLayout.CENTER);
			panelParameterValues.add(panelRemoveParameterButton, BorderLayout.SOUTH);
			panelParameterValues.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			
			
			
			
		panelOKCancelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonOK = new JButton("OK");
			buttonOK.addActionListener(this);
			buttonCancel = new JButton("Cancel");
			buttonCancel.addActionListener(this);
			panelOKCancelButtons.add(buttonOK);
			panelOKCancelButtons.add(buttonCancel);
			
		setLayout(new BorderLayout());
		
		add(panelAddNewParameter, BorderLayout.NORTH);
		add(panelParameterValues, BorderLayout.CENTER);
		add(panelOKCancelButtons, BorderLayout.SOUTH);
		
		pack();
	}
	
	
	
}
