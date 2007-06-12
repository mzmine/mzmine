package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

public class ExperimentalParametersSetupDialog extends JDialog implements ActionListener {

	private JPanel panelAddNewParameter;
	
		private JLabel labelAddNewParameter;
	
		private JPanel panelParameterInformation;
			private JPanel panelNameAndType;
			
				private JPanel panelName;
					private JLabel labelName;
					private JTextField fieldName;
			
				private JPanel panelType;
					private ButtonGroup buttongroupType;
					private JRadioButton radiobuttonNumerical;
					private JRadioButton radiobuttonCathegorical;
			
					
			private JPanel panelFields;
			
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
						private JTable tableCathegories;
						private JPanel panelAddCathegoryButtons;
							private JButton buttonAddCathegory;
							private JButton buttonRemoveCathegory;
				
		private JPanel panelAddParameterButtons;
			private JButton buttonAddParameter;
			private JButton buttonRemoveParameter;
			
	private JPanel panelParameterValues;
		private JLabel labelParameterValues;
		private JScrollPane scrollParameterValues;
		private JTable tableParameterValues;
	
	private JPanel panelOKCancelButtons;
		private JButton buttonOK;
		private JButton buttonCancel;
	
	private ExitCode exitCode = ExitCode.UNKNOWN;
	
	
	public ExperimentalParametersSetupDialog(Frame owner) {

		setTitle("Setup experimental parameters and values");
		initComponents();
		
		setLocationRelativeTo(owner);
	}

	public ExitCode getExitCode() {
		return exitCode;
	}
	
	public void actionPerformed(ActionEvent actionEvent) {

		Object src = actionEvent.getSource();
		if (src == buttonOK) {
			// Add new parameters to project
			
			// Set parameter values to OpenedRawDataFiles
			
			exitCode = ExitCode.OK;
			dispose();
		}
		
		if (src == buttonCancel) {
			exitCode = ExitCode.CANCEL;
			dispose();
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
					radiobuttonCathegorical = new JRadioButton("Cathegorical values");
					buttongroupType.add(radiobuttonNumerical);
					buttongroupType.add(radiobuttonCathegorical);
						
				// Fields for different types of parameters
				panelFields = new JPanel(new GridLayout(1,2));
					
				
					// Min, default and max for numerical				
					panelNumerical = new JPanel(new BorderLayout());				

						panelNumericalFields = new JPanel(new GridLayout(3,2,5,2));
							labelMinValue = new JLabel("Minimum value");
							fieldMinValue = new JFormattedTextField();
							labelDefaultValue = new JLabel("Default value");
							fieldDefaultValue = new JFormattedTextField();
							labelMaxValue = new JLabel("Maximum value");
							fieldMaxValue = new JFormattedTextField();
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
							tableCathegories = new JTable();
							scrollCathegories.add(tableCathegories);
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
							
			panelAddParameterButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
				buttonAddParameter = new JButton("Add parameter");
				buttonRemoveParameter = new JButton("Remove parameter");
				panelAddParameterButtons.add(buttonAddParameter);
				panelAddParameterButtons.add(buttonRemoveParameter);
				
			
				
			panelAddNewParameter.add(labelAddNewParameter, BorderLayout.NORTH);
			panelAddNewParameter.add(panelParameterInformation, BorderLayout.CENTER);
			panelAddNewParameter.add(panelAddParameterButtons, BorderLayout.SOUTH);
			panelAddNewParameter.setBorder(BorderFactory.createEmptyBorder(5, 5, 25, 5));
			
		panelParameterValues = new JPanel(new BorderLayout());
			labelParameterValues = new JLabel("Values for experimental parameters");
			scrollParameterValues = new JScrollPane();
			tableParameterValues = new JTable();
			scrollParameterValues.add(tableParameterValues);
			panelParameterValues.add(labelParameterValues, BorderLayout.NORTH);
			panelParameterValues.add(scrollParameterValues, BorderLayout.CENTER);
		
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
