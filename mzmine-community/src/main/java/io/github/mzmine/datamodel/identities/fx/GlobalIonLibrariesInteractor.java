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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.cloud.CloudCatalog;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ApplyModelChangesToGlobalService;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.BrowseCloudCatalog;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.CreateNewLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.DiscardModelChanges;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.EditSelectedLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ExportSelectedLibraries;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ImportLibraries;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ReloadGlobalServiceChanges;
import io.github.mzmine.datamodel.identities.global.ApplyResult;
import io.github.mzmine.datamodel.identities.global.ConflictReport;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryDTO;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.global.ValidationResult;
import io.github.mzmine.datamodel.identities.global.ValidationResult.ValidationError;
import io.github.mzmine.datamodel.identities.global.ValidationResult.ValidationWarning;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonPartDefinition;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.dialogs.NotificationService.NotificationType;
import io.github.mzmine.javafx.mvci.FxInteractor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Updates in the {@link GlobalIonLibraryService}
 */
class GlobalIonLibrariesInteractor extends FxInteractor<GlobalIonLibrariesModel> {

  private static final Logger logger = Logger.getLogger(
      GlobalIonLibrariesInteractor.class.getName());

  protected GlobalIonLibrariesInteractor(GlobalIonLibrariesModel model) {
    super(model);
    model.setGlobalIonsVersion(GlobalIonLibraryService.getGlobalLibrary().getVersion());
    updateModel();
    // attach listener to update the model if global ions have changed
    // only the current global version is changed and this will show an interface to the user to
    // trigger the update
    GlobalIonLibraryService.getGlobalLibrary().addChangeListener(
        event -> FxThread.runLater(() -> model.setGlobalIonsVersion(event.version())));
  }

  public boolean askRemoveIonPartDefinition(IonPartDefinition def) {
    // find all libraries with def in them
    final LibrariesWithIonPart matchingLibs = findLibraryIonTypesUsingDefinition(def);

    // cannot change any builtin internal lib
    final boolean anyInternalLib = matchingLibs.libraries().keySet().stream()
        .anyMatch(IonLibrary::isInternalLibrary);
    if (anyInternalLib) {
      DialogLoggerUtil.showNotification(NotificationType.INFO,
          "Cannot change internally defined ion parts.",
          "The selected ion building block is used in internal libraries and cannot be changed.");
      return false;
    }
    final long cloudLibs = matchingLibs.libraries().keySet().stream()
        .filter(IonLibrary::isCloudLibrary).count();

    String cloudMessage =
        cloudLibs == 0 ? "" : "Cloud libraries changed: %d\n".formatted(cloudLibs);

    if (DialogLoggerUtil.showDialogYesNo("Cascade remove ion building block?", """
        Do you want to remove all ion types from all libraries that use ion building block:
        "%s"
        Otherwise the ion building block will stay defined.
        
        This changes:
        %s%s""".formatted(def.toString(), cloudMessage, matchingLibs.toString()))) {

      // change all libs
      model.getIonTypes().removeAll(matchingLibs.types());

      final ObservableList<IonLibrary> targetLibs = model.getLibraries();
      for (Entry<IonLibrary, List<IonType>> entry : matchingLibs.libraries().entrySet()) {
        final IonLibrary lib = entry.getKey();

        final Set<IonType> toRemove = Set.copyOf(entry.getValue());

        List<IonType> ions = new ArrayList<>(lib.ions().size() - toRemove.size());
        for (IonType ion : lib.ions()) {
          if (!toRemove.contains(ion)) {
            ions.add(ion);
          }
        }

        targetLibs.remove(lib);
        // add changed library
        targetLibs.add(new UnmodifiableIonLibrary(lib.id(), lib.origin(), lib.name(), ions));

        model.getEventHandler().accept(new ApplyModelChangesToGlobalService());
      }
    }
    return false;
  }

  private LibrariesWithIonPart findLibraryIonTypesUsingDefinition(IonPartDefinition def) {
    final List<IonType> types = model.getIonTypes().stream()
        .filter(ionType -> ionType.containsPart(def)).toList();

    Map<IonLibrary, List<IonType>> libraries = new HashMap<>();

    for (IonLibrary library : model.getLibraries()) {
      final List<IonType> libTypes = library.ions().stream()
          .filter(ionType -> ionType.containsPart(def)).toList();
      if (libTypes.isEmpty()) {
        continue;
      }
      libraries.put(library, libTypes);
    }

    return new LibrariesWithIonPart(def, libraries, types);
  }


