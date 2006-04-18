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
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGaps;
import net.sf.mzmine.methods.alignment.AlignmentResultFilterByGapsParameters;
import net.sf.mzmine.methods.alignment.AlignmentResultProcessorParameters;
import net.sf.mzmine.methods.alignment.FastAligner;
import net.sf.mzmine.methods.alignment.FastAlignerParameters;
import net.sf.mzmine.methods.alignment.GapFillerParameters;
import net.sf.mzmine.methods.alignment.JoinAligner;
import net.sf.mzmine.methods.alignment.JoinAlignerParameters;
import net.sf.mzmine.methods.alignment.LinearNormalizer;
import net.sf.mzmine.methods.alignment.LinearNormalizerParameters;
import net.sf.mzmine.methods.alignment.NormalizerParameters;
import net.sf.mzmine.methods.alignment.PeakListAlignerParameters;
import net.sf.mzmine.methods.alignment.SimpleGapFiller;
import net.sf.mzmine.methods.alignment.SimpleGapFillerParameters;
import net.sf.mzmine.methods.filtering.ChromatographicMedianFilter;
import net.sf.mzmine.methods.filtering.ChromatographicMedianFilterParameters;
import net.sf.mzmine.methods.filtering.CropFilter;
import net.sf.mzmine.methods.filtering.CropFilterParameters;
import net.sf.mzmine.methods.filtering.FilterParameters;
import net.sf.mzmine.methods.filtering.MeanFilter;
import net.sf.mzmine.methods.filtering.MeanFilterParameters;
import net.sf.mzmine.methods.filtering.SavitzkyGolayFilter;
import net.sf.mzmine.methods.filtering.SavitzkyGolayFilterParameters;
import net.sf.mzmine.methods.peakpicking.CentroidPicker;
import net.sf.mzmine.methods.peakpicking.CentroidPickerParameters;
import net.sf.mzmine.methods.peakpicking.IncompleteIsotopePatternFilter;
import net.sf.mzmine.methods.peakpicking.IncompleteIsotopePatternFilterParameters;
import net.sf.mzmine.methods.peakpicking.LocalPicker;
import net.sf.mzmine.methods.peakpicking.LocalPickerParameters;
import net.sf.mzmine.methods.peakpicking.PeakListProcessorParameters;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.methods.peakpicking.RecursiveThresholdPicker;
import net.sf.mzmine.methods.peakpicking.RecursiveThresholdPickerParameters;
import net.sf.mzmine.methods.peakpicking.SimpleDeisotoper;
import net.sf.mzmine.methods.peakpicking.SimpleDeisotoperParameters;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


public class BatchModeDialog extends javax.swing.JInternalFrame implements ActionListener {

	private String[] filterNames = { "not-in-use", "Mean spectra filter", "Savitzky-Golay spectra filter", "Chromatographic median filter", "Cropping filter"};
	private String[] peakPickerNames = { "not-in-use", "Recursive threshold peak detector", "Local maxima peak detector", "Centroid peak detector"};
	private String[] peakListProcessorNames = { "not-in-use", "Simple deisotoper", "Isotope pattern filter"};
	private String[] alignmentMethodNames = { "not-in-use", "Slow aligner", "Fast aligner"};
	private String[] alignmentFilterNames = { "not-in-use", "Filter by number of detections"};
	private String[] gapFillingMethodNames = { "not-in-use", "Basic gap-filler"};
	private String[] normalizationMethodNames = { "not-in-use", "Linear normalizer"};

    private MainWindow mainWin;

	private JButton btnStart;
	private JButton btnCancel;

	private JComboBox cmbFilter1;
	private JComboBox cmbFilter2;
	private JComboBox cmbFilter3;
	private JComboBox cmbPicker1;
	private JComboBox cmbPeakListProcessor1;
	private JComboBox cmbPeakListProcessor2;
	private JComboBox cmbAligner1;
	private JComboBox cmbAlignmentFilter1;
	private JComboBox cmbFiller1;
	private JComboBox cmbNormalizer1;

	private JButton btnFilter1;
	private JButton btnFilter2;
	private JButton btnFilter3;
	private JButton btnPicker1;
	private JButton btnPeakListProcessor1;
	private JButton btnPeakListProcessor2;
	private JButton btnAligner1;
	private JButton btnAlignmentFilter1;
	private JButton btnFiller1;
	private JButton btnNormalizer1;

	private JLabel lblFilter1;
	private JLabel lblFilter2;
	private JLabel lblFilter3;
	private JLabel lblPicker1;
	private JLabel lblPeakListProcessor1;
	private JLabel lblPeakListProcessor2;
	private JLabel lblAligner1;
	private JLabel lblAlignmentFilter1;
	private JLabel lblFiller1;
	private JLabel lblNormalizer1;

	private FilterParameters paramFilter1;
	private FilterParameters paramFilter2;
	private FilterParameters paramFilter3;
	private PeakPickerParameters paramPicker1;
	private PeakListProcessorParameters paramPeakListProcessor1;
	private PeakListProcessorParameters paramPeakListProcessor2;
	private PeakListAlignerParameters paramAligner1;
	private AlignmentResultProcessorParameters paramAlignmentFilter1;
	private GapFillerParameters paramFiller1;
	private NormalizerParameters paramNormalizer1;

