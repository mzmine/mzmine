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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaksummary;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;

/**
 * 
 */
public class PeakSummaryVisualizer implements MZmineModule, ActionListener {

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        /*
         * desktop.addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA, "Peaklist
         * summary", "Plotcan", KeyEvent.VK_N, false, this, null);
         */
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Peak list row summary";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameters) {
    }

    public static void showNewPeakSummaryWindow(PeakListRow row) {
        PeakSummaryWindow newWindow = new PeakSummaryWindow(row);
        MZmineCore.getDesktop().addInternalFrame(newWindow);
    }

}