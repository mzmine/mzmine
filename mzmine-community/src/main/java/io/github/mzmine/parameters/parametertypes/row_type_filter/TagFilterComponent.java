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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.datamodel.features.types.fx.TagCheckBoxPane;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.DoubleSupplier;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compact tag filter with a hover editor when the Tags column wraps differently.
 */
public final class TagFilterComponent extends HBox implements ValuePropertyComponent<BitSet> {

  private static final int COMPACT_TAG_COUNT = FeatureListPreferences.DEFAULT_TAG_LABELS.size();

  private final @NotNull ObjectProperty<BitSet> value = new SimpleObjectProperty<>(new BitSet());
  private final @NotNull TagCheckBoxPane compactPane = new TagCheckBoxPane(Pos.CENTER);
  private final @NotNull TagCheckBoxPane expandedPane = new TagCheckBoxPane(Pos.CENTER);
  private final @NotNull Label overflowLabel = new Label();
  private final @NotNull Popup popup = new Popup();
  private final @NotNull VBox popupContent = new VBox(expandedPane);
  private final @NotNull PauseTransition hideDelay = new PauseTransition(Duration.millis(250));
  private @NotNull List<String> tagLabels = FeatureListPreferences.DEFAULT_TAG_LABELS;
  private @NotNull DoubleSupplier columnWidthSupplier = () -> TagCheckBoxPane.DEFAULT_COLUMN_WIDTH;

  public TagFilterComponent(final int tagCount) {
    setAlignment(Pos.CENTER_LEFT);
    setSpacing(3);
    setMaxHeight(Region.USE_PREF_SIZE);
    setPickOnBounds(true);
    setCursor(Cursor.HAND);
    setFocusTraversable(true);
    setAccessibleText("Tag filter. Edit here or hover for the Tags column layout.");
    compactPane.setCompactColumns(COMPACT_TAG_COUNT / 2);
    compactPane.setOnSelectionChanged(this::updateValue);
    expandedPane.setOnSelectionChanged(this::updateValue);

    popupContent.setPadding(new Insets(6));
    popupContent.setStyle(
        "-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border;");
    popup.getContent().add(popupContent);
    popup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_BOTTOM_LEFT);
    popup.setAutoHide(true);
    popup.setHideOnEscape(true);
    popup.setAutoFix(true);
    popup.showingProperty().addListener((_, _, _) -> updatePreviewVisibility());
    popupContent.setOnMouseEntered(_ -> hideDelay.stop());
    popupContent.setOnMouseExited(_ -> scheduleHide());
    hideDelay.setOnFinished(_ -> {
      if (!isHover() && !popupContent.isHover()) {
        popup.hide();
      }
    });

    getChildren().setAll(compactPane, overflowLabel);
    setOnMouseEntered(_ -> showPopup());
    setOnMouseExited(_ -> scheduleHide());
    setOnMouseClicked(_ -> showPopup());
    setOnKeyPressed(event -> {
      if (event.getTarget() != this) {
        return;
      }
      if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
        if (requiresPopup()) {
          showPopup();
        } else if (compactPane.getTagCount() > 0) {
          compactPane.getCheckBox(0).requestFocus();
        }
        event.consume();
      }
    });
    visibleProperty().addListener((_, _, visible) -> {
      if (!visible) {
        popup.hide();
      }
    });

    value.addListener((_, _, tags) -> updateCheckBoxes(tags));
    setTagCount(tagCount);
  }

  public void setTagCount(final int tagCount) {
    if (tagCount < 0) {
      throw new IllegalArgumentException("Tag count must not be negative: " + tagCount);
    }
    if (expandedPane.getTagCount() == tagCount) {
      return;
    }
    updateLabels(tagCount);
    overflowLabel.setText("+" + Math.max(0, tagCount - COMPACT_TAG_COUNT));
    overflowLabel.setManaged(tagCount > COMPACT_TAG_COUNT);
    updatePreviewVisibility();

    final BitSet trimmed = getValue();
    trimmed.clear(tagCount, Math.max(tagCount, trimmed.length()));
    setValue(trimmed);
  }

  public void setTagLabels(@NotNull final List<String> labels) {
    tagLabels = List.copyOf(labels);
    if (expandedPane.getTagCount() == tagLabels.size()) {
      updateLabels(tagLabels.size());
    } else {
      setTagCount(tagLabels.size());
    }
  }

  private void updateLabels(final int tagCount) {
    final List<String> labels = new ArrayList<>(tagCount);
    for (int index = 0; index < tagCount; index++) {
      labels.add(index < tagLabels.size() ? tagLabels.get(index) : "Tag index " + index);
    }
    expandedPane.setTagLabels(labels);
    compactPane.setTagLabels(labels.subList(0, Math.min(tagCount, COMPACT_TAG_COUNT)));
    updateCheckBoxes(value.get());
  }

  public void ensureTagCount(final int tagCount) {
    if (tagCount <= expandedPane.getTagCount()) {
      return;
    }
    setTagCount(tagCount);
  }

  public void setColumnWidthSupplier(@NotNull final DoubleSupplier supplier) {
    columnWidthSupplier = supplier;
  }

  private void updateValue(final int tagIndex, final boolean selected) {
    final BitSet selectedTags = getValue();
    selectedTags.set(tagIndex, selected);
    value.set(selectedTags);
  }

  private boolean requiresPopup() {
    final int tagCount = expandedPane.getTagCount();
    if (tagCount > COMPACT_TAG_COUNT) {
      return true;
    }
    final int compactColumns = Math.min(tagCount, COMPACT_TAG_COUNT / 2);
    final int tableColumns = Math.min(tagCount,
        compactPane.columnsForColumnWidth(columnWidthSupplier.getAsDouble()));
    return compactColumns != tableColumns;
  }

  private void updateCheckBoxes(@Nullable final BitSet selectedTags) {
    compactPane.setSelectedTags(selectedTags);
    expandedPane.setSelectedTags(selectedTags);
  }

  public @NotNull BitSet getValue() {
    return (BitSet) value.get().clone();
  }

  public void setValue(@Nullable final BitSet selectedTags) {
    final BitSet copy = selectedTags == null ? new BitSet() : (BitSet) selectedTags.clone();
    copy.clear(expandedPane.getTagCount(), Math.max(expandedPane.getTagCount(), copy.length()));
    value.set(copy);
    updateCheckBoxes(copy);
  }

  private void scheduleHide() {
    hideDelay.playFromStart();
  }

  private void updatePreviewVisibility() {
    // Keep the preview managed so opening the popup does not resize the filter bar.
    compactPane.setVisible(!popup.isShowing());
    overflowLabel.setVisible(!popup.isShowing() && expandedPane.getTagCount() > COMPACT_TAG_COUNT);
  }

  private void showPopup() {
    if (!requiresPopup()) {
      popup.hide();
      return;
    }
    if (popup.isShowing()) {
      hideDelay.stop();
      return;
    }
    expandedPane.setColumnWidth(columnWidthSupplier.getAsDouble());
    if (getScene() == null) {
      return;
    }
    hideDelay.stop();
    popup.getScene().getStylesheets().setAll(getScene().getStylesheets());
    popupContent.applyCss();
    final var filterBounds = localToScreen(getBoundsInLocal());
    if (filterBounds == null) {
      return;
    }
    // decision: grow upward over the feature table, away from the status bar.
    popup.show(this, filterBounds.getMinX(), filterBounds.getMaxY());
  }

  @Override
  public @NotNull Property<BitSet> valueProperty() {
    return value;
  }
}
