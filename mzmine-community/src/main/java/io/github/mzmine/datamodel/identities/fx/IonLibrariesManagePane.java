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

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.util.FxLayout.newBorderPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;

import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.CreateNewLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.EditSelectedLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;

/**
 * Ion libraries pane just for viewing and then selecting which one to edit or to create a new one.
 *
 */
class IonLibrariesManagePane extends BorderPane {

  // ion types of the selected library
  private final ListProperty<IonType> ionTypes = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  IonLibrariesManagePane(@NotNull GlobalIonLibrariesModel model,
      @NotNull Consumer<GlobalIonLibrariesEvent> eventHandler) {
    // create main layout
    final IonLibraryListView libraryList = new IonLibraryListView(model.librariesProperty(), true,
        true, true);

    libraryList.setCreateNewAction(() -> eventHandler.accept(new CreateNewLibrary()));
    libraryList.setEditSelectedAction(
        library -> eventHandler.accept(new EditSelectedLibrary(library)));

    final IonTypeListView typesList = new IonTypeListView(ionTypes, false);

    final var selectedIonLibrary = libraryList.selectedIonLibraryProperty();
    final ObservableValue<@NotNull String> selectedLibraryName = selectedIonLibrary.map(
            lib -> "Ion types in library: " + lib.name())
        .orElse("Select library on left to view ion content");

    selectedIonLibrary.subscribe((_, lib) -> ionTypes.setAll(lib != null ? lib.ions() : List.of()));

    final var titlePane = newHBox(Insets.EMPTY,
        FxIconUtil.newIconButtonOpenUrl(FxIcons.QUESTION_CIRCLE, FxIconUtil.DEFAULT_LARGE_ICON_SIZE,
            """
                Manage, copy, and create new ion libraries. Select an ion library to see its ion types content on the right.
                mzmine default libraries cannot be deleted or changed.
                Click to open the documentation.""",
            "https://mzmine.github.io/mzmine_documentation/ions/ions.html#define-libraries"),
        newBoldTitle("Available ion libraries"));

    final BorderPane librarySelectionLeft = newBorderPane().defaultPadding() //
        .top(titlePane) //
        .center(libraryList).build();

    Pane libraryDetailPane = newBorderPane().defaultPadding() //
        .top(newBoldTitle(selectedLibraryName)) //
        .center(typesList).build(); //
    // only show details once selected
    typesList.visibleProperty().bind(selectedIonLibrary.isNotNull());

    setCenter(libraryDetailPane);
    setLeft(librarySelectionLeft);
  }

}
