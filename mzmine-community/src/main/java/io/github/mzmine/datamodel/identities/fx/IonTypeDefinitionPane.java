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

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newTextField;
import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_PADDING_INSETS;
import static io.github.mzmine.javafx.components.util.FxLayout.gridRow;
import static io.github.mzmine.javafx.components.util.FxLayout.newGrid2Col;
import static io.github.mzmine.javafx.components.util.FxLayout.newVBox;

import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.iontype.IonPartDefinition;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonType.IonTypeStringFlavor;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.util.StringUtils;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

class IonTypeDefinitionPane extends BorderPane {

  private final IonTypeListView listView;
  private final ObservableList<IonType> types;
  private final ObservableList<IonPartDefinition> partsDefinitions;

  public enum IonTypeDefinition {
    STRING, COMBINED
  }

  private final StringProperty parsedIonTypeString = new SimpleStringProperty();
  private final ObjectProperty<IonType> parsedIonType = new SimpleObjectProperty<>();
  private final ListProperty<IonPart> unknownParts = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  public IonTypeDefinitionPane(IonTypeListView listView, ObservableList<IonType> types,
      ObservableList<IonPartDefinition> partsDefinitions) {
    this.listView = listView;
    this.types = types;
    this.partsDefinitions = partsDefinitions;
    setPadding(DEFAULT_PADDING_INSETS);

    setTop(createIonTypeByStringPane());
    setCenter(createUnknownPartDefinitionPane());

    // option to change it to a tabpane in case other ion type definitions are planned
//        new TabPane( //
//        newTab("Specify format", createIonTypeByStringPane()), //
//        newTab("Combine parts", createIonTypeByStringPane()) //
//    ));

    // on any change - update part
    PauseTransition updateDelay = new PauseTransition(Duration.millis(250));
    updateDelay.setOnFinished(_ -> updateCurrentType());
    PropertyUtils.onChange(updateDelay::playFromStart, partsDefinitions, parsedIonTypeString);

    // find unknowns
    parsedIonType.subscribe((nv) -> {
      if (nv == null) {
        unknownParts.clear();
      } else {
        unknownParts.setAll(nv.stream().filter(IonPart::isUndefinedMass).toList());
      }
    });
  }

  private Node createIonTypeByStringPane() {
    var lbParsingResult = newBoldLabel(
        parsedIonType.map(ion -> ion.toString(IonTypeStringFlavor.FULL_WITH_MASS))
            .orElse("Cannot parse input"));

    final TextField inputText = newTextField(10, parsedIonTypeString,
        "Format: [2M-H2O+2H]+2 or with charge M+(Cu+2)", """
            Enter ion types like adducts, in source fragments, and clusters.
            Best format uses brackets and charge state: [M+2H]+2 but both are optional.
            Define charge of individual parts in (): M+(Cu+2)-H equals [M+(Cu+2)-H]+""");

    ionParsingValidation(inputText);

    //
    final Button btnAdd = FxButtons.createDisabledButton("Add", FxIcons.ADD,
        "Add new ion type based on formatted entry",
        parsedIonType.isNull().or(Bindings.isNotEmpty(unknownParts)),
        () -> addIonType(parsedIonType.get()));

    var lbUnknown = newBoldLabel(unknownParts.map(parts -> parts.isEmpty() ? ""
        : "%d parts unknown: %s".formatted(parts.size(), StringUtils.join(parts, ", ",
            part -> part.toString(IonPartStringFlavor.SIMPLE_NO_CHARGE)))));

    return newGrid2Col( //
        newLabel("Ion type:"), inputText, //
        newLabel("Result:"), lbParsingResult, //
        gridRow(lbUnknown), //
        gridRow(btnAdd) //
    );
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

  private Node createUnknownPartDefinitionPane() {
    final ObservableValue<IonPart> firstUnknown = unknownParts.map(
        parts -> parts.isEmpty() ? null : parts.getFirst());

    final StringProperty firstUnknownTitle = new SimpleStringProperty();

    final IonPartDefinitionPane partDefPane = new IonPartDefinitionPane(this::addPartDefinition,
        false);

    final Pane unknownMainPane = newVBox(Pos.TOP_LEFT, new Insets(20, 0, 10, 25), //
        newBoldTitle(firstUnknownTitle), //
        FxLabels.addIconGraphic(FxIcons.INFO_CIRCLE,
            newLabel("Define the charge and either the formula or mass.")), //
        partDefPane);

    firstUnknown.subscribe(unknown -> {
      unknownMainPane.setVisible(unknown != null);
      firstUnknownTitle.set(unknown == null ? null
          : "Define unknown part named: " + unknown.toString(IonPartStringFlavor.SIMPLE_NO_CHARGE));
      if (unknown != null) {
        partDefPane.setNameOnly(unknown.name());
      }
    });
    return unknownMainPane;
  }

  private void addPartDefinition(IonPartDefinition partDef) {
    if (partDef == null || partsDefinitions.contains(partDef)) {
      return;
    }
    // add definition here, change listener will trigger and hide the unknown definition pane
    // or will show the next unknown
    GlobalIonLibraryService.getGlobalLibrary().addPartDefinition(partDef);
    partsDefinitions.add(partDef);
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
    if (!types.contains(type)) {
      types.add(type);
      listView.getListView().getSelectionModel().select(type);
      listView.getListView().scrollTo(type);
    }
  }
}
