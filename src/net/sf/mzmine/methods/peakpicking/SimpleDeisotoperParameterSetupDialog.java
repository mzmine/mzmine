/*
    Copyright 2005-2006 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.methods.peakpicking;

import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;

import java.util.*;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;

import java.text.NumberFormat;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;


/**
 * This class represent a dialog for adjusting parameter values for simple deisotoper method
 *
 * @version 30 March 2006
 */
public class SimpleDeisotoperParameterSetupDialog extends ModalJInternalFrame implements java.awt.event.ActionListener {


	// VARIABLES

	// Main panel: everything
	private JPanel pnlAll;

	// Middle: contains parameter selections
	private JPanel pnlMiddle;

	private JLabel lblChargeStatesTitle;
	private JPanel pnlChargePanel;
	private JLabel lblChargeStatesOne;
	private JCheckBox cbChargeStatesOne;
	private JLabel lblChargeStatesTwo;
	private JCheckBox cbChargeStatesTwo;
	private JLabel lblChargeStatesThree;
	private JCheckBox cbChargeStatesThree;

	private JLabel lblMonotonicShape;
	private JCheckBox cbMonotonicShape;

	private JLabel lblRTTolerance;
	private JFormattedTextField txtRTTolerance;

	private JLabel lblMZTolerance;
	private JFormattedTextField txtMZTolerance;

	// Bottom: OK & cancel buttons
	private JPanel pnlBottom;
	private JButton btnOK;
	private JButton btnCancel;


	// Parameters
	private SimpleDeisotoperParameters parameters;


	// Exit code (1=OK, -1=Cancel)
	private int exitCode = -1;


    /**
     * Initializes dialog
     *
     * @param	generalParameters		MZmine GeneralParameters. There are required for determining current peak measurement type
     * @param	_parameters				Current AlignmentResultExporter parameter settings
     *
     */
    public SimpleDeisotoperParameterSetupDialog(SimpleDeisotoperParameters _parameters) {

		parameters = _parameters;

		// Build the form
        initComponents();

		// Put current parameter settings to form
        getSettingsToForm();

    }

	/**
	 * Implementation of ActionListener interface
	 */
    public void actionPerformed(java.awt.event.ActionEvent e) {
		Object src = e.getSource();

		// OK button
		if (src == btnOK) {

			// Store current settings to parameters object
			setSettingsFromForm();

			// Set exit code
			exitCode = 1;

			// Hide form
			disposeModal();
		}

		// Cancel button
		if (src == btnCancel) {

			// Set exit code
			exitCode = -1;

			// Hide form
			disposeModal();

		}

	}


