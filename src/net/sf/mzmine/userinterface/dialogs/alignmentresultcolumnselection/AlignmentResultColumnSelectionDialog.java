package net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


import net.sf.mzmine.io.util.AlignmentResultExporter;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection.CommonColumnType;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection.RawDataColumnType;
import net.sf.mzmine.visualizers.alignmentresult.table.AlignmentResultTable;

public class AlignmentResultColumnSelectionDialog extends JInternalFrame implements ActionListener {
	
	private AlignmentResultColumnSelection currentColumnSelection;
	private AlignmentResultColumnSelectionAcceptor columnSelectionAcceptor;
	private String title;
	
	private Hashtable<CommonColumnType, JCheckBox> commonColumnCheckBoxes;
	private Hashtable<RawDataColumnType, JCheckBox> rawDataColumnCheckBoxes;
	
	private JButton btnOk;
	private JButton btnCancel;

	public AlignmentResultColumnSelectionDialog(AlignmentResultColumnSelection currentColumnSelection, AlignmentResultColumnSelectionAcceptor columnSelectionAcceptor) {
		
		this.currentColumnSelection = currentColumnSelection; 
		this.columnSelectionAcceptor = columnSelectionAcceptor;
		
		initComponents();
		
	}

	
	public void initComponents() {

		setTitle("Please select columns");
		setLayout(new BorderLayout());
		
		// Generate label and check box for each possible common column
		JPanel pnlCommon = new JPanel();
		pnlCommon.setLayout(new BoxLayout(pnlCommon, BoxLayout.PAGE_AXIS));
		
		commonColumnCheckBoxes = new Hashtable<CommonColumnType, JCheckBox>();

		JLabel commonColsTitle = new JLabel("Available common columns");
		pnlCommon.add(commonColsTitle);
		
		
		pnlCommon.add(Box.createRigidArea(new Dimension(0,5)));

		for (CommonColumnType c : CommonColumnType.values()) {

			JCheckBox commonColumnCheckBox = new JCheckBox();
			commonColumnCheckBox.setText(c.getColumnName());
			commonColumnCheckBoxes.put(c, commonColumnCheckBox);

			if (currentColumnSelection.isSelectedCommonColumnType(c)) 
				commonColumnCheckBox.setSelected(true);
			
			commonColumnCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
			pnlCommon.add(commonColumnCheckBox);
			
		}
		
		pnlCommon.add(Box.createVerticalGlue());

		
		// Generate label and check box for each possible raw data column
		JPanel pnlRaw = new JPanel();
		pnlRaw.setLayout(new BoxLayout(pnlRaw, BoxLayout.PAGE_AXIS));
		
		rawDataColumnCheckBoxes = new Hashtable<RawDataColumnType, JCheckBox>();
		
		JLabel rawDataColsTitle = new JLabel("Available raw data columns");
		pnlRaw.add(rawDataColsTitle);
		
		for (RawDataColumnType c : RawDataColumnType.values()) {

			JCheckBox rawDataColumnCheckBox = new JCheckBox();
			rawDataColumnCheckBox.setText(c.getColumnName());
			rawDataColumnCheckBoxes.put(c, rawDataColumnCheckBox);
			
			if (currentColumnSelection.isSelectedRawDataColumnType(c)) 
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
			AlignmentResultColumnSelection columnSelection = new AlignmentResultColumnSelection();
				
			Enumeration<CommonColumnType> ccEnum = commonColumnCheckBoxes.keys();
			while (ccEnum.hasMoreElements()) {
				CommonColumnType ccType = ccEnum.nextElement();
				JCheckBox ccBox = commonColumnCheckBoxes.get(ccType);
				if (ccBox.isSelected()) {
					columnSelection.addCommonColumn(ccType);
				}
			}
			
			Enumeration<RawDataColumnType> rcEnum = rawDataColumnCheckBoxes.keys();
			while (rcEnum.hasMoreElements()) {
				RawDataColumnType rcType = rcEnum.nextElement();
				JCheckBox rcBox = rawDataColumnCheckBoxes.get(rcType);
				if (rcBox.isSelected()) {
					columnSelection.addRawDataColumn(rcType);
				}
			}
			
			columnSelectionAcceptor.setColumnSelection(columnSelection);
			setVisible(false);
			dispose();
			
		}
		
		if (e.getSource()==btnCancel) {
			setVisible(false);
			dispose();
		}
		
		
	}	
	
	private class JCheckBoxComparator implements Comparator<JCheckBox> {
		
		public int compare(JCheckBox o1, JCheckBox o2) {
			return o1.getText().compareTo(o2.getText());
		}
			
	}
	
}
