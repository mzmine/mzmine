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
import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_SPACE;
import static io.github.mzmine.javafx.components.util.FxLayout.newBorderPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;
import static io.github.mzmine.javafx.components.util.FxLayout.newVBox;
import static javafx.geometry.Insets.EMPTY;

import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.AddIons;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.ChangeState;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.ComposeAddLibraries;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.Save;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditModel.EditState;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.components.util.FxLayout.GridColumnGrow;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

/**
 * Opens a new pane to edit a single ion library in a new Tab
 */
class IonLibraryEditViewBuilder extends FxViewBuilder<IonLibraryEditModel> {

  @NotNull
  private final GlobalIonLibrariesModel parentModel;
  private final Consumer<IonLibraryEditEvent> eventHandler;

  IonLibraryEditViewBuilder(@NotNull GlobalIonLibrariesModel parentModel,
      @NotNull IonLibraryEditModel editModel, Consumer<IonLibraryEditEvent> eventHandler) {
    super(editModel);
    this.parentModel = parentModel;
    this.eventHandler = eventHandler;
  }

  @Override
  public Region build() {

    final IonTypeListView typesList = new IonTypeListView(model.getIonTypes());

// add the top menu to the parent layout to have it flow over left and center pane
    final Node topMenu = typesList.removeTopMenu();
    final BorderPane mainPane = newBorderPane(createCenterControls());
    mainPane.setTop(newVBox(newBoldTitle(model.titleProperty()), topMenu));
    mainPane.setLeft(typesList);
    return mainPane;
  }

  private Node createCenterControls() {
    // main options to add more ions
    final Pane mainEditControls = createEditButtonsPane();

    // specialized panes depending on state
    final Pane addGlobalIons = createAddGlobalIonsPane();
    final Pane composeLibrariesPane = createComposeLibrariesPane();

    final BorderPane centerControls = newBorderPane(mainEditControls);
    // name and save buttons always there
    centerControls.setTop(createNameSavePane());
    // depending on state other panes are shown
    centerControls.centerProperty().bind(model.editStateProperty().map(state -> switch (state) {
      case MAIN -> mainEditControls;
      case ADD_FROM_GLOBAL_IONS -> addGlobalIons;
      case COMPOSE_LIBRARIES -> composeLibrariesPane;
    }).orElse(null));

    return centerControls;
  }

  private Pane createAddGlobalIonsPane() {
    final IonTypeListView listView = new IonTypeListView(parentModel.getIonTypes(), false);
    final MultipleSelectionModel<IonType> selection = listView.getListView().getSelectionModel();
    final BooleanBinding noneSelected = selection.selectedItemProperty().isNull();

    final VBox centerButtons = newVBox(Pos.TOP_LEFT, //
        FxButtons.createDisabledButton("Add selected ions",
            "Add selected ions (right) to the ion library (left)", noneSelected, () -> {
              eventHandler.accept(new AddIons(List.copyOf(selection.getSelectedItems())));
              selection.clearSelection();
            }), //
        FxButtons.createButton("Page back",
            "Return the main page with more options how to add ions.",
            () -> eventHandler.accept(new ChangeState(EditState.MAIN))) //
    );

    final GridPane grid = FxLayout.applyGrid2Col(new GridPane(), GridColumnGrow.RIGHT, EMPTY,
        DEFAULT_SPACE,
        // first row
        null, listView.removeTopMenu(),
        // second row
        centerButtons, listView);

    // grow last row
    grid.getRowConstraints().add(FxLayout.newFillHeightRow());
    return grid;
  }

  private Pane createComposeLibrariesPane() {
    final IonLibraryListView listView = IonLibraryListView.createImmutableMultiSelect(
        parentModel.getLibraries());
    final MultipleSelectionModel<IonLibrary> selection = listView.getListView().getSelectionModel();
    final BooleanBinding noneSelected = selection.selectedItemProperty().isNull();
    final VBox centerButtons = newVBox(Pos.TOP_LEFT, //
        FxButtons.createDisabledButton("Add whole libraries",
            "Adds all ions of the selected libraries (right) to the ion library (left)",
            noneSelected, () -> {
              eventHandler.accept(
                  new ComposeAddLibraries(List.copyOf(selection.getSelectedItems())));
              selection.clearSelection();
            }), //
        FxButtons.createButton("Page back",
            "Return the main page with more options how to add ions.",
            () -> eventHandler.accept(new ChangeState(EditState.MAIN))) //
    );

    final GridPane grid = FxLayout.applyGrid2Col(new GridPane(), GridColumnGrow.RIGHT, EMPTY,
        DEFAULT_SPACE,
        // first row
        null, listView.removeTopMenu(),
        // second row
        centerButtons, listView);

    // grow last row
    grid.getRowConstraints().add(FxLayout.newFillHeightRow());
    return grid;
  }

  private @NotNull HBox createNameSavePane() {
    // more space to bottom for clearer separation
    return newHBox(new Insets(0, 0, DEFAULT_SPACE * 3, 0), FxLabels.newBoldLabel("Name:"),
        FxTextFields.newAutoGrowTextField(model.nameProperty(), "The current library name"),
        FxButtons.createDisabledButton("Save", "Save and overwrite the ion library",
            model.sameAsOriginalProperty(), () -> eventHandler.accept(new Save(false))),
        FxButtons.createDisabledButton("Save copy",
            "Create a copy (requires a new name) and saves this copy. The original list stays unchanged.",
            model.sameAsOriginalProperty(), () -> eventHandler.accept(new Save(true))));
  }

  private @NotNull VBox createEditButtonsPane() {
    return newVBox(Pos.TOP_LEFT, EMPTY, FxButtons.createButton("Add from global ions",
            "Add ions from the global ion list, which contains all ion definitions and harmonizes the mass differences and names.",
            _ -> eventHandler.accept(new ChangeState(EditState.ADD_FROM_GLOBAL_IONS))),
        FxButtons.createButton("Compose libraries",
            "Add all ions from other libraries to compose libraries.",
            _ -> eventHandler.accept(new ChangeState(EditState.COMPOSE_LIBRARIES))));
  }

}
