package net.sf.mzmine.visualizers.alignmentresult.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


import net.sf.mzmine.visualizers.alignmentresult.table.AlignmentResultTableColumnSelection.CommonColumnType;
import net.sf.mzmine.visualizers.alignmentresult.table.AlignmentResultTableColumnSelection.RawDataColumnType;

public class AlignmentResultTableColumnSelectionDialog extends JInternalFrame implements ActionListener {
	
	private AlignmentResultTable table;
	private String title;
	
	private Hashtable<JCheckBox, CommonColumnType> commonColumnCheckBoxes;
	private Hashtable<JCheckBox, RawDataColumnType> rawDataColumnCheckBoxes;
	
	private JButton btnOk;
	private JButton btnCancel;

	public AlignmentResultTableColumnSelectionDialog(AlignmentResultTable table) {
		this.table = table;
		initComponents();
	}
	
	public void initComponents() {

		setTitle("Please select columns");
		setLayout(new BorderLayout());
		
		// Generate label and check box for each possible common column
		JPanel pnlCommon = new JPanel();
		pnlCommon.setLayout(new BoxLayout(pnlCommon, BoxLayout.PAGE_AXIS));
		
		commonColumnCheckBoxes = new Hashtable<JCheckBox, CommonColumnType>();

		JLabel commonColsTitle = new JLabel("Available common columns");
		pnlCommon.add(commonColsTitle);
		
		
		pnlCommon.add(Box.createRigidArea(new Dimension(0,5)));

		AlignmentResultTableColumnSelection columnSelection = table.getColumnSelection();
		System.err.println("Number of selected common cols = " + columnSelection.getNumberOfCommonColumns());
		System.err.println("Number of selected raw data cols = " + columnSelection.getNumberOfRawDataColumns());
		
	
		for (CommonColumnType c : CommonColumnType.values()) {

			JCheckBox commonColumnCheckBox = new JCheckBox();
			commonColumnCheckBox.setText(c.getColumnName());
			commonColumnCheckBoxes.put(commonColumnCheckBox, c);

			if (columnSelection.isSelectedCommonColumnType(c)) 
				commonColumnCheckBox.setSelected(true);
			
			commonColumnCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
			pnlCommon.add(commonColumnCheckBox);
			
		}
		
		pnlCommon.add(Box.createVerticalGlue());

		
		// Generate label and check box for each possible raw data column
		JPanel pnlRaw = new JPanel();
		pnlRaw.setLayout(new BoxLayout(pnlRaw, BoxLayout.PAGE_AXIS));
		
		rawDataColumnCheckBoxes = new Hashtable<JCheckBox, RawDataColumnType>();
		
		JLabel rawDataColsTitle = new JLabel("Available raw data columns");
		pnlRaw.add(rawDataColsTitle);
		
		for (RawDataColumnType c : RawDataColumnType.values()) {

			JCheckBox rawDataColumnCheckBox = new JCheckBox();
			rawDataColumnCheckBox.setText(c.getColumnName());
			rawDataColumnCheckBoxes.put(rawDataColumnCheckBox, c);
			
			if (columnSelection.isSelectedRawDataColumnType(c)) 
				rawDataColumnCheckBox.setSelected(true);
			
			rawDataColumnCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
			pnlRaw.add(rawDataColumnCheckBox);
			
		}
		
		pnlRaw.add(Box.createVerticalGlue());
	

		// Create and add buttons 
		btnOk = new JButton("OK");
		btnOk.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnOk);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(btnCancel);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
		
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		mainPanel.add(pnlCommon);
		mainPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		mainPanel.add(pnlRaw);
		add(mainPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.PAGE_END);

		// 
		pack();
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==btnOk) {
			AlignmentResultTableColumnSelection columnSelection = new AlignmentResultTableColumnSelection();
			
			Enumeration<JCheckBox> checkBoxEnum = commonColumnCheckBoxes.keys();
			while (checkBoxEnum.hasMoreElements()) {
				JCheckBox checkBox = checkBoxEnum.nextElement();
				if (checkBox.isSelected()) {
					CommonColumnType c = commonColumnCheckBoxes.get(checkBox);
					columnSelection.addCommonColumn(c);
				}
			}
			
			checkBoxEnum = rawDataColumnCheckBoxes.keys();
			while (checkBoxEnum.hasMoreElements()) {
				JCheckBox checkBox = checkBoxEnum.nextElement();
				if (checkBox.isSelected()) {
					RawDataColumnType c = rawDataColumnCheckBoxes.get(checkBox);
					columnSelection.addRawDataColumn(c);
				}
			}
			
			table.setColumnSelection(columnSelection);
			setVisible(false);
			dispose();
			
		}
		
		if (e.getSource()==btnCancel) {
			setVisible(false);
			dispose();
		}
		
		
	}	
	
}
