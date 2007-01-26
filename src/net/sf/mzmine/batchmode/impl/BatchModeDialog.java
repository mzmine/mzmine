/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.batchmode.impl;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import net.sf.mzmine.batchmode.BatchModeController;
import net.sf.mzmine.batchmode.BatchModeController.BatchModeStep;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;
import net.sf.mzmine.data.impl.SimpleParameterValue;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.util.GUIUtils;

class BatchModeDialog extends JDialog implements ActionListener {

    static final int PADDING_SIZE = 5;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private BatchMode batchModeModule;
    private Hashtable<BatchModeStep, ArrayList<Method>> registeredMethods;
    
    private JComboBox methodForRawDataFilter1;
    private JComboBox methodForRawDataFilter2;
    private JComboBox methodForRawDataFilter3;
    private JComboBox methodForPeakDetection;
    private JComboBox methodForPeakListProcessor1;
    private JComboBox methodForPeakListProcessor2;
    private JComboBox methodForPeakListProcessor3;
    private JComboBox methodForAlignment;
    private JComboBox methodForAlignmentProcessor1;
    private JComboBox methodForAlignmentProcessor2;
    private JComboBox methodForAlignmentProcessor3;
    
    
    
    private Hashtable<JButton, JComboBox> comboForButton;
    private Hashtable<JComboBox, JButton> buttonForCombo;
    
    private BatchModeParameters parameters;
    
    // dialog components
    private JButton btnOK, btnCancel;
    
    public BatchModeDialog(Frame owner, Hashtable<BatchModeStep, ArrayList<Method>> registeredMethods, BatchModeParameters parameters) {
    	
        // Make dialog modal
        super(owner, "Batch mode setup", true);

        this.registeredMethods = registeredMethods;
        this.parameters = parameters;
        
        initComponents();
        
        fetchParameterValues();
        
        // finalize the dialog
		pack();
		setLocationRelativeTo(owner);
		

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();
            
        if (src.getClass() == JButton.class) {
        	      	
	        if (comboForButton.containsKey(src)) {
	        	JComboBox comboBox = comboForButton.get(src);
	        	Method method = (Method)comboBox.getSelectedItem();
	        	method.askParameters();
	        }
	        
	        if (src == btnOK) {           
	            storeParameterValues();
	            dispose();
	        }
	
	        if (src == btnCancel) {
	            dispose();
	        }
	        
        }
        
        if (src.getClass() == JComboBox.class) {
        	
        	JComboBox combo = (JComboBox)src;
        	JButton btn = buttonForCombo.get(combo);
        	
        	if (combo.getSelectedIndex()==0) btn.setEnabled(false);
        	else btn.setEnabled(true);
        	
        	if (combo == methodForPeakDetection) {
        		if (combo.getSelectedIndex()==0) {
        			methodForPeakListProcessor1.setEnabled(false); methodForPeakListProcessor1.setSelectedIndex(0);
        			methodForPeakListProcessor2.setEnabled(false); methodForPeakListProcessor2.setSelectedIndex(0);
        			methodForPeakListProcessor3.setEnabled(false); methodForPeakListProcessor3.setSelectedIndex(0);
        			methodForAlignment.setEnabled(false); methodForAlignment.setSelectedIndex(0);
        			methodForAlignmentProcessor1.setEnabled(false); methodForAlignmentProcessor1.setSelectedIndex(0);
        			methodForAlignmentProcessor2.setEnabled(false); methodForAlignmentProcessor2.setSelectedIndex(0);
        			methodForAlignmentProcessor3.setEnabled(false); methodForAlignmentProcessor3.setSelectedIndex(0);
        		} else {
        			methodForPeakListProcessor1.setEnabled(true);
        			methodForPeakListProcessor2.setEnabled(true);
        			methodForPeakListProcessor3.setEnabled(true);
        			methodForAlignment.setEnabled(true);
        		}     		
        	}
        	
        	if (combo == methodForAlignment) {
        		if (combo.getSelectedIndex()==0) {
        			methodForAlignmentProcessor1.setEnabled(false); methodForAlignmentProcessor1.setSelectedIndex(0);
        			methodForAlignmentProcessor2.setEnabled(false); methodForAlignmentProcessor2.setSelectedIndex(0);
        			methodForAlignmentProcessor3.setEnabled(false); methodForAlignmentProcessor3.setSelectedIndex(0);

        		} else {
        			methodForAlignmentProcessor1.setEnabled(true);
        			methodForAlignmentProcessor2.setEnabled(true);
        			methodForAlignmentProcessor3.setEnabled(true);
        			
        		}
        	}
        	
        	
        }
        
    }
    
    
    private void initComponents() {

    	/*
    	GridBagConstraints constraints = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();
        JPanel components = new JPanel(layout);
        */
    	
    	comboForButton = new Hashtable<JButton, JComboBox>();
    	buttonForCombo = new Hashtable<JComboBox, JButton>();
    	
		// Panel where everything is collected
		JPanel pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(pnlAll);

		// Two more panels: one for labels and another for text fields
		JPanel pnlLabels = new JPanel(new GridLayout(0,1));
		JPanel pnlFields = new JPanel(new GridLayout(0,1));
		JPanel pnlParamButtons = new JPanel(new GridLayout(0,1)); // CONTINUE FROM HERE pnlButtons is bad (used) name 
		
		pnlLabels.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		pnlFields.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		pnlParamButtons.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		
		// Raw data filter 1
		methodForRawDataFilter1 = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodRawDataFilter1, pnlLabels, pnlFields, pnlParamButtons); 
		
