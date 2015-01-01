/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.util.dialogs;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * A JFileChooser with convenience functions for loading and saving files.
 *
 * @author $Author$
 * @version $Revision$
 */
public class LoadSaveFileChooser extends JFileChooser {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create the chooser.
     *
     * @param title
     *            dialog title.
     */
    public LoadSaveFileChooser(final String title) {

	setDialogTitle(title);
    }

    /**
     * Prompt the user for the file to load.
     *
     * @param parent
     *            parent window for dialogs.
     * @return the selected file (or null if no choice is made).
     */
    public File getLoadFile(final Component parent) {

	// Clear default selection.
	setSelectedFile(new File(""));

	// Select a file.
	File file = null;
	if (showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {

	    // Get the selected file.
	    file = getSelectedFile();
	}

	return file;
    }

    /**
     * Prompt the user for the file to save to. If the selected file exists then
     * the user is prompted to confirm overwriting it. If the filename extension
     * is missing it is added.
     *
     * @param parent
     *            parent window for dialogs.
     * @param extension
     *            extension to append.
     * @return the selected file (or null if no choice is made).
     */
    public File getSaveFile(final Component parent, final String extension) {

	return getSaveFile(parent, null, extension);
    }

    /**
     * Prompt the user for the file to save to. If the selected file exists then
     * the user is prompted to confirm overwriting it. If the filename extension
     * is missing it is added.
     *
     * @param parent
     *            parent window for dialogs.
     * @param fileName
     *            name of default selected file.
     * @param extension
     *            extension to append.
     * @return the selected file (or null if no choice is made).
     */
    public File getSaveFile(final Component parent, final String fileName,
	    final String extension) {

	// Set default selection.
	setSelectedFile(fileName == null ? new File("")
		: applyFileNameExtension(new File(fileName), extension));

	// Select a file.
	File file = null;
	boolean done = false;
	while (!done) {
	    done = true;
	    if (showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {

		// Get the selected file.
		file = getSelectedFile();

		if (file == null)
		    return null;

		if (!file.exists()) {
		    file = applyFileNameExtension(file, extension);
		}

		if (file == null)
		    return null;

		// Does the file exist?
		if (file.exists()
			&& JOptionPane.showConfirmDialog(parent,
				"Do you want to overwrite the file?",
				"File Exists", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {

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
     * @param file
     *            the file.
     * @param extension
     *            filename extension (without the '.').
     * @return if the file is not null and doesn't end with the correct
     *         extension then a new file with the extension appended to the name
     *         is returned, otherwise the file is returned unchanged.
     */
    private static File applyFileNameExtension(final File file,
	    final String extension) {

	final String name = file.getName();
	final String dotExt = '.' + extension;
	return name.toLowerCase().endsWith(dotExt.toLowerCase()) ? file
		: new File(file.getParent(), name
			+ (name.endsWith(".") ? extension : dotExt));
    }
}
