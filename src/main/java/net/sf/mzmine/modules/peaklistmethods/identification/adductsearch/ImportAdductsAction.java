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

package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.parametertypes.AdductsComponent;
import net.sf.mzmine.util.dialogs.LoadSaveFileChooser;

import com.Ostermiller.util.CSVParser;

/**
 * An action to handle importing adducts from a file.
 *
 * @author $Author$
 * @version $Revision$
 */

public class ImportAdductsAction extends AbstractAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Logger.
    private static final Logger LOG = Logger
	    .getLogger(ImportAdductsAction.class.getName());

    // Filename extension.
    private static final String FILENAME_EXTENSION = "csv";

    private LoadSaveFileChooser chooser;

    /**
     * Create the action.
     */
    public ImportAdductsAction() {

	super("Import...");
	putValue(SHORT_DESCRIPTION, "Import custom adducts from a CSV file");

	chooser = null;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

	// Parent component.
	final AdductsComponent parent = (AdductsComponent) SwingUtilities
		.getAncestorOfClass(AdductsComponent.class,
			(Component) e.getSource());

	if (parent != null) {

	    // Create the chooser if necessary.
	    if (chooser == null) {

		chooser = new LoadSaveFileChooser("Select Adducts File");
		chooser.addChoosableFileFilter(new FileNameExtensionFilter(
			"Comma-separated values files", FILENAME_EXTENSION));
	    }

	    // Select a file.
	    final File file = chooser.getLoadFile(parent);
	    if (file != null) {

		// Read the CSV file into a string array.
		String[][] csvLines = null;
		try {

		    csvLines = CSVParser.parse(new FileReader(file));
		} catch (IOException ex) {
		    final Window window = (Window) SwingUtilities
			    .getAncestorOfClass(Window.class,
				    (Component) e.getSource());
		    final String msg = "There was a problem reading the adducts file.";
		    MZmineCore.getDesktop().displayErrorMessage(window,
			    "I/O Error", msg + "\n(" + ex.getMessage() + ')');
		    LOG.log(Level.SEVERE, msg, ex);
		}

		// Read the adducts data.
		if (csvLines != null) {

		    // Load adducts from CSV data into parent choices.
		    parent.setChoices(loadAdductsIntoChoices(csvLines,
			    (AdductType[]) parent.getChoices()));
		}
	    }
	}
    }

    /**
     * Load the adducts into the list of adduct choices.
     *
     * @param lines
     *            CSV lines to parse.
     * @param adducts
     *            the current adduct choices.
     * @return a new list of adduct choices that includes the original choices
     *         plus any new ones found by parsing the CSV lines.
     */
    private static AdductType[] loadAdductsIntoChoices(final String[][] lines,
	    final AdductType[] adducts) {

	// Create a list of adducts.
	final ArrayList<AdductType> choices = new ArrayList<AdductType>(
		Arrays.asList(adducts));

	int i = 1;
	for (final String[] line : lines) {

	    if (line.length >= 2) {

		try {

		    // Create new adduct and add it to the choices if it's new.
		    final AdductType adduct = new AdductType(line[0],
			    Double.parseDouble(line[1]));
		    if (!choices.contains(adduct)) {

			choices.add(adduct);
		    }
		} catch (final NumberFormatException ignored) {

		    LOG.warning("Invalid numeric value (" + line[1]
			    + ") - ignored.");
		}
	    } else {

		LOG.warning("Line #" + i
			+ " contains too few fields - ignored.");
	    }
	    i++;
	}

	return choices.toArray(new AdductType[choices.size()]);
    }
}