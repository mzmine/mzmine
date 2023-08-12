/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.gui.framework.fx.FilterableTreeItem;
import io.github.mzmine.gui.framework.fx.TreeItemPredicate;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineModuleCategory.MainCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;

public class BatchModuleTreePane extends BorderPane {

  private static final Logger logger = Logger.getLogger(BatchModuleTreePane.class.getName());
  // by using linked hash map, the items will be added to the tree view as specified in the modules list
  private final Map<MainCategory, FilterableTreeItem<Object>> mainCategoryItems = new LinkedHashMap<>();
  private final Map<MZmineModuleCategory, FilterableTreeItem<Object>> categoryItems = new LinkedHashMap<>();
  private final TreeView<Object> treeView = new TreeView<>();
  private final TextField searchField = new TextField();

  private Consumer<MZmineProcessingModule> eventHandler;

  public BatchModuleTreePane() {
    BorderPane bottom = new BorderPane(searchField);
    bottom.setLeft(new Label("Search "));

    setCenter(treeView);
    setBottom(bottom);

    // set data to tree
    for (Class<? extends MZmineProcessingModule> moduleClass : BatchModeModulesList.MODULES) {
      final MZmineProcessingModule module = MZmineCore.getModuleInstance(moduleClass);
      final MZmineModuleCategory category = module.getModuleCategory();
      final FilterableTreeItem<Object> categoryItem = categoryItems.computeIfAbsent(category, c -> {
        final FilterableTreeItem<Object> item = new FilterableTreeItem<>(c);
        final FilterableTreeItem<Object> mainItem = mainCategoryItems.computeIfAbsent(
            c.getMainCategory(), FilterableTreeItem::new);
        mainItem.getSourceChildren().add(item);
        return item;
      });
      categoryItem.getSourceChildren()
          .add(new FilterableTreeItem<>(new BatchModuleWrapper(module)));
    }

    final FilterableTreeItem<Object> originalRoot = new FilterableTreeItem<>("Root");
    originalRoot.getSourceChildren().addAll(mainCategoryItems.values());
    treeView.setRoot(originalRoot);
    treeView.setShowRoot(false);

    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      // filter, expand, and select
      TreeItemPredicate<Object> predicate = TreeItemPredicate.createSubStringPredicate(
          newValue.split(" "));

      var firstMatchingNode = originalRoot.expandAllMatches(predicate);
      treeView.getSelectionModel().select(firstMatchingNode);
    });

    searchField.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        event.consume();
        addSelectedModule();
      }
    });

    treeView.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
        e.consume();
        addSelectedModule();
      }
    });

    treeView.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        event.consume();
        addSelectedModule();
      }
    });

  }

  public void setOnAddModuleEventHandler(final Consumer<MZmineProcessingModule> eventHandler) {
    this.eventHandler = eventHandler;
  }

  /**
   * Module in tree was clicked or otherwise activated. Fire event to handler
   */
  public void addSelectedModule() {
    if (eventHandler != null) {
      // Processing module selected?
      TreeItem<Object> selectedNode = treeView.getSelectionModel().getSelectedItem();
      if (selectedNode == null || selectedNode.getValue() == null) {
        return;
      }
      final Object selectedItem = selectedNode.getValue();
      if (!(selectedItem instanceof BatchModuleWrapper wrappedModule)) {
        return;
      }

      if (!(wrappedModule.getModule() instanceof MZmineProcessingModule module)) {
        logger.finest(
            "Cannot add module that is not an MZmineProcessingModule " + wrappedModule.toString());
        return;
      }

      eventHandler.accept(module);
    }
  }


}
