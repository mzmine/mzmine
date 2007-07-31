package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class ProjectionPlotSetupDialog extends JDialog implements ActionListener, ItemListener {

	private static final Color[] colors = { Color.lightGray, Color.red, Color.green, Color.blue, Color.magenta, Color.orange};

	private Desktop desktop;

	private SimpleParameterSet parameters;

	private HashMap<Parameter, HashSet<Object>> experimentalParameterValues;
	private Parameter selectedExperimentalParameter;
	private HashMap<Object, Color> colorsForExperimentalParameterValues;

	private ExitCode exitCode = ExitCode.UNKNOWN;

	private JComboBox comboExperimentalParameter;

	private JTable table;

	private JRadioButton radiobuttonPeakMeasurementTypeArea;
	private JRadioButton radiobuttonPeakMeasurementTypeHeight;

	private JPanel pnlButtons;
	private JButton btnOK;
	private JButton btnCancel;

	private ColorTableModel tableModel;


	/**
	 * Constructor: creates new form SelectOneGroupDialog
	 */
	public ProjectionPlotSetupDialog(Desktop desktop, PeakList peakList, SimpleParameterSet parameters) {

        super(desktop.getMainFrame(), "Set parameter values for the plot", true);
        
        MZmineProject currentProject = MZmineCore.getCurrentProject();

		this.desktop = desktop;
		this.parameters = parameters;

		// Collect different experimental parameters and their values
		experimentalParameterValues = new HashMap<Parameter, HashSet<Object>>();

		for (RawDataFile rdf : peakList.getRawDataFiles()) {

			Parameter[] experimentalParameters = currentProject.getParameters();
			for (Parameter p : experimentalParameters) {
				// Check if this parameter has been seen already
				HashSet<Object> values;
				if (!(experimentalParameterValues.containsKey(p))) {
					values = new HashSet<Object>();
					experimentalParameterValues.put(p, values);
				}

				// Store value of this parameter if it is a new one
				values = experimentalParameterValues.get(p);
				Object value = currentProject.getParameterValue(p, rdf);
				values.add(value);
			}
			
		}

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

			// Store information about height/area selection
			if (radiobuttonPeakMeasurementTypeArea.isSelected())
				parameters.setParameterValue(ProjectionPlot.MeasurementType, ProjectionPlot.MeasurementTypeArea);
			else
				parameters.setParameterValue(ProjectionPlot.MeasurementType, ProjectionPlot.MeasurementTypeHeight);

			// TODO: Construct a table mapping parameter values of selectedExperimentalParameter to selected colors

			exitCode = ExitCode.OK;
			dispose();

		}

		// Cancel button
		if (src == btnCancel) {

			exitCode = ExitCode.CANCEL;
			dispose();

		}

	}

	public void itemStateChanged(ItemEvent event) {

		if (event.getStateChange() == ItemEvent.SELECTED) {
			// Set selected parameter
			selectedExperimentalParameter = (Parameter)event.getItem();

			// Assign default colors to parameter's values
			colorsForExperimentalParameterValues = new HashMap<Object, Color>();
			Object[] values = experimentalParameterValues.get(selectedExperimentalParameter).toArray(new Object[0]);
			for (int ind=0; ind<values.length; ind++) {

				colorsForExperimentalParameterValues.put(values[ind], colors[ind % colors.length]);
			}

			// Initialize table for changing default colors
			tableModel = new ColorTableModel(table, values, colorsForExperimentalParameterValues.values().toArray(new Color[0]) );
			table.setModel(tableModel);
			table.getColumnModel().getColumn(1).setCellRenderer(new ProjectionPlotSetupDialogTableCellRenderer());
			table.getColumnModel().getColumn(1).setCellEditor(new ColorComboBoxEditor(colors));

		}

	}

	private void initComponents() {


		// This one should be removed/modified
		setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);

		// Place components to frame

		JPanel pnlTop = new JPanel(new BorderLayout());
		JLabel labelExperimentalParameter = new JLabel("Choose which experimental parameter is used for coloring the plot");
		comboExperimentalParameter = new JComboBox(experimentalParameterValues.keySet().toArray(new Parameter[0]));
		comboExperimentalParameter.addItemListener(this);
		pnlTop.add(labelExperimentalParameter, BorderLayout.NORTH);
		pnlTop.add(comboExperimentalParameter, BorderLayout.CENTER);
		pnlTop.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));




		// - Middle panel (scroll & table)
		JPanel pnlMiddle = new javax.swing.JPanel();
		pnlMiddle.setLayout(new BorderLayout());

		table = new JTable();
		JScrollPane scrSamples = new javax.swing.JScrollPane(table);
		scrSamples.setPreferredSize(new java.awt.Dimension(350,200));

		pnlMiddle.add(scrSamples, BorderLayout.CENTER);


		// - Bottom panel (buttons)
		JPanel pnlBottom = new javax.swing.JPanel();
		pnlBottom.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
		pnlButtons = new javax.swing.JPanel();
		btnOK = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();


		pnlBottom.setLayout(new BorderLayout());

		btnOK.setText("OK");
		btnCancel.setText("Cancel");
		btnOK.addActionListener(this);
		btnCancel.addActionListener(this);
		pnlButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		pnlBottom.add(pnlButtons, BorderLayout.SOUTH);

		//	Parameter values

		JPanel panelPeakMeasurement = new JPanel();
		JLabel labelPeakMeasurementType = new JLabel("Peak measurement type: ");
		panelPeakMeasurement.add(labelPeakMeasurementType);
		ButtonGroup buttongroupPeakMeasurementType = new ButtonGroup();
		radiobuttonPeakMeasurementTypeArea = new JRadioButton("Peak area");
		panelPeakMeasurement.add(radiobuttonPeakMeasurementTypeArea);
		buttongroupPeakMeasurementType.add(radiobuttonPeakMeasurementTypeArea);
		radiobuttonPeakMeasurementTypeHeight = new JRadioButton("Peak height");
		panelPeakMeasurement.add(radiobuttonPeakMeasurementTypeHeight);
		buttongroupPeakMeasurementType.add(radiobuttonPeakMeasurementTypeHeight);

		pnlBottom.add(panelPeakMeasurement, BorderLayout.NORTH);

		// - Finally add everything to the main pane
		JPanel pnlAll = new javax.swing.JPanel();
		pnlAll.setLayout(new BorderLayout());
		pnlAll.add(pnlBottom, BorderLayout.SOUTH);
		pnlAll.add(pnlMiddle, BorderLayout.CENTER);
		pnlAll.add(pnlTop, BorderLayout.NORTH);

		getContentPane().add(pnlAll, BorderLayout.CENTER);

		pack();
		setResizable(false);
		setLocationRelativeTo(desktop.getMainFrame());

	}

	public Parameter getColoringParameter() {
		return selectedExperimentalParameter;
	}

	public HashMap<Object, Color> getColorsForParameterValues() {
		return colorsForExperimentalParameterValues;
	}

	/**
	 * Method for reading exit code
	 */
	public ExitCode getExitCode() {
		return exitCode;
	}





	public class ColorComboBoxEditor extends DefaultCellEditor {


		public ColorComboBoxEditor(Color[] colors) {
			super(new ProjectionPlotSetupDialogComboBox(colors));
		}
	}


	private class ColorTableModel extends AbstractTableModel {

		String[] colNames = { "Parameter value", "Color" };

		private Object[] values;
		private Color[] colors;
		private JTable table;

		public ColorTableModel(JTable table, Object[] values, Color[] colors) {
			this.table = table;
			this.values = values;
			this.colors = colors;
		}

		public int getColumnCount() {
			return 2;
		}

		public int getRowCount() {
			return values.length;
		}

		public Object getValueAt(int row, int column) {
			if (column == 0) { return values[row].toString(); }
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
