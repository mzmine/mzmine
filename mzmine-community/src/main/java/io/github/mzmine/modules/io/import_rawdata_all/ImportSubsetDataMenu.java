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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxPopOvers;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesComponent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// a menu bar to import or subset files directly or as a subset currently only used in wizard - in
/// data import module we would need a way to reuse the correct import parameters
public class ImportSubsetDataMenu extends HBox {

  private static final int DEFAULT_SUBSET_SIZE = 10;

  public ImportSubsetDataMenu(final @NotNull FileNamesComponent component) {
    final ButtonBase quickImportButton = FxIconUtil.newIconButton(FxIcons.LOAD,
        "Quick import selected files", () -> importFiles(component.getValue()));
    final ButtonBase subsetButton = FxIconUtil.newIconButton(FxIcons.FILTER,
        "Import a random subset of selected files");
    final IntegerComponent subsetSizeComponent = new IntegerComponent(65, 1, null);
    subsetSizeComponent.setText(String.valueOf(DEFAULT_SUBSET_SIZE));
    subsetSizeComponent.setToolTipText("Number of files to import");

    final TextArea subsetTextArea = new TextArea();
    subsetTextArea.setPrefColumnCount(55);
    subsetTextArea.setPrefRowCount(10);
    subsetTextArea.setMinHeight(500);
    subsetTextArea.setPrefHeight(500);
    subsetTextArea.setEditable(false);
    subsetTextArea.setFont(FileNamesComponent.smallFont);

    final PopOver subsetPopOver = FxPopOvers.newPopOver(null);
    final Button importSubsetButton = FxButtons.createButton("Import subset", FxIcons.LOAD,
        "Import the current subset", () -> {
          if (importFiles(filesFromText(subsetTextArea.getText()))) {
            subsetPopOver.hide();
          }
        });

    final ButtonBase rerollButton = FxIconUtil.newIconButton(FxIcons.RELOAD,
        "Redo random subsampling",
        () -> updateSubsetSelection(component, subsetSizeComponent, subsetTextArea));
    final ButtonBase cancelButton = FxIconUtil.newIconButton(FxIcons.X, "Cancel",
        subsetPopOver::hide);

    subsetSizeComponent.getChildren().addFirst(new Label("Files"));
    subsetSizeComponent.getChildren().add(rerollButton);

    final HBox actionRow = FxLayout.newHBox(Pos.CENTER_RIGHT, Insets.EMPTY, importSubsetButton,
        cancelButton);
    final VBox content = FxLayout.newVBox(Pos.CENTER_LEFT, FxLayout.DEFAULT_PADDING_INSETS, true,
        subsetSizeComponent, subsetTextArea, actionRow);
    subsetPopOver.setContentNode(content);

    subsetSizeComponent.addValueChangedListener(
        () -> updateSubsetSelection(component, subsetSizeComponent, subsetTextArea));
    subsetButton.setOnAction(_ -> {
      updateSubsetSelection(component, subsetSizeComponent, subsetTextArea);
      if (subsetPopOver.showingProperty().get()) {
        subsetPopOver.hide();
      } else {
        subsetPopOver.show(subsetButton);
      }
    });

    // final parent layout
    setAlignment(Pos.CENTER);
    getChildren().addAll(quickImportButton, subsetButton);
  }

  private static void updateSubsetSelection(final @NotNull FileNamesComponent component,
      final @NotNull IntegerComponent subsetSizeComponent, final @NotNull TextArea subsetTextArea) {
    final File[] selectedFiles = Arrays.stream(component.getValue()).filter(Objects::nonNull)
        .toArray(File[]::new);
    final int subsetSize = getSubsetSize(subsetSizeComponent, selectedFiles.length);
    subsetTextArea.setText(filesToText(randomSubset(selectedFiles, subsetSize)));
  }

  private static int getSubsetSize(final @NotNull IntegerComponent subsetSizeComponent,
      final int fileCount) {
    if (fileCount <= 0) {
      return 0;
    }
    try {
      final int parsedValue = Integer.parseInt(subsetSizeComponent.getText());
      return Math.min(Math.max(parsedValue, 1), fileCount);
    } catch (NumberFormatException ex) {
      return Math.min(DEFAULT_SUBSET_SIZE, fileCount);
    }
  }

  private static @NotNull File[] randomSubset(final @NotNull File[] files, final int subsetSize) {
    if (subsetSize <= 0) {
      return new File[0];
    }

    final ArrayList<File> shuffledFiles = Arrays.stream(files).filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(shuffledFiles);
    return shuffledFiles.stream().limit(subsetSize).sorted(
        Comparator.comparing(File::getPath, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(File::getPath)).toArray(File[]::new);
  }

  private static @NotNull String filesToText(final @NotNull File[] files) {
    return Arrays.stream(files).filter(Objects::nonNull).map(File::getPath)
        .collect(Collectors.joining("\n"));
  }

  private static @NotNull File[] filesFromText(final @Nullable String text) {
    if (text == null || text.isBlank()) {
      return new File[0];
    }

    return text.lines().map(String::trim).filter(path -> !path.isBlank()).map(File::new)
        .toArray(File[]::new);
  }

  private static boolean importFiles(final @NotNull File[] selectedFiles) {
    final File[] files = Arrays.stream(selectedFiles).filter(Objects::nonNull).toArray(File[]::new);
    if (files.length == 0) {
      MZmineCore.getDesktop().displayErrorMessage("Select at least one file to import.");
      return false;
    }

    final ParameterSet parameters = AllSpectralDataImportParameters.create(
        ConfigService.getPreferences().getVendorImportParameters(), files, null, null);
    MZmineCore.runMZmineModule(AllSpectralDataImportModule.class, parameters);
    return true;
  }
}