  public void handleEvent(@NotNull GlobalIonLibrariesEvent event) {
    switch (event) {
      case DiscardModelChanges _ -> askDiscardModelChanges();
      case ApplyModelChangesToGlobalService _ -> applyToGlobalIons(false);
      case CreateNewLibrary _ -> createNewLibraryInTab();
      case EditSelectedLibrary(IonLibrary library) -> editLibraryInTab(library);
      case ReloadGlobalServiceChanges _ -> updateModel();
      case BrowseCloudCatalog(var catalog) -> browseCloudCatalog(catalog);
      case ExportSelectedLibraries(var libraries) -> {
        ExportIonLibrariesModule.showExportDialog(libraries);
      }
      case ImportLibraries _ -> {
        ImportIonLibrariesModule.showDialog();
      }
    }
  }

  private void askDiscardModelChanges() {
    if (DialogLoggerUtil.showDialogYesNo("Discard local changes to ion?",
        "Discarding the local changes will reset the ion libraries.")) {
      updateModel();
    }
  }

  /**
   * Push changes to {@link GlobalIonLibraryService} via the versioned apply API. Handles the three
   * possible outcomes: applied, invalid, or conflicting with external changes.
   *
   * @param applyDirectlyIgnoreValidation
   */
  public void applyToGlobalIons(boolean applyDirectlyIgnoreValidation) {
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    final ApplyResult result = global.applyUpdates(model.getRetrievalVersion(),
        snapshotModelAsDto(global.getVersion()), applyDirectlyIgnoreValidation);

    switch (result) {
      case ApplyResult.Applied(int newVersion) -> onApplied(newVersion);
      case ApplyResult.Invalid(ValidationResult vr) -> showValidationErrors(vr);
      case ApplyResult.Conflict(int currentVersion, ConflictReport report) ->
          showConflictDialog(currentVersion, report);
    }
  }

  private void onApplied(int newVersion) {
    model.setLastModelUpdate(null);
    // get latest model from global after applying
    updateModel();
  }

  private void showValidationErrors(@NotNull ValidationResult vr) {
    final String errors = vr.errors().stream().map(ValidationError::message)
        .collect(Collectors.joining("\n• ", "• ", ""));
    final String warnings = vr.warnings().isEmpty() ? ""
        : vr.warnings().stream().map(ValidationWarning::message)
          .collect(Collectors.joining("\n• ", "\n\nWarnings:\n• ", ""));

    logger.fine(
        "Ion library validation errors of view state and model state: " + errors + ", warnings: "
            + warnings);

    final Optional<ButtonType> choice = DialogLoggerUtil.showDialog(AlertType.WARNING,
        "Mismatch of libraries contents",
        "There are conflicts with the libraries view and model states. Either fix them or directly apply the changes.\n\n"
            + errors + warnings, true, ButtonType.APPLY, ButtonType.CANCEL);

    if (choice.isPresent() && choice.get() == ButtonType.APPLY) {
      applyToGlobalIons(true);
    }
  }

  private void showConflictDialog(int currentVersion, @NotNull ConflictReport report) {
    final ButtonType BUTTON_KEEP_MINE = new ButtonType("Keep my changes", ButtonData.OTHER);
    final ButtonType BUTTON_TAKE_EXTERNAL = new ButtonType("Discard and reload", ButtonData.OTHER);
    final ButtonType BUTTON_REVIEW_MANUALLY = new ButtonType("Review manually",
        ButtonData.CANCEL_CLOSE);
    final String summary = summariseConflict(report);
    DialogLoggerUtil.showDialog(AlertType.WARNING, "Global ion library changed elsewhere",
        "Someone or something else updated the ion library while you were editing.\n\n" + summary
            + "\n\nHow would you like to proceed?", true, BUTTON_KEEP_MINE, BUTTON_TAKE_EXTERNAL,
        BUTTON_REVIEW_MANUALLY).ifPresent(btn -> {
      if (btn == BUTTON_KEEP_MINE) {
        // user wants to overwrite external changes — rebase onto currentVersion and retry
        model.setRetrievalVersion(currentVersion);
        applyToGlobalIons(false);
      } else if (btn == BUTTON_TAKE_EXTERNAL) {
        updateModel();
      }
      // REVIEW_MANUALLY / X-close: do nothing; user will resolve in the editor
    });
  }