	private int[] rawDataIDs;

	private BatchModeDialogParameters params;






    /**
     * Constructor
     * Initializes the dialog
     */
    public BatchModeDialog(MainWindow _mainWin, int[] _rawDataIDs) {
		mainWin = _mainWin;
		rawDataIDs = _rawDataIDs;
        initComponents();
        //setLocationRelativeTo(mainWin);
        setSettingsToForm();

		JDesktopPane desktop = mainWin.getDesktop();
		desktop.add(this);
		setVisible(true);
		setLocation(( desktop.getWidth()-getWidth() ) / 2,  ( desktop.getHeight()-getHeight() ) / 2 );

    }

    /**
     * This method is used by constructor to create all objects for the dialog
     */
    private void initComponents() {//GEN-BEGIN:initComponents

		// Set dialog title and component layout
        setTitle("Define batch run");
        getContentPane().setLayout(new java.awt.BorderLayout());

		// This listener calls cancel button method when user tries to unexpectedly close the window
/*
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
				mainWin.getStatusBar().setStatusText("Batch run cancelled.");
				setVisible(false);
            }
        });
*/
        addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
				mainWin.getStatusBar().setStatusText("Batch run cancelled.");
				dispose(); // setVisible(false);
				mainWin.getDesktop().remove(BatchModeDialog.this);
            }
			public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
				setLocation((mainWin.getWidth()-getWidth())/2, (mainWin.getHeight()-getHeight())/2);
			}
        });


		setResizable(true);

		// Buttons are placed on their own panel at the bottom of the dialog
        btnStart = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

		JPanel pnlButtons = new JPanel();
        btnStart.setText("Start batch operations");
        btnStart.addActionListener(this);

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(this);

		pnlButtons.add(btnStart);
        pnlButtons.add(btnCancel);

		// Build a panel of available processing options


		//JPanel pnlSteps = new JPanel();
		//pnlSteps.setLayout(new java.awt.GridLayout(8,3));

		JPanel pnlSteps = new JPanel();
		JPanel pnlStepsLeft = new JPanel();
		JPanel pnlStepsMid = new JPanel();
		JPanel pnlStepsRight = new JPanel();

		//SpringLayout spl = new SpringLayout();
//		pnlSteps.setLayout(spl);
		pnlSteps.setLayout(new BorderLayout());
		pnlStepsLeft.setLayout(new java.awt.GridLayout(10,1));
		pnlStepsMid.setLayout(new java.awt.GridLayout(10,1));
		pnlStepsRight.setLayout(new java.awt.GridLayout(10,1));
/*
		Dimension tmpDim = new Dimension(200,200); pnlStepsLeft.setPreferredSize(tmpDim); pnlStepsLeft.setMinimumSize(tmpDim); pnlStepsLeft.setMaximumSize(tmpDim);
		tmpDim = new Dimension(200,200); pnlStepsMid.setPreferredSize(tmpDim); pnlStepsMid.setMinimumSize(tmpDim); pnlStepsMid.setMaximumSize(tmpDim);
		tmpDim = new Dimension(150,200); pnlStepsRight.setPreferredSize(tmpDim); pnlStepsRight.setMinimumSize(tmpDim); pnlStepsRight.setMaximumSize(tmpDim);
*/


		pnlSteps.add(pnlStepsLeft, BorderLayout.WEST);
		pnlSteps.add(pnlStepsMid, BorderLayout.CENTER);
		pnlSteps.add(pnlStepsRight, BorderLayout.EAST);
