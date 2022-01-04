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

package io.github.mzmine.parameters.parametertypes.ionidentity;


import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.actions.AddIonModificationAction;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.actions.CombineESIAdductsAction;
import io.github.mzmine.parameters.parametertypes.MultiChoiceComponent;
import java.util.List;
import javafx.scene.layout.HBox;

/**
 * A component for selecting adducts.
 *
 */
public class IonModificationComponent extends HBox {

  protected MultiChoiceComponent<IonModification> adducts;
  protected MultiChoiceComponent<IonModification> mods;

  /**
   * Create the component.
   *
   * @param choicesAdducts the adduct choices.
   */
  public IonModificationComponent(List<IonModification> choicesAdducts, List<IonModification> choicesMods) {
    setFillHeight(true);
    setSpacing(5);

    adducts = new MultiChoiceComponent<>(choicesAdducts, List.of(IonModification.getDefaultValuesPos()), null,
            IonModification.H, // just any object as a parser
        false, true, true, false
    );
    // add top label
    adducts.setTitle("Adducts");
    // add buttons
    adducts.addButton("Add", new AddIonModificationAction(adducts));
    adducts.addButton("Combine", new CombineESIAdductsAction(adducts));
    adducts.addButton("Reset positive", (e) -> adducts.setChoices(IonModification.getDefaultValuesPos()));
    adducts.addButton("Reset negative", (e) -> adducts.setChoices(IonModification.getDefaultValuesNeg()));

    mods = new MultiChoiceComponent<IonModification>(choicesMods, List.of(IonModification.getDefaultModifications()), null,
            IonModification.H, // just any object as a parser
        false, true, true, false
    );
    // add top label
    mods.setTitle("Modifications");
    // add buttons
    mods.addButton("Add", new AddIonModificationAction(mods));
    mods.addButton("Combine", new CombineESIAdductsAction(mods));
    mods.addButton("Reset", (e) -> mods.setChoices(IonModification.getDefaultModifications()));

    getChildren().addAll(adducts, mods);
  }

  public IonModification[][] getChoices() {
    IonModification[] ad = adducts.getChoices().toArray(IonModification[]::new);
    IonModification[] md = mods.getChoices().toArray(IonModification[]::new);
    IonModification[][] all = {ad, md};
    return all;
  }

  /**
   * Get the users selections.
   *
   * @return the selected choices.
   */
  public IonModification[][] getValue() {
    IonModification[] ad = adducts.getValue().toArray(IonModification[]::new);
    IonModification[] md = mods.getValue().toArray(IonModification[]::new);
    IonModification[][] all = {ad, md};
    return all;
  }

  public void setValue(final IonModification[][] values) {
    if (values != null) {
      if (values[0] != null)
        adducts.setValue(List.of(values[0]));
      if (values[1] != null)
        mods.setValue(List.of(values[1]));
    }
  }

  public MultiChoiceComponent<IonModification> getMods() {
    return mods;
  }

  public MultiChoiceComponent<IonModification> getAdducts() {
    return adducts;
  }

}
