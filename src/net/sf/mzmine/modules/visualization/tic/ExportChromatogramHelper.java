/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Helper utilities of exporting chromatogram.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ExportChromatogramHelper {

    // CSV extension.
    private static final String CSV_EXTENSION = "csv";

    // Export file chooser.
    private static JFileChooser exportChooser = null;

    /**
     * Utility class - no public constructor.
     */
    private ExportChromatogramHelper() {

        // no public access.
    }

    /**
     * Prompt the user for the file to export to.
     *
     * @param parent   parent window for dialogs.
     * @param fileName default name of selected file.
     * @return the selected file (or null if no choice is made).
     */
    public static File getExportFile(final Component parent, final String fileName) {

        // Create the chooser if necessary.
        if (exportChooser == null) {

            exportChooser = new JFileChooser();
            exportChooser.setApproveButtonMnemonic('E');
            exportChooser.setApproveButtonToolTipText("Export chromatogram to the selected file");
            exportChooser.setDialogTitle("Export Chromatogram");
            exportChooser.addChoosableFileFilter(
                    new FileNameExtensionFilter("Comma-separated values files", CSV_EXTENSION));
        }

        // Set default selection.
        exportChooser.setSelectedFile(applyFileNameExtension(new File(fileName)));

        // Select a file.
        File file = null;
        for (boolean done = false; !done; ) {
            done = true;
            if (exportChooser.showDialog(parent, "Export") == JFileChooser.APPROVE_OPTION) {

                // Get the selected file.
                file = applyFileNameExtension(exportChooser.getSelectedFile());

                // Does the file exist.
                if (file.exists() &&
                    JOptionPane.showConfirmDialog(parent,
                                                  "Do you want to overwrite the file?",
                                                  "File Exists",
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.WARNING_MESSAGE) !=
                    JOptionPane.YES_OPTION) {

                    // Ask again.
                    done = false;
                }
            }
        }

        return file;
    }

    /**
     * Returns the file with the correct extension.
     *
     * @param file the file.
     * @return if the file is not null and doesn't end with the correct extension then a new file with the extension
     *         appended to the name is returned, otherwise the file is returned unchanged.
     */
    private static File applyFileNameExtension(File file) {

        if (file != null) {
            final String name = file.getName();
            final String extension = '.' + CSV_EXTENSION;
            if (!name.toLowerCase().endsWith(extension)) {
                file = new File(file.getParent(), name + extension);
            }
        }
        return file;
    }
}
