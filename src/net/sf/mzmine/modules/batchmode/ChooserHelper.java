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

package net.sf.mzmine.modules.batchmode;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Helper utilities for choosing files.
 *
 * @version $Revision$
 */
public class ChooserHelper {

    // XML extension.
    private static final String XML_EXTENSION = "xml";

    // File chooser.
    private static JFileChooser chooser = null;

    /**
     * Utility class - no public constructor.
     */
    private ChooserHelper() {

        // no public access.
    }

    /**
     * Prompt the user for the file to load.
     *
     * @param parent parent window for dialogs.
     * @return the selected file (or null if no choice is made).
     */
    public static File getLoadFile(final Component parent) {

        // Create the chooser if necessary.
        if (chooser == null) {

            createChooser();
        }

        // Clear default selection.
        chooser.setSelectedFile(new File(""));

        // Select a file.
        File file = null;
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {

            // Get the selected file.
            file = chooser.getSelectedFile();
        }

        return file;
    }

    /**
     * Prompt the user for the file to save to.
     *
     * @param parent parent window for dialogs.
     * @return the selected file (or null if no choice is made).
     */
    public static File getSaveFile(final Component parent) {

        // Create the chooser if necessary.
        if (chooser == null) {

            createChooser();
        }

        // Clear default selection.
        chooser.setSelectedFile(new File(""));

        // Select a file.
        File file = null;
        for (boolean done = false; !done; ) {
            done = true;
            if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {

                // Get the selected file.
                file = applyFileNameExtension(chooser.getSelectedFile());

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
     * Create the chooser.
     */
    private static void createChooser() {
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("XML files", XML_EXTENSION));
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
            final String extension = '.' + XML_EXTENSION;
            if (!name.toLowerCase().endsWith(extension)) {
                file = new File(file.getParent(), name + extension);
            }
        }
        return file;
    }
}
