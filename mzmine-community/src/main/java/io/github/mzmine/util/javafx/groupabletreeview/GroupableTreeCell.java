/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.javafx.groupabletreeview;

import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom TreeCell for {@link GroupableTreeView} that renders leaf items using configurable text and
 * graphic extractors, and renders group nodes with their group name in bold. Supports drag & drop
 * for reordering.
 * <p>
 * Overrides {@link #layoutChildren} to remove the default disclosure node indent that TreeCellSkin
 * reserves for all cells (even leaf items). For leaf items at the top level, this eliminates the
 * unwanted left indent.
 *
 * @param <T> the type of items in the tree
 */
public class GroupableTreeCell<T> extends TreeCell<T> {

  private final Function<T, String> textExtractor;
  private final @Nullable Function<T, Node> graphicFactory;
  private final @Nullable GroupableTreeView<T> ownerTreeView;

  // saved reference to the default disclosure node so we can restore it for group items
  private @Nullable Node savedDisclosureNode;

  /**
   * @param textExtractor  function to extract display text from an item
   * @param graphicFactory optional function to create a graphic node for an item, may be null
   * @param ownerTreeView  the owning GroupableTreeView, used for drag & drop move operations. May
   *                       be null if drag & drop is not needed.
   */
  public GroupableTreeCell(@NotNull final Function<T, String> textExtractor,
      @Nullable final Function<T, Node> graphicFactory,
      @Nullable final GroupableTreeView<T> ownerTreeView) {
    this.textExtractor = textExtractor;
    this.graphicFactory = graphicFactory;
    this.ownerTreeView = ownerTreeView;
    setupDragAndDrop();
  }

  @Override
  protected void updateItem(final @Nullable T item, final boolean empty) {
    super.updateItem(item, empty);

    // capture the default disclosure node on first use so we can restore it later
    if (savedDisclosureNode == null && getDisclosureNode() != null) {
      savedDisclosureNode = getDisclosureNode();
    }

    if (empty || getTreeItem() == null) {
      setText(null);
      setGraphic(null);
      setFont(Font.getDefault());
      setDisclosureNode(null);
      return;
    }

    if (getTreeItem() instanceof GroupTreeItem<T> group) {
      setText(group.getGroupName());
      setGraphic(null);
      setFont(Font.font(getFont().getFamily(), FontWeight.BOLD, getFont().getSize()));
      // restore disclosure node for groups so the expand/collapse arrow is shown
      setDisclosureNode(savedDisclosureNode);
    } else if (item != null) {
      setText(textExtractor.apply(item));
      setGraphic(graphicFactory != null ? graphicFactory.apply(item) : null);
      setFont(Font.getDefault());
      // null disclosure node for leaf items
      setDisclosureNode(null);
    } else {
      setText(null);
      setGraphic(null);
      setFont(Font.getDefault());
      setDisclosureNode(null);
    }
  }

  @Override
  protected void layoutChildren() {
    // let the default skin layout first
    super.layoutChildren();

    // decision: TreeCellSkin always reserves disclosureWidth (default 18px) even for leaf cells.
    // For non-group items, shift all children left to compensate for that reserved space.
    if (!(getTreeItem() instanceof GroupTreeItem)) {
      final Node disclosureNode = getDisclosureNode();
      // only adjust when there is no disclosure node (leaf items)
      if (disclosureNode == null || !disclosureNode.isVisible()) {
        final double shift = computeDisclosureShift();
        if (shift > 0) {
          for (final Node child : getChildren()) {
            child.setLayoutX(child.getLayoutX() - shift);
          }
        }
      }
    }
  }

  /**
   * Computes the disclosure width that TreeCellSkin reserved. This is the default 18px or whatever
   * the max disclosure width is for this tree view.
   */
  private double computeDisclosureShift() {
    // assumption: TreeCellSkin uses a default of 18 when no disclosure node is set,
    // plus 3px padding when no graphic is present on the tree item
    // TODO: 18px is the JavaFX default but may change with CSS theming or HiDPI scaling.
    //  Derive from the actual skin disclosure width if possible.
    final int graphicPadding =
        (getTreeItem() != null && getTreeItem().getGraphic() == null) ? 0 : 3;
    return 18 + graphicPadding;
  }

  /**
   * @return true if the drag event originated from a cell in this tree view (internal reorder)
   */
  private boolean isInternalDrag(@NotNull final DragEvent event) {
    return event.getGestureSource() instanceof GroupableTreeCell<?> && event.getDragboard()
        .hasString();
  }

  private void setupDragAndDrop() {
    setOnDragDetected(event -> {
      if (getItem() == null || getTreeItem() instanceof GroupTreeItem) {
        return;
      }

      final Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
      final ClipboardContent content = new ClipboardContent();
      // TODO: identifying the dragged item by its visual flat row index is fragile if items
      //  shift between drag-start and drop. Consider storing a stable item identity instead.
      content.putString(String.valueOf(getIndex()));
      final WritableImage snapshot = this.snapshot(null, null);
      dragboard.setDragView(snapshot);
      dragboard.setContent(content);
      event.consume();
    });

    setOnDragOver(event -> {
      // only handle internal tree reorder drags; let external file drags bubble to scene handler
      if (isInternalDrag(event)) {
        event.acceptTransferModes(TransferMode.MOVE);
        event.consume();
      }
    });

    setOnDragEntered(event -> {
      if (event.getGestureSource() != this && event.getDragboard().hasString()) {
        setOpacity(0.3);
      }
    });

    setOnDragExited(event -> {
      if (event.getGestureSource() != this && event.getDragboard().hasString()) {
        setOpacity(1);
      }
    });

    setOnDragDropped(event -> {
      // only handle internal tree reorder drags; let external file drags bubble to scene handler
      if (!isInternalDrag(event)) {
        return;
      }

      if (ownerTreeView == null || getTreeItem() == null) {
        event.setDropCompleted(false);
        event.consume();
        return;
      }

      final Dragboard db = event.getDragboard();
      final int draggedIndex = Integer.parseInt(db.getString());
      final TreeItem<T> draggedItem = getTreeView().getTreeItem(draggedIndex);
      if (draggedItem != null && draggedItem.getValue() != null) {
        // determine target parent and index
        final TreeItem<T> targetItem = getTreeItem();
        final TreeItem<T> targetParent;
        int targetIndex;

        if (targetItem instanceof GroupTreeItem<T>) {
          // dropping onto a group: add at end of group
          targetParent = targetItem;
          targetIndex = targetItem.getChildren().size();
        } else {
          // dropping onto a leaf: insert relative to the target
          targetParent = targetItem.getParent();
          targetIndex = targetParent.getChildren().indexOf(targetItem);

          // if dragging downward, place after the target item
          if (draggedIndex < getIndex()) {
            targetIndex++;
          }
        }

        ownerTreeView.moveItem(draggedItem.getValue(), targetParent, targetIndex);
      }
      event.setDropCompleted(true);
      event.consume();
    });

    setOnDragDone(DragEvent::consume);
  }
}
