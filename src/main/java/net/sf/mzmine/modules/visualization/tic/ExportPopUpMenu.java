/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.visualization.tic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.sf.mzmine.datamodel.RawDataFile;

/**
 * A pop-up menu to select chromatograms for export.
 */
public class ExportPopUpMenu extends JMenu implements MenuListener {

    // The visualizer window.
    private final TICVisualizerWindow visualizer;

    /**
     * Create the menu item.
     *
     * @param window the visualizer window.
     */
    public ExportPopUpMenu(final TICVisualizerWindow window) {

        super("Export chromatogram...");
        visualizer = window;
        addMenuListener(this);
    }

    @Override
    public void menuSelected(final MenuEvent e) {

        // Clear the menu.
        removeAll();

        // Add the raw data files to the menu and hash table.
        for (final RawDataFile dataFile : visualizer.getRawDataFiles()) {

            // Add menu item for file.
            final JMenuItem item = new JMenuItem(dataFile.getName());
            add(item);


            // Handle item selection.
            item.addActionListener(new ActionListener() {

                @Override public void actionPerformed(final ActionEvent event) {

                    if (dataFile != null) {
                        visualizer.exportChromatogram(dataFile);
                    }
                }
            });
        }
    }

    @Override
    public void menuDeselected(final MenuEvent e) {
        // do nothing
    }

    @Override
    public void menuCanceled(final MenuEvent e) {
        // do nothing
    }
}
