/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.util.ExitCode;


public class ColumnSelectionDialog extends JDialog
        implements ActionListener {

    private ExitCode exitCode = ExitCode.CANCEL;
    
    private ColumnSet columnSelection;

    private Hashtable<ColumnType, JCheckBox> commonColumnCheckBoxes;
    private Hashtable<ColumnType, JCheckBox> rawDataColumnCheckBoxes;

    private JButton btnOk;
    private JButton btnCancel;

    public ColumnSelectionDialog(Frame owner,
            ColumnSet columnSelection) {
        
        // Make dialog modal
        super(owner, true);
    
        this.columnSelection = columnSelection;

        setTitle("Please select columns");
        setLayout(new BorderLayout());

        // Generate label and check box for each possible common column
        JPanel pnlCommon = new JPanel();
        pnlCommon.setLayout(new BoxLayout(pnlCommon, BoxLayout.PAGE_AXIS));

        commonColumnCheckBoxes = new Hashtable<ColumnType, JCheckBox>();

        JLabel commonColsTitle = new JLabel("Available common columns");
        pnlCommon.add(commonColsTitle);

        pnlCommon.add(Box.createRigidArea(new Dimension(0, 5)));

        for (ColumnType c : columnSelection.getCommonColumns()) {

            JCheckBox commonColumnCheckBox = new JCheckBox();
            commonColumnCheckBox.setText(c.getColumnName());
            commonColumnCheckBoxes.put(c, commonColumnCheckBox);

            commonColumnCheckBox.setSelected(columnSelection.isColumnSelected(c));

            commonColumnCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlCommon.add(commonColumnCheckBox);

        }

        pnlCommon.add(Box.createVerticalGlue());

        // Generate label and check box for each possible raw data column
        JPanel pnlRaw = new JPanel();
        pnlRaw.setLayout(new BoxLayout(pnlRaw, BoxLayout.PAGE_AXIS));

        rawDataColumnCheckBoxes = new Hashtable<ColumnType, JCheckBox>();

        JLabel rawDataColsTitle = new JLabel("Available raw data columns");
        pnlRaw.add(rawDataColsTitle);

        for (ColumnType c : columnSelection.getRawDataColumns()) {

            JCheckBox rawDataColumnCheckBox = new JCheckBox();
            rawDataColumnCheckBox.setText(c.getColumnName());
            rawDataColumnCheckBoxes.put(c, rawDataColumnCheckBox);

            rawDataColumnCheckBox.setSelected(columnSelection.isColumnSelected(c));

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

        pack();
        setLocationRelativeTo(owner);
        
    }

    public void actionPerformed(ActionEvent e) {

        Object src = e.getSource();
        
        if (src == btnOk) {

            Enumeration<ColumnType> ccEnum = commonColumnCheckBoxes.keys();
            while (ccEnum.hasMoreElements()) {
                ColumnType ccType = ccEnum.nextElement();
                JCheckBox ccBox = commonColumnCheckBoxes.get(ccType);
                columnSelection.setColumnSelected(ccType, ccBox.isSelected());
            }

            Enumeration<ColumnType> rcEnum = rawDataColumnCheckBoxes.keys();
            while (rcEnum.hasMoreElements()) {
                ColumnType rcType = rcEnum.nextElement();
                JCheckBox rcBox = rawDataColumnCheckBoxes.get(rcType);
                columnSelection.setColumnSelected(rcType, rcBox.isSelected());
            }
            exitCode = ExitCode.OK;
            dispose();

        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }

    }
    
    /**
     * Method for reading exit code
     * 
     */
    public ExitCode getExitCode() {
        return exitCode;
    }

}
