/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.util.presets.manage;

import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_SPACE;
import static io.github.mzmine.javafx.components.util.FxLayout.newBorderPane;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.util.presets.Preset;
import io.github.mzmine.util.presets.PresetCategory;
import io.github.mzmine.util.presets.PresetStore;
import io.github.mzmine.util.presets.manage.ManagePresetsEvent.AddDefaultPresetsEvent;
import io.github.mzmine.util.presets.manage.ManagePresetsEvent.RemoveSelectedPresetsEvent;
import io.github.mzmine.util.presets.manage.ManagePresetsEvent.RenameSelectedPresetEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

class ManagePresetsViewBuilder extends FxViewBuilder<ManagePresetsModel> {

  protected ManagePresetsViewBuilder(ManagePresetsModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final Node treeView = createCategoryTreeView();

    final Pane selectedGroupPane = createSelectedGroupPane();
    SplitPane splitPane = new SplitPane(treeView, selectedGroupPane);
    splitPane.setDividerPositions(0.25);
    SplitPane.setResizableWithParent(treeView, false); // focus resize on center

    final BorderPane content = FxLayout.newBorderPane(splitPane);
    content.setTop(createTitle());
    return content;
  }

  private Node createTitle() {
    final StringExpression title = Bindings.concat(
        "(Presets are in beta phase) Selected preset group: ", model.selectedGroupStoreProperty()
            .map(store -> store != null ? store.getPresetGroup() : ""));
    return FxLabels.newBoldTitle(title);
  }

  private @NotNull Node createCategoryTreeView() {
    final TreeItem<Object> root = new TreeItem<>("Preset categories & groups:");
    TreeView<Object> treeView = new TreeView<>(root);
    treeView.setShowRoot(true);

    model.getAllStores()
        .addListener((ListChangeListener<? super PresetStore<?>>) _ -> allStoresChanged(root));

    // select and scroll to group
    model.selectedGroupStoreProperty().subscribe((selectedGroup) -> {
      scrollToStoreTreeItem(treeView, selectedGroup);
    });

    treeView.getSelectionModel().selectedItemProperty().subscribe((_, newItem) -> {
      if (newItem instanceof StoreGroupItem storeItem) {
        model.setSelectedGroupStore(storeItem.getStore());
      }
    });
    return newBorderPane(treeView);
  }

  private static void scrollToStoreTreeItem(TreeView<Object> treeView,
      PresetStore<?> selectedGroup) {
    for (TreeItem<Object> category : treeView.getRoot().getChildren()) {
      for (TreeItem<Object> item : category.getChildren()) {
        if (item instanceof StoreGroupItem storeItem) {
          if (storeItem.getStore().equals(selectedGroup)) {
            treeView.getSelectionModel().select(item);
            final int index = treeView.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
              treeView.scrollTo(index);
            }
            return;
          }
        }
      }
    }
  }

  static class StoreGroupItem extends TreeItem<Object> {

    private final PresetStore<?> store;

    StoreGroupItem(PresetStore<?> store) {
      super(store.getPresetGroup());
      this.store = store;
    }

    public PresetStore<?> getStore() {
      return store;
    }
  }

  private void allStoresChanged(TreeItem<Object> root) {
    rebuildTree(root);
  }

  private void rebuildTree(TreeItem<Object> root) {
    final Map<PresetCategory, List<PresetStore<?>>> categories = model.getAllStores().stream()
        .collect(Collectors.groupingBy(PresetStore::getPresetCategory));

    List<TreeItem<Object>> catItems = new ArrayList<>();

    for (var entry : categories.entrySet()) {
      final PresetCategory category = entry.getKey();
      TreeItem<Object> categoryItem = new TreeItem<>(category);
      catItems.add(categoryItem);
      categoryItem.setExpanded(true);

      for (PresetStore<?> group : entry.getValue()) {
        StoreGroupItem groupItem = new StoreGroupItem(group);
        categoryItem.getChildren().add(groupItem);
      }
    }
    root.getChildren().setAll(catItems);
    root.setExpanded(true);
  }

  /**
   * Center pane
   */
  private @NotNull Pane createSelectedGroupPane() {
    // auto sort
    final SortedList<Preset> sorted = model.getSelectedGroupStorePresets()
        .sorted(Preset::compareTo);
    ListView<Preset> presetsView = new ListView<>(sorted);
    presetsView.setPrefWidth(500);

    // create placeholder for preset edit component
    final StackPane presetEditorHolder = new StackPane();
    final BooleanExpression visible = Bindings.isNotEmpty(presetEditorHolder.getChildren());
    presetEditorHolder.visibleProperty().bind(visible);
    presetEditorHolder.managedProperty().bind(visible);

    final TextField nameField = FxTextFields.newAutoGrowTextField(null,
        "Change the name and click rename");

    final Button renameButton = FxButtons.createButton("Rename", FxIcons.EDIT,
        "Edit name of selected preset (select with arrow keys up/down).",
        () -> renameClicked(presetsView));

    renameButton.disableProperty().bind(model.selectedPresetProperty()
        .flatMap(editPresetModel -> editPresetModel.modifiedProperty().not()).orElse(false));

    // layout button bar
    final Button defaultsButton = FxButtons.createButton("Add defaults", FxIcons.ADD,
        "Add all defaults that are missing (either deleted or new defaults)",
        this::addDefaultsClicked);
    defaultsButton.visibleProperty().bind(model.storeHasDefaultsProperty());
    defaultsButton.managedProperty().bind(model.storeHasDefaultsProperty());

    final HBox buttonBar = FxLayout.newHBox(new Insets(0, 0, DEFAULT_SPACE, 0), //
        nameField, renameButton, //
        FxButtons.createButton("Remove", FxIcons.X_CIRCLE,
            "Remove selected preset (select with arrow keys up/down) or press delete key to remove.",
            () -> removeClicked(presetsView)), //
        defaultsButton //
    );

    model.presetEditorProperty().subscribe(editor -> {
      if (editor == null) {
        presetEditorHolder.getChildren().clear();
        return;
      }
      presetEditorHolder.getChildren().setAll(editor.getEditorNode());
    });

    bindSelectedPreset(presetsView, nameField);

    final VBox vBox = FxLayout.newVBox(Pos.TOP_LEFT, buttonBar, presetEditorHolder, presetsView);
    VBox.setVgrow(presetsView, Priority.ALWAYS);
    return vBox;
  }

  private void bindSelectedPreset(ListView<Preset> presetsView, TextField nameField) {
    presetsView.getSelectionModel().selectedItemProperty().subscribe(model::setSelectedPreset);

    model.selectedPresetProperty().subscribe((old, nv) -> {
      if (old != null) {
        nameField.textProperty().unbindBidirectional(old.nameProperty());
      }
      if (nv != null) {
        nameField.textProperty().bindBidirectional(nv.nameProperty());
      }
      presetsView.getSelectionModel().select(nv == null ? null : nv.getOriginalPreset());
    });
  }

  private void addDefaultsClicked() {
    model.consumeEvent(new AddDefaultPresetsEvent());
  }

  private void removeClicked(ListView<Preset> presetsView) {
    final ObservableList<Preset> selected = presetsView.getSelectionModel().getSelectedItems();
    if (selected.isEmpty()) {
      return;
    }
    // list copy of otherwise inplace will cause exception
    model.consumeEvent(new RemoveSelectedPresetsEvent(List.copyOf(selected)));
  }

  private void renameClicked(ListView<Preset> presetsView) {
    final Preset selected = presetsView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    model.consumeEvent(new RenameSelectedPresetEvent(model.getSelectedPreset(), false));
  }
}
