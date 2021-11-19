/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.actions;


import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.parameters.parametertypes.MultiChoiceComponent;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * An action to add custom adducts.
 */
public class CombineESIAdductsAction implements EventHandler<ActionEvent> {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private MultiChoiceComponent<IonModification> parent;

  /**
   * Create the action.
   */
  public CombineESIAdductsAction(MultiChoiceComponent<IonModification> parent) {
    super();
    this.parent = parent;
  }

  @Override
  public void handle(ActionEvent event) {
    if (parent != null) {
      // Show dialog.
      CombineIonModificationDialog dialog =
          new CombineIonModificationDialog(parent.getChoices());
      dialog.showAndWait();
      List<IonModification> add = dialog.getNewTypes();
      if (!add.isEmpty()) {
        addAll(add);
      }
    }
  }

  private void addAll(List<IonModification> add) {
    // Add to list of choices (if not already present).
    List<IonModification> choices = parent.getChoices();

    add.stream().filter(a -> !choices.contains(a)).forEach(a -> choices.add(a));

    parent.setChoices(choices);
  }
}
