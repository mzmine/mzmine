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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ApplyModelChangesToGlobalService;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.AddIons;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.CloseTab;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.ComposeAddLibraries;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.Save;
import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import io.github.mzmine.javafx.components.util.FxTabs;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Tab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controller to edit or create new ion library
 */
class IonLibraryEditController extends FxController<IonLibraryEditModel> {

  private final @NotNull GlobalIonLibrariesModel parentModel;
  private final IonLibraryEditViewBuilder viewBuilder;

  /**
   * For create new
   */
  IonLibraryEditController(@NotNull GlobalIonLibrariesModel parentModel) {
    this(parentModel, null);
  }

  /**
   * @param parentModel used to update
   * @param library     if null then create a new library if not null then edit existing library
   */
  IonLibraryEditController(@NotNull GlobalIonLibrariesModel parentModel,
      @Nullable IonLibrary library) {
    super(new IonLibraryEditModel(library));
    this.parentModel = parentModel;
    parentModel.editTabTitleProperty().bind(model.titleProperty());
    viewBuilder = new IonLibraryEditViewBuilder(parentModel, model, this::handleEvent);
  }

  public Tab createEditTab() {
    final Tab tab = FxTabs.newTab(model.titleProperty(), buildView());
    tab.setClosable(true);

    tab.setOnCloseRequest(e -> {
      if (model.isSameAsOriginal() || DialogLoggerUtil.showDialogYesNo("Discard unsaved changes?",
          "Do you want to discard unsaved changes and close the tab?")) {
        return; // close
      }
      // else consume event to not close
      e.consume();
    });
    return tab;
  }

  void handleEvent(@NotNull IonLibraryEditEvent event) {
    switch (event) {
      case Save(boolean saveAsCopy) -> {
        if (saveLibrary(saveAsCopy)) {
          parentModel.setLibraryEditActive(false);
        }
      }
      case AddIons(List<IonType> ions) -> addIons(ions);
      case ComposeAddLibraries(List<IonLibrary> libraries) -> composeAddLibraries(libraries);
      case CloseTab _ -> {
        if (model.isSameAsOriginal() || DialogLoggerUtil.showDialogYesNo(AlertType.WARNING,
            "Discard unsaved changes?",
            "There are unsaved changes for the current library. Discard and close the tab?")) {
          parentModel.setLibraryEditActive(false);
          model.setLibrary(null);
        }
      }
    }
  }

  public void composeAddLibraries(@Nullable List<IonLibrary> libraries) {
    if (libraries == null || libraries.isEmpty()) {
      return;
    }

    Set<IonType> all = new HashSet<>(model.getIonTypes());
    for (IonLibrary library : libraries) {
      all.addAll(library.ions());
    }
    model.getIonTypes().setAll(all);
  }

  public void addIons(List<IonType> ions) {
    if (ions == null || ions.isEmpty()) {
      return;
    }

    Set<IonType> all = new HashSet<>(model.getIonTypes());
    all.addAll(ions);
    model.getIonTypes().setAll(all);
  }

  @Override
  protected @NotNull FxViewBuilder<IonLibraryEditModel> getViewBuilder() {
    return viewBuilder;
  }

  /**
   * Save the library
   *
   * @param saveAsCopy save as a copy (requires new name) otherwise overwrite an original input
   *                   library.
   * @return true if saved
   */
  public boolean saveLibrary(boolean saveAsCopy) {
    // requires a change
    if (model.isSameAsOriginal()) {
      return true;
    }
    // cannot overwrite name of internal libs
    final String newName = model.getName().trim();
    if (IonLibraries.isInternalLibrary(newName)) {
      return false;
    }

    // check if name was changed
    final IonLibrary original = model.getLibrary();
    final boolean keepsOriginalName = original != null && original.name().equals(newName);
    if (saveAsCopy && keepsOriginalName) {
      // name was never changed - only needed for save copy
      DialogLoggerUtil.showErrorNotification("Requires name change to save a copy!",
          "Please change the name before saving a copy.");
      return false;
    }

    // on save overwrite original library always ask if user is sure
    if (!saveAsCopy && original != null && !DialogLoggerUtil.showDialogYesNo(
        "Overwrite original library named " + original.name(),
        "Do you want to overwrite the original library?")) {
      return false;
    }

    synchronized (parentModel.getLibraries()) {
      final boolean nameUnique = keepsOriginalName || isLibraryNameUnique(newName);

      // name exists already? ask overwrite
      if (nameUnique || DialogLoggerUtil.showDialogYesNo("Overwrite library named " + newName,
          "A library exists with this name, do you want to overwrite it?")) {

        // overwrite: keep the original identity/origin so references upstream stay valid.
        // save-as-copy (or brand-new library): fresh id + local origin.
        final List<IonType> ions = List.copyOf(model.getIonTypes());
        final UnmodifiableIonLibrary newLibrary = (!saveAsCopy && original != null) //
            ? new UnmodifiableIonLibrary(original.id(), original.origin(), newName, ions) //
            : new UnmodifiableIonLibrary(newName, ions);

        if (!saveAsCopy) {
          parentModel.getLibraries().remove(original);
        }
        if (!nameUnique) {
          parentModel.getLibraries().removeIf(l -> l.name().equals(newName));
        }
        parentModel.getLibraries().add(newLibrary);
        model.setLibrary(newLibrary);
        // trigger model to global update directly
        parentModel.getEventHandler().accept(new ApplyModelChangesToGlobalService());
        return true;
      }
    }
    return false;
  }

  public IonLibraryEditModel getModel() {
    return model;
  }

  /**
   * @return true if no library carries this name
   */
  private boolean isLibraryNameUnique(String newName) {
    final List<IonLibrary> allLibraries = List.copyOf(parentModel.getLibraries());
    return allLibraries.stream().noneMatch(l -> l.name().equals(newName));
  }
}
