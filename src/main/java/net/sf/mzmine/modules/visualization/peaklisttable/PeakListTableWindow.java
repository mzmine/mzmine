/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaklisttable;

import java.util.ArrayList;
import java.util.List;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.print.PrinterException;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable.PrintMode;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTableModel;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTableColumnModel;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.util.ExitCode;

public class PeakListTableWindow extends JFrame implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JScrollPane scrollPane;

    private PeakListTable table;

    private ParameterSet parameters;
    
    private JTextField filterTextId;
    private JTextField filterTextMz;
    private JTextField filterTextRt;
    private JTextField filterTextIdentity;
    private JTextField filterTextComment;


    /**
     * Constructor: initializes an empty visualizer
     */
    PeakListTableWindow(PeakList peakList, ParameterSet parameters) {

	super("Peak list: " + peakList.getName());

	this.parameters = parameters;

	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setBackground(Color.white);

	// Build tool bar
	PeakListTableToolBar toolBar = new PeakListTableToolBar(this);
	add(toolBar, BorderLayout.EAST);

	// Build table
	table = new PeakListTable(this, parameters, peakList);

	scrollPane = new JScrollPane(table);

	add(scrollPane, BorderLayout.CENTER);

	
	JPanel filterPane = new JPanel(new GridLayout(1,10));
    //filterBy = new JComboBox(new Object[]{"Nothing", "m/z", "RT","Identity","Comment"});
    filterTextId = new JTextField(10);
    filterTextMz = new JTextField(10);
    filterTextRt = new JTextField(10);
    filterTextIdentity = new JTextField(10);
    filterTextComment = new JTextField(10);
    //filterPane.add(filterBy);
    filterPane.add(new JLabel("ID:",SwingConstants.CENTER));
    filterPane.add(filterTextId);
    filterPane.add(new JLabel("m/z:",SwingConstants.CENTER));
    filterPane.add(filterTextMz);
    filterPane.add(new JLabel("RT:",SwingConstants.CENTER));
    filterPane.add(filterTextRt);
    filterPane.add(new JLabel("Identity:",SwingConstants.CENTER));
    filterPane.add(filterTextIdentity);
    filterPane.add(new JLabel("Comment:",SwingConstants.CENTER));
    filterPane.add(filterTextComment);

    addListener(filterTextId);
    addListener(filterTextMz);
    addListener(filterTextRt);
    addListener(filterTextIdentity);
    addListener(filterTextComment);
	
	
    add(filterPane, BorderLayout.NORTH);
    
	// Add the Windows menu
	JMenuBar menuBar = new JMenuBar();
	menuBar.add(new WindowsMenu());
	setJMenuBar(menuBar);

	pack();

	// get the window settings parameter
	ParameterSet paramSet = MZmineCore.getConfiguration()
		.getModuleParameters(PeakListTableModule.class);
	WindowSettingsParameter settings = paramSet
		.getParameter(PeakListTableParameters.windowSettings);

	// update the window and listen for changes
	settings.applySettingsToWindow(this);
	this.addComponentListener(settings);

    }

    public int getJScrollSizeWidth() {
	return table.getPreferredSize().width;
    }

    public int getJScrollSizeHeight() {
	return table.getPreferredSize().height;
    }

    
    
    public void addListener(JTextField filterText){
        filterText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }
        });
    }

    public void updateFilter() {
        //Object selected = filterBy.getSelectedItem();
        
        List<RowFilter<Object,Object>> rowSorters = new ArrayList<RowFilter<Object,Object>>();

        TableRowSorter<PeakListTableModel> sorter = table.getTableRowSorter();

        String textId = "(?i)" + filterTextId.getText();
        String textMz = "(?i)" + filterTextMz.getText();
        String textRt = "(?i)" + filterTextRt.getText();
        String textIdentity = "(?i)" + filterTextIdentity.getText();
        String textComment = "(?i)" + filterTextComment.getText();

        rowSorters.add(RowFilter.regexFilter(textId,0));
        rowSorters.add(RowFilter.regexFilter(textMz,1));
        rowSorters.add(RowFilter.regexFilter(textRt,2));
        rowSorters.add(RowFilter.regexFilter(textIdentity,3));
        rowSorters.add(RowFilter.regexFilter(textComment,4));


        sorter.setRowFilter(RowFilter.andFilter(rowSorters));

    }
    
    /**
     * Methods for ActionListener interface implementation
     */
    public void actionPerformed(ActionEvent event) {

	String command = event.getActionCommand();

	if (command.equals("PROPERTIES")) {

	    ExitCode exitCode = parameters.showSetupDialog(this, true);
	    if (exitCode == ExitCode.OK) {
		int rowHeight = parameters.getParameter(
			PeakListTableParameters.rowHeight).getValue();
		table.setRowHeight(rowHeight);

		PeakListTableColumnModel cm = (PeakListTableColumnModel) table
			.getColumnModel();
		cm.createColumns();

	    }
	}

	if (command.equals("AUTOCOLUMNWIDTH")) {
	    // Auto size column width based on data
	    for (int column = 0; column < table.getColumnCount(); column++) {
		TableColumn tableColumn = table.getColumnModel().getColumn(
			column);
		if (tableColumn.getHeaderValue() != "Peak shape"
			&& tableColumn.getHeaderValue() != "Status") {
		    TableCellRenderer renderer = tableColumn
			    .getHeaderRenderer();
		    if (renderer == null) {
			renderer = table.getTableHeader().getDefaultRenderer();
		    }
		    Component component = renderer
			    .getTableCellRendererComponent(table,
				    tableColumn.getHeaderValue(), false, false,
				    -1, column);
		    int preferredWidth = component.getPreferredSize().width + 20;
		    tableColumn.setPreferredWidth(preferredWidth);
		}
	    }
	}

	if (command.equals("PRINT")) {
	    try {
		table.print(PrintMode.FIT_WIDTH);
	    } catch (PrinterException e) {
		MZmineCore.getDesktop().displayException(this, e);
	    }
	}
    } 
}
