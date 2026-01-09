/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesController;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesTab;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.javafx.components.controls.ListSelectTextField;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterComponent;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Use composite parameter to allow changes later to add more filters like polarity and charge.
 */
public class IonLibraryComponent extends BorderPane implements ParameterComponent<IonLibrary> {

  private final StringProperty selectedName = new SimpleStringProperty();
  private final StringProperty numIonsText = new SimpleStringProperty();
  private final StringProperty conflictWithLocalText = new SimpleStringProperty();
  private final StringProperty updateWithGlobalTooltip = new SimpleStringProperty();
  private final BooleanBinding hasConflictWithLocal = conflictWithLocalText.isNotEmpty();

  private final @NotNull ObjectProperty<IonLibrary> library;

  public IonLibraryComponent(IonLibrary lib) {
    final Color negativeColor = ConfigService.getDefaultColorPalette().getNegativeColor();

    final var infoBox = FxLayout.newFlowPane(Insets.EMPTY, //
        newLabel(numIonsText),
        // shows potential issues like mismatch of IonPart definition with the global definitions
        colored(newBoldLabel(conflictWithLocalText), negativeColor) //
    );

    final String tooltip = """
        Ion libraries are defined globally in the '%s' tab, use the button to open the tab and to define a list of ion types to use.""".formatted(
        GlobalIonLibrariesTab.HEADER);
    final GlobalIonLibrariesController controller = GlobalIonLibrariesController.getInstance();
    final var selectedLibraryField = new ListSelectTextField<>(true,
        controller.librariesProperty());
    library = selectedLibraryField.selectedItemProperty();

    FxTextFields.applyToField(selectedLibraryField, null, selectedName, "Select ion library",
        tooltip);

    final ButtonBase updateWithGlobalLibrary = FxIconUtil.newIconButton(FxIcons.RELOAD,
        updateWithGlobalTooltip, this::exchangeIonLibraryForGlobalVersion);
    updateWithGlobalLibrary.visibleProperty().bind(hasConflictWithLocal);
    updateWithGlobalLibrary.managedProperty().bind(hasConflictWithLocal);

    final var topBox = FxLayout.newFlowPane(Insets.EMPTY, //
        selectedLibraryField, //
        updateWithGlobalLibrary, createButton("Define libraries", FxIcons.GEAR_PREFERENCES, tooltip,
            GlobalIonLibrariesTab::showTab) //
    );

    setTop(topBox);
    setCenter(infoBox);
    library.setValue(lib);

    library.subscribe((nv) -> {
      if (nv == null) {
        numIonsText.setValue(null);
        conflictWithLocalText.set(null);
        updateWithGlobalTooltip.set(null);
      } else {
        checkConflictWithGlobalLibrary(nv);
        // also define tooltip
        updateWithGlobalTooltip.set("""
            Exchange this ion list with the one defined in the global ions with the same name: "%s".
            Generally the current list is fine to use to reproduce results, if this is not required it may be better to update the list to the current version.
            This ion list differs from the globally defined ion list with the same name or there may be differences in how ion parts are defined, e.g., different delta mass or alternative names.""".formatted(
            nv.name()));

        long positives = 0;
        long negatives = 0;
        long neutrals = 0;
        for (IonType ion : nv.ions()) {
          switch (ion.getPolarity()) {
            case POSITIVE -> positives++;
            case NEGATIVE -> negatives++;
            case NEUTRAL -> neutrals++;
          }
        }
        numIonsText.set(
            "Total entries: %d (%d positive / %d negative / %d neutral)".formatted(nv.getNumIons(),
                positives, negatives, neutrals));
      }
    });
  }

  /**
   * Check for conflicts with global libraries where the name is equal but content is different
   */
  private void checkConflictWithGlobalLibrary(@Nullable IonLibrary lib) {
    if (lib == null) {
      conflictWithLocalText.set(null);
      return;
    }
    String conflict = null;

    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    final IonLibrary existing = global.getLibraryForName(lib.name()).orElse(null);
    if (existing != null && !existing.equalContentIgnoreOrder(lib)) {
      conflict = "The ion library '%s' selected in the parameter component differs from the local version. ";
      if (existing.getNumIons() == lib.getNumIons()) {
        conflict = "The sizes are the same, but the defined ions may differ slightly.";
      } else if (existing.getNumIons() < lib.getNumIons()) {
        conflict = "The local library is smaller than the currently selected.";
      } else if (existing.getNumIons() < lib.getNumIons()) {
        conflict = "The local library is larger than the currently selected.";
      }
    }
    conflictWithLocalText.set(conflict);
  }

  private void exchangeIonLibraryForGlobalVersion() {
    final IonLibrary current = this.library.getValue();
    if (current == null) {
      return;
    }

    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    global.getLibraryForName(current.name()).ifPresent(library::setValue);
  }

  @Override
  public IonLibrary getValue() {
    return library.getValue();
  }

  @Override
  public void setValue(IonLibrary value) {
    if (value == null) {
      selectedName.set("");
    }
    library.set(value);
  }
}
