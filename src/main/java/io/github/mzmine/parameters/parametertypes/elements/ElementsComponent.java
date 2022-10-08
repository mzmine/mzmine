/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.elements;

import io.github.mzmine.util.dialogs.PeriodicTableDialog;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.openscience.cdk.Element;

public class ElementsComponent extends HBox {

  private final PeriodicTableDialog periodicTableDialog = new PeriodicTableDialog(true);
  private final ObservableList<Element> selectedElements;

  public ElementsComponent(List<Element> defaultElements) {

    Button setupBtn = new Button("Setup");
    Text elementsText = new Text(defaultElements.stream()
        .map(Element::getSymbol)
        .map(Object::toString)
        .collect(Collectors.joining(", ")));

    // Bind selectedElements with selected elements of periodicTableDialog
    selectedElements = periodicTableDialog.getSelectedElements();
    selectedElements.addListener((ListChangeListener<Element>) c
        -> elementsText.setText(c.getList().stream()
        .map(Element::getSymbol)
        .map(Object::toString)
        .collect(Collectors.joining(", "))));

    super.getChildren().addAll(elementsText, setupBtn);
    super.setSpacing(10d);
    super.setAlignment(Pos.CENTER_LEFT);

    setupBtn.setOnAction(e -> periodicTableDialog.showAndWait());
  }

  public void setValue(List<Element> value) {
    this.periodicTableDialog.setSelectedElements(value);
  }

  public List<Element> getValue() {
    return this.selectedElements;
  }
}