/*
		spl.putConstraint(SpringLayout.WEST, pnlSteps, 5, SpringLayout.WEST, pnlStepsLeft);
		spl.putConstraint(SpringLayout.EAST, pnlStepsLeft, 5, SpringLayout.WEST, pnlStepsMid);
		spl.putConstraint(SpringLayout.EAST, pnlStepsMid, 5, SpringLayout.WEST, pnlStepsRight);
		spl.putConstraint(SpringLayout.EAST, pnlStepsRight, 5, SpringLayout.EAST, pnlSteps);

		spl.putConstraint(SpringLayout.SOUTH, pnlSteps, 5, SpringLayout.SOUTH, pnlStepsLeft);
		spl.putConstraint(SpringLayout.SOUTH, pnlSteps, 5, SpringLayout.SOUTH, pnlStepsMid);
		spl.putConstraint(SpringLayout.SOUTH, pnlSteps, 5, SpringLayout.SOUTH, pnlStepsRight);

		spl.putConstraint(SpringLayout.NORTH, pnlSteps, 5, SpringLayout.NORTH, pnlStepsLeft);
		spl.putConstraint(SpringLayout.NORTH, pnlSteps, 5, SpringLayout.NORTH, pnlStepsMid);
		spl.putConstraint(SpringLayout.NORTH, pnlSteps, 5, SpringLayout.NORTH, pnlStepsRight);
*/



		lblFilter1 = new JLabel("First raw data filter");
		lblFilter2 = new JLabel("Second raw data filter");
		lblFilter3 = new JLabel("Third raw data filter");
		lblPicker1 = new JLabel("Peak detector");
		lblPeakListProcessor1 = new JLabel("Peak list processor 1");
		lblPeakListProcessor2 = new JLabel("Peak list processor 2");
		lblAligner1 = new JLabel("Alignment method");
		lblAlignmentFilter1 = new JLabel("Alignment filter 1");
		lblFiller1 = new JLabel("Gap-filling method");
		lblNormalizer1 = new JLabel("Normalization");

		cmbFilter1 = new JComboBox(filterNames);
		cmbFilter2 = new JComboBox(filterNames);
		cmbFilter3 = new JComboBox(filterNames);
		cmbPicker1 = new JComboBox(peakPickerNames);
		cmbPeakListProcessor1 = new JComboBox(peakListProcessorNames);
		cmbPeakListProcessor2 = new JComboBox(peakListProcessorNames);
		cmbAligner1 = new JComboBox(alignmentMethodNames);
		cmbAlignmentFilter1 = new JComboBox(alignmentFilterNames);
		cmbFiller1 = new JComboBox(gapFillingMethodNames);
		cmbNormalizer1 = new JComboBox(normalizationMethodNames);

		cmbFilter1.addActionListener(this);
		cmbFilter2.addActionListener(this);
		cmbFilter3.addActionListener(this);
		cmbPicker1.addActionListener(this);
		cmbPeakListProcessor1.addActionListener(this);	cmbPeakListProcessor1.setEnabled(false);
		cmbPeakListProcessor2.addActionListener(this);	cmbPeakListProcessor2.setEnabled(false);
		cmbAligner1.addActionListener(this);			cmbAligner1.setEnabled(false);
		cmbAlignmentFilter1.addActionListener(this);	cmbAlignmentFilter1.setEnabled(false);
		cmbFiller1.addActionListener(this);				cmbFiller1.setEnabled(false);
		cmbNormalizer1.addActionListener(this); 		cmbNormalizer1.setEnabled(false);

		btnFilter1 = new JButton("Properties...");				btnFilter1.addActionListener(this);				btnFilter1.setEnabled(false);
		btnFilter2 = new JButton("Properties...");				btnFilter2.addActionListener(this);				btnFilter2.setEnabled(false);
		btnFilter3 = new JButton("Properties...");				btnFilter3.addActionListener(this);				btnFilter3.setEnabled(false);
		btnPicker1 = new JButton("Properties...");				btnPicker1.addActionListener(this);				btnPicker1.setEnabled(false);
		btnPeakListProcessor1 = new JButton("Properties...");	btnPeakListProcessor1.addActionListener(this);	btnPeakListProcessor1.setEnabled(false);
		btnPeakListProcessor2 = new JButton("Properties...");	btnPeakListProcessor2.addActionListener(this);	btnPeakListProcessor2.setEnabled(false);
		btnAligner1 = new JButton("Properties...");				btnAligner1.addActionListener(this);			btnAligner1.setEnabled(false);
		btnAlignmentFilter1 = new JButton("Properties...");		btnAlignmentFilter1.addActionListener(this);	btnAlignmentFilter1.setEnabled(false);
		btnFiller1 = new JButton("Properties...");				btnFiller1.addActionListener(this);				btnFiller1.setEnabled(false);
		btnNormalizer1 = new JButton("Properties...");			btnNormalizer1.addActionListener(this);			btnNormalizer1.setEnabled(false);

		pnlStepsLeft.add(lblFilter1);				pnlStepsMid.add(cmbFilter1);			pnlStepsRight.add(btnFilter1);
		pnlStepsLeft.add(lblFilter2);				pnlStepsMid.add(cmbFilter2);			pnlStepsRight.add(btnFilter2);
		pnlStepsLeft.add(lblFilter3);				pnlStepsMid.add(cmbFilter3);			pnlStepsRight.add(btnFilter3);
		pnlStepsLeft.add(lblPicker1);				pnlStepsMid.add(cmbPicker1);			pnlStepsRight.add(btnPicker1);
		pnlStepsLeft.add(lblPeakListProcessor1);	pnlStepsMid.add(cmbPeakListProcessor1);	pnlStepsRight.add(btnPeakListProcessor1);
		pnlStepsLeft.add(lblPeakListProcessor2);	pnlStepsMid.add(cmbPeakListProcessor2);	pnlStepsRight.add(btnPeakListProcessor2);
		pnlStepsLeft.add(lblAligner1);				pnlStepsMid.add(cmbAligner1);			pnlStepsRight.add(btnAligner1);
		pnlStepsLeft.add(lblAlignmentFilter1);		pnlStepsMid.add(cmbAlignmentFilter1);	pnlStepsRight.add(btnAlignmentFilter1);
		pnlStepsLeft.add(lblFiller1);				pnlStepsMid.add(cmbFiller1);			pnlStepsRight.add(btnFiller1);
		pnlStepsLeft.add(lblNormalizer1);			pnlStepsMid.add(cmbNormalizer1);		pnlStepsRight.add(btnNormalizer1);

		getContentPane().add(pnlSteps,BorderLayout.CENTER);
        getContentPane().add(pnlButtons,BorderLayout.SOUTH);

        pack();

	}


	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		FileDialog fileOpenDialog;


		// ComboBox FILTER1
		if (src == cmbFilter1) {
			btnFilter1.setEnabled(true);
			switch(cmbFilter1.getSelectedIndex()) {
				case -1:
				case 0:
					btnFilter1.setEnabled(false);
					paramFilter1 = null;
					break;
				case 1:
					paramFilter1 = mainWin.getParameterStorage().getMeanFilterParameters();
					break;
				case 2:
					paramFilter1 = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();
					break;
				case 3:
					paramFilter1 = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();
					break;
				case 4:
					paramFilter1 = mainWin.getParameterStorage().getCropFilterParameters();
					break;

			}
		}

		// ComboBox FILTER2
		if (src == cmbFilter2) {
			btnFilter2.setEnabled(true);
			switch(cmbFilter2.getSelectedIndex()) {
				case -1:
				case 0:
					btnFilter2.setEnabled(false);
					paramFilter2 = null;
					break;
				case 1:
					paramFilter2 = mainWin.getParameterStorage().getMeanFilterParameters();
					break;
				case 2:
					paramFilter2 = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();
					break;
				case 3:
					paramFilter2 = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();
					break;
				case 4:
					paramFilter2 = mainWin.getParameterStorage().getCropFilterParameters();
					break;
			}
		}

		// ComboBox FILTER3
		if (src == cmbFilter3) {
			btnFilter3.setEnabled(true);
			switch(cmbFilter3.getSelectedIndex()) {
				case -1:
				case 0:
					btnFilter3.setEnabled(false);
					paramFilter3 = null;
					break;
				case 1:
					paramFilter3 = mainWin.getParameterStorage().getMeanFilterParameters();
					break;
				case 2:
					paramFilter3 = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();
					break;
				case 3:
					paramFilter3 = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();
					break;
				case 4:
					paramFilter3 = mainWin.getParameterStorage().getCropFilterParameters();
					break;
			}
		}

		// ComboBox PICKER1
		if (src == cmbPicker1) {
			btnPicker1.setEnabled(true);
			switch(cmbPicker1.getSelectedIndex()) {
				case -1:
				case 0:
					btnPicker1.setEnabled(false);
					cmbAligner1.setSelectedIndex(0);
					cmbAligner1.setEnabled(false);
					paramPicker1 = null;
					break;
				case 1:
					paramPicker1 = mainWin.getParameterStorage().getRecursiveThresholdPickerParameters();
					cmbPeakListProcessor1.setEnabled(true);
					cmbPeakListProcessor2.setEnabled(true);
					cmbAligner1.setEnabled(true);
					break;
				case 2:
					paramPicker1 = mainWin.getParameterStorage().getLocalPickerParameters();
					cmbPeakListProcessor1.setEnabled(true);
					cmbPeakListProcessor2.setEnabled(true);
					cmbAligner1.setEnabled(true);
					break;
				case 3:
					paramPicker1 = mainWin.getParameterStorage().getCentroidPickerParameters();
					cmbPeakListProcessor1.setEnabled(true);
					cmbPeakListProcessor2.setEnabled(true);
					cmbAligner1.setEnabled(true);
					break;
			}
		}

		// ComboBox PEAKLISTPROCESSOR1
		if (src == cmbPeakListProcessor1) {
			btnPeakListProcessor1.setEnabled(true);
			switch(cmbPeakListProcessor1.getSelectedIndex()) {
				case -1:
				case 0:
					btnPeakListProcessor1.setEnabled(false);
					paramPeakListProcessor1 = null;
					break;
				case 1:
					paramPeakListProcessor1 = mainWin.getParameterStorage().getSimpleDeisotoperParameters();
					cmbPeakListProcessor1.setEnabled(true);
					break;
					/*
				case 2:
					paramPeakListProcessor1 = mainWin.getParameterStorage().getCombinatorialDeisotoperParameters();
					cmbPeakListProcessor1.setEnabled(true);
					break;
					*/
				case 2:
					paramPeakListProcessor1 = mainWin.getParameterStorage().getIncompleteIsotopePatternFilterParameters();
					cmbPeakListProcessor1.setEnabled(true);
					break;
			}
		}

		// ComboBox PEAKLISTPROCESSOR2
		if (src == cmbPeakListProcessor2) {
			btnPeakListProcessor2.setEnabled(true);
			switch(cmbPeakListProcessor2.getSelectedIndex()) {
				case -1:
				case 0:
					btnPeakListProcessor2.setEnabled(false);
					paramPeakListProcessor2 = null;
					break;
				case 1:
					paramPeakListProcessor2 = mainWin.getParameterStorage().getSimpleDeisotoperParameters();
					cmbPeakListProcessor2.setEnabled(true);
					break;
					/*
				case 2:
					paramPeakListProcessor2 = mainWin.getParameterStorage().getCombinatorialDeisotoperParameters();
					cmbPeakListProcessor2.setEnabled(true);
					break;
					*/
				case 2:
					paramPeakListProcessor2 = mainWin.getParameterStorage().getIncompleteIsotopePatternFilterParameters();
					cmbPeakListProcessor2.setEnabled(true);
					break;
			}
		}


		// ComboBox ALIGNER1
		if (src == cmbAligner1) {
			btnAligner1.setEnabled(true);
			switch(cmbAligner1.getSelectedIndex()) {
				case -1:
				case 0:
					cmbAlignmentFilter1.setSelectedIndex(0);
					cmbAlignmentFilter1.setEnabled(false);
					cmbFiller1.setSelectedIndex(0);
					cmbFiller1.setEnabled(false);
					cmbNormalizer1.setSelectedIndex(0);
					cmbNormalizer1.setEnabled(false);
					btnAligner1.setEnabled(false);
					paramAligner1 = null;
					break;
				case 1:
					paramAligner1 = mainWin.getParameterStorage().getJoinAlignerParameters();
					cmbAlignmentFilter1.setEnabled(true);
					cmbFiller1.setEnabled(true);
					cmbNormalizer1.setEnabled(true);
					break;
				case 2:
					paramAligner1 = mainWin.getParameterStorage().getFastAlignerParameters();
					cmbAlignmentFilter1.setEnabled(true);
					cmbFiller1.setEnabled(true);
					cmbNormalizer1.setEnabled(true);
					break;
			}
		}

		// ComboBox ALIGNMENT FILTER1
		if (src == cmbAlignmentFilter1) {
			btnAlignmentFilter1.setEnabled(true);
			switch(cmbAlignmentFilter1.getSelectedIndex()) {
				case -1:
				case 0:
					btnAlignmentFilter1.setEnabled(false);
					paramAlignmentFilter1 = mainWin.getParameterStorage().getAlignmentResultFilterByGapsParameters();
					break;
				case 1:

			}

		}

		// ComboBox FILLER1
		if (src == cmbFiller1) {
			btnFiller1.setEnabled(true);
			switch(cmbFiller1.getSelectedIndex()) {
				case -1:
				case 0:
					btnFiller1.setEnabled(false);
					paramFiller1 = null;
					break;
				case 1:
					paramFiller1 = mainWin.getParameterStorage().getSimpleGapFillerParameters();
					break;
			}
		}

		// ComboBox NORMALIZER1
		if (src == cmbNormalizer1) {
			btnNormalizer1.setEnabled(true);
			switch(cmbNormalizer1.getSelectedIndex()) {
				case -1:
				case 0:
					btnNormalizer1.setEnabled(false);
					paramNormalizer1 = null;
					break;
				case 1:
					paramNormalizer1 = mainWin.getParameterStorage().getLinearNormalizerParameters();
					break;
			}
		}



		// Button START
		if (src == btnStart) {

			getSettingsFromForm();

			dispose(); // setVisible(false);
			mainWin.getDesktop().remove(this);
			mainWin.repaint();

			if (cmbFilter1.getSelectedIndex()==0) { paramFilter1 = null; }
			if (cmbFilter2.getSelectedIndex()==0) { paramFilter2 = null; }
			if (cmbFilter3.getSelectedIndex()==0) { paramFilter3 = null; }
			if (cmbPeakListProcessor1.getSelectedIndex()==0) { paramPeakListProcessor1 = null; }
			if (cmbPeakListProcessor2.getSelectedIndex()==0) { paramPeakListProcessor2 = null; }
			if (cmbPicker1.getSelectedIndex()==0) { paramPicker1 = null; }
			if (cmbAligner1.getSelectedIndex()==0) { paramAligner1 = null; }
			if (cmbAlignmentFilter1.getSelectedIndex()==0) { paramAlignmentFilter1 = null; }
			if (cmbFiller1.getSelectedIndex()==0) { paramFiller1 = null; }
			if (cmbNormalizer1.getSelectedIndex()==0) { paramNormalizer1 = null; }

/*			mainWin.getClientForCluster().startBatch(	rawDataIDs,
														paramFilter1,
														paramFilter2,
														paramFilter3,
														paramPicker1,
														paramPeakListProcessor1,
														paramPeakListProcessor2,
														paramAligner1,
														paramAlignmentFilter1,
														paramFiller1,
														paramNormalizer1	);
*/
		}

		// Button CANCEL
		if (src == btnCancel) {

			getSettingsFromForm();

			mainWin.getStatusBar().setStatusText("Batch run cancelled.");
			dispose(); // setVisible(false);
			mainWin.getDesktop().remove(this);
			mainWin.repaint();
		}

		// Button FILTER1 PROPERTIES
		if (src == btnFilter1) {
			switch(cmbFilter1.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramFilter1 = new MeanFilter().askParameters(mainWin, mainWin.getParameterStorage().getMeanFilterParameters());
					if (paramFilter1==null) { paramFilter1 = mainWin.getParameterStorage().getMeanFilterParameters();	}
					else { mainWin.getParameterStorage().setMeanFilterParameters((MeanFilterParameters)paramFilter1); }
					break;
				case 2:
					paramFilter1 = new SavitzkyGolayFilter().askParameters(mainWin, mainWin.getParameterStorage().getSavitzkyGolayFilterParameters());
					if (paramFilter1==null) { paramFilter1 = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();	}
					else { mainWin.getParameterStorage().setSavitzkyGolayFilterParameters((SavitzkyGolayFilterParameters)paramFilter1); }
					break;
				case 3:
					paramFilter1 = new ChromatographicMedianFilter().askParameters(mainWin, mainWin.getParameterStorage().getChromatographicMedianFilterParameters());
					if (paramFilter1==null) { paramFilter1 = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();	}
					else { mainWin.getParameterStorage().setChromatographicMedianFilterParameters((ChromatographicMedianFilterParameters)paramFilter1); }
					break;
				case 4:
					paramFilter1 = new CropFilter().askParameters(mainWin, mainWin.getParameterStorage().getCropFilterParameters());
					if (paramFilter1==null) { paramFilter1 = mainWin.getParameterStorage().getCropFilterParameters(); }
					else { mainWin.getParameterStorage().setCropFilterParameters((CropFilterParameters)paramFilter1); }
					break;

			}
			toFront();
		}

		// Button FILTER2 PROPERTIES
		if (src == btnFilter2) {
			switch(cmbFilter2.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramFilter2 = new MeanFilter().askParameters(mainWin, mainWin.getParameterStorage().getMeanFilterParameters());
					if (paramFilter2==null) { paramFilter2 = mainWin.getParameterStorage().getMeanFilterParameters();	}
					else { mainWin.getParameterStorage().setMeanFilterParameters((MeanFilterParameters)paramFilter2); }
					break;
				case 2:
					paramFilter2 = new SavitzkyGolayFilter().askParameters(mainWin, mainWin.getParameterStorage().getSavitzkyGolayFilterParameters());
					if (paramFilter2==null) { paramFilter2 = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();	}
					else { mainWin.getParameterStorage().setSavitzkyGolayFilterParameters((SavitzkyGolayFilterParameters)paramFilter2); }
					break;
				case 3:
					paramFilter2 = new ChromatographicMedianFilter().askParameters(mainWin, mainWin.getParameterStorage().getChromatographicMedianFilterParameters());
					if (paramFilter2==null) { paramFilter2 = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();	}
					else { mainWin.getParameterStorage().setChromatographicMedianFilterParameters((ChromatographicMedianFilterParameters)paramFilter2); }
					break;
				case 4:
					paramFilter2 = new CropFilter().askParameters(mainWin, mainWin.getParameterStorage().getCropFilterParameters());
					if (paramFilter2==null) { paramFilter2 = mainWin.getParameterStorage().getCropFilterParameters(); }
					else { mainWin.getParameterStorage().setCropFilterParameters((CropFilterParameters)paramFilter2); }
					break;
			}
			toFront();
		}

		// Button FILTER3 PROPERTIES
		if (src == btnFilter3) {
			switch(cmbFilter3.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramFilter3 = new MeanFilter().askParameters(mainWin, mainWin.getParameterStorage().getMeanFilterParameters());
					if (paramFilter3==null) { paramFilter3 = mainWin.getParameterStorage().getMeanFilterParameters();	}
					else { mainWin.getParameterStorage().setMeanFilterParameters((MeanFilterParameters)paramFilter3); }
					break;
				case 2:
					paramFilter3 = new SavitzkyGolayFilter().askParameters(mainWin, mainWin.getParameterStorage().getSavitzkyGolayFilterParameters());
					if (paramFilter3==null) { paramFilter3 = mainWin.getParameterStorage().getSavitzkyGolayFilterParameters();	}
					else { mainWin.getParameterStorage().setSavitzkyGolayFilterParameters((SavitzkyGolayFilterParameters)paramFilter3); }
					break;
				case 3:
					paramFilter3 = new ChromatographicMedianFilter().askParameters(mainWin, mainWin.getParameterStorage().getChromatographicMedianFilterParameters());
					if (paramFilter3==null) { paramFilter3 = mainWin.getParameterStorage().getChromatographicMedianFilterParameters();	}
					else { mainWin.getParameterStorage().setChromatographicMedianFilterParameters((ChromatographicMedianFilterParameters)paramFilter3); }
					break;
				case 4:
					paramFilter3 = new CropFilter().askParameters(mainWin, mainWin.getParameterStorage().getCropFilterParameters());
					if (paramFilter3==null) { paramFilter3 = mainWin.getParameterStorage().getCropFilterParameters(); }
					else { mainWin.getParameterStorage().setCropFilterParameters((CropFilterParameters)paramFilter3); }
					break;
			}
			toFront();
		}

		// Button PICKER1 PROPERTIES
		if (src == btnPicker1) {
			switch(cmbPicker1.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramPicker1 = new RecursiveThresholdPicker().askParameters(mainWin, mainWin.getParameterStorage().getRecursiveThresholdPickerParameters());
					if (paramPicker1==null) { paramPicker1 = mainWin.getParameterStorage().getRecursiveThresholdPickerParameters(); }
					else { mainWin.getParameterStorage().setRecursiveThresholdPickerParameters((RecursiveThresholdPickerParameters)paramPicker1); }
					break;
				case 2:
					paramPicker1 = new LocalPicker().askParameters(mainWin, mainWin.getParameterStorage().getLocalPickerParameters());
					if (paramPicker1==null) { paramPicker1 = mainWin.getParameterStorage().getLocalPickerParameters(); }
					else { mainWin.getParameterStorage().setLocalPickerParameters((LocalPickerParameters)paramPicker1); }
					break;
				case 3:
					paramPicker1 = new CentroidPicker().askParameters(mainWin, mainWin.getParameterStorage().getCentroidPickerParameters());
					if (paramPicker1==null) { paramPicker1 = mainWin.getParameterStorage().getCentroidPickerParameters(); }
					else { mainWin.getParameterStorage().setCentroidPickerParameters((CentroidPickerParameters)paramPicker1); }
					break;
			}
			toFront();
		}

		// Button PEAKLISTPROCESSOR1 PROPERTIES
		if (src == btnPeakListProcessor1) {
			switch(cmbPeakListProcessor1.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramPeakListProcessor1 = new SimpleDeisotoper().askParameters(mainWin, mainWin.getParameterStorage().getSimpleDeisotoperParameters());
					if (paramPeakListProcessor1==null) { paramPeakListProcessor1 = mainWin.getParameterStorage().getSimpleDeisotoperParameters(); }
					else { mainWin.getParameterStorage().setSimpleDeisotoperParameters((SimpleDeisotoperParameters)paramPeakListProcessor1); }
					break;
					/*
				case 2:
					paramPeakListProcessor1 = new CombinatorialDeisotoper().askParameters(mainWin, mainWin.getParameterStorage().getCombinatorialDeisotoperParameters());
					if (paramPeakListProcessor1==null) { paramPeakListProcessor1 = mainWin.getParameterStorage().getCombinatorialDeisotoperParameters(); }
					else { mainWin.getParameterStorage().setCombinatorialDeisotoperParameters((CombinatorialDeisotoperParameters)paramPeakListProcessor1); }
					break;
					*/
				case 2:
					paramPeakListProcessor1 = new IncompleteIsotopePatternFilter().askParameters(mainWin, mainWin.getParameterStorage().getIncompleteIsotopePatternFilterParameters());
					if (paramPeakListProcessor1==null) { paramPeakListProcessor1 = mainWin.getParameterStorage().getIncompleteIsotopePatternFilterParameters(); }
					else { mainWin.getParameterStorage().setIncompleteIsotopePatternFilterParameters((IncompleteIsotopePatternFilterParameters)paramPeakListProcessor1); }
					break;
			}
			toFront();
		}

		// Button PEAKLISTPROCESSOR2 PROPERTIES
		if (src == btnPeakListProcessor2) {
			switch(cmbPeakListProcessor2.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramPeakListProcessor2 = new SimpleDeisotoper().askParameters(mainWin, mainWin.getParameterStorage().getSimpleDeisotoperParameters());
					if (paramPeakListProcessor2==null) { paramPeakListProcessor2 = mainWin.getParameterStorage().getSimpleDeisotoperParameters(); }
					else { mainWin.getParameterStorage().setSimpleDeisotoperParameters((SimpleDeisotoperParameters)paramPeakListProcessor2); }
					break;
					/*
				case 2:
					paramPeakListProcessor2 = new CombinatorialDeisotoper().askParameters(mainWin, mainWin.getParameterStorage().getCombinatorialDeisotoperParameters());
					if (paramPeakListProcessor2==null) { paramPeakListProcessor2 = mainWin.getParameterStorage().getCombinatorialDeisotoperParameters(); }
					else { mainWin.getParameterStorage().setCombinatorialDeisotoperParameters((CombinatorialDeisotoperParameters)paramPeakListProcessor2); }
					break;
					*/
				case 2:
					paramPeakListProcessor2 = new IncompleteIsotopePatternFilter().askParameters(mainWin, mainWin.getParameterStorage().getIncompleteIsotopePatternFilterParameters());
					if (paramPeakListProcessor2==null) { paramPeakListProcessor2 = mainWin.getParameterStorage().getIncompleteIsotopePatternFilterParameters(); }
					else { mainWin.getParameterStorage().setIncompleteIsotopePatternFilterParameters((IncompleteIsotopePatternFilterParameters)paramPeakListProcessor2); }
					break;
			}
			toFront();
		}


		// Button ALIGNER1 PROPERTIES
		if (src == btnAligner1) {
			switch(cmbAligner1.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramAligner1 = new JoinAligner().askParameters(mainWin, mainWin.getParameterStorage().getJoinAlignerParameters());
					if (paramAligner1==null) { paramAligner1 = mainWin.getParameterStorage().getJoinAlignerParameters(); }
					else { mainWin.getParameterStorage().setJoinAlignerParameters((JoinAlignerParameters)paramAligner1); }
					break;
				case 2:
					paramAligner1 = new FastAligner().askParameters(mainWin, mainWin.getParameterStorage().getFastAlignerParameters());
					if (paramAligner1==null) { paramAligner1 = mainWin.getParameterStorage().getFastAlignerParameters(); }
					else { mainWin.getParameterStorage().setFastAlignerParameters((FastAlignerParameters)paramAligner1); }
					break;
			}
			toFront();
		}


		// Button ALIGNMENT FILTER1 PROPERTIES
		if (src == btnAlignmentFilter1) {
			switch(cmbAlignmentFilter1.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramAlignmentFilter1 = new AlignmentResultFilterByGaps().askParameters(mainWin, mainWin.getParameterStorage().getAlignmentResultFilterByGapsParameters());
					if (paramAlignmentFilter1==null) { paramAlignmentFilter1 = mainWin.getParameterStorage().getAlignmentResultFilterByGapsParameters(); }
					else { mainWin.getParameterStorage().setAlignmentResultFilterByGapsParameters((AlignmentResultFilterByGapsParameters)paramAlignmentFilter1); }
					break;
			}
			toFront();
		}

		// Button FILLER1 PROPERTIES
		if (src == btnFiller1) {
			switch(cmbFiller1.getSelectedIndex()) {
				case -1:
				case 0:
					break;
				case 1:
					paramFiller1 = new SimpleGapFiller().askParameters(mainWin, mainWin.getParameterStorage().getSimpleGapFillerParameters());
					if (paramFiller1==null) { paramFiller1 = mainWin.getParameterStorage().getSimpleGapFillerParameters(); }
					else { mainWin.getParameterStorage().setSimpleGapFillerParameters((SimpleGapFillerParameters)paramFiller1); }
					break;
			}
			toFront();
		}

		// Button NORMALIZER1 PROPERTIES
		if (src == btnNormalizer1) {
			switch(cmbNormalizer1.getSelectedIndex()) {
				case -1:
				case 0:
					btnNormalizer1.setEnabled(false);
					paramNormalizer1 = null;
					break;
				case 1:
					LinearNormalizerParameters tmp = mainWin.getParameterStorage().getLinearNormalizerParameters();
					paramNormalizer1 = new LinearNormalizer().askParameters(mainWin, mainWin.getParameterStorage().getLinearNormalizerParameters());
					if (paramNormalizer1==null) { paramNormalizer1 = mainWin.getParameterStorage().getLinearNormalizerParameters(); }
					else { mainWin.getParameterStorage().setLinearNormalizerParameters((LinearNormalizerParameters)paramNormalizer1); }

					break;
			}
			toFront();
		}

	}

	public void setSettingsToForm() {
		params = mainWin.getParameterStorage().getBatchModeDialogParameters();

		cmbFilter1.setSelectedIndex(0);
		cmbFilter2.setSelectedItem(0);
		cmbFilter3.setSelectedItem(0);
		cmbPicker1.setSelectedItem(0);
		cmbPeakListProcessor1.setSelectedItem(0);
		cmbPeakListProcessor2.setSelectedItem(0);
		cmbAligner1.setSelectedItem(0);
		cmbAlignmentFilter1.setSelectedItem(0);
		cmbFiller1.setSelectedItem(0);
		cmbNormalizer1.setSelectedItem(0);


		for (int i=1; i<cmbFilter1.getItemCount(); i++) {
			String item = (String)(cmbFilter1.getItemAt(i));;
			if (item.equals(params.getSelectedFilter1Name())) {
				cmbFilter1.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbFilter2.getItemCount(); i++) {
			String item = (String)(cmbFilter2.getItemAt(i));;
			if (item.equals(params.getSelectedFilter2Name())) {
				cmbFilter2.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbFilter3.getItemCount(); i++) {
			String item = (String)(cmbFilter3.getItemAt(i));;
			if (item.equals(params.getSelectedFilter3Name())) {
				cmbFilter3.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbPicker1.getItemCount(); i++) {
			String item = (String)(cmbPicker1.getItemAt(i));;
			if (item.equals(params.getSelectedPicker1Name())) {
				cmbPicker1.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbPeakListProcessor1.getItemCount(); i++) {
			String item = (String)(cmbPeakListProcessor1.getItemAt(i));;
			if (item.equals(params.getSelectedPeakListProcessor1Name())) {
				cmbPeakListProcessor1.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbPeakListProcessor2.getItemCount(); i++) {
			String item = (String)(cmbPeakListProcessor2.getItemAt(i));;
			if (item.equals(params.getSelectedPeakListProcessor2Name())) {
				cmbPeakListProcessor2.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbAligner1.getItemCount(); i++) {
			String item = (String)(cmbAligner1.getItemAt(i));;
			if (item.equals(params.getSelectedAligner1Name())) {
				cmbAligner1.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbAlignmentFilter1.getItemCount(); i++) {
			String item = (String)(cmbAlignmentFilter1.getItemAt(i));;
			if (item.equals(params.getSelectedAlignmentFilter1Name())) {
				cmbAlignmentFilter1.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbFiller1.getItemCount(); i++) {
			String item = (String)(cmbFiller1.getItemAt(i));;
			if (item.equals(params.getSelectedFiller1Name())) {
				cmbFiller1.setSelectedIndex(i);
				break;
			}
		}
		for (int i=1; i<cmbNormalizer1.getItemCount(); i++) {
			String item = (String)(cmbNormalizer1.getItemAt(i));;
			if (item.equals(params.getSelectedNormalizer1Name())) {
				cmbNormalizer1.setSelectedIndex(i);
				break;
			}
		}
	}

	public void getSettingsFromForm() {
		params.setSelectedFilter1Name((String)(cmbFilter1.getSelectedItem()));
		params.setSelectedFilter2Name((String)(cmbFilter2.getSelectedItem()));
		params.setSelectedFilter3Name((String)(cmbFilter3.getSelectedItem()));
		params.setSelectedPicker1Name((String)(cmbPicker1.getSelectedItem()));
		params.setSelectedPeakListProcessor1Name((String)(cmbPeakListProcessor1.getSelectedItem()));
		params.setSelectedPeakListProcessor2Name((String)(cmbPeakListProcessor2.getSelectedItem()));
		params.setSelectedAligner1Name((String)(cmbAligner1.getSelectedItem()));
		params.setSelectedAlignmentFilter1Name((String)(cmbAlignmentFilter1.getSelectedItem()));
		params.setSelectedFiller1Name((String)(cmbFiller1.getSelectedItem()));
		params.setSelectedNormalizer1Name((String)(cmbNormalizer1.getSelectedItem()));

		mainWin.getParameterStorage().setBatchModeDialogParameters(params);
	}

}