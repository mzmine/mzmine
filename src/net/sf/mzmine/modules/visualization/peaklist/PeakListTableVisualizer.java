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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.report.JFreeReportBoot;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;

public class PeakListTableVisualizer implements MZmineModule,
        ActionListener {

    private Desktop desktop;
    private PeakListTableColumns columnSelection;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.desktop = core.getDesktop();

        columnSelection = new PeakListTableColumns();

        JFreeReportBoot.getInstance().start();

        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Alignment result list view",
                this, null, KeyEvent.VK_A, false, true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList alignmentResults[] = desktop.getSelectedAlignmentResults();

        if (alignmentResults.length == 0) {
            desktop.displayErrorMessage("Please select at least one alignment result");
            return;
        }

        for (PeakList alignmentResult : alignmentResults) {

            logger.finest("Showing a new alignment result list view");

            PeakListTableWindow alignmentResultView = new PeakListTableWindow(
                    this, alignmentResult);
            desktop.addInternalFrame(alignmentResultView);

        }

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Alignment result table visualizer";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public PeakListTableColumns getParameterSet() {
        return columnSelection;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        columnSelection = (PeakListTableColumns) parameterValues;
    }

}