/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications;

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
import com.Ostermiller.util.CSVParser;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.parametertypes.LipidModificationChoiceComponent;
import net.sf.mzmine.util.dialogs.LoadSaveFileChooser;

/**
 * An action to handle importing lipid modifications from a file.
 *
 * @author $Author$
 * @version $Revision$
 */

public class ImportLipidModificationsAction extends AbstractAction {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  // Logger.
  private static final Logger LOG =
      Logger.getLogger(ImportLipidModificationsAction.class.getName());

  // Filename extension.
  private static final String FILENAME_EXTENSION = "csv";

  private LoadSaveFileChooser chooser;

  /**
   * Create the action.
   */
  public ImportLipidModificationsAction() {

    super("Import...");
    putValue(SHORT_DESCRIPTION, "Import lipid modifications from a CSV file");

    chooser = null;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {

    // Parent component.
    final LipidModificationChoiceComponent parent =
        (LipidModificationChoiceComponent) SwingUtilities
            .getAncestorOfClass(LipidModificationChoiceComponent.class, (Component) e.getSource());

    if (parent != null) {

      // Create the chooser if necessary.
      if (chooser == null) {

        chooser = new LoadSaveFileChooser("Select lipid modification file");
        chooser.addChoosableFileFilter(
            new FileNameExtensionFilter("Comma-separated values files", FILENAME_EXTENSION));
      }

      // Select a file.
      final File file = chooser.getLoadFile(parent);
      if (file != null) {

        // Read the CSV file into a string array.
        String[][] csvLines = null;
        try {

          csvLines = CSVParser.parse(new FileReader(file));
        } catch (IOException ex) {
          final Window window =
              (Window) SwingUtilities.getAncestorOfClass(Window.class, (Component) e.getSource());
          final String msg = "There was a problem reading the lipid modification file.";
          MZmineCore.getDesktop().displayErrorMessage(window, "I/O Error",
              msg + "\n(" + ex.getMessage() + ')');
          LOG.log(Level.SEVERE, msg, ex);
        }

        // Read the lipid modifications data.
        if (csvLines != null) {

          // Load adducts from CSV data into parent choices.
          parent.setChoices(loadLipidModificationsIntoChoices(csvLines,
              (LipidModification[]) parent.getChoices()));
        }
      }
    }
  }

  /**
   * Load the adducts into the list of adduct choices.
   *
   * @param lines CSV lines to parse.
   * @param adducts the current adduct choices.
   * @return a new list of adduct choices that includes the original choices plus any new ones found
   *         by parsing the CSV lines.
   */
  private static LipidModification[] loadLipidModificationsIntoChoices(final String[][] lines,
      final LipidModification[] modifications) {

    // Create a list of lipid modifications.
    final ArrayList<LipidModification> choices =
        new ArrayList<LipidModification>(Arrays.asList(modifications));

    int i = 1;
    for (final String line[] : lines) {
      try {

        // Create new modification and add it to the choices if it's new.
        final LipidModification modification = new LipidModification(line[0]);
        if (!choices.contains(modification)) {

          choices.add(modification);
        }
      } catch (final NumberFormatException ignored) {

        LOG.warning("Couldn't find lipid modifier in line " + line[0]);
      }
    }

    return choices.toArray(new LipidModification[choices.size()]);
  }
}
