/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.userinterface.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JButton;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.components.ComponentCellRenderer;
import net.sf.mzmine.userinterface.components.RawFileSettingTableModel;

import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.project.MZmineProject;

import com.sun.java.ExampleFileFilter;

/**
 * File open dialog
 */
public class RawFileSettingDialog extends JDialog implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private File filePaths[];
    private JTable table;
    private MZmineProject project;
    private String status;
    
    public RawFileSettingDialog(ArrayList lostFiles) {

        super(MZmineCore.getDesktop().getMainFrame(), "I found some missing Raw data files",
                true);
        project=MZmineCore.getCurrentProject();
        logger.finest("Displaying file open dialog");
        
        table = new JTable(new RawFileSettingTableModel(lostFiles));
        table.setDefaultRenderer(JComponent.class, new ComponentCellRenderer());
        table.getColumnModel().getColumn(RawFileSettingTableModel.Column.BUTTON.getValue()).setCellEditor(new RawFileTableEditor());
        
        //set apperance 
        table.getColumnModel().getColumn(RawFileSettingTableModel.Column.MARKER.getValue()).setPreferredWidth(3);
        table.getColumnModel().getColumn(RawFileSettingTableModel.Column.STATUS.getValue()).setPreferredWidth(5);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel panel = new JPanel(new FlowLayout());
 
        JButton button_ok=new JButton("OK");
        button_ok.setActionCommand("OK");
        button_ok.addActionListener(this);
        JButton button_cancel=new JButton("Cancel");
        button_cancel.setActionCommand("cancel");
        button_cancel.addActionListener(this);
        
        panel.add(button_ok);
        panel.add(button_cancel);
        add(panel,BorderLayout.PAGE_END);
        pack();
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
        
        
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed (ActionEvent event) {

        String command = event.getActionCommand();

        // check if user clicked "Open"
        if (command.equals("OK")) {
        	this.filePaths=new File[table.getRowCount()];
        	int row;
        	for (row=0;row<table.getRowCount();row++){
        		filePaths[row]=(File)table.getValueAt(row, RawFileSettingTableModel.Column.FILEPATH.getValue());

        		this.status="ok";	
        		
        	}
        }else{
        	this.filePaths=null;
        	this.status="cancelled";
        }

        // discard this dialog
        dispose();

    }
    
    public File [] getResult(){
    	return this.filePaths;
    }
}
