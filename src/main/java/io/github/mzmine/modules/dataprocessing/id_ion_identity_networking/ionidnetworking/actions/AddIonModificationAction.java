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
import io.github.mzmine.datamodel.identities.iontype.IonModificationType;
import io.github.mzmine.gui.framework.listener.DelayedDocumentListener;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.*;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * An action to add custom adducts.
 *
 */
public class AddIonModificationAction implements  EventHandler<ActionEvent> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private MultiChoiceComponent<IonModification> parent;

  /**
   * Create the action.
   */
  public AddIonModificationAction(MultiChoiceComponent<IonModification> parent) {
    super();
    this.parent = parent;
  }

  @Override
  public void handle(ActionEvent event) {
    if (parent != null) {

      // Show dialog.
      final AddESIAdductParameters parameters = new AddESIAdductParameters();
      if (parameters.showSetupDialog(true,
          IonModificationType.ADDUCT, IonModificationType.CLUSTER, IonModificationType.NEUTRAL_LOSS,
          IonModificationType.ISOTOPE) == ExitCode.OK) {

        //
        int charge = 0;
        Double mz = null;
        String name = parameters.getParameter(AddESIAdductParameters.NAME).getValue();
        IonModificationType type = parameters.getParameter(AddESIAdductParameters.TYPE).getValue();
        // Create new adduct.
        SumformulaParameter form = parameters.getParameter(AddESIAdductParameters.FORMULA);
        if (form.checkValue() && !form.getValue().isEmpty()) {
          if (name.isEmpty()) {
            name = form.getValue();
          }
          double test = form.getMonoisotopicMass();
          if (test != 0) {
            mz = test;
            charge = form.getCharge();
          }
        }
        if (mz == null) {
          mz = parameters.getParameter(AddESIAdductParameters.MASS_DIFFERENCE).getValue();
          charge = parameters.getParameter(AddESIAdductParameters.CHARGE).getValue();
        }

        final IonModification adduct = new IonModification(type, name, mz, charge);

        // Add to list of choices (if not already present).
        final Collection<IonModification> choices =
            new ArrayList<>(parent.getChoices());

        if (!choices.contains(adduct)) {
          choices.add(adduct);
          parent.setChoices(choices.toArray(new IonModification[choices.size()]));
        }
      }
    }
  }

  /**
   * Represents an adduct.
   */
  private static class AddESIAdductParameters extends SimpleParameterSet {

    // type
    private static final ComboParameter<IonModificationType> TYPE = new ComboParameter<>("Type",
        "The type of ion modification", IonModificationType.values(), IonModificationType.ADDUCT);

    // Adduct name.
    private static final StringParameter NAME =
        new StringParameter("Name", "A name to identify the new adduct.", "", false);

    // Sum formula
    private static final SumformulaParameter FORMULA = new SumformulaParameter("Sum formula",
        "Put - infront of the sum formula if this is a neutral loss. Will override mass difference and charge. Is used as name if name is empty.",
        "", false);

    // Adduct mass difference.
    private static final DoubleParameter MASS_DIFFERENCE = new DoubleParameter("Mass difference",
        "Mass difference for the new adduct", MZmineCore.getConfiguration().getMZFormat(), 0d);

    private static final IntegerParameter CHARGE =
        new IntegerParameter("Charge", "Charge of adduct", 1, false);

    private AddESIAdductParameters() {
      super(new Parameter[] {TYPE, NAME, FORMULA, MASS_DIFFERENCE, CHARGE});
    }

    @Override
    public ExitCode showSetupDialog(boolean valueCheckRequired) {
      return this.showSetupDialog(valueCheckRequired, (IonModificationType[]) null);
    }

    public ExitCode showSetupDialog(boolean valueCheckRequired,
        IonModificationType... types) {
      ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);

      // enable
      TextField com =  dialog.getComponentForParameter(FORMULA);
      com.textProperty().addListener((txt, old, newValue) -> {
        dialog.getComponentForParameter(MASS_DIFFERENCE)
            .setDisable(!newValue.isEmpty());
        dialog.getComponentForParameter(CHARGE).setDisable(!newValue.isEmpty());
      });

      ComboBox<IonModificationType> comType =
          dialog.getComponentForParameter(TYPE);
      if(comType!=null) {
        if (types != null && types.length > 0 && types[0] != null) {
          comType.setItems(FXCollections.observableArrayList(types));
          comType.getSelectionModel().select(0);
        } else {
          comType.setVisible(false);
        }
      }

      dialog.showAndWait();
      return dialog.getExitCode();
    }
  }
}
