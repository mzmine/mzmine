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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Content of the "linked feature tables" popover. Renders one row per outgoing link with a
 * checkbox toggling the link active state and a hyperlink that focuses the linked dashboard's
 * tab or window.
 */
public class FeatureTableLinksPopoverContent extends VBox {

  private static final Logger logger = Logger.getLogger(
      FeatureTableLinksPopoverContent.class.getName());

  private final @NotNull FxFeatureTableController owner;
  private final ListView<FeatureTableLink> listView = new ListView<>();

  public FeatureTableLinksPopoverContent(@NotNull FxFeatureTableController owner) {
    this.owner = owner;
    setPadding(new Insets(8));
    setSpacing(6);
    setPrefWidth(640);

    final Label title = FxLabels.newBoldLabel("Linked feature tables");
    final Label hint = FxLabels.newLabel(
        "Toggle to sync this table's selection into the linked dashboard.");
    hint.setWrapText(true);

    listView.setItems(owner.getOutgoingLinks());
    listView.setCellFactory(_ -> new LinkCell());
    listView.setPlaceholder(FxLabels.newLabel("No linked tables. Open another dashboard via "
        + "double-click on an annotation to create a link."));
    listView.setPrefHeight(160);
    VBox.setVgrow(listView, Priority.ALWAYS);

    getChildren().addAll(title, hint, listView);
  }

  /**
   * Prune expired weak references; call before the popover is shown so stale entries are not
   * displayed.
   */
  public void refresh() {
    owner.pruneExpiredLinks();
    listView.refresh();
  }

  // --- helpers -------------------------------------------------------------

  /**
   * Focus the window and tab that contain {@code target}'s root view.
   */
  private static void focusTarget(@NotNull FxFeatureTableController target) {
    final Region root = target.getRootView();
    if (root == null) {
      logger.fine("Cannot focus link target: root view not built yet");
      return;
    }
    final Scene scene = root.getScene();
    if (scene == null) {
      logger.fine("Cannot focus link target: not currently attached to a scene");
      return;
    }
    final Window window = scene.getWindow();
    if (window instanceof Stage stage) {
      stage.toFront();
      stage.requestFocus();
    }
    final Tab tab = findContainingTab(scene.getRoot(), root);
    if (tab != null && tab.getTabPane() != null) {
      tab.getTabPane().getSelectionModel().select(tab);
    }
  }

  /**
   * Walk the scene graph from {@code from} looking for a {@link TabPane} whose tab's content has
   * {@code target} as a descendant. Returns the matching {@link Tab}, or {@code null}.
   */
  private static @Nullable Tab findContainingTab(@Nullable Parent from, @NotNull Node target) {
    if (from == null) {
      return null;
    }
    for (Node child : from.getChildrenUnmodifiable()) {
      if (child instanceof TabPane tp) {
        for (Tab tab : tp.getTabs()) {
          final Node content = tab.getContent();
          if (content != null && isDescendant(content, target)) {
            return tab;
          }
        }
      }
      if (child instanceof Parent p) {
        final Tab found = findContainingTab(p, target);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static boolean isDescendant(@NotNull Node ancestor, @NotNull Node node) {
    Node cur = node;
    while (cur != null) {
      if (cur == ancestor) {
        return true;
      }
      cur = cur.getParent();
    }
    return false;
  }

  private static @NotNull String displayTitle(@NotNull FxFeatureTableController target) {
    final FeatureTableOwner tableOwner = target.getFeatureTable().getTableOwner();
    final String base = switch (tableOwner) {
      case COMPOUND_DASHBOARD -> "Compound dashboard";
      case STATS_DASHBOARD -> "Stats dashboard";
      case LIPID_DASHBOARD -> "Lipid QC dashboard";
      case NETWORK_DASHBOARD -> "Network dashboard";
      case FEATURE_INTEGRATION_DASHBOARD -> "Feature integration dashboard";
      case OTHER_DETECTOR_CORRELATION -> "Other detector correlation";
      case FEATURE_TABLE_TAB -> "Feature table";
      case UNDEFINED -> "Feature table";
    };
    final FeatureList flist = target.getFeatureList();
    return flist == null ? base : base + " — " + flist.getName();
  }

  // --- inner class ---------------------------------------------------------

  private final class LinkCell extends ListCell<FeatureTableLink> {

    private final CheckBox activeBox = new CheckBox();
    private final Hyperlink titleLink = new Hyperlink();
    private final HBox layout = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, activeBox,
        titleLink);

    // The currently-bound active property (so we can unbind on rebind to a different cell value).
    private @Nullable javafx.beans.property.BooleanProperty boundActive;

    LinkCell() {
      titleLink.setOnAction(_ -> {
        final FeatureTableLink link = getItem();
        if (link == null) {
          return;
        }
        final FxFeatureTableController target = link.getTarget();
        if (target != null) {
          focusTarget(target);
        }
      });
      HBox.setHgrow(titleLink, Priority.ALWAYS);
      activeBox.setTooltip(new javafx.scene.control.Tooltip(
          "When enabled, selecting a row in this table updates the linked dashboard."));
    }

    @Override
    protected void updateItem(FeatureTableLink item, boolean empty) {
      super.updateItem(item, empty);
      if (boundActive != null) {
        activeBox.selectedProperty().unbindBidirectional(boundActive);
        boundActive = null;
      }
      if (empty || item == null) {
        setGraphic(null);
        setText(null);
        return;
      }
      final FxFeatureTableController target = item.getTarget();
      if (target == null) {
        // Stale weak reference; will be pruned on next refresh().
        setGraphic(null);
        setText("(closed)");
        return;
      }
      titleLink.setText(displayTitle(target));
      activeBox.selectedProperty().bindBidirectional(item.active());
      boundActive = item.active();
      setText(null);
      setGraphic(layout);
    }
  }
}
