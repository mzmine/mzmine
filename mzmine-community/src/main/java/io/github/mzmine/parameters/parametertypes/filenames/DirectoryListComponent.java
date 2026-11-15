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

package io.github.mzmine.parameters.parametertypes.filenames;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxPopOvers;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.util.IconCodeSupplier;
import io.github.mzmine.javafx.validation.DecorationTargetProvider;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single-row preview of an ordered directory list. The popup edits the same value property used by
 * parameter dialogs, including changes that leave the first directory unchanged.
 */
public class DirectoryListComponent extends HBox implements ValuePropertyComponent<File[]>,
    DecorationTargetProvider {

  private static final double CELL_HEIGHT = 20;
  private static final int ICON_HEIGHT = 15;

  private final ObjectProperty<File[]> value = new SimpleObjectProperty<>(new File[0]);
  private final CustomTextField preview = new CustomTextField();
  private final Label more = new Label();
  private final ListView<File> list = new ListView<>();
  private final PopOver popup = FxPopOvers.newPopOver(list);

  public DirectoryListComponent() {
    super(FxLayout.DEFAULT_SPACE);
    setAlignment(Pos.CENTER_LEFT);
    setMaxHeight(USE_PREF_SIZE);

    preview.setId("directory-preview");
    preview.setEditable(false);
    preview.setPrefColumnCount(25);
    preview.setMinWidth(100);
    preview.setCursor(Cursor.HAND);
    preview.setPromptText("No directories selected");
    preview.setAccessibleText("Directories in priority order");
    preview.setAccessibleHelp(
        "Click to reorder or remove directories. Use plus to add a directory.");
    more.setId("more-directories");
    more.setDisable(true);
    more.setMouseTransparent(true);
    more.setPadding(new Insets(0, 6, 0, 4));
    preview.setRight(more);
    preview.setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY) {
        togglePopup();
      }
    });
    preview.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE
          || event.getCode() == KeyCode.DOWN) {
        togglePopup();
        event.consume();
      }
    });

    Button add = compactButton(FxIcons.PLUS, "Add directory", this::chooseDirectory);
    add.setId("add-directory");
    add.setAccessibleText("Add directory");
    add.prefHeightProperty().bind(preview.heightProperty());
    add.setMinHeight(USE_PREF_SIZE);
    add.setMaxHeight(USE_PREF_SIZE);
    getChildren().setAll(preview, add);
    HBox.setHgrow(preview, Priority.ALWAYS);

    list.setId("directory-list");
    list.setFixedCellSize(CELL_HEIGHT);
    list.setPlaceholder(new Label("Use + to add a directory"));
    list.setCellFactory(_ -> new DirectoryCell());
    list.setOnKeyPressed(event -> {
      int index = list.getSelectionModel().getSelectedIndex();
      if (event.getCode() == KeyCode.DELETE && index >= 0) {
        removeDirectory(index);
        event.consume();
      }
    });
    disabledProperty().addListener((_, _, disabled) -> {
      if (disabled) {
        popup.hide();
      }
    });
    sceneProperty().addListener((_, _, scene) -> {
      if (scene == null) {
        popup.hide();
      }
    });
    value.addListener((_, _, _) -> updatePreview());
    updatePreview();
  }

  @Override
  public ObjectProperty<File[]> valueProperty() {
    return value;
  }

  public File[] getValue() {
    File[] directories = value.get();
    return directories == null ? new File[0] : directories.clone();
  }

  public void setValue(@Nullable File[] directories) {
    value.set(directories == null ? new File[0]
        : Arrays.stream(directories).filter(Objects::nonNull).toArray(File[]::new));
  }

  public void addDirectory(@NotNull File directory) {
    var directories = new ArrayList<>(Arrays.asList(getValue()));
    if (!directories.contains(directory)) {
      directories.add(directory);
      setValue(directories.toArray(File[]::new));
    }
  }

  private void removeDirectory(int index) {
    var directories = new ArrayList<>(Arrays.asList(getValue()));
    if (index >= 0 && index < directories.size()) {
      directories.remove(index);
      setValue(directories.toArray(File[]::new));
      list.getSelectionModel().select(Math.min(index, directories.size() - 1));
    }
  }

  private void moveDirectory(int index, int destination) {
    var directories = new ArrayList<>(Arrays.asList(getValue()));
    if (index >= 0 && index < directories.size() && destination >= 0
        && destination < directories.size()) {
      directories.add(destination, directories.remove(index));
      setValue(directories.toArray(File[]::new));
      list.getSelectionModel().select(destination);
    }
  }

  private void updatePreview() {
    File[] directories = getValue();
    preview.setText(directories.length == 0 ? "" : directories[0].getPath());
    preview.positionCaret(0);
    more.setText("(" + Math.max(0, directories.length - 1) + " more)");
    more.setVisible(directories.length > 1);
    more.setManaged(directories.length > 1);
    list.setItems(FXCollections.observableArrayList(directories));
    list.setPrefHeight(Math.max(2, Math.min(8, directories.length)) * CELL_HEIGHT + 2);
  }

  private void togglePopup() {
    if (popup.isShowing()) {
      popup.hide();
    } else if (getScene() != null && getScene().getWindow() != null && getScene().getWindow()
        .isShowing()) {
      list.setPrefWidth(Math.max(480, preview.getWidth()));
      popup.show(this);
      list.requestFocus();
    }
  }

  private void chooseDirectory() {
    popup.hide();
    DirectoryChooser chooser = new DirectoryChooser();
    chooser.setTitle("Add directory");
    File[] directories = getValue();
    File initialDirectory = directories.length == 0 ? null : directories[0].getAbsoluteFile();
    while (initialDirectory != null && !initialDirectory.isDirectory()) {
      initialDirectory = initialDirectory.getParentFile();
    }
    if (initialDirectory != null) {
      chooser.setInitialDirectory(initialDirectory);
    }
    File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
    if (selected != null) {
      addDirectory(selected);
    }
  }

  @Override
  public @NotNull Node getDecorationTarget() {
    return preview;
  }

  private static Button compactButton(IconCodeSupplier icon, String tooltip, Runnable action) {
    Button button = FxButtons.createButton(FxIconUtil.getFontIcon(icon, ICON_HEIGHT), tooltip,
        action);
    button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    // The theme's regular button padding otherwise makes buttons taller than the fixed list rows.
    button.setStyle("-fx-padding: 0 4;");
    return button;
  }

  private final class DirectoryCell extends ListCell<File> {

    private final Label index = new Label();
    private final Label path = new Label();
    private final Button up = compactButton(FxIcons.ARROW_UP, "Move directory up",
        () -> moveDirectory(getIndex(), getIndex() - 1));
    private final Button down = compactButton(FxIcons.ARROW_DOWN, "Move directory down",
        () -> moveDirectory(getIndex(), getIndex() + 1));
    private final Button remove = compactButton(FxIcons.X, "Remove directory",
        () -> removeDirectory(getIndex()));
    private final HBox row = new HBox(4, index, path, up, down, remove);

    private DirectoryCell() {
      setStyle("-fx-padding: 0 4;");
      up.setId("move-up");
      down.setId("move-down");
      remove.setId("remove-directory");
      up.setAccessibleText("Move directory up");
      down.setAccessibleText("Move directory down");
      remove.setAccessibleText("Remove directory");
      index.setMinWidth(24);
      path.setMinWidth(0);
      path.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(path, Priority.ALWAYS);
      row.setAlignment(Pos.CENTER_LEFT);
      row.prefWidthProperty().bind(widthProperty().subtract(16));
      indexProperty().addListener((_, _, _) -> updateControls());
    }

    @Override
    protected void updateItem(File directory, boolean empty) {
      super.updateItem(directory, empty);
      setText(null);
      if (empty || directory == null) {
        setGraphic(null);
        setTooltip(null);
      } else {
        path.setText(directory.getPath());
        setTooltip(new Tooltip(directory.getPath()));
        setGraphic(row);
      }
      updateControls();
    }

    private void updateControls() {
      index.setText((getIndex() + 1) + ".");
      up.setDisable(getIndex() <= 0);
      down.setDisable(getIndex() < 0 || getIndex() >= list.getItems().size() - 1);
    }
  }
}
