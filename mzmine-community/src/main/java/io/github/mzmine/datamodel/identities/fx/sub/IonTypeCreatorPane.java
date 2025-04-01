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

import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonTypeParser;
import io.github.mzmine.javafx.components.factories.FxButtons;
import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newTextField;
import static io.github.mzmine.javafx.components.util.FxLayout.gridRow;
import static io.github.mzmine.javafx.components.util.FxLayout.newBorderPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newGrid2Col;
import static io.github.mzmine.javafx.components.util.FxTabs.newTab;
import io.github.mzmine.javafx.properties.PropertyUtils;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public class IonTypeCreatorPane extends BorderPane {

  public enum IonTypeDefinition {
    STRING, COMBINED
  }

  private final ListView<IonType> typesListView;
  private final StringProperty parsedIonTypeString = new SimpleStringProperty();
  private final ObjectProperty<IonType> parsedIonType = new SimpleObjectProperty<>();

  public IonTypeCreatorPane(ObservableList<IonType> types) {

    typesListView = new ListView<>(types);
    var leftListPane = newBorderPane(typesListView);
    setLeft(leftListPane);
    setTop(newBoldTitle("Global ion types: Adducts, in source fragments, and clusters"));
    setCenter(new TabPane( //
        newTab("Specify format", createIonTypeByStringPane()), //
        newTab("Combine parts", createIonTypeByStringPane()) //
    ));

    // on any change - update part
    PauseTransition updateDelay = new PauseTransition(Duration.millis(500));
    updateDelay.setOnFinished(_ -> updateCurrentType());
    PropertyUtils.onChange(updateDelay::playFromStart, parsedIonTypeString);
    // 
  }

  private Node createIonTypeByStringPane() {
    return newBorderPane(newGrid2Col( //
        newLabel("Ion type:"),
        newTextField(10, parsedIonTypeString, "Format: [M-H2O+2H]+2 or M+ACN+H",
            "Enter ion types like adducts, in source fragments, and clusters"), //
        gridRow(FxButtons.createDisabledButton("Add", "Add new ion type based on formatted entry",
            parsedIonType.isNull(), () -> addIonType(parsedIonType.get())))
        //
    ));
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
    ObservableList<IonType> items = typesListView.getItems();
    if (!items.contains(type)) {
      items.add(type);
    }
  }
}
