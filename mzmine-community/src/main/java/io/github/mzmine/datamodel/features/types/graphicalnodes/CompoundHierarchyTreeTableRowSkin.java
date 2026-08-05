package io.github.mzmine.datamodel.features.types.graphicalnodes;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.TreeTableRowSkin;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

/**
 * Custom row skin that draws the compound-hierarchy stripe in the tree's indent area. The stripe
 * is positioned manually in {@link #layoutChildren(double, double, double, double)} so it is
 * visible on every row, including leaves where the default disclosure node is hidden. The default
 * disclosure node (with its built-in expand/collapse click handler) is left untouched so the
 * arrow click keeps working.
 */
class CompoundHierarchyTreeTableRowSkin extends TreeTableRowSkin<ModularFeatureListRow> {

  private static final double STRIPE_WIDTH = 4d;
  // assumption: -fx-indent default from Modena's .tree-table-row-cell rule.
  private static final double INDENT_PER_LEVEL = 10d;

  private final Region stripe;

  CompoundHierarchyTreeTableRowSkin(@NotNull final TreeTableRow<ModularFeatureListRow> row,
      @NotNull final Region stripe, @NotNull final ObservableValue<Boolean> showStripe) {
    super(row);
    this.stripe = stripe;

    stripe.setManaged(false);
    stripe.setMouseTransparent(true);
    stripe.visibleProperty().bind(showStripe);

    // Insert at index 0 so the disclosure node (added later by the parent skin) renders on top
    // of the stripe - the transparent disclosure background lets the stripe show through while
    // the arrow stays visible.
    getChildren().add(0, stripe);
  }

  @Override
  protected void layoutChildren(double x, double y, double w, double h) {
    super.layoutChildren(x, y, w, h);

    // TableRowSkinBase rebuilds its children via getChildren().setAll(cells) whenever the
    // visible columns change (add/remove/reorder), which silently drops the stripe. Re-insert
    // after super so the stripe survives those resets.
    if (!getChildren().contains(stripe)) {
      getChildren().add(0, stripe);
    }

    final TreeTableRow<ModularFeatureListRow> row = getSkinnable();
    final TreeItem<ModularFeatureListRow> item = row.getTreeItem();
    if (item == null || item.getValue() == null) {
      return;
    }
    final TreeTableView<ModularFeatureListRow> tv = row.getTreeTableView();
    if (tv == null) {
      return;
    }

    int level = tv.getTreeItemLevel(item);
    if (!tv.isShowRoot()) {
      level--;
    }
    if (level < 0) {
      level = 0;
    }

    final double indentX = x + level * INDENT_PER_LEVEL;
    stripe.resizeRelocate(indentX, y + 2d, STRIPE_WIDTH, Math.max(0d, h - 4d));
  }
}
