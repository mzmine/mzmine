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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable.PrintMode;
import javax.swing.JTextField;
import javax.swing.RowFilter;
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
    
    private JTextField jtfFilter = new JTextField();
    

    /**
     * Constructor: initializes an empty visualizer
     */
    PeakListTableWindow(PeakList peakList, ParameterSet parameters) {

	super("Peak list: " + peakList.getName());

	this.parameters = parameters;

	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setBackground(Color.white);

	// Build toolbar
	PeakListTableToolBar toolBar = new PeakListTableToolBar(this);
	add(toolBar, BorderLayout.EAST);

	// Build table
	table = new PeakListTable(this, parameters, peakList);

	scrollPane = new JScrollPane(table);

	add(scrollPane, BorderLayout.CENTER);

	TableRowSorter<PeakListTableModel> rowSorter = table.getTableRowSorter();
	JPanel panel = new JPanel(new BorderLayout());
	panel.add(new JLabel("Search:"),
            BorderLayout.WEST);
	panel.add(jtfFilter, BorderLayout.CENTER);
	
    add(panel, BorderLayout.SOUTH);
    
    jtfFilter.getDocument().addDocumentListener(new DocumentListener(){

        @Override
        public void insertUpdate(DocumentEvent e) {
            String text = jtfFilter.getText();

            if (text.trim().length() == 0) {
                rowSorter.setRowFilter(null);
            } else {
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)"+text));
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            String text = jtfFilter.getText();

            if (text.trim().length() == 0) {
                rowSorter.setRowFilter(null);
            } else {
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)"+text));
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    });
    
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
