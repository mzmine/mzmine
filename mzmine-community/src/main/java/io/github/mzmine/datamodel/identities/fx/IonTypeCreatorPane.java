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
import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_PADDING_INSETS;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;

import io.github.mzmine.datamodel.identities.iontype.IonPartDefinition;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class IonTypeCreatorPane extends BorderPane {

  public IonTypeCreatorPane(ObservableList<IonType> types,
      ObservableList<IonPartDefinition> partsDefinitions) {
    setPadding(DEFAULT_PADDING_INSETS);

    final var titlePane = newHBox(Insets.EMPTY,
        FxIconUtil.newIconButtonOpenUrl(FxIcons.QUESTION_CIRCLE, FxIconUtil.DEFAULT_LARGE_ICON_SIZE,
            """
                Define ion types, which combine ion building blocks and a multimer count.
                Click to open the documentation.""",
            "https://mzmine.github.io/mzmine_documentation/ions/ions.html#define-types"),
        newBoldTitle("All currently defined ion types"));

    IonTypeListView typesListView = new IonTypeListView(types);

    final IonTypeDefinitionPane typeDefPane = new IonTypeDefinitionPane(typesListView, types,
        partsDefinitions);

    setLeft(typesListView);
    // space is already enough
    setTop(new VBox(0, titlePane, typesListView.removeTopMenu()));
    setCenter(typeDefPane);
  }

}
