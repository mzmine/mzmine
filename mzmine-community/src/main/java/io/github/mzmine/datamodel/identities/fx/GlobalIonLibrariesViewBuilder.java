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

import static io.github.mzmine.javafx.components.factories.FxButtons.createButton;

import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ApplyModelChangesToGlobalService;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.DiscardModelChanges;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesEvent.ReloadGlobalServiceChanges;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.components.util.FxTabs;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.util.function.Consumer;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

class GlobalIonLibrariesViewBuilder extends FxViewBuilder<GlobalIonLibrariesModel> {

  private final Consumer<GlobalIonLibrariesEvent> eventHandler;

  public GlobalIonLibrariesViewBuilder(final GlobalIonLibrariesModel model,
      final Consumer<GlobalIonLibrariesEvent> eventHandler) {
    super(model);
    this.eventHandler = eventHandler;
  }


  @Override
  public Region build() {
    var tabPane = new TabPane(//
        FxTabs.newTab("Ion libraries", new IonLibrariesManagePane(model, eventHandler)),
        FxTabs.newTab("Define ion types", createIonTypesPane()),
        FxTabs.newTab("Define building blocks", createIonPartsPane()));

    var topNotificationsMenu = createNotificationsMenu();
    final BorderPane main = new BorderPane();
    main.setCenter(tabPane);
    main.setTop(topNotificationsMenu);
    return main;
  }

  private Node createNotificationsMenu() {
    final BooleanBinding modelChangedProperty = model.lastModelUpdateProperty().isNotNull();
    // options to apply changes of the fxmodel to the global ions
    final HBox modelChangePane = FxLayout.newHBox(Insets.EMPTY,
        FxIconUtil.getFontIcon(FxIcons.INFO_CIRCLE),
        FxLabels.newBoldLabel("Ion definitions were changed in tab, apply?"),
        createButton("Apply", FxIcons.CHECK_CIRCLE,
            "Apply changes made in this tab to the global ion libraries which are used for ion parsing and matching.",
            () -> eventHandler.accept(new ApplyModelChangesToGlobalService())), //
        createButton("Discard", FxIcons.X_CIRCLE,
            "Discard changes made in this tab and reset lists.",
            () -> eventHandler.accept(new DiscardModelChanges())) //
    );
    modelChangePane.visibleProperty().bind(modelChangedProperty);

    // Global ions have changed
    final HBox globalChangePane = FxLayout.newHBox(Insets.EMPTY,
        FxIconUtil.getFontIcon(FxIcons.INFO_CIRCLE), //
        FxLabels.newBoldLabel("Global ion libraries were changed, load?"),
        createButton("Load", FxIcons.CHECK_CIRCLE,
            "The global ion definitions were changed internally, load these changes? The global ion definitions are used for ion parsing.",
            () -> eventHandler.accept(new ReloadGlobalServiceChanges())) //
    );
    globalChangePane.visibleProperty().bind(model.globalVersionChangedProperty());

    final HBox main = FxLayout.newHBox(FxLayout.DEFAULT_SPACE * 2, modelChangePane,
        new Separator(Orientation.VERTICAL), globalChangePane);

    // only visible if one is visible
    main.visibleProperty()
        .bind(globalChangePane.visibleProperty().or(modelChangePane.visibleProperty()));
    // keep managed true to safe space that is needed
//    FxLayout.bindManagedToVisible(main);

    return main;
  }

  private Node createIonTypesPane() {
    return new IonTypeCreatorPane(model.ionTypesProperty(), model.getPartsDefinitions());
  }

  private Node createIonPartsPane() {
    return new IonPartCreatorPane(model.partsDefinitionsProperty());
  }
}
