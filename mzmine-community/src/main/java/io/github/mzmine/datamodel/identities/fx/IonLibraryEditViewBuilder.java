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
import io.github.mzmine.datamodel.identities.IonPartDefinition;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.AddIons;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.ComposeAddLibraries;
import io.github.mzmine.datamodel.identities.fx.IonLibraryEditEvent.Save;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.components.util.FxLayout.GridColumnGrow;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.validation.FxValidation;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
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
    final BorderPane mainPane = newBorderPane(
        createCenterControlsAccordion(typesList, model.getIonTypes(),
            parentModel.getPartsDefinitions()));
    mainPane.setTop(newVBox(newBoldTitle(model.titleProperty()), topMenu));
    mainPane.setLeft(typesList);
    return mainPane;
  }

  private Node createCenterControlsAccordion(IonTypeListView typesList,
      ObservableList<IonType> ionTypes, ObservableList<IonPartDefinition> partsDefinitions) {

    var accordion = FxLayout.newAccordion(false //
        , new TitledPane("Add from global ions", createAddGlobalIonsPane()) //
        , new TitledPane("Define new ion type",
            createDefineIonTypePane(typesList, ionTypes, partsDefinitions)) //
        , new TitledPane("Compose/add whole libraries", createComposeLibrariesPane()) //
    );

    final BorderPane centerControls = newBorderPane(accordion);
    centerControls.setTop(createNameSavePane());
    return centerControls;
  }

  private Node createDefineIonTypePane(IonTypeListView typesList, ObservableList<IonType> ionTypes,
      ObservableList<IonPartDefinition> partsDefinitions) {
    return new IonTypeDefinitionPane(typesList, ionTypes, partsDefinitions);
  }

  private Pane createAddGlobalIonsPane() {
    final IonTypeListView listView = new IonTypeListView(parentModel.getIonTypes(), false);
    final MultipleSelectionModel<IonType> selection = listView.getListView().getSelectionModel();
    final BooleanBinding noneSelected = selection.selectedItemProperty().isNull();

    final VBox centerButtons = newVBox(Pos.TOP_LEFT, //
        FxButtons.createDisabledButton("Add selected ions", FxIcons.ADD,
            "Add selected ions (right) to the ion library (left)", noneSelected, () -> {
              eventHandler.accept(new AddIons(List.copyOf(selection.getSelectedItems())));
              selection.clearSelection();
            }) //
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
        FxButtons.createDisabledButton("Add whole libraries", FxIcons.ADD,
            "Adds all ions of the selected libraries (right) to the ion library that is being edited (left)",
            noneSelected, () -> {
              eventHandler.accept(
                  new ComposeAddLibraries(List.copyOf(selection.getSelectedItems())));
              selection.clearSelection();
            }) //
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
    final BooleanBinding canSave = model.sameAsOriginalProperty()
        .or(model.nameRestrictedProperty());

    final TextField nameField = FxTextFields.newAutoGrowTextField(model.nameProperty(),
        "The current library name");
    FxValidation.registerErrorValidator(nameField, model.nameIssueProperty());

    return newHBox(new Insets(0, 0, DEFAULT_SPACE * 3, 0), FxLabels.newBoldLabel("Name:"),
        nameField,
        FxButtons.createDisabledButton("Save", FxIcons.SAVE, "Save and overwrite the ion library",
            canSave, () -> eventHandler.accept(new Save(false))),
        FxButtons.createDisabledButton("Save copy", FxIcons.PLUS_CIRCLE,
            "Create a copy (requires a new name) and saves this copy. The original list stays unchanged.",
            canSave, () -> eventHandler.accept(new Save(true))));
  }

}
