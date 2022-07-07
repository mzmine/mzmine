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

package io.github.mzmine.parameters.parametertypes.ionidentity;


import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.actions.AddIonModificationAction;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.actions.CombineESIAdductsAction;
import io.github.mzmine.parameters.parametertypes.MultiChoiceComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.scene.layout.HBox;

/**
 * A component for selecting adducts.
 */
public class IonModificationComponent extends HBox {

  protected MultiChoiceComponent<IonModification> adducts;
  protected MultiChoiceComponent<IonModification> mods;

  /**
   * Create the component.
   *
   * @param choicesAdducts the adduct choices.
   */
  public IonModificationComponent(List<IonModification> choicesAdducts,
      List<IonModification> choicesMods) {
    setFillHeight(true);
    setSpacing(5);

    adducts = new MultiChoiceComponent<>(choicesAdducts,
        List.of(IonModification.getDefaultValuesPos()), null, IonModification.H,
        // just any object as a parser
        true, true, false, true, true, false);
    // add top label
    adducts.setTitle("Adducts");
    // add buttons
    adducts.addButton("Add", new AddIonModificationAction(adducts));
    adducts.addButton("Combine", new CombineESIAdductsAction(adducts));
    adducts.addButton("Reset positive",
        (e) -> adducts.setChoices(IonModification.getDefaultValuesPos()));
    adducts.addButton("Reset negative",
        (e) -> adducts.setChoices(IonModification.getDefaultValuesNeg()));

    mods = new MultiChoiceComponent<>(choicesMods,
        List.of(IonModification.getDefaultModifications()), null, IonModification.H,
        // just any object as a parser
        true, true, false, true, true, false);
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
    return new IonModification[][]{ad, md};
  }

  /**
   * Get the users selections.
   *
   * @return the selected choices.
   */
  public IonModification[][] getValue() {
    IonModification[] ad = adducts.getValue().stream().filter(Objects::nonNull)
        .toArray(IonModification[]::new);
    IonModification[] md = mods.getValue().stream().filter(Objects::nonNull)
        .toArray(IonModification[]::new);
    return new IonModification[][]{ad, md};
  }

  public void setValue(final IonModification[][] values) {
    if (values != null && values.length == 2) {
      if (values[0] != null) {
        adducts.setValue(Arrays.stream(values[0]).filter(Objects::nonNull).toList());
      }
      if (values[1] != null) {
        mods.setValue(Arrays.stream(values[1]).filter(Objects::nonNull).toList());
      }
    }
  }

  public MultiChoiceComponent<IonModification> getMods() {
    return mods;
  }

  public MultiChoiceComponent<IonModification> getAdducts() {
    return adducts;
  }

}
