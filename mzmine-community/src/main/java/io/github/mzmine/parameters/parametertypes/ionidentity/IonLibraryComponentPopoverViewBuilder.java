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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.factories.FxLabels.newItalicLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static javafx.scene.input.KeyCode.ENTER;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.fx.IonLibraryListView;
import io.github.mzmine.datamodel.identities.fx.IonTypeListView;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.util.concurrent.atomic.AtomicLong;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the content shown inside the {@link IonLibraryComponentPopover}: on the left a summary of
 * the selected library plus a read-only preview of its ions; on the right an
 * {@link IonLibraryListView} to pick a different library.
 */
public class IonLibraryComponentPopoverViewBuilder extends
    FxViewBuilder<IonLibraryComponentPopoverModel> {

  private final @Nullable Runnable onCloseRequested;
  private @Nullable IonLibraryListView libraryList;
  private final @NotNull ObservableList<IonType> previewIons = FXCollections.observableArrayList();

  public IonLibraryComponentPopoverViewBuilder(IonLibraryComponentPopoverModel model,
      @Nullable Runnable onCloseRequested) {
    super(model);
    this.onCloseRequested = onCloseRequested;
  }

  @Override
  public Region build() {
    final BorderPane root = new BorderPane();
    root.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    root.setMinHeight(400);
    root.setPrefWidth(900);
    root.setPrefHeight(600);

    final SplitPane split = new SplitPane(buildLeftSelectedLibInfoPane(), buildLibraryList());
    split.setDividerPositions(0.5);
    root.setCenter(split);

    return root;
  }

  private Region buildLeftSelectedLibInfoPane() {
    model.selectedLibraryProperty().subscribe(this::refreshPreviewIons);

    final IonTypeListView ionPreview = new IonTypeListView(previewIons, false);
    ionPreview.setSearchFieldVisible(false);
    ionPreview.getListView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    final VBox left = FxLayout.newVBox(Insets.EMPTY, buildInfoPane(), ionPreview);
    VBox.setVgrow(ionPreview, Priority.ALWAYS);
    return left;
  }

  private Region buildInfoPane() {
    final ObservableValue<@NotNull String> selectedName = model.selectedLibraryProperty()
        .map(IonLibrary::name).orElse("No library selected");
    final ObservableValue<@NotNull String> numIonsSummary = model.selectedLibraryProperty()
        .map(IonLibraryComponentPopoverViewBuilder::summarizeIonCounts).orElse("");
    final ObservableValue<@NotNull String> sourceInfo = model.selectedLibraryProperty()
        .map(IonLibraryComponentPopoverViewBuilder::describeSource).orElse("");

    final Label title = newBoldTitle(selectedName);
    final Label counts = newLabel(numIonsSummary);
    final Label source = newItalicLabel(sourceInfo);

    // reserve whitespace below the info block before the preview list
    final VBox info = FxLayout.newVBox(new Insets(0, 0, FxLayout.DEFAULT_SPACE, 0), //
        newLabel(Styles.BOLD_SEMI_TITLE, "Selected library:"), //
        title, counts, source);

    return info;
  }

  private Region buildLibraryList() {
    libraryList = new IonLibraryListView(model.librariesProperty(), false, false, false);
    libraryList.getListView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    libraryList.addEventListener(event -> {
      switch (event.type()) {
        case ITEM_ACTIVATED -> {
          if (!event.selectedItems().isEmpty()) {
          model.setSelectedLibrary(event.selectedItems().getFirst());
          }
        }
        case CREATE_NEW, EDIT, IMPORT, EXPORT -> {
          // nothing to do here. those are exclusive for the global ion pane
        }
      }
    });


    final ButtonBase closeButton = FxIconUtil.newIconButton(FxIcons.X_CIRCLE, "Close pane",
        onCloseRequested);

    final var info = FxLayout.newHBox(new Insets(0, 0, FxLayout.DEFAULT_SPACE, 0), //
        newLabel(Styles.BOLD_SEMI_TITLE, "Other available libraries:"), FxLayout.newHVFillSpacer(),
        closeButton);

    final BorderPane mainLib = FxLayout.newBorderPane(new Insets(0, 0, 0, FxLayout.DEFAULT_SPACE),
        libraryList);
    mainLib.setTop(info);

    // Close on double-click
    libraryList.getListView().setOnMouseClicked(event -> {
      if (event.getClickCount() == 2 && onCloseRequested != null) {
        onCloseRequested.run();
      }
    });

    // Close on double Enter key press / use event filter to always get the event
    final AtomicLong lastEnterTime = new AtomicLong(0);
    mainLib.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == ENTER) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnterTime.get() < 250 && onCloseRequested != null) {
          onCloseRequested.run();
        }
        lastEnterTime.set(currentTime);
      }
    });

    return mainLib;
  }

  private void refreshPreviewIons(@Nullable IonLibrary lib) {
    if (lib == null) {
      previewIons.clear();
    } else {
      previewIons.setAll(lib.ions());
    }
  }

  /**
   * Request focus on the search field. Called by the controller after the popover is shown so the
   * user can immediately start typing to filter the list.
   */
  public void focusSearchField() {
    if (libraryList != null) {
      libraryList.focusSearchField();
    }
  }

  /**
   * Align the list selection with the current model value. Intended to be called when the popover
   * is shown so the currently active library is highlighted.
   */
  public void syncListSelectionFromModel() {
    if (libraryList == null) {
      return;
    }
    final IonLibrary current = model.getSelectedLibrary();
    if (current == null) {
      libraryList.getListView().getSelectionModel().clearSelection();
    } else {
      libraryList.getListView().getSelectionModel().select(current);
      final int idx = libraryList.getListView().getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        libraryList.getListView().scrollTo(idx);
      }
    }
  }

  private static @NotNull String summarizeIonCounts(@NotNull IonLibrary lib) {
    long positives = 0;
    long negatives = 0;
    long neutrals = 0;
    for (IonType ion : lib.ions()) {
      final PolarityType polarity = ion.getPolarity();
      if (polarity == PolarityType.POSITIVE) {
        positives++;
      } else if (polarity == PolarityType.NEGATIVE) {
        negatives++;
      } else if (polarity == PolarityType.NEUTRAL) {
        neutrals++;
      }
    }
    return "Total entries: %d (%d positive / %d negative / %d neutral)".formatted(lib.getNumIons(),
        positives, negatives, neutrals);
  }

  private static @NotNull String describeSource(@NotNull IonLibrary lib) {
    if (IonLibraries.isInternalLibrary(lib)) {
      return "Built-in mzmine default library.";
    }
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    final IonLibrary inGlobal = global.getLibraryForName(lib.name()).orElse(null);
    if (inGlobal == null) {
      return "Externally loaded library – not in the global ion library list.";
    }
    if (inGlobal.equalIons(lib)) {
      return "From global ion library list.";
    }
    return "Local variant – differs from the global library with the same name.";
  }
}
