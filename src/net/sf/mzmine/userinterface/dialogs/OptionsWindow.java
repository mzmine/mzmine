/*
    Copyright 2005 VTT Biotechnology

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
package net.sf.mzmine.userinterface.dialogs;
import java.awt.BorderLayout;

import javax.swing.JDesktopPane;
import javax.swing.JPanel;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.GeneralParameters;



/**
 * This class represent a dialog for adjusting various parameter settings
 */
public class OptionsWindow extends javax.swing.JInternalFrame {

    private MainWindow mainWin;
    private GeneralParameters paramSettings;

    /**
     * Constructor
     * Initializes the dialog
     */
    public OptionsWindow(MainWindow _mainWin) {
		mainWin = _mainWin;
		//paramSettings = mainWin.getParameterStorage().getGeneralParameters();
        initComponents();

		getSettingsToForm();

		//JDesktopPane desktop = mainWin.getDesktop();
		//desktop.add(this);
		setVisible(true);
		//setLocation(( desktop.getWidth()-getWidth() ) / 2,  ( desktop.getHeight()-getHeight() ) / 2 );

    }

    /**
     * This method is used by constructor to create all objects for the dialog
     */
    private void initComponents() {//GEN-BEGIN:initComponents

		// Set dialog title and component layout
        setTitle("Preferences");
        getContentPane().setLayout(new java.awt.BorderLayout());

		// This listener calls cancel button method when user tries to unexpectedly close the window
		/*
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                btnCancelActionPerformed(null);
            }
        });
        */
        addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
                btnCancelActionPerformed(null);
            }
        });



		// Buttons are placed on their own panel at the bottom of the dialog
        btnOK = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        btnApply = new javax.swing.JButton();

		JPanel btnPanel = new JPanel();
        btnOK.setText("OK");
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        btnPanel.add(btnOK);

        btnApply.setText("Apply");
        btnApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApplyActionPerformed(evt);
            }
        });
        btnPanel.add(btnApply);

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        btnPanel.add(btnCancel);



		// Parameter settings are divided to tabs in a tabbedpane
        jTabbedPane1 = new javax.swing.JTabbedPane();


		// Tab for visualizer settings

		pnlVisualizerSettings = new javax.swing.JPanel();

        lblRTFormatting = new javax.swing.JLabel();
        rbgrpRTFormatting = new javax.swing.ButtonGroup();
        rbRTInSecs = new javax.swing.JRadioButton();
        rbRTInMinsAndSecs = new javax.swing.JRadioButton();

        lblMZFormatting = new javax.swing.JLabel();
        rbgrpMZFormatting = new javax.swing.ButtonGroup();
        rbMZTwoDecimals = new javax.swing.JRadioButton();
        rbMZThreeDecimals = new javax.swing.JRadioButton();
        rbMZFourDecimals = new javax.swing.JRadioButton();

		rbgrpTypeOfData = new javax.swing.ButtonGroup();
		lblTypeOfData = new javax.swing.JLabel();
		rbTypeOfDataContinuous = new javax.swing.JRadioButton();
		rbTypeOfDataCentroids = new javax.swing.JRadioButton();

        pnlVisualizerSettings.setLayout(new java.awt.GridLayout(10,0));


        lblRTFormatting.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblRTFormatting.setText("Retention time formatting");
        pnlVisualizerSettings.add(lblRTFormatting);

        rbRTInSecs.setText("Show time in seconds");
        rbgrpRTFormatting.add(rbRTInSecs);
        pnlVisualizerSettings.add(rbRTInSecs);

        rbRTInMinsAndSecs.setText("Show time in minutes and seconds");
        rbgrpRTFormatting.add(rbRTInMinsAndSecs);
        pnlVisualizerSettings.add(rbRTInMinsAndSecs);


        lblMZFormatting.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblMZFormatting.setText("M/Z value formatting");
        pnlVisualizerSettings.add(lblMZFormatting);

        rbMZTwoDecimals.setText("Show two decimal digits");
        rbgrpMZFormatting.add(rbMZTwoDecimals);
        pnlVisualizerSettings.add(rbMZTwoDecimals);

        rbMZThreeDecimals.setSelected(true);
        rbMZThreeDecimals.setText("Show three decimal digits");
        rbgrpMZFormatting.add(rbMZThreeDecimals);
        pnlVisualizerSettings.add(rbMZThreeDecimals);

        rbMZFourDecimals.setText("Show four decimal digits");
        rbgrpMZFormatting.add(rbMZFourDecimals);
        pnlVisualizerSettings.add(rbMZFourDecimals);



        lblTypeOfData.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTypeOfData.setText("Type of data");
        pnlVisualizerSettings.add(lblTypeOfData);

        rbTypeOfDataContinuous.setText("Continuous data");
        rbgrpTypeOfData.add(rbTypeOfDataContinuous);
        pnlVisualizerSettings.add(rbTypeOfDataContinuous);

        rbTypeOfDataCentroids.setText("Centroided data");
        rbgrpTypeOfData.add(rbTypeOfDataCentroids);
        pnlVisualizerSettings.add(rbTypeOfDataCentroids);



        jTabbedPane1.addTab("Visualizers", pnlVisualizerSettings);



        // Tab for peak measuring settings

        pnlPeakMeasuringSettings = new javax.swing.JPanel();

		rbgrpPMSetting = new javax.swing.ButtonGroup();
		lblPMSetting = new javax.swing.JLabel();
		rbPMHeight = new javax.swing.JRadioButton();
		rbPMArea = new javax.swing.JRadioButton();

		pnlPeakMeasuringSettings.setLayout(new java.awt.GridLayout(7,0));

        lblPMSetting.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblPMSetting.setText("Peak measuring setting");
        pnlPeakMeasuringSettings.add(lblPMSetting);

        rbPMHeight.setSelected(true);
        rbPMHeight.setText("Use peak height");
        rbgrpPMSetting.add(rbPMHeight);
        pnlPeakMeasuringSettings.add(rbPMHeight);

        rbPMArea.setText("Use peak area");
        rbgrpPMSetting.add(rbPMArea);
        pnlPeakMeasuringSettings.add(rbPMArea);

        jTabbedPane1.addTab("Peak measuring", pnlPeakMeasuringSettings);


        // Tab for misc settings

        pnlMiscSettings = new javax.swing.JPanel();

        rbgrpLoggingLevel = new javax.swing.ButtonGroup();
        lblLoggingLevel = new javax.swing.JLabel();
        rbLoggingLevelNormal = new javax.swing.JRadioButton();
        rbLoggingLevelDebug = new javax.swing.JRadioButton();
        rbLoggingLevelNone = new javax.swing.JRadioButton();

        rbgrpCopyOnLoad = new javax.swing.ButtonGroup();
        lblCopyOnLoad = new javax.swing.JLabel();
        rbCopyOnLoadYes = new javax.swing.JRadioButton();
        rbCopyOnLoadNo = new javax.swing.JRadioButton();

        pnlMiscSettings.setLayout(new java.awt.GridLayout(7,0));

        lblLoggingLevel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblLoggingLevel.setText("Logging level");
        pnlMiscSettings.add(lblLoggingLevel);

        rbLoggingLevelNormal.setSelected(true);
        rbLoggingLevelNormal.setText("Normal");
        rbgrpLoggingLevel.add(rbLoggingLevelNormal);
        pnlMiscSettings.add(rbLoggingLevelNormal);

        rbLoggingLevelDebug.setSelected(false);
        rbLoggingLevelDebug.setText("Debug");
        rbgrpLoggingLevel.add(rbLoggingLevelDebug);
        pnlMiscSettings.add(rbLoggingLevelDebug);

        rbLoggingLevelNone.setSelected(true);
        rbLoggingLevelNone.setText("None");
        rbgrpLoggingLevel.add(rbLoggingLevelNone);
        pnlMiscSettings.add(rbLoggingLevelNone);

        lblCopyOnLoad.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblCopyOnLoad.setText("Create copy during load");
        pnlMiscSettings.add(lblCopyOnLoad);

        rbCopyOnLoadYes.setSelected(false);
        rbCopyOnLoadYes.setText("Yes");
        rbgrpCopyOnLoad.add(rbCopyOnLoadYes);
        pnlMiscSettings.add(rbCopyOnLoadYes);

        rbCopyOnLoadNo.setSelected(true);
        rbCopyOnLoadNo.setText("No");
        rbgrpCopyOnLoad.add(rbCopyOnLoadNo);
        pnlMiscSettings.add(rbCopyOnLoadNo);

        // NOT YET IN USE jTabbedPane1.addTab("Miscellaneous", pnlMiscSettings);


		// Add tabbed panel and button panel to the dialog
        getContentPane().add(jTabbedPane1,BorderLayout.CENTER);
        getContentPane().add(btnPanel,BorderLayout.SOUTH);

        pack();
	}



	/**
	 * This method is called when OK button is clicked
	 */
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        setSettingsFromForm();
		dispose(); //setVisible(false);
		//mainWin.getDesktop().remove(this);
		mainWin.repaint();

	}


	/**
	 * This method is called when Apply button is clicked
	 */
    private void btnApplyActionPerformed(java.awt.event.ActionEvent evt) {
		setSettingsFromForm();
		mainWin.repaint();

    }


	/**
	 * This method is called when Cancel button is clicked
	 */
    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
		//paramSettings = mainWin.getParameterStorage().getGeneralParameters();
        getSettingsToForm();
        dispose(); // setVisible(false);
       // mainWin.getDesktop().remove(this);
        mainWin.repaint();
    }


	/**
	 * Restores form's controls by retrieving previous parameter values
	 */
	public void getSettingsToForm() {

		// Visualizer settings
		if (paramSettings.getChromatographicLabelFormat() == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC) {
			rbgrpRTFormatting.setSelected(rbRTInSecs.getModel(), true);
		}
		if (paramSettings.getChromatographicLabelFormat() == GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_MINSEC) {
			rbgrpRTFormatting.setSelected(rbRTInMinsAndSecs.getModel(), true);
		}

		if (paramSettings.getMZLabelFormat() == GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_TWODIGITS) {
			rbgrpMZFormatting.setSelected(rbMZTwoDecimals.getModel(), true);
		}
		if (paramSettings.getMZLabelFormat() == GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_THREEDIGITS) {
			rbgrpMZFormatting.setSelected(rbMZThreeDecimals.getModel(), true);
		}
		if (paramSettings.getMZLabelFormat() == GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_FOURDIGITS) {
			rbgrpMZFormatting.setSelected(rbMZFourDecimals.getModel(), true);
		}

		if (paramSettings.getTypeOfData() == GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CONTINUOUS) 	{ rbgrpTypeOfData.setSelected(rbTypeOfDataContinuous.getModel(), true); }
		if (paramSettings.getTypeOfData() == GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CENTROIDS) 	{ rbgrpTypeOfData.setSelected(rbTypeOfDataCentroids.getModel(), true); }

		// Peak measuring settings
		if (paramSettings.getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT)	{ rbgrpPMSetting.setSelected(rbPMHeight.getModel(), true); }
		if (paramSettings.getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA)	{ rbgrpPMSetting.setSelected(rbPMArea.getModel(), true); }


		// Misc settings
		if (paramSettings.getLoggingLevel() == GeneralParameters.PARAMETERVALUE_LOGGINGLEVEL_NORMAL) { rbgrpLoggingLevel.setSelected(rbLoggingLevelNormal.getModel(), true); }
		if (paramSettings.getLoggingLevel() == GeneralParameters.PARAMETERVALUE_LOGGINGLEVEL_DEBUG) { rbgrpLoggingLevel.setSelected(rbLoggingLevelDebug.getModel(), true); }
		if (paramSettings.getLoggingLevel() == GeneralParameters.PARAMETERVALUE_LOGGINGLEVEL_NONE) { rbgrpLoggingLevel.setSelected(rbLoggingLevelNone.getModel(), true); }

		if (paramSettings.getCopyOnLoad() == GeneralParameters.PARAMETERVALUE_COPYONLOAD_YES) { rbgrpCopyOnLoad.setSelected(rbCopyOnLoadYes.getModel(), true); }
		if (paramSettings.getCopyOnLoad() == GeneralParameters.PARAMETERVALUE_COPYONLOAD_NO) { rbgrpCopyOnLoad.setSelected(rbCopyOnLoadNo.getModel(), true); }

	}

	/**
	 * Puts values from form's controls to real parameter variables
	 */
	public void setSettingsFromForm() {
		// Visualizer settings
        if (rbgrpRTFormatting.getSelection() == rbRTInSecs.getModel())			{ paramSettings.setChromatographicLabelFormat(GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_SEC); }
        if (rbgrpRTFormatting.getSelection() == rbRTInMinsAndSecs.getModel()) 	{ paramSettings.setChromatographicLabelFormat(GeneralParameters.PARAMETERVALUE_CHROMATOGRAPHICLABELFORMAT_MINSEC); }

        if (rbgrpMZFormatting.getSelection() == rbMZTwoDecimals.getModel()) 	{ paramSettings.setMZLabelFormat(GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_TWODIGITS); }
        if (rbgrpMZFormatting.getSelection() == rbMZThreeDecimals.getModel()) 	{ paramSettings.setMZLabelFormat(GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_THREEDIGITS);}
        if (rbgrpMZFormatting.getSelection() == rbMZFourDecimals.getModel()) 	{ paramSettings.setMZLabelFormat(GeneralParameters.PARAMETERVALUE_MZLABELFORMAT_FOURDIGITS); }

		if (rbgrpTypeOfData.getSelection() == rbTypeOfDataContinuous.getModel())	{ paramSettings.setTypeOfData(GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CONTINUOUS); }
		if (rbgrpTypeOfData.getSelection() == rbTypeOfDataCentroids.getModel())		{ paramSettings.setTypeOfData(GeneralParameters.PARAMETERVALUE_TYPEOFDATA_CENTROIDS); }

        // Peak measuring settings
        if (rbgrpPMSetting.getSelection() == rbPMHeight.getModel()) { paramSettings.setPeakMeasuringType(GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT); }
        if (rbgrpPMSetting.getSelection() == rbPMArea.getModel()) 	{ paramSettings.setPeakMeasuringType(GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA); }

        // Misc settings
        if (rbgrpLoggingLevel.getSelection() == rbLoggingLevelNormal.getModel()) { paramSettings.setLoggingLevel(GeneralParameters.PARAMETERVALUE_LOGGINGLEVEL_NORMAL); }
        if (rbgrpLoggingLevel.getSelection() == rbLoggingLevelDebug.getModel()) { paramSettings.setLoggingLevel(GeneralParameters.PARAMETERVALUE_LOGGINGLEVEL_DEBUG); }
        if (rbgrpLoggingLevel.getSelection() == rbLoggingLevelNone.getModel()) { paramSettings.setLoggingLevel(GeneralParameters.PARAMETERVALUE_LOGGINGLEVEL_NONE); }

        if (rbgrpCopyOnLoad.getSelection() == rbCopyOnLoadYes.getModel()) { paramSettings.setCopyOnLoad(GeneralParameters.PARAMETERVALUE_COPYONLOAD_YES); }
        if (rbgrpCopyOnLoad.getSelection() == rbCopyOnLoadNo.getModel()) { paramSettings.setCopyOnLoad(GeneralParameters.PARAMETERVALUE_COPYONLOAD_NO); }



	}



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnOK;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnApply;

    // Tabbed pane for holding all different settings panels
    private javax.swing.JTabbedPane jTabbedPane1;


	// Visualizer settings
    private javax.swing.JPanel pnlVisualizerSettings;

    private javax.swing.ButtonGroup rbgrpRTFormatting;
    private javax.swing.JLabel lblRTFormatting;
    private javax.swing.JRadioButton rbRTInSecs;
    private javax.swing.JRadioButton rbRTInMinsAndSecs;

    private javax.swing.ButtonGroup rbgrpMZFormatting;
    private javax.swing.JLabel lblMZFormatting;
    private javax.swing.JRadioButton rbMZTwoDecimals;
    private javax.swing.JRadioButton rbMZThreeDecimals;
    private javax.swing.JRadioButton rbMZFourDecimals;

    private javax.swing.ButtonGroup rbgrpTypeOfData;
    private javax.swing.JLabel lblTypeOfData;
    private javax.swing.JRadioButton rbTypeOfDataContinuous;
    private javax.swing.JRadioButton rbTypeOfDataCentroids;


	// Peak measuring settings
    private javax.swing.JPanel pnlPeakMeasuringSettings;

    private javax.swing.ButtonGroup rbgrpPMSetting;
    private javax.swing.JLabel lblPMSetting;
    private javax.swing.JRadioButton rbPMHeight;
    private javax.swing.JRadioButton rbPMArea;
    private javax.swing.JRadioButton rbPMBoth;


    // General settings
    private javax.swing.JPanel pnlMiscSettings;

    private javax.swing.ButtonGroup rbgrpLoggingLevel;
    private javax.swing.JLabel lblLoggingLevel;
    private javax.swing.JRadioButton rbLoggingLevelNormal;
    private javax.swing.JRadioButton rbLoggingLevelDebug;
    private javax.swing.JRadioButton rbLoggingLevelNone;

    private javax.swing.ButtonGroup rbgrpCopyOnLoad;
    private javax.swing.JLabel lblCopyOnLoad;
    private javax.swing.JRadioButton rbCopyOnLoadYes;
    private javax.swing.JRadioButton rbCopyOnLoadNo;




    // End of variables declaration//GEN-END:variables

}
