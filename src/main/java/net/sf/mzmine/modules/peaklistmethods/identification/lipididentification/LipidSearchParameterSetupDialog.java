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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidDatabaseTableDialog;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;

/**
 * Parameter setup dialog for lipid search module
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchParameterSetupDialog extends ParameterSetupDialog {

  private final JPanel buttonsPanel;
  private final JButton showDatabaseTable;

  private static final long serialVersionUID = 1L;

  public LipidSearchParameterSetupDialog(Window parent, boolean valueCheckRequired,
      ParameterSet parameters) {
    super(parent, valueCheckRequired, parameters);

    // Create Buttons panel.
    buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
    add(buttonsPanel, BorderLayout.SOUTH);

    // Add buttons
    showDatabaseTable = GUIUtils.addButton(buttonsPanel, "Show database", null, this);
    this.mainPanel.addCenter(showDatabaseTable, 0, 110, 3, 1);
    showDatabaseTable
        .setToolTipText("Show a database table for the selected classes and parameters");
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    updateParameterSetFromComponents();

    final Object src = e.getSource();

    // Create database
    if (showDatabaseTable.equals(src)) {
      // commit the changes to the parameter set

      Object[] selectedObjects = LipidSearchParameters.lipidClasses.getValue();
      // Convert Objects to LipidClasses
      LipidClasses[] selectedLipids =
          Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
              .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);

      LipidDatabaseTableDialog databaseTable = new LipidDatabaseTableDialog(selectedLipids);

      databaseTable.setVisible(true);
    }

    if (btnOK.equals(src)) {
      closeDialog(ExitCode.OK);
    }

    if (btnCancel.equals(src)) {
      closeDialog(ExitCode.CANCEL);
    }

    if ((src instanceof JCheckBox) || (src instanceof JComboBox)) {
      parametersChanged();
    }
  }
}
