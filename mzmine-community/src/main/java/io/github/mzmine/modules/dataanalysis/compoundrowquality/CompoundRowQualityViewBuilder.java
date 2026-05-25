package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Builds the CompoundRow quality pane. The view consists of a vertical "Compound quality" panel
 * with a bold title row, a progress spinner, and a list of collapsible items (one per check). Each
 * item shows a status icon, the check label, and a short summary; expanding it reveals detail
 * lines.
 */
public class CompoundRowQualityViewBuilder extends FxViewBuilder<CompoundRowQualityModel> {

  private SimpleColorPalette colors;
  public static final int MAIN_WIDTH = 350;

  /// Cap the panel width so it does not stretch indefinitely on wide displays.
  private static final double MAX_PANE_WIDTH = 500;
  /// Approx width reserved for the TitledPane disclosure arrow + insets, used when binding the
  /// header HBox width so the header fills the title bar.
  private static final double TITLE_ARROW_RESERVED = 8d;
  private DoubleBinding contentWidth;

  protected CompoundRowQualityViewBuilder(CompoundRowQualityModel model) {
    super(model);
  }

  @Override
  public Region build() {
    colors = ConfigService.getDefaultColorPalette();

    final VBox itemList = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);

    final Label emptyState = FxLabels.newItalicLabel("Select a compound row to see quality checks");
    emptyState.setPadding(new Insets(FxLayout.DEFAULT_SPACE));

    final BorderPane content = new BorderPane(itemList);
    final ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
    scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    VBox.setVgrow(scroll, Priority.ALWAYS);

    final Label title = FxLabels.newBoldTitle("Compound quality");

    final HBox titleBar = FxLayout.newHBox(Pos.CENTER_LEFT, FxLayout.DEFAULT_PADDING_INSETS, title);

    final BorderPane outer = new BorderPane(scroll);
    outer.setTop(titleBar);
//    outer.setMinWidth(MAIN_WIDTH);
    outer.setPrefWidth(MAIN_WIDTH);
//    outer.setMaxWidth(MAX_PANE_WIDTH);

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

    contentWidth = outer.widthProperty().subtract(TITLE_ARROW_RESERVED);
    content.prefWidthProperty().bind(contentWidth);
    content.maxWidthProperty().bind(contentWidth);

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

  private TitledPane buildItem(QualityCheckResult r) {
    final FontIcon icon = FxIconUtil.getFontIcon(r.status().icon(), FxIconUtil.DEFAULT_ICON_SIZE,
        colorFor(r.status()));

    final Region mainPane = r.buildMainPane();
    // Min 0 so HBox can shrink mainPane below its preferred content width; wrapping labels inside
    // handle the rest. Without this, a long line of text forces the entire title bar wider than
    // the TitledPane and the panel grows past the ScrollPane viewport.
    mainPane.setMinWidth(0);
    HBox.setHgrow(mainPane, Priority.ALWAYS);
    final HBox header = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, icon, mainPane);
    header.setMinWidth(0);

    final TitledPane pane = new TitledPane();
    pane.setGraphic(header);
    pane.setText(null);
    pane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    pane.prefWidthProperty().bind(contentWidth);
    pane.maxWidthProperty().bind(contentWidth);
    // Bind both pref and max width so the header never exceeds the title-bar width; pref alone
    // lets large child minWidths grow the HBox past the TitledPane.
//    header.prefWidthProperty().bind(contentWidth);
//    header.maxWidthProperty().bind(contentWidth);

    final Region subPane = r.buildSubPane();
    if (subPane == null) {
      pane.setCollapsible(false);
    } else {
      // wrap so the body aligns under the summary (offset by icon width) regardless of what
      // the result built
      subPane.setMinWidth(0);
      final VBox body = FxLayout.newVBox(Pos.TOP_LEFT, FxLayout.DEFAULT_PADDING_INSETS, true,
          subPane);
      body.setMinWidth(0);
      pane.setContent(body);
      pane.setExpanded(false);
    }

    return pane;
  }

  private Color colorFor(QualityCheckStatus status) {
    return switch (status) {
      case PASS -> colors.getPositiveColor();
      case WARN -> ColorsFX.YELLOW_WARN; // maybe add additional to color palette
      case FAIL -> colors.getNegativeColor();
      case UNAVAILABLE -> colors.getNeutralColor();
    };
  }
}
