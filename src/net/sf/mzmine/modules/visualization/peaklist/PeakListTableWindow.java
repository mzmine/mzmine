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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableModel;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

import org.jfree.report.JFreeReport;
import org.jfree.report.modules.gui.base.PreviewDialog;
import org.jfree.report.modules.parser.base.ReportGenerator;

import com.sun.java.swing.plaf.windows.resources.windows;

public class PeakListTableWindow extends JInternalFrame implements
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    PeakListTableVisualizer visualizer;
    private JScrollPane scrollPane;

    private PeakListTableModel model;
    private PeakListTable table;
    private PeakListTableParameters myParameters;

    /**
     * Constructor: initializes an empty visualizer
     */
    public PeakListTableWindow(PeakListTableVisualizer visualizer,
            PeakList peakList) {

        super(peakList.toString(), true, true, true, true);

        setResizable(true);
        setIconifiable(true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        this.visualizer = visualizer;

        // Build toolbar
        PeakListTableToolBar toolBar = new PeakListTableToolBar(this);
        add(toolBar, BorderLayout.EAST);

        myParameters = visualizer.getParameterSet().clone();

        // Build table
        table = new PeakListTable(visualizer, this, myParameters, peakList);

        scrollPane = new JScrollPane(table);

        add(scrollPane, BorderLayout.CENTER);

        pack();

    }

    /**
     * Methods for ActionListener interface implementation
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("ZOOM_TO_PEAK")) {
            // TODO
        }

        if (command.equals("PROPERTIES")) {

            PeakListTablePropertiesDialog dialog = new PeakListTablePropertiesDialog(
                    myParameters);
            dialog.setVisible(true);
            if (dialog.getExitCode() == ExitCode.OK) {
                table.setRowHeight(myParameters.getRowHeight());
                PeakListTableColumnModel cm = (PeakListTableColumnModel) table.getColumnModel();
                cm.createColumns();
                visualizer.setParameters(myParameters);
            }
        }

        if (command.equals("PRINT")) {
            try {
                ReportGenerator gener = ReportGenerator.getInstance();
                JFreeReport report;
                URL def = this.getClass().getResource(
                        "print-report-definition.xml");
                logger.finest("report definition: " + def);
                report = gener.parseReport(def);
                report.setData(model);
                JDialog dial;
                dial = new PreviewDialog(report);
                dial.pack();
                dial.setLocationRelativeTo(null);
                dial.setVisible(true);
            } catch (Exception ex) {
                logger.log(Level.WARNING,
                        "Could not generate report for printing", ex);
            }
        }

    }

}
