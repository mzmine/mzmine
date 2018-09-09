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
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import net.sf.mzmine.parameters.parametertypes.LipidModificationChoiceComponent;

/**
 * Action to remove a lipid modification
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class RemoveLipidModificationsAction extends AbstractAction {

  private static final long serialVersionUID = 1L;

  /**
   * Create the action.
   */
  public RemoveLipidModificationsAction() {

    super("Remove");
    putValue(SHORT_DESCRIPTION, "Remove all lipid modification");

  }

  @Override
  public void actionPerformed(final ActionEvent e) {

    // Parent component.
    final LipidModificationChoiceComponent parent =
        (LipidModificationChoiceComponent) SwingUtilities
            .getAncestorOfClass(LipidModificationChoiceComponent.class, (Component) e.getSource());

    if (parent != null) {
      parent.setChoices(new LipidModification[0]);
    }
  }
}
