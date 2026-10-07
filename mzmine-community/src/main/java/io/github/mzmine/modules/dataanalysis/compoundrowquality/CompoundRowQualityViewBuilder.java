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

package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Builds the CompoundRow quality pane. The view consists of a vertical "Compound quality" panel
 * with a bold title row and a list of {@link QualityCheckItem} cards (one per check). Each card
 * carries a status icon, a small expand toggle below the icon, the check's main pane, and an
 * optional expandable sub pane.
 */
public class CompoundRowQualityViewBuilder extends FxViewBuilder<CompoundRowQualityModel> {

  public static final int MAIN_WIDTH = 350;

  private SimpleColorPalette colors;
  /// Bound to {@code itemList.widthProperty()} once the item list exists; cards then clamp their
  /// max width to this so they cannot grow past the ScrollPane viewport.
  private ReadOnlyDoubleProperty contentWidth;

  protected CompoundRowQualityViewBuilder(CompoundRowQualityModel model) {
    super(model);
  }

  @Override
  public Region build() {
    colors = ConfigService.getDefaultColorPalette();

    final VBox itemList = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);

    final Label emptyState = FxLabels.newItalicLabel("Select a compound row to see quality checks");
    emptyState.setPadding(new Insets(FxLayout.DEFAULT_SPACE));

    final ScrollPane scroll = new ScrollPane(itemList);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
    // ALWAYS instead of AS_NEEDED: keeps the viewport width stable when items are added or
    // removed. Otherwise the scroll bar appears/disappears between rebuilds and every card has to
    // re-layout against the new viewport width, which causes the visible resize flicker.
    scroll.setVbarPolicy(ScrollBarPolicy.ALWAYS);
    VBox.setVgrow(scroll, Priority.ALWAYS);

    final Label title = FxLabels.newBoldTitle("Compound quality");
    // Spacer eats the remaining width so the gear button sits hard-right next to the title.
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final ButtonBase configButton = FxIconUtil.newIconButton(FxIcons.GEAR_PREFERENCES,
        "Configure compound quality checks", this::openConfigDialog);
    final HBox titleBar = FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.DEFAULT_PADDING_INSETS, title,
        spacer, configButton);

    final BorderPane outer = new BorderPane(scroll);
    outer.setTop(titleBar);
    outer.setPrefWidth(MAIN_WIDTH);

    // Bind to the actual container width the cards live in. fitToWidth=true keeps itemList sized
    // to the scroll viewport, so this already accounts for the scroll bar.
    contentWidth = itemList.widthProperty();

    final Runnable rebuild = () -> {
      final List<Node> nodes;
      if (model.getSelectedCompoundRow() == null) {
        nodes = List.of(emptyState);
      } else if (model.getResults().isEmpty()) {
        nodes = List.of(FxLabels.newItalicLabel("Computing…"));
      } else {
        nodes = renderItems(model.getResults());
      }
      itemList.getChildren().setAll(nodes);
    };

    PropertyUtils.onChangeList(rebuild, model.getResults());
    model.selectedCompoundRowProperty().subscribe(_ -> rebuild.run());

    rebuild.run();
    return outer;
  }

  private List<Node> renderItems(List<QualityCheckResult> results) {
    final List<Node> out = new ArrayList<>(results.size());
    for (final QualityCheckResult r : results) {
      out.add(buildItem(r));
    }
    return out;
  }

  private QualityCheckItem buildItem(QualityCheckResult r) {
    final QualityCheckItem item = new QualityCheckItem(r, colorFor(r.status()),
        model.expandedStateByTypeProperty());
    // Clamp each card's width to the scroll-pane content width so the card cannot grow past the
    // viewport regardless of inner content. Pref + max bound together; minWidth=0 is set inside
    // the item so a long label wraps instead of pushing the card wider.
    item.prefWidthProperty().bind(contentWidth);
    item.maxWidthProperty().bind(contentWidth);
    return item;
  }

  /// Open the {@link CompoundRowQualityCheckParameters} setup dialog. Works on a clone so a Cancel
  /// leaves both {@code MZmineConfiguration} and the model untouched; on OK the clone is written
  /// back to both. Replacing the model's property reference (rather than mutating in place) is what
  /// fires the recompute subscription in the controller.
  private void openConfigDialog() {
    final ParameterSet edited = ConfigService.getConfiguration()
        .getModuleParameters(CompoundRowQualityCheckModule.class).cloneParameterSet();
    final ExitCode exit = edited.showSetupDialog(true);
    if (exit != ExitCode.OK) {
      return;
    }
    ConfigService.getConfiguration()
        .setModuleParameters(CompoundRowQualityCheckModule.class, edited);
    model.checkParametersProperty().set(edited);
  }

  private Color colorFor(QualityCheckStatus status) {
    return switch (status) {
      case PASS -> colors.getPositiveColor();
      case WARN -> ColorsFX.YELLOW_WARN; // maybe add additional to color palette
      case FAIL -> colors.getNegativeColor();
      // DOES_NOT_APPLY results are filtered out by the interactor; the branch exists so the
      // switch stays exhaustive and the color is reasonable if a check ever leaks through.
      case UNAVAILABLE, DOES_NOT_APPLY -> colors.getNeutralColor();
    };
  }
}
