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

package io.github.mzmine.datamodel.identities.fx.sub;

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newTextField;
import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_PADDING_INSETS;
import static io.github.mzmine.javafx.components.util.FxLayout.gridRow;
import static io.github.mzmine.javafx.components.util.FxLayout.newBorderPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newGrid2Col;

import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonType.IonTypeStringFlavor;
import io.github.mzmine.datamodel.identities.IonTypeParser;
import io.github.mzmine.datamodel.identities.IonTypeSorting;
import io.github.mzmine.javafx.components.FilterableListView;
import io.github.mzmine.javafx.components.FilterableListView.MenuControls;
import io.github.mzmine.javafx.components.MappingListCell;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxListViews;
import io.github.mzmine.javafx.components.util.FxLayout.Position;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.util.StringUtils;
import java.util.List;
import java.util.Optional;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class IonTypeCreatorPane extends BorderPane {

  public enum IonTypeDefinition {
    STRING, COMBINED
  }

  private final FilterableListView<IonType> typesListView;
  private final StringProperty parsedIonTypeString = new SimpleStringProperty();
  private final ObjectProperty<IonType> parsedIonType = new SimpleObjectProperty<>();
  private final ListProperty<IonPart> unknownParts = new SimpleListProperty<>(
      FXCollections.observableArrayList());


  // additional properties for the list view
  private final ObjectProperty<IonTypeSorting> listSorting = new SimpleObjectProperty<>(
      IonTypeSorting.getIonTypeDefault());

  public IonTypeCreatorPane(ObservableList<IonType> types) {
    setPadding(DEFAULT_PADDING_INSETS);

    setTop(newBoldTitle("Ion types: Global list"));

    typesListView = createIonTypeListView(types);
    typesListView.setCenter(createIonTypeByStringPane());

    // option to change it to a tabpane in case other ion type definitions are planned
//        new TabPane( //
//        newTab("Specify format", createIonTypeByStringPane()), //
//        newTab("Combine parts", createIonTypeByStringPane()) //
//    ));
    typesListView.setLeft(typesListView.getListView());
    setCenter(typesListView);

    // on any change - update part
    PauseTransition updateDelay = new PauseTransition(Duration.millis(500));
    updateDelay.setOnFinished(_ -> updateCurrentType());
    PropertyUtils.onChange(updateDelay::playFromStart, parsedIonTypeString);

    // find unknowns
    parsedIonType.subscribe((nv) -> {
      if (nv == null) {
        unknownParts.clear();
      } else {
        unknownParts.setAll(nv.stream().filter(IonPart::isUnknown).toList());
      }
    });
  }

  private @NotNull FilterableListView<IonType> createIonTypeListView(
      final ObservableList<IonType> types) {
    // create a list view with addtional controls for sorting and filtering
    // sorting:
    final HBox sortingCombo = FxComboBox.createLabeledComboBox("Sort by:",
        FXCollections.observableList(List.of(IonTypeSorting.values())), listSorting);

    final List<Node> additionalNodes = List.of(sortingCombo);
    final List<MenuControls> stdButtons = List.of(MenuControls.CLEAR_BTN, MenuControls.REMOVE_BTN);

    // create the list view
    final FilterableListView<IonType> typeListView = FxListViews.newFilterableListView(types, true,
        SelectionMode.MULTIPLE, Position.TOP, Pos.CENTER_LEFT, stdButtons, additionalNodes);

    typeListView.getListView().setCellFactory(
        param -> new MappingListCell<>(ion -> ion.toString(IonTypeStringFlavor.FULL_WITH_MASS)));

    // set comparator and filter
    listSorting.subscribe(nv -> typeListView.sortingComparatorProperty().set(nv.getComparator()));

    // apply filter now:
    return typeListView;
  }

  private Node createIonTypeByStringPane() {

    var lbParsingResult = newBoldLabel(
        parsedIonType.map(ion -> ion.toString(IonTypeStringFlavor.FULL_WITH_MASS))
            .orElse("Cannot parse input"));

    final TextField inputText = newTextField(10, parsedIonTypeString,
        "Format: [2M-H2O+2H]+2 or M+ACN+H",
        "Enter ion types like adducts, in source fragments, and clusters");

    ionParsingValidation(inputText);

    //
    final Button btnAdd = FxButtons.createDisabledButton("Add",
        "Add new ion type based on formatted entry",
        parsedIonType.isNull().or(Bindings.isNotEmpty(unknownParts)),
        () -> addIonType(parsedIonType.get()));

    // handle unknowns
    final ObservableValue<String> firstUnknown = unknownParts.map(
        parts -> parts.isEmpty() ? "All known"
            : "Define unknown: " + parts.getFirst().toString(IonPartStringFlavor.SIMPLE_NO_CHARGE));

    final Button btnDefineUnknown = FxButtons.createDisabledButton("Define unknown",
        "At least one entered ion building block is unknown, maybe a typo or an unknown name. Provide formula or mass and charge in 'Define building blocks'.",
        Bindings.isEmpty(unknownParts), this::defineFirstUnknownPart);
    btnDefineUnknown.textProperty().bind(firstUnknown);

    var lbUnknown = newBoldLabel(unknownParts.map(parts -> parts.isEmpty() ? ""
        : "%d parts unknown: %s".formatted(parts.size(), StringUtils.join(parts, ", ",
            part -> part.toString(IonPartStringFlavor.SIMPLE_NO_CHARGE)))));

    return newBorderPane(newGrid2Col( //
        newLabel("Ion type:"), inputText, //
        newLabel("Result:"), lbParsingResult, //
        gridRow(lbUnknown), //
        gridRow(btnAdd, btnDefineUnknown) //
    ));
  }

  private void ionParsingValidation(TextField inputText) {
    StringBinding errorBinding = Bindings.createStringBinding(() -> {
      IonType value = parsedIonType.getValue();
      if (value == null && StringUtils.hasValue(inputText.getText())) {
        return "Cannot parse ion type for %s. Input correct format, e.g., [2M-H2O+2H]+2 or M+ACN+H".formatted(
            parsedIonTypeString.getValue());
      }
      return null; // no error
    }, parsedIonType, inputText.textProperty());

    FxValidation.registerErrorValidator(inputText, errorBinding);
  }

  private void defineFirstUnknownPart() {
    final Optional<IonPart> first = unknownParts.stream().findFirst();
    if (first.isPresent()) {
      // TODO
    }
  }

  private void updateCurrentType() {
    String ionStr = parsedIonTypeString.get();
    IonType ion = IonTypeParser.parse(ionStr);
    parsedIonType.set(ion);
  }

  private void addIonType(final IonType type) {
    if (type == null) {
      return;
    }
    ObservableList<IonType> items = typesListView.getOriginalItems();
    if (!items.contains(type)) {
      items.add(type);
      typesListView.getListView().getSelectionModel().select(type);
      typesListView.getListView().scrollTo(type);
    }
  }
}
