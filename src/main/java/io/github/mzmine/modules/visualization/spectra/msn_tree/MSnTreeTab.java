/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.msn_tree;

import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MSnTreeTab extends SimpleTab {

  private final AtomicLong currentThread = new AtomicLong(0L);
  private TreeView<PrecursorIonTreeNode> treeView;
  private VBox spectraPane;
  private int lastSelectedItem = -1;

  public MSnTreeTab() {
    super("MSn Tree", true, false);

    BorderPane main = new BorderPane();

    // add tree to the left
    // buttons over tree
    HBox buttons = new HBox(5, // add buttons
        createButton("Expand", e -> expandTreeView(true)),
        createButton("Collapse", e -> expandTreeView(false)));

    treeView = new TreeView<>();
    ScrollPane treeScroll = new ScrollPane(treeView);
    //    treeScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
    treeScroll.setFitToHeight(true);
    treeScroll.setFitToWidth(true);

    TreeItem<PrecursorIonTreeNode> root = new TreeItem<>();
    root.setExpanded(true);
    treeView.setRoot(root);
    treeView.setShowRoot(false);
    treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    treeView.getSelectionModel().selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> showSpectra(newValue)));

    BorderPane left = new BorderPane();
    left.setTop(buttons);
    left.setCenter(treeScroll);

    // create spectra grid
    spectraPane = new VBox();
    ScrollPane scrollSpectra = new ScrollPane(spectraPane);
    scrollSpectra.setFitToHeight(true);

    SplitPane splitPane = new SplitPane(left, scrollSpectra);
    splitPane.setDividerPositions(0.22);
    main.setCenter(splitPane);

    // add main to tab
    main.getStyleClass().add("region-match-chart-bg");
    this.setContent(main);

    main.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.DOWN) {
        nextPrecursor();
        main.requestFocus();
        e.consume();
      } else if (e.getCode() == KeyCode.UP) {
        previousPrecursor();
        main.requestFocus();
        e.consume();
      }
    });
  }

  private Button createButton(String title, EventHandler<ActionEvent> action) {
    final Button button = new Button(title);
    button.setOnAction(action);
    return button;
  }

  /**
   * Set raw data file and update tree
   *
   * @param raw update all views to this raw file
   */
  public synchronized void setRawDataFile(RawDataFile raw) {
    lastSelectedItem = -1;
    treeView.getRoot().getChildren().clear();
    spectraPane.getChildren().clear();

    // track current thread
    final long current = currentThread.incrementAndGet();
    Thread thread = new Thread(() -> {
      // run on different thread
      final List<PrecursorIonTree> trees = ScanUtils.getMSnFragmentTrees(raw);
      MZmineCore.runLater(() -> {
        if (current == currentThread.get()) {

          treeView.getRoot().getChildren()
              .addAll(trees.stream().map(t -> createTreeItem(t.getRoot())).toList());

          expandTreeView(treeView.getRoot(), true);
        }
      });
    });
    thread.start();
  }

  private TreeItem<PrecursorIonTreeNode> createTreeItem(PrecursorIonTreeNode node) {
    final var item = new TreeItem<>(node);
    item.getChildren()
        .addAll(node.getChildPrecursors().stream().map(this::createTreeItem).toList());
    return item;
  }

  private void showSpectra(TreeItem<PrecursorIonTreeNode> node) {
    showSpectra(node == null ? null : node.getValue());
  }

  public void showSpectra(PrecursorIonTreeNode any) {

    spectraPane.getChildren().clear();
    // add spectra
    if (any != null) {
      PrecursorIonTreeNode root = any.getRoot();

    }
  }

  private void expandTreeView(boolean expanded) {
    expandTreeView(treeView.getRoot(), expanded);
  }

  private void expandTreeView(TreeItem<?> item, boolean expanded) {
    if (item != null && !item.isLeaf()) {
      item.setExpanded(expanded);
      for (TreeItem<?> child : item.getChildren()) {
        expandTreeView(child, expanded);
      }
    }
    treeView.getRoot().setExpanded(true);
  }

  public void previousPrecursor() {
    if (lastSelectedItem > 0) {
      lastSelectedItem--;
      treeView.getSelectionModel().select(getMS2Nodes().get(lastSelectedItem));
    }
  }

  private ObservableList<TreeItem<PrecursorIonTreeNode>> getMS2Nodes() {
    return treeView.getRoot().getChildren();
  }

  public void nextPrecursor() {
    if (lastSelectedItem + 1 < getMS2Nodes().size()) {
      lastSelectedItem++;
      treeView.getSelectionModel().select(treeView.getRoot().getChildren().get(lastSelectedItem));
    }
  }


  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if (rawDataFiles != null && rawDataFiles.size() > 0) {
      setRawDataFile(rawDataFiles.stream().findFirst().get());
    }
  }
}
