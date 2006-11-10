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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.batchmode.BatchModeController;
import net.sf.mzmine.batchmode.BatchModeController.BatchModeStep;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.util.GUIUtils;

class BatchModeDialog extends JDialog implements ActionListener {

    static final int PADDING_SIZE = 5;
    
    private BatchMode batchModeModule;
    private Hashtable<BatchModeStep, ArrayList<Method>> registeredMethods;
    
    private JComboBox methodForRawDataFilter1;
    private JComboBox methodForRawDataFilter2;
    private JComboBox methodForRawDataFilter3;
    private JComboBox methodForPeakDetection;
    private JComboBox methodForPeakListProcessor1;
    private JComboBox methodForPeakListProcessor2;
    private JComboBox methodForPeakListProcessor3;
    
    
    
    // dialog components
    private JButton btnOK, btnCancel;
    
    public BatchModeDialog(Frame owner, Hashtable<BatchModeStep, ArrayList<Method>> registeredMethods) {
    	
        // Make dialog modal
        super(owner, "Batch mode setup", true);
        
        this.registeredMethods = registeredMethods;
        
        initComponents();
        
        // finalize the dialog
		pack();
		setLocationRelativeTo(owner);
		

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();
        
        if (src == btnOK) {
            
            dispose();

        }

        if (src == btnCancel) {
            dispose();
        }
        
    }
    
    
    private void initComponents() {

    	/*
    	GridBagConstraints constraints = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();
        JPanel components = new JPanel(layout);
        */
    	
		// Panel where everything is collected
		JPanel pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(pnlAll);

		// Two more panels: one for labels and another for text fields
		JPanel pnlLabels = new JPanel(new GridLayout(0,1));
		JPanel pnlFields = new JPanel(new GridLayout(0,1));
		
		pnlLabels.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		pnlFields.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		
		// Raw data filter 1
		Object[] o 	= initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodRawDataFilter1); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);

		// Raw data filter 2
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodRawDataFilter2); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);
 
		// Raw data filter 3
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodRawDataFilter3); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);
		
		// Peak picker
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodPeakPicker); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);
		
		// Peak list processor 1
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodPeakListProcessor1); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);		

		// Peak list processor 2
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodPeakListProcessor2); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);
		
		// Peak list processor 3
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodPeakListProcessor3); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);		

		// Alignment method
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodAligner); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);		

		// Alignment result processor 1
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodAlignmentProcessor1); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);			

		// Alignment result processor 2
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodAlignmentProcessor2); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);		

		// Alignment result processor 3
		o = initializeSingleItem(BatchModeStep.RAWDATAFILTERING, BatchModeParameters.methodAlignmentProcessor3); 
		pnlFields.add((JComboBox)o[0]);
		pnlLabels.add((JLabel)o[1]);		
		
		// Setup buttons
        JPanel pnlButtons = new JPanel();
        btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
        btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);

        // Add everything to main panel
		pnlAll.add(pnlLabels,BorderLayout.CENTER);
		pnlAll.add(pnlFields,BorderLayout.LINE_END);
		pnlAll.add(pnlButtons,BorderLayout.SOUTH);
        
        
        //GUIUtils.addMargin(pnlAll, PADDING_SIZE);
        add(pnlAll);
    	
    }
    
    private Object[] initializeSingleItem(BatchModeStep step, Parameter param) {
    	
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
		
		Object o[] = new Object[2];
		o[0] = comboBox;
		o[1] = label;
		return o;
    	
    }

}