/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import static io.github.mzmine.javafx.components.factories.FxButtons.createButton;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.components.factories.FxTexts.colored;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesController;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesTab;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterComponent;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/**
 * Use composite parameter to allow changes later to add more filters like polarity and charge.
 */
public class IonLibraryComponent extends BorderPane implements ParameterComponent<IonLibrary> {

  private final StringProperty selectedName = new SimpleStringProperty();
  private final StringProperty numIonsText = new SimpleStringProperty();
  private final StringProperty conflictWithLocalText = new SimpleStringProperty();
  private final StringProperty updateWithGlobalTooltip = new SimpleStringProperty();
  private final BooleanBinding hasConflictWithLocal = conflictWithLocalText.isNotEmpty();

  private final ObjectProperty<IonLibrary> library = new SimpleObjectProperty<>();

  public IonLibraryComponent(IonLibrary lib) {
    library.setValue(lib);
    library.subscribe((nv) -> {
      if (nv == null) {
        selectedName.setValue(null);
        numIonsText.setValue(null);
        conflictWithLocalText.set(null);
        updateWithGlobalTooltip.set(null);
      } else {
        // TODO find conflicts with global definition
        conflictWithLocalText.set(null);
        // also define tooltip
        updateWithGlobalTooltip.set("""
            Exchange this ion list with the one defined in the global ions with the same name: "%s".
            Generally the current list is fine to use to reproduce results, if this is not required it may be better to update the list to the current version.
            This ion list differs from the globally defined ion list with the same name or there may be differences in how ion parts are defined, e.g., different delta mass or alternative names.""".formatted(
            nv.getName()));

        final long positives = nv.getIons().stream()
            .filter(type -> type.getPolarity() == PolarityType.POSITIVE).count();
        final long negatives = nv.getIons().stream()
            .filter(type -> type.getPolarity() == PolarityType.NEGATIVE).count();
        numIonsText.set(
            "Num ion types: %d (%d positive / %d negative)".formatted(nv.getNumIons(), positives,
                negatives));

        selectedName.set(nv.getName());
      }
    });

    selectedName.subscribe(newName -> handleSelectedNameChange(newName));

    final Color negativeColor = ConfigService.getDefaultColorPalette().getNegativeColor();

    final var infoBox = FxLayout.newFlowPane(Insets.EMPTY, //
        newLabel(numIonsText),
        // shows potential issues like mismatch of IonPart definition with the global definitions
        colored(newBoldLabel(conflictWithLocalText), negativeColor) //
    );

    final String tooltip = """
        Ion libraries are defined globally in the '%s' tab, use the button to open the tab and to define a list of ion types to use.""".formatted(
        GlobalIonLibrariesTab.HEADER);
    final TextField selectedLibraryField = FxTextFields.newAutoGrowTextField(selectedName,
        "Select ion library", tooltip, 6, 40);

    final ButtonBase updateWithGlobalLibrary = FxIconUtil.newIconButton(FxIcons.RELOAD,
        updateWithGlobalTooltip, this::exchangeIonListForGlobalVersion);
    updateWithGlobalLibrary.visibleProperty().bind(hasConflictWithLocal);
    updateWithGlobalLibrary.managedProperty().bind(hasConflictWithLocal);

    final GlobalIonLibrariesController controller = GlobalIonLibrariesController.getInstance();
    FxTextFields.bindAutoCompletion(selectedLibraryField, controller.librariesProperty());

    final var topBox = FxLayout.newFlowPane(Insets.EMPTY, //
        selectedLibraryField, //
        updateWithGlobalLibrary, createButton("Define libraries", FxIcons.GEAR_PREFERENCES, tooltip,
            GlobalIonLibrariesTab::showTab) //
    );

    setTop(topBox);
    setCenter(infoBox);
  }

  private void exchangeIonListForGlobalVersion() {
    // TODO implement way to get the global library with that name
  }

  private void handleSelectedNameChange(String newName) {
    final IonLibrary currentLibrary = getValue();
    if (currentLibrary != null && newName.equals(currentLibrary.getName())) {
      // TODO
    }
  }

  @Override
  public IonLibrary getValue() {
    return library.getValue();
  }

  @Override
  public void setValue(IonLibrary value) {
    library.set(value);
  }
}
