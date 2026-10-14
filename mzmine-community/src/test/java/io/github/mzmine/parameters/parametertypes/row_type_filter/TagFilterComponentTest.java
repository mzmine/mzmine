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

import fxinitializer.InitJavaFX;
import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.datamodel.features.types.fx.TagCheckBoxPane;
import io.github.mzmine.main.ConfigService;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.jetbrains.annotations.NotNull;

@EnabledOnOs(OS.WINDOWS)
class TagFilterComponentTest {

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
  }

  private static void assertVisibleGapsMatch(@NotNull final TagCheckBoxPane pane) {
    final Node firstBox = pane.getCheckBox(0).lookup(".box");
    final Node secondBox = pane.getCheckBox(1).lookup(".box");
    final Node thirdBox = pane.getCheckBox(2).lookup(".box");
    Assertions.assertNotNull(firstBox);
    Assertions.assertNotNull(secondBox);
    Assertions.assertNotNull(thirdBox);
    final Bounds first = firstBox.localToScene(firstBox.getBoundsInLocal());
    final Bounds second = secondBox.localToScene(secondBox.getBoundsInLocal());
    final Bounds third = thirdBox.localToScene(thirdBox.getBoundsInLocal());
    Assertions.assertEquals(first.getMinY(), second.getMinY(), 2);
    final double horizontalGap = second.getMinX() - first.getMaxX();
    final double verticalGap = third.getMinY() - first.getMaxY();
    Assertions.assertTrue(horizontalGap >= 0);
    Assertions.assertTrue(verticalGap >= 0);
    Assertions.assertEquals(horizontalGap, verticalGap, 1,
        "Visible checkbox gaps should match after CSS layout");
  }

  private static void runOnFxAndWait(@NotNull final Runnable action) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Throwable[] error = new Throwable[1];
    submitWhenToolkitReady(() -> {
      try {
        Platform.setImplicitExit(false);
        action.run();
      } catch (Throwable thrown) {
        error[0] = thrown;
      } finally {
        latch.countDown();
      }
    });
    Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX task did not finish");
    if (error[0] != null) {
      throw new AssertionError(error[0]);
    }
  }

  private static void submitWhenToolkitReady(@NotNull final Runnable action)
      throws InterruptedException {
    final long deadline = System.currentTimeMillis() + 30_000;
    while (true) {
      try {
        Platform.runLater(action);
        return;
      } catch (IllegalStateException toolkitNotInitialized) {
        if (System.currentTimeMillis() > deadline) {
          throw toolkitNotInitialized;
        }
        Thread.sleep(50);
      }
    }
  }

  @Test
  void compactPreviewKeepsDefaultTagCountAndSharesSelectionWithExpandedPane() throws Exception {
    runOnFxAndWait(() -> {
      final TagFilterComponent filter = new TagFilterComponent(8);
      final TagCheckBoxPane compact = (TagCheckBoxPane) filter.getChildren().getFirst();
      final Label overflow = (Label) filter.getChildren().get(1);
      Assertions.assertEquals(FeatureListPreferences.DEFAULT_TAG_LABELS.size(),
          compact.getChildren().size());
      Assertions.assertEquals("+" + (8 - FeatureListPreferences.DEFAULT_TAG_LABELS.size()),
          overflow.getText());

      filter.setTagLabels(List.of("A", "B", "C", "D", "E", "F", "G", "H"));
      final CheckBox first = (CheckBox) compact.getChildren().getFirst();
      Assertions.assertEquals("A", first.getTooltip().getText());
      first.setSelected(true);
      Assertions.assertTrue(first.isSelected());
      Assertions.assertTrue(filter.getValue().get(0));

      final BitSet selected = new BitSet();
      selected.set(0);
      selected.set(7);
      filter.setValue(selected);
      Assertions.assertTrue(first.isSelected());
      Assertions.assertTrue(filter.getValue().get(7));

      filter.setTagLabels(List.of("A", "B", "C", "D"));
      Assertions.assertFalse(filter.getValue().get(7));
      Assertions.assertEquals(4, compact.getChildren().size());
      Assertions.assertFalse(overflow.isManaged());
    });
  }

  @Test
  void matchingLayoutEditsInPlaceWithoutPopover() throws Exception {
    runOnFxAndWait(() -> {
      final TagFilterComponent filter = new TagFilterComponent(
          FeatureListPreferences.DEFAULT_TAG_LABELS.size());
      filter.setColumnWidthSupplier(() -> 35);
      final Stage stage = new Stage();
      try {
        stage.setScene(new Scene(new StackPane(filter)));
        ConfigService.getConfiguration().getTheme().apply(stage.getScene().getStylesheets());
        stage.show();
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();

        filter.getOnMouseEntered().handle(null);
        Assertions.assertTrue(Window.getWindows().stream()
            .noneMatch(window -> window instanceof Popup && window.isShowing()));
        final TagCheckBoxPane compact = (TagCheckBoxPane) filter.getChildren().getFirst();
        final CheckBox first = (CheckBox) compact.getChildren().getFirst();
        first.setSelected(true);
        Assertions.assertTrue(first.isSelected());
        Assertions.assertTrue(filter.getValue().get(0));
      } finally {
        stage.hide();
      }
    });
  }

  @Test
  void narrowedColumnOpensPopoverForDefaultTagCount() throws Exception {
    runOnFxAndWait(() -> {
      final TagFilterComponent filter = new TagFilterComponent(
          FeatureListPreferences.DEFAULT_TAG_LABELS.size());
      filter.setColumnWidthSupplier(() -> 22);
      final Stage stage = new Stage();
      try {
        stage.setScene(new Scene(new StackPane(filter)));
        ConfigService.getConfiguration().getTheme().apply(stage.getScene().getStylesheets());
        stage.show();
        filter.getOnMouseEntered().handle(null);

        Assertions.assertTrue(Window.getWindows().stream()
            .anyMatch(window -> window instanceof Popup && window.isShowing()));
        Assertions.assertFalse(filter.getChildren().getFirst().isVisible());
      } finally {
        stage.hide();
      }
    });
  }

  @Test
  void popupCentersItsIncompleteRowLikeTheTableCell() throws Exception {
    runOnFxAndWait(() -> {
      final TagFilterComponent filter = new TagFilterComponent(6);
      filter.setColumnWidthSupplier(() -> 63);
      final Stage stage = new Stage();
      try {
        stage.setScene(new Scene(new StackPane(filter)));
        ConfigService.getConfiguration().getTheme().apply(stage.getScene().getStylesheets());
        stage.show();
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        final var filterBounds = filter.localToScreen(filter.getBoundsInLocal());
        filter.getOnMouseEntered().handle(null);

        final Popup popup = (Popup) Window.getWindows().stream()
            .filter(window -> window instanceof Popup && window.isShowing()).findFirst()
            .orElseThrow();
        final VBox content = (VBox) popup.getContent().getFirst();
        final TagCheckBoxPane expanded = (TagCheckBoxPane) content.getChildren().getFirst();
        content.applyCss();
        content.layout();
        final CheckBox first = (CheckBox) expanded.getChildren().getFirst();
        final CheckBox fourth = (CheckBox) expanded.getChildren().get(3);
        final CheckBox fifth = (CheckBox) expanded.getChildren().get(4);
        final CheckBox sixth = (CheckBox) expanded.getChildren().get(5);
        Assertions.assertEquals(first.getLayoutY(), fourth.getLayoutY());
        Assertions.assertTrue(fifth.getLayoutY() > first.getLayoutY());
        final double firstRowCenter =
            (first.getBoundsInParent().getMinX() + fourth.getBoundsInParent().getMaxX()) / 2;
        final double secondRowCenter =
            (fifth.getBoundsInParent().getMinX() + sixth.getBoundsInParent().getMaxX()) / 2;
        Assertions.assertEquals(firstRowCenter, secondRowCenter, 1);
        Assertions.assertEquals(filterBounds.getMinX(), popup.getX(), 2);
        Assertions.assertEquals(filterBounds.getMaxY(), popup.getY() + popup.getHeight(), 2);
      } finally {
        stage.hide();
      }
    });
  }

  @Test
  void sharedPaneWrapsWhenColumnNarrows() throws Exception {
    runOnFxAndWait(() -> {
      final TagCheckBoxPane pane = new TagCheckBoxPane(Pos.CENTER_LEFT);
      pane.setTagLabels(List.of("A", "B", "C", "D"));
      new Scene(pane);
      pane.applyCss();
      final double checkboxWidth = pane.getCheckBox(0).prefWidth(-1);
      final double threeColumnWidth = 3 * checkboxWidth + 2 * pane.getHgap() + 4;
      pane.setColumnWidth(threeColumnWidth);
      pane.resize(threeColumnWidth, 100);
      pane.layout();
      Assertions.assertEquals(pane.getCheckBox(0).getLayoutY(), pane.getCheckBox(2).getLayoutY());
      Assertions.assertTrue(pane.getCheckBox(3).getLayoutY() > pane.getCheckBox(0).getLayoutY());

      final double oneColumnWidth = checkboxWidth + 2;
      pane.setColumnWidth(oneColumnWidth);
      pane.resize(oneColumnWidth, 100);
      pane.layout();
      Assertions.assertTrue(pane.getCheckBox(1).getLayoutY() > pane.getCheckBox(0).getLayoutY());
    });
  }

  @Test
  void compactPaneAdaptsToPresentationStylesheet() throws Exception {
    runOnFxAndWait(() -> {
      final TagCheckBoxPane pane = new TagCheckBoxPane(Pos.CENTER);
      pane.setTagLabels(List.of("A", "B", "C", "D"));
      pane.setCompactColumns(2);
      final Stage stage = new Stage();
      try {
        stage.setScene(new Scene(new StackPane(pane)));
        ConfigService.getConfiguration().getTheme().apply(stage.getScene().getStylesheets());
        stage.show();
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        final double normalWidth = pane.getPrefWidth();
        assertVisibleGapsMatch(pane);

        stage.getScene().getStylesheets().add("themes/MZmine_default_presentation.css");
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        stage.getScene().getRoot().layout();
        Assertions.assertTrue(pane.getPrefWidth() > normalWidth);
        assertVisibleGapsMatch(pane);

      } finally {
        stage.hide();
      }
    });
  }

  @Test
  void paneOwnsAndReusesCheckboxesAndSelection() throws Exception {
    runOnFxAndWait(() -> {
      final TagCheckBoxPane pane = new TagCheckBoxPane(Pos.CENTER);
      final AtomicInteger changes = new AtomicInteger();
      pane.setOnSelectionChanged((_, _) -> changes.incrementAndGet());
      pane.setTagLabels(List.of("First", "Second", "Third"));
      final CheckBox first = pane.getCheckBox(0);
      final BitSet selected = new BitSet();
      selected.set(1);
      pane.setSelectedTags(selected);
      Assertions.assertEquals(0, changes.get());
      Assertions.assertTrue(pane.getCheckBox(1).isSelected());

      pane.setTagLabels(List.of("Renamed", "Second", "Third"));
      Assertions.assertSame(first, pane.getCheckBox(0));
      Assertions.assertEquals("Renamed", first.getTooltip().getText());
      first.setSelected(true);
      Assertions.assertEquals(1, changes.get());
      Assertions.assertTrue(pane.getSelectedTags().get(0));

      pane.setTagLabels(List.of("Renamed"));
      Assertions.assertEquals(1, pane.getTagCount());
      Assertions.assertFalse(pane.getSelectedTags().get(1));
    });
  }

  @Test
  void panePreservesAnExtraStoredTagWhenAnotherCheckboxChanges() throws Exception {
    runOnFxAndWait(() -> {
      final TagCheckBoxPane pane = new TagCheckBoxPane(Pos.CENTER);
      pane.setTagLabels(List.of("A", "B", "C", "D", ""));
      final BitSet storedTags = new BitSet();
      storedTags.set(4);
      pane.setSelectedTags(storedTags);
      Assertions.assertNull(pane.getCheckBox(4).getTooltip());

      pane.getCheckBox(0).setSelected(true);
      Assertions.assertTrue(pane.getSelectedTags().get(0));
      Assertions.assertTrue(pane.getSelectedTags().get(4));

      pane.setTagLabels(List.of("A", "B", "C", "D"));
      pane.setSelectedTags(new BitSet());
      Assertions.assertEquals(4, pane.getTagCount());
    });
  }

  @Test
  void expandedEditorUsesColumnWidthAndUpdatesTheFilter() throws Exception {
    runOnFxAndWait(() -> {
      final TagFilterComponent filter = new TagFilterComponent(8);
      filter.setColumnWidthSupplier(() -> 22);
      final Stage stage = new Stage();
      try {
        stage.setScene(new Scene(new StackPane(filter)));
        ConfigService.getConfiguration().getTheme().apply(stage.getScene().getStylesheets());
        stage.show();
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        final double compactHeight = filter.getHeight();
        final double compactWidth = filter.getWidth();
        final TagCheckBoxPane compact = (TagCheckBoxPane) filter.getChildren().getFirst();
        final var filterBounds = filter.localToScreen(filter.getBoundsInLocal());
        final CheckBox firstCompact = (CheckBox) compact.getChildren().getFirst();
        final CheckBox secondCompact = (CheckBox) compact.getChildren().get(1);
        final CheckBox thirdCompact = (CheckBox) compact.getChildren().get(2);
        final CheckBox fourthCompact = (CheckBox) compact.getChildren().get(3);
        Assertions.assertEquals(firstCompact.getLayoutY(), secondCompact.getLayoutY(),
            "Compact width=" + compact.getWidth() + ", checkbox width=" + firstCompact.getWidth()
                + ", insets=" + compact.getInsets());
        Assertions.assertTrue(thirdCompact.getLayoutY() > firstCompact.getLayoutY());
        Assertions.assertEquals(thirdCompact.getLayoutY(), fourthCompact.getLayoutY());
        filter.getOnMouseEntered().handle(null);

        final Popup popup = (Popup) Window.getWindows().stream()
            .filter(window -> window instanceof Popup && window.isShowing()).findFirst()
            .orElseThrow();
        Assertions.assertFalse(compact.isVisible());
        Assertions.assertTrue(compact.isManaged());
        Assertions.assertFalse(((Label) filter.getChildren().get(1)).isVisible());
        Assertions.assertEquals(compactHeight, filter.getHeight());
        Assertions.assertEquals(compactWidth, filter.getWidth());
        Assertions.assertEquals(stage.getScene().getStylesheets(),
            popup.getScene().getStylesheets());
        final VBox popupContent = (VBox) popup.getContent().getFirst();
        final TagCheckBoxPane expanded = (TagCheckBoxPane) popupContent.getChildren().getFirst();
        Assertions.assertEquals(22, expanded.getPrefWidth());
        Assertions.assertEquals(8, expanded.getChildren().size());
        popupContent.applyCss();
        popupContent.layout();
        final CheckBox expandedFirst = (CheckBox) expanded.getChildren().getFirst();
        Assertions.assertEquals(filterBounds.getMinX(), popup.getX(), 2);
        Assertions.assertEquals(filterBounds.getMaxY(), popup.getY() + popup.getHeight(), 2);

        final CheckBox last = (CheckBox) expanded.getChildren().getLast();
        last.setSelected(true);
        Assertions.assertTrue(filter.getValue().get(7));
        final CheckBox compactFirst = (CheckBox) ((TagCheckBoxPane) filter.getChildren()
            .getFirst()).getChildren().getFirst();
        compactFirst.setSelected(true);
        Assertions.assertTrue(filter.getValue().get(0));
        Assertions.assertTrue(expandedFirst.isSelected());
        expandedFirst.setSelected(false);
        Assertions.assertFalse(compactFirst.isSelected());
        expandedFirst.setSelected(true);
        Assertions.assertTrue(compactFirst.isSelected());
        Assertions.assertTrue(expandedFirst.isSelected());
        popup.hide();
        Assertions.assertTrue(compact.isVisible());
        Assertions.assertTrue(((Label) filter.getChildren().get(1)).isVisible());
      } finally {
        stage.hide();
      }
    });
  }
}
