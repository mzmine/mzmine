/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;

import org.jfree.report.JFreeReportBoot;

public class PeakListTableVisualizer implements MZmineModule, ActionListener {

    private Desktop desktop;
    private PeakListTableParameters parameters;
    private static PeakListTableVisualizer myInstance;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();
        myInstance = this;

        // Disable default logging of JFreeReport library
        System.setProperty("org.jfree.base.NoDefaultDebug", "true");

        // Boot the JFreeReport library in a new thread, because it may take a
        // couple of seconds and we don't want to block MZmine startup
        Thread bootThread = new Thread() {

            public void run() {
                JFreeReportBoot.getInstance().start();
            }

        };
        bootThread.start();

        parameters = new PeakListTableParameters();

        desktop.addMenuItem(MZmineMenu.VISUALIZATIONPEAKLIST,
                "Peak list table", "Shows a table view of a peak list",
                KeyEvent.VK_L, false, this, null);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList peakLists[] = desktop.getSelectedPeakLists();

        if (peakLists.length == 0) {
            desktop.displayErrorMessage("Please select peak list");
            return;
        }

        for (PeakList peakList : peakLists) {

            logger.finest("Showing a new peak list table view");

            PeakListTableWindow alignmentResultView = new PeakListTableWindow(
                    peakList);
            desktop.addInternalFrame(alignmentResultView);

        }
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Peak list table visualizer";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public PeakListTableParameters getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        parameters = (PeakListTableParameters) parameterValues;
    }

    public static PeakListTableVisualizer getInstance() {
        return myInstance;
    }

}