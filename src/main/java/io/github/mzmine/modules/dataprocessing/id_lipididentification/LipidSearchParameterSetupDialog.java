/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidDatabaseTableDialog;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.util.GUIUtils;

/**
 * Parameter setup dialog for lipid search module
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchParameterSetupDialog extends ParameterSetupDialog {

    private final JPanel buttonsPanel;
    private final JButton showDatabaseTable;
    private Object[] selectedObjects;

    private static Logger logger = Logger
            .getLogger(LipidSearchParameterSetupDialog.class.getName());

    private static final long serialVersionUID = 1L;

    public LipidSearchParameterSetupDialog(Window parent,
            boolean valueCheckRequired, ParameterSet parameters) {
        super(parent, valueCheckRequired, parameters);

        // Create Buttons panel.
        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        add(buttonsPanel, BorderLayout.SOUTH);

        // Add buttons
        showDatabaseTable = GUIUtils.addButton(buttonsPanel, "Show database",
                null, this);
        this.mainPanel.addCenter(showDatabaseTable, 0, 110, 3, 1);
        showDatabaseTable.setToolTipText(
                "Show a database table for the selected classes and parameters");
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        super.actionPerformed(e);

        updateParameterSetFromComponents();

        final Object src = e.getSource();

        // Create database
        if (showDatabaseTable.equals(src)) {
            try {
                // commit the changes to the parameter set
                selectedObjects = LipidSearchParameters.lipidClasses.getValue();
                // Convert Objects to LipidClasses
                LipidClasses[] selectedLipids = Arrays.stream(selectedObjects)
                        .filter(o -> o instanceof LipidClasses)
                        .map(o -> (LipidClasses) o)
                        .toArray(LipidClasses[]::new);
                LipidDatabaseTableDialog databaseTable = new LipidDatabaseTableDialog(
                        selectedLipids);
                databaseTable.setVisible(true);
            } catch (Exception t) {
                logger.log(Level.WARNING, "Cannot show database table", t);
            }
        }
    }
}
