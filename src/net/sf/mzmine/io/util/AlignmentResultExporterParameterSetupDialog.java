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

package net.sf.mzmine.io.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 * This class represent a dialog for adjusting parameter values for alignment result exporting
 *
 * @version 30 March 2006
 */
public class AlignmentResultExporterParameterSetupDialog extends JDialog implements java.awt.event.ActionListener {

	// CONSTANTS

	// Names for different export types
	private final String[] exportTypeNames = {"Compact", "Full", "Custom" };



	// VARIABLES

	// Main panel: everything
	private JPanel pnlAll;

	// Top: selector for export type
	private JPanel pnlTop;
	private JRadioButton rbExportTypeCompact;
	private JRadioButton rbExportTypeWide;
	private JRadioButton rbExportTypeCustom;
	private ButtonGroup bgrpExportType;

	// Left: selection for common columns
	private JPanel pnlLeft;
	private JLabel lblCommonColsTitle;
	private JLabel lblCommonColsStandardCompoundInformation;
	private JCheckBox cbCommonColsStandardCompoundInformation;
	private JLabel lblCommonColsIsotopePatternInformation;
	private JCheckBox cbCommonColsIsotopePatternInformation;
	private JLabel lblCommonColsAverageMZRT;
	private JCheckBox cbCommonColsAverageMZRT;
	private JLabel lblCommonColsChargeState;
	private JCheckBox cbCommonColsChargeState;
	private JLabel lblCommonColsNumFound;
	private JCheckBox cbCommonColsNumFound;


	// Right: selection for raw data specific columns
	private JPanel pnlRight;
	private JLabel lblRawDataColsTitle;
	private JLabel lblRawDataColsPeakMZ;
	private JCheckBox cbRawDataColsPeakMZ;
	private JLabel lblRawDataColsPeakRT;
	private JCheckBox cbRawDataColsPeakRT;
	private JLabel lblRawDataColsPeakHeight;
	private JCheckBox cbRawDataColsPeakHeight;
	private JLabel lblRawDataColsPeakArea;
	private JCheckBox cbRawDataColsPeakArea;
	private JLabel lblRawDataColsPeakStatus;
	private JCheckBox cbRawDataColsPeakStatus;


	// Bottom: OK & cancel buttons
	private JPanel pnlBottom;
	private JButton btnOK;
	private JButton btnCancel;


	// Parameters
	private AlignmentResultExporterParameters parameters;
	//private GeneralParameters generalParameters;

	// Exit code (1=OK, -1=Cancel)
	private int exitCode = -1;


    /**
     * Initializes dialog
     *
     * @param	generalParameters		MZmine GeneralParameters. There are required for determining current peak measurement type
     * @param	_parameters				Current AlignmentResultExporter parameter settings
     *
     */
    public AlignmentResultExporterParameterSetupDialog( AlignmentResultExporterParameters _parameters) {

		//generalParameters = _generalParameters;
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
			dispose();
		}

		// Cancel button
		if (src == btnCancel) {

			// Set exit code
			exitCode = -1;

			// Hide form
			dispose();

		}

		// COMPACT radio button
		if (src == rbExportTypeCompact) {

			// Set selections to COMPACT
	//		parameters.setExportType(AlignmentResultExporterParameters.EXPORTTYPE_COMPACT, generalParameters);
			getSettingsToForm();

		}

		// WIDE radio button
		if (src == rbExportTypeWide) {

			// Set selections to WIDE
		//	parameters.setExportType(AlignmentResultExporterParameters.EXPORTTYPE_WIDE, generalParameters);
			getSettingsToForm();

		}

		// CUSTOM radio button
		if (src == rbExportTypeCustom) {

		//	// Set selections to CUSTOM
		//	parameters.setExportType(AlignmentResultExporterParameters.EXPORTTYPE_CUSTOM, generalParameters);
			getSettingsToForm();
		}

