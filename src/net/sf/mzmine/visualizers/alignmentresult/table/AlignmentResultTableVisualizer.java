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

package net.sf.mzmine.visualizers.alignmentresult.table;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.AlignmentResult;


public class AlignmentResultTableVisualizer implements MZmineModule, ActionListener, ListSelectionListener {

	private Desktop desktop;
	private JMenuItem myMenuItem;

	private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.desktop = core.getDesktop();

        desktop.addMenuSeparator(MZmineMenu.VISUALIZATION);

        myMenuItem = desktop.addMenuItem(MZmineMenu.VISUALIZATION, "Alignment result list view",
                this, null, KeyEvent.VK_A, false, false);
        desktop.addSelectionListener(this);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
/*
        OpenedRawDataFile dataFiles[] = desktop.getSelectedDataFiles();

        for (OpenedRawDataFile dataFile : dataFiles) {
			if (dataFile.getCurrentFile().hasData(PeakList.class)) {

				logger.finest("Showing a new peak list view");

            	PeakListTableViewWindow peakListTable = new PeakListTableViewWindow(dataFile);
            	desktop.addInternalFrame(peakListTable);
			}
        }
*/
    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {

		AlignmentResult[] alignmentResults = desktop.getItemSelector().getSelectedAlignmentResults();
		if (alignmentResults.length>0) myMenuItem.setEnabled(true);

    }


}