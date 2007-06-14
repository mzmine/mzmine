package net.sf.mzmine.userinterface.dialogs.experimentalparametersetupdialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.io.OpenedRawDataFile;
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
				private JRadioButton radiobuttonCathegorical;
				private JPanel panelNumerical;
					private JPanel panelNumericalFields;
						private JLabel labelMinValue;
						private JFormattedTextField fieldMinValue;					
						private JLabel labelDefaultValue;
						private JFormattedTextField fieldDefaultValue;
						private JLabel labelMaxValue;
						private JFormattedTextField fieldMaxValue;
				private JPanel panelCathegorical;
					private JPanel panelCathegoricalFields;
						private JScrollPane scrollCathegories;
						private JList listCathegories;
						private JPanel panelAddCathegoryButtons;
							private JButton buttonAddCathegory;
							private JButton buttonRemoveCathegory;
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
	
	private HashSet<String> cathegories;
	private OpenedRawDataFile[] dataFiles;
	private Hashtable<Parameter, Object[]> parameterValues;
	private Vector<Parameter> removedParameters;
	
	private Desktop desktop;
	
	public ExperimentalParametersSetupDialog(Desktop desktop, OpenedRawDataFile[] dataFiles) {
		super(desktop.getMainFrame(), true);
		 
		cathegories = new HashSet<String>();
		parameterValues = new Hashtable<Parameter, Object[]>();
		removedParameters = new Vector<Parameter>();
		
		this.desktop = desktop;
		this.dataFiles = dataFiles;
		
		setTitle("Setup experimental parameters and values");
		initComponents();
		
		copyParameterValuesFromRawDataFiles();
		
		setupTableModel();
		
		setLocationRelativeTo(desktop.getMainFrame());
		
	}

	public ExitCode getExitCode() {
		return exitCode;
	}
	
	public void actionPerformed(ActionEvent actionEvent) {

		Object src = actionEvent.getSource();
		if (src == buttonOK) {
			// Add parameters to data files if they do not exist already, and set values
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
				Parameter.ParameterType paramType = Parameter.ParameterType.DOUBLE;
				
				Double minValue = Double.NEGATIVE_INFINITY;
				if (fieldMinValue.getValue()!=null)
					minValue = ((Number)fieldMinValue.getValue()).doubleValue();
				
				Double defaultValue = 0.0;
				if (fieldDefaultValue.getValue()!=null) {
					
					defaultValue = ((Number)fieldDefaultValue.getValue()).doubleValue();
					System.out.println("Read default value " + defaultValue + " from the form.");
				} else {
					System.out.println("fieldDefaultValue.getValue()==null");
				}
				
				Double maxValue = Double.POSITIVE_INFINITY;
				if (fieldMaxValue.getValue()!=null)
					maxValue = ((Number)fieldMaxValue.getValue()).doubleValue();
				
				parameter = new SimpleParameter(paramType, paramName, null, null, defaultValue, minValue, maxValue);
			}
			if (radiobuttonCathegorical.isSelected()) {
				Parameter.ParameterType paramType = Parameter.ParameterType.STRING;
				String[] possibleValues = cathegories.toArray(new String[0]);
				parameter = new SimpleParameter(paramType, paramName, null, possibleValues[0], possibleValues);
			}
			
			// Initialize with default value
			Object[] values = new Object[dataFiles.length];
			for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) 
				values[dataFileIndex] = parameter.getDefaultValue();
			
			System.out.println("Initialized values with " + values[0]);
			
			
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
		
		if ((src == radiobuttonNumerical) ||(src == radiobuttonCathegorical)) {
			if (radiobuttonNumerical.isSelected()) {
				switchNumericalFields(true);
				switchCathegoricalFields(false);
			} else {
				switchNumericalFields(false);
				switchCathegoricalFields(true);
			}
		}
		
		if (src == buttonAddCathegory) {
			desktop.displayErrorMessage("Not yet implemented.");
		}
		
		if (src == buttonRemoveCathegory) {
			desktop.displayErrorMessage("Not yet implemented.");
		}		
		
	}
	
	private void switchNumericalFields(boolean enabled) {
		labelMinValue.setEnabled(enabled);
		fieldMinValue.setEnabled(enabled);
		labelDefaultValue.setEnabled(enabled);
		fieldDefaultValue.setEnabled(enabled);
		labelMaxValue.setEnabled(enabled);
		fieldMaxValue.setEnabled(enabled);
	}
	
	private void switchCathegoricalFields(boolean enabled) {
		listCathegories.setEnabled(enabled);
		buttonAddCathegory.setEnabled(enabled);
		buttonRemoveCathegory.setEnabled(enabled);
	}
	
	private void copyParameterValuesToRawDataFiles() {
		
		// Remove all old parameters
		for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) {
			OpenedRawDataFile file = dataFiles[dataFileIndex];
			
			for (Parameter p : file.getParameters().getParameters()) {
				file.getParameters().removeParameter(p);
			}
		}
		
		// Create new parameters and set values
		for (int columnIndex=0; columnIndex<parameterValues.keySet().size(); columnIndex++) {
			Parameter parameter = tablemodelParameterValues.getParameter(columnIndex+1);
			
			for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) {
				OpenedRawDataFile file = dataFiles[dataFileIndex];
				
				file.getParameters().addParameter(parameter);
				Object value = tablemodelParameterValues.getValueAt(dataFileIndex, columnIndex+1);
				if (parameter.getType()==Parameter.ParameterType.DOUBLE) {
					Double doubleValue=null;
					if (value instanceof Double)
						doubleValue = (Double)value;
					if (value instanceof String) 
						doubleValue = Double.parseDouble((String)value);
					file.getParameters().setParameterValue(parameter, doubleValue);
				}
				if (parameter.getType()==Parameter.ParameterType.STRING) {
					file.getParameters().setParameterValue(parameter, (String)value);
				}
				
			}

		}
		
	}
	
	private void copyParameterValuesFromRawDataFiles() {

		
		for (int dataFileIndex=0; dataFileIndex<dataFiles.length; dataFileIndex++) {
			
			OpenedRawDataFile file = dataFiles[dataFileIndex];
			
			// Loop through all experimental paramters defined for this file
			for (Parameter p : file.getParameters().getParameters()) {
				
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
				values[dataFileIndex] = file.getParameters().getParameterValue(p);
				
			}
			
		}
		
	}
	
	
	private void setupTableModel() {
		
		tablemodelParameterValues = new ExperimentalParametersSetupDialogTableModel(dataFiles, parameterValues);
		tableParameterValues.setModel(tablemodelParameterValues);
		
		radiobuttonNumerical.setSelected(true);
		switchNumericalFields(true);
		switchCathegoricalFields(false);
		
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
					radiobuttonCathegorical = new JRadioButton("Cathegorical values");
					radiobuttonNumerical.addActionListener(this);
					radiobuttonCathegorical.addActionListener(this);
					buttongroupType.add(radiobuttonNumerical);
					buttongroupType.add(radiobuttonCathegorical);
						
				// Fields for different types of parameters
				panelFields = new JPanel(new GridLayout(1,2));
					
				
					// Min, default and max for numerical				
					panelNumerical = new JPanel(new BorderLayout());				

						panelNumericalFields = new JPanel(new GridLayout(3,2,5,2));
							labelMinValue = new JLabel("Minimum value");
							fieldMinValue = new JFormattedTextField(NumberFormat.getNumberInstance());
							labelDefaultValue = new JLabel("Default value");
							fieldDefaultValue = new JFormattedTextField(NumberFormat.getNumberInstance());
							labelMaxValue = new JLabel("Maximum value");
							fieldMaxValue = new JFormattedTextField(NumberFormat.getNumberInstance());
							panelNumericalFields.add(labelMinValue);
							panelNumericalFields.add(fieldMinValue);
							panelNumericalFields.add(labelDefaultValue);
							panelNumericalFields.add(fieldDefaultValue);
							panelNumericalFields.add(labelMaxValue);
							panelNumericalFields.add(fieldMaxValue);
							panelNumericalFields.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
						
					
						panelNumerical.add(radiobuttonNumerical, BorderLayout.NORTH);
						panelNumerical.add(panelNumericalFields, BorderLayout.CENTER);
						panelNumerical.setBorder(BorderFactory.createEtchedBorder());
						
					panelCathegorical = new JPanel(new BorderLayout());
					
						// List of values for cathegorical
						panelCathegoricalFields = new JPanel(new BorderLayout());
							scrollCathegories = new JScrollPane();
							listCathegories = new JList();
							scrollCathegories.add(listCathegories);
							panelAddCathegoryButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
								buttonAddCathegory = new JButton("Add cathegory");
								buttonRemoveCathegory = new JButton("Remove cathegory");
								panelAddCathegoryButtons.add(buttonAddCathegory);
								panelAddCathegoryButtons.add(buttonRemoveCathegory);
							panelCathegoricalFields.add(scrollCathegories, BorderLayout.CENTER);
							panelCathegoricalFields.add(panelAddCathegoryButtons, BorderLayout.SOUTH);
							panelCathegoricalFields.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

						panelCathegorical.add(radiobuttonCathegorical, BorderLayout.NORTH);
						panelCathegorical.add(panelCathegoricalFields, BorderLayout.CENTER);
						panelCathegorical.setBorder(BorderFactory.createEtchedBorder());
					
					panelFields.add(panelNumerical);
					panelFields.add(panelCathegorical);
					
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
			tablemodelParameterValues = new ExperimentalParametersSetupDialogTableModel(new OpenedRawDataFile[0], new Hashtable<Parameter, Object[]>());
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
