package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * One quality-check card. Replaces the previous {@link javafx.scene.control.TitledPane} wrapper so
 * width handling is fully controlled by the host (the surrounding
 * {@link javafx.scene.control.ScrollPane}); every nested container has {@code minWidth = 0} so a
 * long line wraps instead of forcing the panel wider than the viewport.
 * <p>
 * Layout (BorderPane regions):
 * <pre>
 *   ┌──────────────────────────────────────────────────────────┐
 *   │ top:    [status icon]   [main content (title + summary)] │
 *   │         [▶ toggle]                                       │
 *   │ ──────────────────────────────────────────────────────── │  (top border of sub pane)
 *   │ center: (sub pane, hidden by default; click toggle/header│
 *   │          to expand)                                      │
 *   └──────────────────────────────────────────────────────────┘
 * </pre>
 * The icon column is vertically centered against the main content so the icon + toggle sit mid-card
 * regardless of how many text lines wrap into the main pane.
 */
public final class QualityCheckItem extends BorderPane {

  /// Half-width of the toggle triangle. The full triangle is {@code 2 * TRIANGLE_HALF_WIDTH} wide
  /// and tall; rendered as a right-pointing triangle (collapsed) that rotates 90° to point down
  /// when expanded.
  private static final double TRIANGLE_HALF_WIDTH = 4.0;

  public QualityCheckItem(@NotNull final QualityCheckResult result,
      @NotNull final Color statusColor) {
    setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    // Min 0 so the surrounding VBox can compress the card down to the ScrollPane width and the
    // wrapping labels inside take over from there.
    setMinWidth(0);
    // Thin bottom border separates adjacent cards. Uses the theme constant so it adapts to dark /
    // light modes.
    setStyle("-fx-border-color: -fx-box-border; -fx-border-width: 0 0 1 0;");

    final FontIcon statusIcon = FxIconUtil.getFontIcon(result.status().icon(),
        FxIconUtil.DEFAULT_ICON_SIZE, statusColor);

    final Region mainContent = result.buildMainPane();
    mainContent.setMinWidth(0);
    HBox.setHgrow(mainContent, Priority.ALWAYS);

    // Left column: status icon stacked on top, optional expand toggle below it. TOP_CENTER aligns
    // the icon + toggle horizontally so they line up on a single vertical axis.
    final VBox iconColumn = FxLayout.newVBox(Pos.TOP_CENTER, Insets.EMPTY, false, statusIcon);
    iconColumn.setMinWidth(FxIconUtil.DEFAULT_ICON_SIZE);

    // CENTER_LEFT so the icon column sits vertically centered against the wrapping main content.
    final HBox header = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, iconColumn, mainContent);
    header.setMinWidth(0);
    setTop(header);

    final Region subContent = result.buildSubPane();
    if (subContent == null) {
      return;
    }

    // Simple triangle indicator below the status icon. Pointing right when collapsed; rotates 90°
    // to point down when expanded. Polygon (not FontIcon) so it renders crisply at this size and
    // doesn't depend on the icon font being loaded. No own click handler — the whole header
    // toggles the sub pane (see below) so the triangle is purely a visual affordance.
    final Polygon toggle = new Polygon( //
        0.0, 0.0, //
        2 * TRIANGLE_HALF_WIDTH, TRIANGLE_HALF_WIDTH, //
        0.0, 2 * TRIANGLE_HALF_WIDTH);
    toggle.setFill(Color.GRAY);
    toggle.setMouseTransparent(true);
    iconColumn.getChildren().add(toggle);

    // Indent sub pane so it lines up under the main content (offset past the icon column). Top
    // border draws a separator between the header and the sub pane content (the bottom border of
    // the card itself sits underneath it).
    subContent.setMinWidth(0);
    final VBox subWrap = FxLayout.newVBox(Pos.TOP_LEFT, new Insets(FxLayout.DEFAULT_SPACE, 0, 0,
        FxIconUtil.DEFAULT_ICON_SIZE + FxLayout.DEFAULT_SPACE), true, subContent);
    subWrap.setMinWidth(0);
    subWrap.setStyle("-fx-border-color: -fx-box-border; -fx-border-width: 1 0 0 0;");
    subWrap.setVisible(false);
    subWrap.setManaged(false);
    setCenter(subWrap);

    // Clicking anywhere on the header (icon, triangle, title, summary) toggles the sub pane. The
    // triangle is set mouse-transparent so its click bubbles up to the header handler;
    // chip-style labels inside the main pane have their own handlers and may consume the event
    // before it reaches us, which is the desired behaviour.
    header.setCursor(Cursor.HAND);
    header.setOnMouseClicked(_ -> {
      final boolean expand = !subWrap.isVisible();
      subWrap.setVisible(expand);
      subWrap.setManaged(expand);
      // 0° -> right (collapsed); 90° -> down (expanded).
      toggle.setRotate(expand ? 90 : 0);
    });
  }
}
