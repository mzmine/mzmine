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

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.util.FxLayout.newBorderPane;

import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.CreateNewLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.EditSelectedLibrary;
import io.github.mzmine.javafx.components.factories.FxSplitPanes;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

/**
 * Ion libraries pane just for viewing and then selecting which one to edit or to create a new one.
 *
 */
class IonLibrariesManagePane extends BorderPane {

  private final ReadOnlyObjectProperty<IonLibrary> selectedIonLibrary;
  // ion types of the selected library
  private final ListProperty<IonType> ionTypes = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  IonLibrariesManagePane(@NotNull GlobalIonLibrariesModel model,
      @NotNull Consumer<GlobalIonLibrariesEvent> eventHandler) {
    this(model.getLibraries(), true, eventHandler);
  }

  IonLibrariesManagePane(@NotNull ObservableList<IonLibrary> libraries, boolean allowRemoveItem,
      @NotNull Consumer<GlobalIonLibrariesEvent> eventHandler) {
    final IonLibraryListView libraryList = new IonLibraryListView(libraries, allowRemoveItem, true,
        true);
    libraryList.setCreateNewAction(() -> eventHandler.accept(new CreateNewLibrary()));
    libraryList.setEditSelectedAction(
        library -> eventHandler.accept(new EditSelectedLibrary(library)));

    final IonTypeListView typesList = new IonTypeListView(ionTypes, false);

    selectedIonLibrary = libraryList.getListView().getSelectionModel().selectedItemProperty();
    final ObservableValue<@NotNull String> selectedLibraryName = selectedIonLibrary.map(
        lib -> "Ions types in: " + lib.name()).orElse("");

    selectedIonLibrary.subscribe((_, lib) -> ionTypes.setAll(lib != null ? lib.ions() : List.of()));

    setCenter(FxSplitPanes.newSplitPane( //
        newBorderPane().defaultPadding() //
            .top(newBoldTitle("Ion libraries")) //
            .center(libraryList).build(), //
        newBorderPane().defaultPadding() //
            .top(newBoldTitle(selectedLibraryName)) //
            .center(typesList).build() //
    ));
  }

}
