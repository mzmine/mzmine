package io.github.mzmine.datamodel.features.types.graphicalnodes;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.color.ColorsFX;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Skin;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom {@link TreeTableRow} that paints a colored vertical stripe in the tree's disclosure
 * (indent) area for every row - including leaves - indicating the compound hierarchy level: blue
 * for compound rows, magenta for ion children, grey for isotope leaves. The stripe is drawn
 * entirely from Java code (no CSS) by setting a background fill on a {@link Region}, and is laid
 * out by {@link CompoundHierarchyTreeTableRowSkin} so that JavaFX's default disclosure-node click
 * handler stays intact.
 */
public class CompoundHierarchyTreeTableRow extends TreeTableRow<ModularFeatureListRow> {

  private static final int MAX_LEVEL = 2;

  private static final Color[] LEVEL_COLORS = {
      ColorsFX.POSITIVE_MARKER_COLORBLIND, // level 0 - compound row
      ColorsFX.MAGENTA,                    // level 1 - ion children
      ColorsFX.NEUTRAL_MARKER              // level 2 - isotope leaves
  };

  private final Region stripe = new Region();
  private final ObservableValue<Boolean> showStripe;

  /**
   * @param showStripe observable gating stripe visibility - typically bound to "compound mode is
   *                   active" so plain feature tables look unchanged.
   */
  public CompoundHierarchyTreeTableRow(@NotNull final ObservableValue<Boolean> showStripe) {
    super();
    this.showStripe = showStripe;

    // Rows are recycled by virtualisation, so update on both treeItem and index changes.
    PropertyUtils.onChange(this::updateStripeColor, treeItemProperty(), indexProperty(), emptyProperty());
    updateStripeColor();
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new CompoundHierarchyTreeTableRowSkin(this, stripe, showStripe);
  }

  private void updateStripeColor() {
    final TreeItem<ModularFeatureListRow> item = getTreeItem();
    if (item == null || item.getValue() == null) {
      stripe.setBackground(Background.EMPTY);
      return;
    }
    final int level = Math.min(levelOf(item), MAX_LEVEL);
    final BackgroundFill fill = new BackgroundFill(LEVEL_COLORS[level], null, null);
    stripe.setBackground(new Background(fill));
  }

  private static int levelOf(@Nullable final TreeItem<?> item) {
    if (item == null) {
      return 0;
    }
    int level = 0;
    TreeItem<?> parent = item.getParent();
    while (parent != null && parent.getValue() != null && level < MAX_LEVEL) {
      level++;
      parent = parent.getParent();
    }
    return level;
  }
}
