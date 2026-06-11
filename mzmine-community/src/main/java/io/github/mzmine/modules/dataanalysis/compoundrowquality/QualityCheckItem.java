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

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import javafx.collections.ObservableMap;
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
      @NotNull final Color statusColor,
      @NotNull final ObservableMap<@NotNull QualityCheckType, @NotNull Boolean> expandedStateByType) {
    setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    // Min 0 so the surrounding VBox can compress the card down to the ScrollPane width and the
    // wrapping labels inside take over from there.
    setMinWidth(0);
    // No max-height clamp so a tall expanded sub pane can grow freely; the surrounding VBox sums
    // child prefHeights and the ScrollPane scrolls past the viewport. Without this an inherited
    // USE_PREF_SIZE max could pin the card to its first-frame height and clip a sub pane that
    // later grows when the user toggles it open.
    setMaxHeight(Double.MAX_VALUE);
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
    // the card itself sits underneath it). Result subclasses can opt out of the indent via
    // {@link QualityCheckResult#wantsFullWidthSubPane()} to claim the full card width — useful
    // for grid-heavy sub panes that need every available pixel.
    subContent.setMinWidth(0);
    final double leftIndent = result.wantsFullWidthSubPane() ? 0
        : (FxIconUtil.DEFAULT_ICON_SIZE + FxLayout.DEFAULT_SPACE);
    final VBox subWrap = FxLayout.newVBox(Pos.TOP_LEFT,
        new Insets(FxLayout.DEFAULT_SPACE, 0, 0, leftIndent), true, subContent);
    subWrap.setMinWidth(0);
    // Same no-clamp on the inner wrap so an expanded sub pane with many rows can grow vertically.
    subWrap.setMaxHeight(Double.MAX_VALUE);
    subWrap.setStyle("-fx-border-color: -fx-box-border; -fx-border-width: 1 0 0 0;");
    // Keep managed locked to visible so a single toggle always invalidates the parent layout
    // chain. Setting them independently has been observed to leave BorderPane.center with a stale
    // prefHeight after the user expanded a card with a tall sub pane.
    subWrap.managedProperty().bind(subWrap.visibleProperty());
    // Restore the last expanded state for this check type. Defaults to false when the map has no
    // entry yet (e.g. a new check type added since the map was created).
    final boolean initiallyExpanded = expandedStateByType.getOrDefault(result.type(), false);
    subWrap.setVisible(initiallyExpanded);
    toggle.setRotate(initiallyExpanded ? 90 : 0);
    setCenter(subWrap);

    // Clicking anywhere on the header (icon, triangle, title, summary) toggles the sub pane. The
    // triangle is set mouse-transparent so its click bubbles up to the header handler;
    // chip-style labels inside the main pane have their own handlers and may consume the event
    // before it reaches us, which is the desired behaviour.
    header.setCursor(Cursor.HAND);
    header.setOnMouseClicked(_ -> {
      final boolean expand = !subWrap.isVisible();
      subWrap.setVisible(expand);
      // 0° -> right (collapsed); 90° -> down (expanded).
      toggle.setRotate(expand ? 90 : 0);
      // Persist the new state so the next view rebuild restores it for this check type.
      expandedStateByType.put(result.type(), expand);
      // Defensive: force a layout pass up the chain. The visible/managed change should already
      // invalidate the BorderPane and the surrounding VBox, but when the sub pane is very tall
      // (e.g. many MS2-available rows) the BorderPane.center prefHeight has occasionally
      // remained stale, causing the next card to render over the bottom of this one. A direct
      // requestLayout on the parent forces a clean recompute.
      requestLayout();
      if (getParent() != null) {
        getParent().requestLayout();
      }
    });
  }
}
