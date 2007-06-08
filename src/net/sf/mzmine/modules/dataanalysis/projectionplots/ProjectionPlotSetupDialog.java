package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.io.OpenedRawDataFile.Operation;
import net.sf.mzmine.modules.BatchStep;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.components.interpolatinglookuppaintscale.InterpolatingLookupPaintScaleSetupDialogTableCellRenderer;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class ProjectionPlotSetupDialog extends JDialog implements ActionListener {

	private static final Color[] colors = { Color.lightGray, Color.red, Color.green, Color.blue, Color.magenta, Color.orange};

	private Desktop desktop;
	
	private SimpleParameterSet parameters;

	private OpenedRawDataFile[] rawDataFiles;
	
	private Hashtable<OpenedRawDataFile, Color> rawDataColors;	

	private ExitCode exitCode = ExitCode.UNKNOWN;
	
	private JTable table;
	private ColorTableModel tableModel;


	/**
	 * Constructor: creates new form SelectOneGroupDialog
	 */
	public ProjectionPlotSetupDialog(Desktop desktop, PeakList peakList, SimpleParameterSet parameters) {

        super(desktop.getMainFrame(), "Select colors for samples", true);

		this.desktop = desktop;
		this.parameters = parameters;
		rawDataFiles = peakList.getRawDataFiles();
		
		rawDataColors = new Hashtable<OpenedRawDataFile, Color>();
		for (OpenedRawDataFile rdf : rawDataFiles)
			rawDataColors.put(rdf, colors[0]);
			


		table = new JTable();
		tableModel = new ColorTableModel(table, rawDataFiles, rawDataColors.values().toArray(new Color[0]) );
		table.setModel(tableModel);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.getColumnModel().getColumn(1).setCellRenderer(new ProjectionPlotSetupDialogTableCellRenderer());
		
		table.getColumnModel().getColumn(1).setCellEditor(new ColorComboBoxEditor(colors));
		
		// Build the form
		initComponents();		

		if (parameters.getParameterValue(ProjectionPlot.MeasurementType)==ProjectionPlot.MeasurementTypeArea) 
			radiobuttonPeakMeasurementTypeArea.setSelected(true);
		else
			radiobuttonPeakMeasurementTypeHeight.setSelected(true);

	}


	/**
	 * Implementation of ActionListener interface
	 */
	public void actionPerformed(java.awt.event.ActionEvent e) {
		Object src = e.getSource();

		// OK button
		if (src == btnOK) {

			if (radiobuttonPeakMeasurementTypeArea.isSelected()) 
				parameters.setParameterValue(ProjectionPlot.MeasurementType, ProjectionPlot.MeasurementTypeArea);
			else
				parameters.setParameterValue(ProjectionPlot.MeasurementType, ProjectionPlot.MeasurementTypeHeight);
			
			for (int tableRow=0; tableRow<rawDataFiles.length; tableRow++)
				rawDataColors.put(rawDataFiles[tableRow], (Color)tableModel.getValueAt(tableRow, 1)); 
			
			exitCode = ExitCode.OK;
			dispose();
			
		}

		// Cancel button
		if (src == btnCancel) {

			exitCode = ExitCode.CANCEL;
			dispose();

		}

	}

	private void initComponents() {


		// This one should be removed/modified
		setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);

		// Place components to frame

		// - Middle panel (scroll & table)
		pnlMiddle = new javax.swing.JPanel();
		pnlMiddle.setLayout(new java.awt.BorderLayout());

		scrSamples = new javax.swing.JScrollPane(table);
		scrSamples.setPreferredSize(new java.awt.Dimension(350,200));

		pnlMiddle.add(scrSamples, java.awt.BorderLayout.CENTER);


		// - Bottom panel (buttons)
		pnlBottom = new javax.swing.JPanel();
		pnlBottom.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
		pnlButtons = new javax.swing.JPanel();
		btnOK = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();


		pnlBottom.setLayout(new java.awt.BorderLayout());

		btnOK.setText("OK");
		btnCancel.setText("Cancel");
		btnOK.addActionListener(this);
		btnCancel.addActionListener(this);
		pnlButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		pnlBottom.add(pnlButtons, java.awt.BorderLayout.SOUTH);

		//	Parameter values

		panelPeakMeasurement = new JPanel();
		labelPeakMeasurementType = new JLabel("Peak measurement type: ");
		panelPeakMeasurement.add(labelPeakMeasurementType);
		ButtonGroup buttongroupPeakMeasurementType = new ButtonGroup();
		radiobuttonPeakMeasurementTypeArea = new JRadioButton("Peak area");
		panelPeakMeasurement.add(radiobuttonPeakMeasurementTypeArea);
		buttongroupPeakMeasurementType.add(radiobuttonPeakMeasurementTypeArea);
		radiobuttonPeakMeasurementTypeHeight = new JRadioButton("Peak height");
		panelPeakMeasurement.add(radiobuttonPeakMeasurementTypeHeight);
		buttongroupPeakMeasurementType.add(radiobuttonPeakMeasurementTypeHeight);

		pnlBottom.add(panelPeakMeasurement, java.awt.BorderLayout.NORTH);
		
		// - Finally add everything to the main pane
		pnlAll = new javax.swing.JPanel();
		pnlAll.setLayout(new java.awt.BorderLayout());
		pnlAll.add(pnlBottom, java.awt.BorderLayout.SOUTH);
		pnlAll.add(pnlMiddle, java.awt.BorderLayout.CENTER);

		getContentPane().add(pnlAll, java.awt.BorderLayout.CENTER);
		
		pack();
		setResizable(false);
		setLocationRelativeTo(desktop.getMainFrame());
		
	}
	
	public Hashtable<OpenedRawDataFile, Color> getRawDataColors() {
		return rawDataColors;
	}
	
	/**
	 * Method for reading exit code
	 */
	public ExitCode getExitCode() {
		return exitCode;
	}


	private javax.swing.JPanel pnlAll;	// contains everything

		private javax.swing.JPanel pnlMiddle;	// contains table for sample names and current labels
			private javax.swing.JScrollPane scrSamples;

		private javax.swing.JPanel pnlBottom;	// contains additional questions and OK & Cancel buttons
			private JPanel panelPeakMeasurement;
				private JLabel labelPeakMeasurementType;
				private ButtonGroup buttongroupPeakMeasurementType;
				private JRadioButton radiobuttonPeakMeasurementTypeArea;
				private JRadioButton radiobuttonPeakMeasurementTypeHeight;

			private javax.swing.JPanel pnlButtons;
				private javax.swing.JButton btnOK;
				private javax.swing.JButton btnCancel;



	public class ColorComboBoxEditor extends DefaultCellEditor {

		
		public ColorComboBoxEditor(Color[] colors) {
			super(new ProjectionPlotSetupDialogComboBox(colors));
		}
	}


	private class ColorTableModel extends AbstractTableModel {

		String[] colNames = { "File", "Color" };

		private OpenedRawDataFile[] files;
		private Color[] colors;
		private JTable table;

		public ColorTableModel(JTable table, OpenedRawDataFile[] files, Color[] colors) {
			this.table = table;
			this.files = files;
			this.colors = colors;
		}

		public int getColumnCount() { 
			return 2;
		}

		public int getRowCount() {
			return files.length;
		}

		public Object getValueAt(int row, int column) {
			if (column == 0) { return files[row].toString(); }
			if (column == 1) { return colors[row]; }
			return null;
		}

		public void setValueAt(Object aValue, int row, int column) {
			if (column==0) { return; }

			int[] rows = table.getSelectedRows();
			for (int r : rows) { colors[r]=(Color)aValue; }
			table.repaint();
			return;
		}

		public String getColumnName(int colInd) {
			return colNames[colInd];
		}

		public boolean isCellEditable(int rowIndex, int colIndex) {
			if (colIndex==0) { return false; }
			if (colIndex==1) { return true; }
			return false;
		}

	}
	
}
