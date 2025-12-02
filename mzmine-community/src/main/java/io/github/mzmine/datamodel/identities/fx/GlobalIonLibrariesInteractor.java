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
import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonPartDefinition;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ApplyModelChangesToGlobalService;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.CreateNewLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.DiscardModelChanges;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.EditSelectedLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ReloadGlobalServiceChanges;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryDTO;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxInteractor;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Updates in the {@link GlobalIonLibraryService}
 */
class GlobalIonLibrariesInteractor extends FxInteractor<GlobalIonLibrariesModel> {


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


  public void handleEvent(@NotNull GlobalIonLibrariesEvent event) {
    switch (event) {
      case DiscardModelChanges _ -> askDiscardModelChanges();
      case ApplyModelChangesToGlobalService _ -> applyToGlobalIons();
      case CreateNewLibrary _ -> createNewLibraryInTab();
      case EditSelectedLibrary(IonLibrary library) -> editLibraryInTab(library);
      case ReloadGlobalServiceChanges _ -> updateModel();
    }
  }

  private void askDiscardModelChanges() {
    if (DialogLoggerUtil.showDialogYesNo("Discard local changes to ion?",
        "Discarding the local changes will reset the ion libraries.")) {
      updateModel();
    }
  }

  /**
   * Push changes to {@link GlobalIonLibraryService}
   */
  public void applyToGlobalIons() {
    final List<IonLibrary> libraries = List.copyOf(model.getLibraries());
    final List<IonType> types = List.copyOf(model.getIonTypes());
    final List<IonPart> parts = List.copyOf(model.getParts());
    final List<IonPartDefinition> partDefinitions = List.copyOf(model.getPartsDefinitions());

    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    final int currentVersion = global.getVersion();
    final int retrivalVersion = model.getRetrivalVersion();
    // TODO add check that if the versions mismatch than we need to apply safer merging of states
    global.applyUpdates(libraries, types, parts, partDefinitions);
    // there is always a delayed change due to the ion libraries that are changed on fx thread delayed
    model.setRetrivalVersion(global.getVersion() + 1);
    model.setLastModelUpdate(null);
  }

  /**
   * Pull ions from {@link GlobalIonLibraryService}
   */
  @Override
  public void updateModel() {
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    FxThread.runLater(() -> {
      final GlobalIonLibraryDTO current = global.getCurrentGlobalLibrary();
      model.setRetrivalVersion(current.version());
      model.setGlobalIonsVersion(current.version());
      model.librariesProperty().setAll(current.libraries());
      model.partsProperty().setAll(current.parts());
      model.ionTypesProperty().setAll(current.types());
      model.partsDefinitionsProperty().setAll(current.partDefinitions());
      model.setLastModelUpdate(null);
    });
  }

  public void createNewLibraryInTab() {
    new IonLibraryEditController(model).showTab();
  }

  public void editLibraryInTab(IonLibrary library) {
    if (library == null) {
      return;
    }
    new IonLibraryEditController(model, library).showTab();
  }
}
