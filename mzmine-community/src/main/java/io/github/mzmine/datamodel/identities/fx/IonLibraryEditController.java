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

import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.SimpleIonLibrary;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.AddIons;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.ChangeState;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.ComposeAddLibraries;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.Save;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditModel.EditState;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    viewBuilder = new IonLibraryEditViewBuilder(parentModel, model, this::handleEvent);
  }

  public Tab showTab() {
    final SimpleTab tab = new SimpleTab("", buildView());
    tab.titleProperty().bind(model.titleProperty());
    MZmineCore.getDesktop().addTab(tab);

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

  private void handleEvent(@NotNull IonLibraryEditEvent event) {
    switch (event) {
      case Save(boolean saveAsCopy) -> saveLibrary(saveAsCopy);
      case ChangeState(EditState state) -> model.setEditState(state);
      case AddIons(List<IonType> ions) -> addIons(ions);
      case ComposeAddLibraries(List<IonLibrary> libraries) -> composeAddLibraries(libraries);
    }
  }

  public void composeAddLibraries(@Nullable List<IonLibrary> libraries) {
    if (libraries == null || libraries.isEmpty()) {
      return;
    }

    Set<IonType> all = new HashSet<>(model.getIonTypes());
    for (IonLibrary library : libraries) {
      all.addAll(library.getIons());
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
   * @param saveAsCopy save as a copy (requires new name) otherwise overwrite original input
   *                   library.
   */
  public void saveLibrary(boolean saveAsCopy) {
    // requires a change
    if (model.isSameAsOriginal()) {
      return;
    }

    // check if name was changed
    final IonLibrary original = model.getLibrary();
    final String newName = model.getName();
    final boolean keepsOriginalName = original != null && original.getName().equals(newName);
    if (saveAsCopy && keepsOriginalName) {
      // name was never changed - only needed for save copy
      DialogLoggerUtil.showErrorNotification("Requires name change to save copy!",
          "Please change the name before saving a copy.");
      return;
    }

    // on save overwrite original library always ask if user is sure
    if (!saveAsCopy && original != null && !DialogLoggerUtil.showDialogYesNo(
        "Overwrite original library named " + original.getName(),
        "Do you want to overwrite the original library?")) {
      return;
    }

    synchronized (parentModel.getLibraries()) {
      final boolean nameUnique = keepsOriginalName || isLibraryNameUnique(newName);

      // name exists already? ask overwrite
      if (nameUnique || DialogLoggerUtil.showDialogYesNo("Overwrite library named " + newName,
          "A library exists with this name, do you want to overwrite it?")) {

        final SimpleIonLibrary newLibrary = new SimpleIonLibrary(newName,
            List.copyOf(model.getIonTypes()));

        if (!saveAsCopy) {
          parentModel.getLibraries().remove(original);
        }
        if (!nameUnique) {
          parentModel.getLibraries().removeIf(l -> l.getName().equals(newName));
        }
        parentModel.getLibraries().add(newLibrary);
        model.setLibrary(newLibrary);
      }
    }
  }

  /**
   * @return true if no library carries this name
   */
  private boolean isLibraryNameUnique(String newName) {
    final List<IonLibrary> allLibraries = List.copyOf(parentModel.getLibraries());
    return allLibraries.stream().noneMatch(l -> l.getName().equals(newName));
  }
}
