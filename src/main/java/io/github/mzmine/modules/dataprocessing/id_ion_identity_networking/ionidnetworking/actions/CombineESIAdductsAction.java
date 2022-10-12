/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
