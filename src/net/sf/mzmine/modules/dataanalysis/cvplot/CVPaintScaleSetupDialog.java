package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.colorchooser.DefaultColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

public class CVPaintScaleSetupDialog extends JDialog implements ActionListener, ListSelectionListener {

	public static final int VALUEFIELD_COLUMNS = 4;
	
	private Desktop desktop; 
	
	private JFormattedTextField fieldValue;

	private JScrollPane scrollpaneLookupValues;
	private JTable tableLookupValues;
	private CVPaintScaleSetupDialogTableModel tableModel;
	
	private TreeMap<Double, Color>lookupTable;

	private JButton buttonAddModify;
	private JButton buttonDelete;
	private JButton buttonColor;
	private JButton buttonOK;
	private JButton buttonCancel;
	
	private JPanel panelLegend;
	
	private ExitCode exitCode  = ExitCode.CANCEL;
	
	public CVPaintScaleSetupDialog(Desktop desktop, InterpolatingLookupPaintScale paintScale) {
    	super(desktop.getMainFrame(), "Select colors for paint scale", true);
    	this.desktop = desktop;
    	
		// Build the form
        initComponents();
        
        Double[] lookupValues = paintScale.getLookupValues(); 
        for (Double lookupValue : lookupValues) {
        	Color color = (Color)paintScale.getPaint(lookupValue);
        	lookupTable.put(lookupValue, color);
        }
        
	}
	
	private void initComponents() {
		
		// Panel for controls and list
		JPanel panelControlsAndList = new JPanel(new BorderLayout());
		add(panelControlsAndList, BorderLayout.CENTER);
		
		// Sub-panel for controls
		JPanel panelControls = new JPanel(new BorderLayout());
		panelControlsAndList.add(panelControls, BorderLayout.NORTH);
		
		// Sub-sub panel for value and color chooser controls
		JPanel panelValueAndColor = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelControls.add(panelValueAndColor, BorderLayout.NORTH);
		
		JLabel labelValue = new JLabel("Value: ");
		panelValueAndColor.add(labelValue);
		fieldValue = new JFormattedTextField(NumberFormat.getNumberInstance());
		fieldValue.setColumns(VALUEFIELD_COLUMNS);
		panelValueAndColor.add(fieldValue);
		
		buttonColor = new JButton("Select color");
		buttonColor.setBackground(new Color(255,255,255));
		buttonColor.addActionListener(this);
		panelValueAndColor.add(buttonColor);
		
		//JPanel panelAddRemoveButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		//panelControls.add(panelAddRemoveButtons, BorderLayout.SOUTH);
		buttonAddModify = new JButton("Add/Modify");
		buttonAddModify.addActionListener(this);
		buttonDelete = new JButton("Delete");
		buttonDelete.addActionListener(this);
		panelValueAndColor.add(new JPanel());
		panelValueAndColor.add(buttonAddModify);
		panelValueAndColor.add(buttonDelete);
		
		
		// Sub-panel for scrollpane & list
		JPanel panelList = new JPanel();
		lookupTable = new TreeMap<Double, Color>();
				
		tableModel = new CVPaintScaleSetupDialogTableModel(lookupTable);
		tableLookupValues = new JTable(tableModel);
		tableLookupValues.getColumnModel().getColumn(1).setCellRenderer(new CVPaintScaleSetupDialogTableCellRenderer(lookupTable));
		tableLookupValues.getSelectionModel().addListSelectionListener(this);
		
		panelControlsAndList.add(panelList, BorderLayout.CENTER);
		scrollpaneLookupValues = new JScrollPane(tableLookupValues);
		panelList.add(scrollpaneLookupValues);
		
		JPanel panelOKCancelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonOK = new JButton("OK");
		buttonOK.addActionListener(this);
		panelOKCancelButtons.add(buttonOK);
		buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);
		panelOKCancelButtons.add(buttonCancel);
		
		
		panelControlsAndList.add(panelOKCancelButtons, BorderLayout.SOUTH);
		
		pack();
        setLocationRelativeTo(desktop.getMainFrame());
        setResizable(false);
        
	}
	
	
	
    public void valueChanged(ListSelectionEvent event) {
    	
    	if (event.getValueIsAdjusting()) return;
    	
    	ListSelectionModel lsm = (ListSelectionModel)event.getSource();
    	
    	int index = lsm.getLeadSelectionIndex();
    	if (index<0) {
    		return;
    	}
    	Double value = lookupTable.keySet().toArray(new Double[0])[index];
    	Color color = lookupTable.get(value);
    	fieldValue.setValue(value);
		buttonColor.setBackground(color);
		
	}

	public void actionPerformed(ActionEvent event) {
    	Object src = event.getSource();
    	if (src == buttonColor) {
    		Color newColor = JColorChooser.showDialog(this, "Please select color", buttonColor.getBackground());
    		buttonColor.setBackground(newColor);
    	}
    	
    	if (src == buttonAddModify) {
    		if (fieldValue.getValue()==null) {
    			desktop.displayErrorMessage("Please enter value first.");
    			return;
    		}
    		
    		Double d = ((Number)fieldValue.getValue()).doubleValue();    		
    		lookupTable.put(d, buttonColor.getBackground());
    		//tableModel.fireTableChanged(new TableModelEvent(tableModel));
    		tableModel.fireTableDataChanged();
    		scrollpaneLookupValues.repaint();
    	}
    	
    	if (src == buttonDelete) {
    		int[] selectedRows = tableLookupValues.getSelectedRows();
    		for (int rowIndex : selectedRows) {
    			Double value = lookupTable.keySet().toArray(new Double[0])[rowIndex];
    			lookupTable.remove(value);
    		}
    		tableModel.fireTableDataChanged();
    		scrollpaneLookupValues.repaint();
    	}
    	
    	if (src == buttonOK) {
    		
    		exitCode = ExitCode.OK;
    		dispose();
    	}
    	
    	if (src == buttonCancel) { 
    		exitCode = ExitCode.CANCEL;
    		dispose();
    	}
	}

	public ExitCode getExitCode() {
		return exitCode;
	}
	
	public InterpolatingLookupPaintScale getPaintScale() {
		InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
		for (Double value : lookupTable.keySet()) {
			Color color = lookupTable.get(value);
			int[] rgb = new int[3];
			rgb[0] = color.getRed();
			rgb[1] = color.getGreen();
			rgb[2] = color.getBlue();
			paintScale.addLookupValue(value, rgb);
		}
		return paintScale;
	}
	
}