		// These don't do anything at the moment
		if (src == cbCommonColsStandardCompoundInformation) {}
		if (src == cbCommonColsIsotopePatternInformation) {}
		if (src == cbCommonColsAverageMZRT) {}
		if (src == cbRawDataColsPeakMZ) {}
		if (src == cbRawDataColsPeakRT) {}
		if (src == cbRawDataColsPeakHeight) {}
		if (src == cbRawDataColsPeakArea) {}
		if (src == cbRawDataColsPeakStatus) {}

	}

	/**
	 * Enables/disables all column specific controls
	 */
	private void setEnabledOnAllColumnControls(boolean b) {

			// labels
			lblCommonColsStandardCompoundInformation.setEnabled(b);
			lblCommonColsIsotopePatternInformation.setEnabled(b);
			lblCommonColsAverageMZRT.setEnabled(b);
			lblCommonColsChargeState.setEnabled(b);
			lblCommonColsNumFound.setEnabled(b);

			lblRawDataColsPeakMZ.setEnabled(b);
			lblRawDataColsPeakRT.setEnabled(b);
			lblRawDataColsPeakHeight.setEnabled(b);
			lblRawDataColsPeakArea.setEnabled(b);
			lblRawDataColsPeakStatus.setEnabled(b);


			// check boxes
			cbCommonColsStandardCompoundInformation.setEnabled(b);
			cbCommonColsIsotopePatternInformation.setEnabled(b);
			cbCommonColsChargeState.setEnabled(b);
			cbCommonColsAverageMZRT.setEnabled(b);
			cbCommonColsNumFound.setEnabled(b);

			cbRawDataColsPeakMZ.setEnabled(b);
			cbRawDataColsPeakRT.setEnabled(b);
			cbRawDataColsPeakHeight.setEnabled(b);
			cbRawDataColsPeakArea.setEnabled(b);
			cbRawDataColsPeakStatus.setEnabled(b);

	}

	/**
	 * Initializes all GUI components
	 */
    private void initComponents() {

		// Set title of the dialog

		setTitle("Select alignment result export type");


		// Main panel

		pnlAll = new JPanel();
		pnlAll.setLayout(new BorderLayout());



		// Top panel

		pnlTop = new JPanel();
		pnlAll.add(pnlTop, BorderLayout.NORTH);

		pnlTop.setLayout(new GridLayout(1,3));
		rbExportTypeCompact = new JRadioButton(exportTypeNames[0]);
		rbExportTypeWide = new JRadioButton(exportTypeNames[1]);
		rbExportTypeCustom = new JRadioButton(exportTypeNames[2]);
		rbExportTypeCompact.addActionListener(this);
		rbExportTypeWide.addActionListener(this);
		rbExportTypeCustom.addActionListener(this);
		bgrpExportType = new ButtonGroup();
		bgrpExportType.add(rbExportTypeCompact);
		bgrpExportType.add(rbExportTypeWide);
		bgrpExportType.add(rbExportTypeCustom);
		pnlTop.add(rbExportTypeCompact);
		pnlTop.add(rbExportTypeWide);
		pnlTop.add(rbExportTypeCustom);



		// Left panel

		pnlLeft = new JPanel();
		pnlAll.add(pnlLeft, BorderLayout.WEST);

		pnlLeft.setLayout(new GridLayout(6,2));

		lblCommonColsTitle = new JLabel("Common columns");
		pnlLeft.add(lblCommonColsTitle);
		pnlLeft.add(new JPanel());

		lblCommonColsStandardCompoundInformation = new JLabel("Standard compound");
		cbCommonColsStandardCompoundInformation = new JCheckBox();
		cbCommonColsStandardCompoundInformation.addActionListener(this);
		pnlLeft.add(lblCommonColsStandardCompoundInformation);
		pnlLeft.add(cbCommonColsStandardCompoundInformation);

		lblCommonColsIsotopePatternInformation = new JLabel("Isotope pattern");
		cbCommonColsIsotopePatternInformation = new JCheckBox();
		cbCommonColsIsotopePatternInformation.addActionListener(this);
		pnlLeft.add(lblCommonColsIsotopePatternInformation);
		pnlLeft.add(cbCommonColsIsotopePatternInformation);

		lblCommonColsChargeState = new JLabel("Charge state");
		cbCommonColsChargeState = new JCheckBox();
		cbCommonColsChargeState.addActionListener(this);
		pnlLeft.add(lblCommonColsChargeState);
		pnlLeft.add(cbCommonColsChargeState);

		lblCommonColsAverageMZRT = new JLabel("Average M/Z and RT");
		cbCommonColsAverageMZRT = new JCheckBox();
		cbCommonColsAverageMZRT.addActionListener(this);
		pnlLeft.add(lblCommonColsAverageMZRT);
		pnlLeft.add(cbCommonColsAverageMZRT);

		lblCommonColsNumFound = new JLabel("Number of found peaks");
		cbCommonColsNumFound = new JCheckBox();
		cbCommonColsNumFound.addActionListener(this);
		pnlLeft.add(lblCommonColsNumFound);
		pnlLeft.add(cbCommonColsNumFound);



		// Right panel

		pnlRight = new JPanel();
		pnlAll.add(pnlRight, BorderLayout.EAST);

		pnlRight.setLayout(new GridLayout(6,2));

		lblRawDataColsTitle = new JLabel("Raw data columns");
		pnlRight.add(lblRawDataColsTitle);
		pnlRight.add(new JPanel());

		lblRawDataColsPeakMZ = new JLabel("Peak M/Z");
		cbRawDataColsPeakMZ = new JCheckBox();
		cbRawDataColsPeakMZ.addActionListener(this);
		pnlRight.add(lblRawDataColsPeakMZ);
		pnlRight.add(cbRawDataColsPeakMZ);

		lblRawDataColsPeakRT = new JLabel("Peak RT");
		cbRawDataColsPeakRT = new JCheckBox();
		cbRawDataColsPeakRT.addActionListener(this);
		pnlRight.add(lblRawDataColsPeakRT);
		pnlRight.add(cbRawDataColsPeakRT);

		lblRawDataColsPeakHeight = new JLabel("Peak height");
		cbRawDataColsPeakHeight = new JCheckBox();
		cbRawDataColsPeakHeight.addActionListener(this);
		pnlRight.add(lblRawDataColsPeakHeight);
		pnlRight.add(cbRawDataColsPeakHeight);

		lblRawDataColsPeakArea = new JLabel("Peak area");
		cbRawDataColsPeakArea = new JCheckBox();
		cbRawDataColsPeakArea.addActionListener(this);
		pnlRight.add(lblRawDataColsPeakArea);
		pnlRight.add(cbRawDataColsPeakArea);


		lblRawDataColsPeakStatus = new JLabel("Peak status");
		cbRawDataColsPeakStatus = new JCheckBox();
		cbRawDataColsPeakStatus.addActionListener(this);
		pnlRight.add(lblRawDataColsPeakStatus);
		pnlRight.add(cbRawDataColsPeakStatus);



		// Bottom panel

		pnlBottom = new JPanel();
		pnlAll.add(pnlBottom, BorderLayout.SOUTH);

		pnlBottom.setLayout(new GridLayout(1,2));

		btnOK = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();

		btnOK.setText("OK");
		btnCancel.setText("Cancel");
		btnOK.addActionListener(this);
		btnCancel.addActionListener(this);
		pnlBottom.setLayout(new FlowLayout(FlowLayout.RIGHT));
		pnlBottom.add(btnOK);
		pnlBottom.add(btnCancel);



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
	public AlignmentResultExporterParameters getParameters() {
		return parameters;
	}



	/**
	 * Transfers settings from parameters object to dialog controls
	 */
	private void getSettingsToForm() {

		// Common columns

		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_STANDARD))			{ cbCommonColsStandardCompoundInformation.setSelected(true); } else { cbCommonColsStandardCompoundInformation.setSelected(false); }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_ISOTOPEPATTERNID))	{ cbCommonColsIsotopePatternInformation.setSelected(true); } else {	cbCommonColsIsotopePatternInformation.setSelected(false); }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_CHARGESTATE))	{ cbCommonColsChargeState.setSelected(true); } else {	cbCommonColsChargeState.setSelected(false); }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_AVERAGEMZ)) 	{ cbCommonColsAverageMZRT.setSelected(true); } else { cbCommonColsAverageMZRT.setSelected(false); }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_NUMFOUND)) 		{ cbCommonColsNumFound.setSelected(true); } else { cbCommonColsNumFound.setSelected(false); }


		// Raw data columns

		if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_MZ))		{ cbRawDataColsPeakMZ.setSelected(true); } else { cbRawDataColsPeakMZ.setSelected(false); }
		if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_RT))		{ cbRawDataColsPeakRT.setSelected(true); } else { cbRawDataColsPeakRT.setSelected(false); }
		if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_HEIGHT))	{ cbRawDataColsPeakHeight.setSelected(true); } else { cbRawDataColsPeakHeight.setSelected(false); }
		if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_AREA))	{ cbRawDataColsPeakArea.setSelected(true); } else { cbRawDataColsPeakArea.setSelected(false); }
		if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_STATUS))	{ cbRawDataColsPeakStatus.setSelected(true); } else { cbRawDataColsPeakStatus.setSelected(false); }

		if (parameters.getExportType() == AlignmentResultExporterParameters.EXPORTTYPE_COMPACT) {
			setEnabledOnAllColumnControls(false);
			bgrpExportType.setSelected(rbExportTypeCompact.getModel(), true);
		}

		if (parameters.getExportType() == AlignmentResultExporterParameters.EXPORTTYPE_WIDE) {
			setEnabledOnAllColumnControls(false);
			bgrpExportType.setSelected(rbExportTypeWide.getModel(), true);
		}

		if (parameters.getExportType() == AlignmentResultExporterParameters.EXPORTTYPE_CUSTOM) {
			setEnabledOnAllColumnControls(true);
			bgrpExportType.setSelected(rbExportTypeCustom.getModel(), true);
		}

	}

	/**
	 * Transfers settings from dialog controls to parameters object
	 */
	private void setSettingsFromForm() {

		// Clear previous settings

		parameters.clearAllSelections();



		// Common columns

		if (cbCommonColsStandardCompoundInformation.isSelected()) {
			parameters.addCommonCol(AlignmentResultExporterParameters.COMMONCOLS_STANDARD);
		}

		if (cbCommonColsIsotopePatternInformation.isSelected()) {
			parameters.addCommonCol(AlignmentResultExporterParameters.COMMONCOLS_ISOTOPEPATTERNID);
			parameters.addCommonCol(AlignmentResultExporterParameters.COMMONCOLS_ISOTOPEPEAKNUMBER);
		}

		if (cbCommonColsChargeState.isSelected()) {
			parameters.addCommonCol(AlignmentResultExporterParameters.COMMONCOLS_CHARGESTATE);
		}

		if (cbCommonColsAverageMZRT.isSelected()) {
			parameters.addCommonCol(AlignmentResultExporterParameters.COMMONCOLS_AVERAGEMZ);
			parameters.addCommonCol(AlignmentResultExporterParameters.COMMONCOLS_AVERAGERT);
		}
		if (cbCommonColsNumFound.isSelected()) {
			parameters.addCommonCol(AlignmentResultExporterParameters.COMMONCOLS_NUMFOUND);
		}



		// Raw data columns

		if (cbRawDataColsPeakMZ.isSelected()) { parameters.addRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_MZ); }
		if (cbRawDataColsPeakRT.isSelected()) { parameters.addRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_RT); }
		if (cbRawDataColsPeakHeight.isSelected()) { parameters.addRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_HEIGHT); }
		if (cbRawDataColsPeakArea.isSelected()) { parameters.addRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_AREA); }
		if (cbRawDataColsPeakStatus.isSelected()) { parameters.addRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_STATUS); }

	}


}