		// Raw data filter 2
		methodForRawDataFilter2 = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodRawDataFilter2, pnlLabels, pnlFields, pnlParamButtons);
		
		// Raw data filter 3
		methodForRawDataFilter3 = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodRawDataFilter3, pnlLabels, pnlFields, pnlParamButtons);	
		
		// Peak picker
		methodForPeakDetection = initializeSingleItem(BatchModeStep.PEAKPICKING, BatchModeParameters.methodPeakPicker, pnlLabels, pnlFields, pnlParamButtons);	
		
		// Peak list processor 1
		methodForPeakListProcessor1 = initializeSingleItem(BatchModeStep.PEAKLISTPROCESSING, BatchModeParameters.methodPeakListProcessor1, pnlLabels, pnlFields, pnlParamButtons); 	
		
		// Peak list processor 2
		methodForPeakListProcessor2 = initializeSingleItem(BatchModeStep.PEAKLISTPROCESSING, BatchModeParameters.methodPeakListProcessor2, pnlLabels, pnlFields, pnlParamButtons); 		
		
		// Peak list processor 3
		methodForPeakListProcessor3 = initializeSingleItem(BatchModeStep.PEAKLISTPROCESSING, BatchModeParameters.methodPeakListProcessor3, pnlLabels, pnlFields, pnlParamButtons); 

		// Alignment method
		methodForAlignment = initializeSingleItem(BatchModeStep.ALIGNMENT, BatchModeParameters.methodAligner, pnlLabels, pnlFields, pnlParamButtons); 

		// Alignment result processor 1
		methodForAlignmentProcessor1 = initializeSingleItem(BatchModeStep.ALIGNMENTPROCESSING, BatchModeParameters.methodAlignmentProcessor1, pnlLabels, pnlFields, pnlParamButtons); 

		// Alignment result processor 2
		methodForAlignmentProcessor2 = initializeSingleItem(BatchModeStep.ALIGNMENTPROCESSING, BatchModeParameters.methodAlignmentProcessor2, pnlLabels, pnlFields, pnlParamButtons); 

		// Alignment result processor 3
		methodForAlignmentProcessor3 = initializeSingleItem(BatchModeStep.ALIGNMENTPROCESSING, BatchModeParameters.methodAlignmentProcessor3, pnlLabels, pnlFields, pnlParamButtons); 
		
		
		
		// Setup buttons
        JPanel pnlButtons = new JPanel();
        btnOK = GUIUtils.addButton(pnlButtons, "Run batch", null, this);
        btnCancel = GUIUtils.addButton(pnlButtons, "Quit batch mode", null, this);

        // Add everything to main panel
		JPanel pnlTmp = new JPanel();
		pnlTmp.setLayout(new BorderLayout());
		pnlTmp.add(pnlLabels, BorderLayout.WEST);
		pnlTmp.add(pnlFields, BorderLayout.CENTER);
		pnlTmp.add(pnlParamButtons, BorderLayout.EAST);
		
		pnlAll.add(pnlTmp ,BorderLayout.CENTER);
		pnlAll.add(pnlButtons,BorderLayout.SOUTH);
              
        //GUIUtils.addMargin(pnlAll, PADDING_SIZE);
        add(pnlAll);
    	
    }
    
    private void fetchParameterValues() {
    	// Setup default values    	
    	methodForRawDataFilter1.setEnabled(true); methodForRawDataFilter1.setSelectedIndex(0);
    	methodForRawDataFilter2.setEnabled(true); methodForRawDataFilter2.setSelectedIndex(0);
    	methodForRawDataFilter3.setEnabled(true); methodForRawDataFilter3.setSelectedIndex(0);
    	methodForPeakDetection.setEnabled(true); methodForPeakDetection.setSelectedIndex(0);
    	
    	// Check if there are some parameter values already setup in the project for batch mode, and copy these settings to the dialog if they are present
    	ParameterValue paramValue = parameters.getParameterValue(BatchModeParameters.methodRawDataFilter1);
    	if (paramValue!=null) methodForRawDataFilter1.setSelectedItem((Method)paramValue.getValue());
    	
    	paramValue = parameters.getParameterValue(BatchModeParameters.methodRawDataFilter2);
    	if (paramValue!=null) methodForRawDataFilter2.setSelectedItem((Method)paramValue.getValue());

    	paramValue = parameters.getParameterValue(BatchModeParameters.methodRawDataFilter3);
    	if (paramValue!=null) methodForRawDataFilter3.setSelectedItem((Method)paramValue.getValue());
    	
    	paramValue = parameters.getParameterValue(BatchModeParameters.methodPeakPicker);
    	if (paramValue!=null) methodForPeakDetection.setSelectedItem((Method)paramValue.getValue());

    	paramValue = parameters.getParameterValue(BatchModeParameters.methodPeakListProcessor1);
    	if (paramValue!=null) methodForPeakListProcessor1.setSelectedItem((Method)paramValue.getValue());    	

    	paramValue = parameters.getParameterValue(BatchModeParameters.methodPeakListProcessor2);
    	if (paramValue!=null) methodForPeakListProcessor2.setSelectedItem((Method)paramValue.getValue());

    	paramValue = parameters.getParameterValue(BatchModeParameters.methodPeakListProcessor3);
    	if (paramValue!=null) methodForPeakListProcessor3.setSelectedItem((Method)paramValue.getValue());

    	paramValue = parameters.getParameterValue(BatchModeParameters.methodAligner);
    	if (paramValue!=null) methodForAlignment.setSelectedItem((Method)paramValue.getValue());    	

    	paramValue = parameters.getParameterValue(BatchModeParameters.methodAlignmentProcessor1);
    	if (paramValue!=null) methodForAlignmentProcessor1.setSelectedItem((Method)paramValue.getValue());
    	
    	paramValue = parameters.getParameterValue(BatchModeParameters.methodAlignmentProcessor2);
    	if (paramValue!=null) methodForAlignmentProcessor2.setSelectedItem((Method)paramValue.getValue());
    	
    	paramValue = parameters.getParameterValue(BatchModeParameters.methodAlignmentProcessor3);
    	if (paramValue!=null) methodForAlignmentProcessor3.setSelectedItem((Method)paramValue.getValue());
    	
    }
    
    private void storeParameterValues() {
    	
    	if (methodForRawDataFilter1.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodRawDataFilter1, new SimpleParameterValue((Method)methodForRawDataFilter1.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodRawDataFilter1);

    	if (methodForRawDataFilter2.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodRawDataFilter2, new SimpleParameterValue((Method)methodForRawDataFilter2.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodRawDataFilter2);
    	
    	if (methodForRawDataFilter3.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodRawDataFilter3, new SimpleParameterValue((Method)methodForRawDataFilter3.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodRawDataFilter3);    	

    	if (methodForPeakDetection.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodPeakPicker, new SimpleParameterValue((Method)methodForPeakDetection.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodPeakPicker);    	

    	if (methodForPeakListProcessor1.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodPeakListProcessor1, new SimpleParameterValue((Method)methodForPeakListProcessor1.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodPeakListProcessor1);

    	if (methodForPeakListProcessor2.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodPeakListProcessor2, new SimpleParameterValue((Method)methodForPeakListProcessor2.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodPeakListProcessor2);

    	if (methodForPeakListProcessor3.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodPeakListProcessor3, new SimpleParameterValue((Method)methodForPeakListProcessor3.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodPeakListProcessor3);

    	if (methodForAlignment.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodAligner, new SimpleParameterValue((Method)methodForAlignment.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodAligner);    	

    	if (methodForAlignmentProcessor1.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodAlignmentProcessor1, new SimpleParameterValue((Method)methodForAlignmentProcessor1.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodAlignmentProcessor1);    	

    	if (methodForAlignmentProcessor2.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodAlignmentProcessor2, new SimpleParameterValue((Method)methodForAlignmentProcessor2.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodAlignmentProcessor2);    	

    	if (methodForAlignmentProcessor3.getSelectedIndex()>0) 
    		parameters.setParameterValue(BatchModeParameters.methodAlignmentProcessor3, new SimpleParameterValue((Method)methodForAlignmentProcessor3.getSelectedItem()));
    	else parameters.removeParameterValue(BatchModeParameters.methodAlignmentProcessor3);    	
    	
    }
    
    private JComboBox initializeSingleItem(BatchModeStep step, Parameter param, JPanel pnlLabels, JPanel pnlFields, JPanel pnlParamButtons) {
    	
    	JComboBox comboBox = null;
    	JLabel label = null;
    	
    	MZmineProject project = MZmineProject.getCurrentProject();
		ParameterValue paramValue = project.getParameterValue(param);
		comboBox = new JComboBox();
		comboBox.addItem("not in use");
		ArrayList<Method> methodsArrayList = registeredMethods.get(step);
		if ( (methodsArrayList!=null) && (methodsArrayList.size()>0) ) {
			Method[] methods = methodsArrayList.toArray(new Method[0]);
			for (Method m : methods) comboBox.addItem(m);
		}
		if ((paramValue!=null) && (paramValue.getValue()!=null)) comboBox.setSelectedItem(paramValue.getValue());
		label = new JLabel(param.getName());
		comboBox.setToolTipText(param.getDescription());
		
		JButton button = new JButton("Parameters...");
		
		button.setEnabled(false);
		pnlFields.add(comboBox);
		pnlLabels.add(label);		
		pnlParamButtons.add(button);
		comboForButton.put(button, comboBox);
		buttonForCombo.put(comboBox, button);
		
		button.addActionListener(this);
		comboBox.addActionListener(this);
		
		return comboBox;
    	
    }

}