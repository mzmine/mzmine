/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidDatabaseTableDialog;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 * Parameter setup dialog for lipid search module
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchParameterSetupDialog extends ParameterSetupDialog {

  private final Button showDatabaseTable;
  private Object[] selectedObjects;

  private static Logger logger = Logger.getLogger(LipidSearchParameterSetupDialog.class.getName());

  public LipidSearchParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    // Add buttons
    showDatabaseTable = new Button("Show database");
    showDatabaseTable
        .setTooltip(new Tooltip("Show a database table for the selected classes and parameters"));
    showDatabaseTable.setOnAction(e -> {
      try {
        updateParameterSetFromComponents();

        // commit the changes to the parameter set
        selectedObjects = LipidSearchParameters.lipidClasses.getValue();
        // Convert Objects to LipidClasses
        LipidClasses[] selectedLipids =
            Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
                .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);
        LipidDatabaseTableDialog databaseTable = new LipidDatabaseTableDialog(selectedLipids);
        databaseTable.show();
      } catch (Exception t) {
        logger.log(Level.WARNING, "Cannot show database table", t);
      }
    });


    pnlButtons.getButtons().add(showDatabaseTable);
  }

}
