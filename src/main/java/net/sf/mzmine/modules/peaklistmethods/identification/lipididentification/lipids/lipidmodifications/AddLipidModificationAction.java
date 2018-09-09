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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.LipidModificationChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * Class to add a lipid modification
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class AddLipidModificationAction extends AbstractAction {

  private static final long serialVersionUID = 1L;
  private LipidModification lipidModification = null;

  /**
   * Create the action.
   */
  public AddLipidModificationAction() {
    super("Add...");
  }

  @Override
  public void actionPerformed(final ActionEvent e) {

    // Parent component.
    final LipidModificationChoiceComponent parent =
        (LipidModificationChoiceComponent) SwingUtilities
            .getAncestorOfClass(LipidModificationChoiceComponent.class, (Component) e.getSource());
    if (parent != null) {
      // Show dialog.
      final ParameterSet parameters = new AddLipidModificationParameters();
      if (parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(),
          true) == ExitCode.OK) {
        // Create new lipid modification
        lipidModification = new LipidModification(
            parameters.getParameter(AddLipidModificationParameters.lipidModification).getValue());

        // Add to list of choices (if not already present).
        final Collection<LipidModification> choices = new ArrayList<LipidModification>(
            Arrays.asList((LipidModification[]) parent.getChoices()));
        if (!choices.contains(lipidModification)) {
          choices.add(lipidModification);
          parent.setChoices(choices.toArray(new LipidModification[choices.size()]));
        }
      }
    }
  }

  /**
   * Represents a lipid modification.
   */
  private static class AddLipidModificationParameters extends SimpleParameterSet {

    // lipid modification
    private static final StringParameter lipidModification =
        new StringParameter("Lipid modification", "Lipid modification");

    private AddLipidModificationParameters() {
      super(new Parameter[] {lipidModification});
    }
  }
}