  private static @NotNull String summariseConflict(@NotNull ConflictReport report) {
    return "• %d librar%s changed on both sides\n• %d librar%s added/renamed locally\n• %d librar%s added/removed externally".formatted(
        report.sameIdDifferentContent().size(),
        report.sameIdDifferentContent().size() == 1 ? "y" : "ies", report.onlyInProposed().size(),
        report.onlyInProposed().size() == 1 ? "y" : "ies", report.onlyInCurrent().size(),
        report.onlyInCurrent().size() == 1 ? "y" : "ies");
  }

  private @NotNull GlobalIonLibraryDTO snapshotModelAsDto(int baseVersion) {
    final List<IonLibrary> libraries = List.copyOf(model.getLibraries());
    final List<IonType> types = List.copyOf(model.getIonTypes());
    final List<IonPart> parts = List.copyOf(model.getParts());
    final List<IonPartDefinition> partDefinitions = List.copyOf(model.getPartsDefinitions());
    return new GlobalIonLibraryDTO(baseVersion, libraries, types, parts, partDefinitions);
  }

  /**
   * Pull ions from {@link GlobalIonLibraryService}
   */
  @Override
  public void updateModel() {
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    FxThread.runLater(() -> {
      final GlobalIonLibraryDTO current = global.getCurrentGlobalLibrary();
      model.setRetrievalVersion(current.version());
      model.setGlobalIonsVersion(current.version());
      model.librariesProperty().setAll(current.libraries());
      model.partsProperty().setAll(current.parts());
      model.ionTypesProperty().setAll(current.types());
      model.partsDefinitionsProperty().setAll(current.partDefinitions());
      model.setLastModelUpdate(null);
    });
  }

  public void createNewLibraryInTab() {
    editLibraryInTab(null);
  }

  public void editLibraryInTab(@Nullable IonLibrary library) {
    final IonLibraryEditController editController = model.getEditLibraryController();
    final IonLibraryEditModel editModel = editController.getModel();
    // tab handles the defense of internal libraries from being changed
    final boolean editActive = model.isLibraryEditActive();
    // library is build edited already and was changed
    if (editActive && !editModel.isSameAsOriginal()) {
      final ButtonType save = new ButtonType("Save changes", ButtonData.YES);
      final ButtonType discard = new ButtonType("Discard changes", ButtonData.APPLY);
      final ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

      String typing = library == null ? "creating" : "editing";
      String type = library == null ? "create new" : "edit";
      final String oldLibName = editModel.getName();

      final boolean isNameRestricted = editModel.nameRestrictedProperty().get();
      if (isNameRestricted) {
        final String message = """
          Requesting to %s library.
          You currently have unsaved changes while working on library:
          %s
          
          The name is reserved for internal libraries,
          so either cancel and change name first or discard any changes and continue?""".formatted(type, oldLibName);

        final String title = "Discard changes to library before %s another?".formatted(typing);
        final ButtonType userAction = DialogLoggerUtil.showDialog(AlertType.WARNING, title, message,
            discard, cancel).orElse(cancel);
        if (userAction.equals(cancel)) {
          return;
        }

      } else {
        final String message = """
          Requesting to %s library.
          You currently have unsaved changes while working on library:
          %s
          
          Do you want to save these changes first?""".formatted(type, oldLibName);
        final String title = "Save library before %s another?".formatted(typing);
        final ButtonType userAction = DialogLoggerUtil.showDialog(AlertType.WARNING, title, message,
            save, discard, cancel).orElse(cancel);
        if (userAction.equals(cancel)) {
          return;
        } else if(userAction.equals(save)) {
          // save old library first
          if (!editController.saveLibrary(false)) {
            // issue while saving
            return;
          }
        }
      }
    }
      // discard changes by setting new edit library
    editModel.setLibrary(library);
    model.setLibraryEditActive(true);
  }

  /**
   * Currently not used but later to connect to shared libraries
   */
  private void browseCloudCatalog(@NotNull CloudCatalog catalog) {
    final var entries = catalog.list();
    if (entries.isEmpty()) {
      DialogLoggerUtil.showDialog(AlertType.INFORMATION, "Cloud catalog",
          "No cloud libraries are available yet. A browse dialog will ship once the catalog is live.",
          true);
      return;
    }
    // the browse dialog is a future-work item; for now list is enough to prove the seam is wired.
    DialogLoggerUtil.showDialog(AlertType.INFORMATION, "Cloud catalog",
        "%d librar%s available (browse UI coming soon).".formatted(entries.size(),
            entries.size() == 1 ? "y" : "ies"), true);
  }

}
