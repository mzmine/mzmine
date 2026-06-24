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

import static io.github.mzmine.javafx.components.factories.FxButtons.createLabelButton;
import static io.github.mzmine.javafx.components.factories.FxLabels.colored;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.util.FxIconUtil.newIconButton;
import static io.github.mzmine.javafx.util.FxIconUtil.newIconButtonOpenUrl;

import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesController;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesTab;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.global.IonLibraryImportResult;
import io.github.mzmine.datamodel.identities.global.IonLibraryImportResult.MergePolicy;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterComponent;
import java.util.List;
import java.util.Optional;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Use composite parameter to allow changes later to add more filters like polarity and charge.
 */
public class IonLibraryComponent extends BorderPane implements ParameterComponent<IonLibrary> {

  private final StringProperty numIonsText = new SimpleStringProperty();
  private final StringProperty conflictWithLocalText = new SimpleStringProperty();
  private final StringProperty updateWithGlobalTooltip = new SimpleStringProperty();
  private final BooleanBinding hasConflictWithLocal = conflictWithLocalText.isNotEmpty();
  private final BooleanProperty globalIsNewer = new SimpleBooleanProperty(false);
  private final StringProperty addToGlobalTooltip = new SimpleStringProperty();
  private final BooleanProperty globalIsMissing = new SimpleBooleanProperty(false);

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

    final IonLibraryComponentPopover libraryPopover = new IonLibraryComponentPopover(
        controller.librariesProperty());
    library = libraryPopover.selectedLibraryProperty();

    final ObservableValue<String> buttonLabel = library.map(IonLibrary::name)
        .orElse("Select ion library");
    final Button selectLibraryButton = createLabelButton(buttonLabel, FxIcons.LIST, tooltip,
        () -> { /* popover toggles via installOn */ });
    libraryPopover.installOn(selectLibraryButton);

    final ButtonBase updateWithGlobalLibrary = FxIconUtil.newIconButton(FxIcons.RELOAD,
        updateWithGlobalTooltip, negativeColor, this::exchangeIonLibraryForGlobalVersion);
    updateWithGlobalLibrary.visibleProperty().bind(globalIsNewer);
    updateWithGlobalLibrary.managedProperty().bind(globalIsNewer);

    final ButtonBase addToGlobalLibrary = FxIconUtil.newIconButton(FxIcons.SAVE, addToGlobalTooltip,
        negativeColor, this::saveToGlobal);
    addToGlobalLibrary.visibleProperty().bind(globalIsMissing);
    addToGlobalLibrary.managedProperty().bind(globalIsMissing);

    final var topBox = FxLayout.newIconPane(Orientation.HORIZONTAL, //
        selectLibraryButton, //
        updateWithGlobalLibrary, //
        addToGlobalLibrary, //
        newIconButton(FxIcons.GEAR_PREFERENCES, tooltip, this::showGlobalTab), //
        newIconButtonOpenUrl(FxIcons.QUESTION_CIRCLE,
            tooltip + "\nClick to open the documentation.",
            "https://mzmine.github.io/mzmine_documentation/ions/ions.html"));

    setTop(topBox);
    setCenter(infoBox);
    library.setValue(lib);

    library.subscribe((nv) -> {
      if (nv == null) {
        globalIsMissing.setValue(false);
        globalIsMissing.set(false);
        numIonsText.setValue(null);
        conflictWithLocalText.set(null);
        updateWithGlobalTooltip.set(null);
        addToGlobalTooltip.set(null);
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

  private void saveToGlobal() {
    final IonLibrary lib = getValue();
    if (lib == null) {
      return;
    }
    final IonLibraryImportResult results = GlobalIonLibraryService.getGlobalLibrary()
        .addLibraries(List.of(lib), MergePolicy.ASK_OLDER_OVERWRITE);
    if (results.isChanged()) {
      GlobalIonLibrariesController.getInstance().updateModel();
      conflictWithLocalText.set(null);
      addToGlobalTooltip.set(null);
      globalIsMissing.set(false);
    }
  }

  private void showGlobalTab() {
    DialogLoggerUtil.showInfoNotification("Define ion libraries in the main window",
        "The '%s' tab (in the main window) enables ion library creation and modification.".formatted(
            GlobalIonLibrariesTab.HEADER));
    GlobalIonLibrariesTab.showTab();
  }

  /**
   * Check for conflicts with global libraries where the name is equal but content is different
   */
  private void checkConflictWithGlobalLibrary(@Nullable IonLibrary lib) {
    globalIsNewer.set(false);
    globalIsMissing.set(false);
    conflictWithLocalText.set(null);
    addToGlobalTooltip.set(null);
    if (lib == null) {
      return;
    }
    String conflict = null;

    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();

    final IonLibrary existing = global.getLibraryForID(lib.id()).orElse(null);
    if (existing != null) {
      if (lib.lastUpdatedDate().isBefore(existing.lastUpdatedDate())) {
        conflict = "The local version of this ion library is NEWER than the loaded parameter library.";
        globalIsNewer.set(true);
      } else if (existing.lastUpdatedDate().isBefore(lib.lastUpdatedDate())) {
        conflict = "The local version of this ion library is OLDER than the loaded parameter library.";
        globalIsMissing.set(true);
        addToGlobalTooltip.set("""
            The loaded library of this parameter is NEWER than the local version of this library.
            Do you want to apply this update to this ion library to the globally available?""");
      }
    } else {
      globalIsMissing.set(true);
      addToGlobalTooltip.set("""
          The loaded library is currently only available in this parameter.
          Do you want to add this ion library to the globally available?""");
    }
    conflictWithLocalText.set(conflict);
  }

  private void exchangeIonLibraryForGlobalVersion() {
    final IonLibrary current = this.getValue();
    if (current == null) {
      return;
    }

    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    final Optional<IonLibrary> existing = global.getLibraryForID(current.id());
    if (existing.isPresent() && DialogLoggerUtil.showDialogYesNo(
        "Exchange ion library with global?",
        "Do you want to replace the parameter ion library with the globally defined?")) {
      library.setValue(existing.get());
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
