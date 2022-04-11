/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