	/**
	 * Initializes all GUI components
	 */
    private void initComponents() {

		// Set title of the dialog

		setTitle("Set parameter values");


		pnlAll = new JPanel();
		pnlAll.setLayout(new BorderLayout());

		// Middle: contains parameter selections
		pnlMiddle = new JPanel();
		pnlMiddle.setLayout(new GridLayout(4,2));

		lblChargeStatesTitle = new JLabel("Charge states");

		pnlChargePanel = new JPanel();
		lblChargeStatesOne = new JLabel("+1");
		cbChargeStatesOne = new JCheckBox();
		lblChargeStatesOne.setLabelFor(cbChargeStatesOne);
		lblChargeStatesTwo = new JLabel("+2");
		cbChargeStatesTwo = new JCheckBox();
		lblChargeStatesTwo.setLabelFor(cbChargeStatesTwo);
		lblChargeStatesThree = new JLabel("+3");
		cbChargeStatesThree = new JCheckBox();
		lblChargeStatesThree.setLabelFor(cbChargeStatesThree);

		pnlChargePanel.add(lblChargeStatesOne);
		pnlChargePanel.add(cbChargeStatesOne);
		pnlChargePanel.add(lblChargeStatesTwo);
		pnlChargePanel.add(cbChargeStatesTwo);
		pnlChargePanel.add(lblChargeStatesThree);
		pnlChargePanel.add(cbChargeStatesThree);

		lblMonotonicShape = new JLabel("Monotonic pattern shape");
		cbMonotonicShape = new JCheckBox();

		NumberFormat rtNumberFormat = NumberFormat.getNumberInstance(); rtNumberFormat.setMinimumFractionDigits(1);
		lblRTTolerance = new JLabel("RT tolerance");
		txtRTTolerance = new JFormattedTextField(rtNumberFormat);

		NumberFormat mzNumberFormat = NumberFormat.getNumberInstance(); mzNumberFormat.setMinimumFractionDigits(3);
		lblMZTolerance = new JLabel("M/Z tolerance");
		txtMZTolerance = new JFormattedTextField(mzNumberFormat);


		pnlMiddle.add(lblChargeStatesTitle);
		pnlMiddle.add(pnlChargePanel);
		pnlMiddle.add(lblMonotonicShape);
		pnlMiddle.add(cbMonotonicShape);
		pnlMiddle.add(lblRTTolerance);
		pnlMiddle.add(txtRTTolerance);
		pnlMiddle.add(lblMZTolerance);
		pnlMiddle.add(txtMZTolerance);

		// Bottom: OK & cancel buttons
		JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnOK = new JButton();
		btnOK.setText("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton();
		btnCancel.setText("Cancel");
		btnCancel.addActionListener(this);

		pnlBottom.add(btnOK);
		pnlBottom.add(btnCancel);

		pnlAll.add(pnlMiddle, BorderLayout.CENTER);
		pnlAll.add(pnlBottom, BorderLayout.SOUTH);


		// Finally add everything to the main pane

        getContentPane().add(pnlAll, java.awt.BorderLayout.CENTER);

        pack();

    }



	/**
	 * Returns exit code
	 * @return	1=OK clicked, -1=cancel clicked
	 */
	public int getExitCode() {
		return exitCode;
	}



	/**
	 * Returns parameter settings
	 */
	public SimpleDeisotoperParameters getParameters() {
		return parameters;
	}



	/**
	 * Transfers settings from parameters object to dialog controls
	 */
	private void getSettingsToForm() {

			if (parameters.monotonicShape) { cbMonotonicShape.setSelected(true); } else { cbMonotonicShape.setSelected(false); }

			if (parameters.chargeStates.contains(SimpleDeisotoperParameters.chargeOne)) { cbChargeStatesOne.setSelected(true); } else { cbChargeStatesOne.setSelected(false); }
			if (parameters.chargeStates.contains(SimpleDeisotoperParameters.chargeTwo)) { cbChargeStatesTwo.setSelected(true); } else { cbChargeStatesTwo.setSelected(false); }
			if (parameters.chargeStates.contains(SimpleDeisotoperParameters.chargeThree)) { cbChargeStatesThree.setSelected(true); } else { cbChargeStatesThree.setSelected(false); }

			txtRTTolerance.setValue(parameters.rtTolerance);
			txtMZTolerance.setValue(parameters.mzTolerance);

	}

	/**
	 * Transfers settings from dialog controls to parameters object
	 */
	private void setSettingsFromForm() {

		if (cbMonotonicShape.isSelected()) { parameters.monotonicShape = true; } else { parameters.monotonicShape = false; }

		parameters.chargeStates.clear();
		if (cbChargeStatesOne.isSelected()) { parameters.chargeStates.add(SimpleDeisotoperParameters.chargeOne); }
		if (cbChargeStatesTwo.isSelected()) { parameters.chargeStates.add(SimpleDeisotoperParameters.chargeTwo); }
		if (cbChargeStatesThree.isSelected()) { parameters.chargeStates.add(SimpleDeisotoperParameters.chargeThree); }

		parameters.rtTolerance = ((Number)(txtRTTolerance.getValue())).doubleValue();
		parameters.mzTolerance = ((Number)(txtMZTolerance.getValue())).doubleValue();

	}


}




