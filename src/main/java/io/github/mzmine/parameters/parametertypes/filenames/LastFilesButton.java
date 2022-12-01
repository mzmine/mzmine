/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.NamedArg;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import org.jetbrains.annotations.Nullable;

/**
 * A button with a pupup menu of files. Used as last files chooser
 *
 * @author Robin Schmid
 */
public class LastFilesButton extends Button implements LastFilesComponent {

  private ContextMenu menu;
  private List<File> lastFiles;
  // listens for click on one of the last files
  // consumer decides what to do
  private Consumer<File> changeListener;

  public LastFilesButton(Consumer<File> changeListener) {
    this("Last", changeListener);
  }

  public LastFilesButton(ImageView icon, Consumer<File> changeListener) {
    this("", icon, changeListener);
  }

  public LastFilesButton(String text, ImageView icon, Consumer<File> changeListener) {
    super(text, icon);
    this.changeListener = changeListener;
    init();
  }

  public LastFilesButton(String text, Consumer<File> changeListener) {
    super(text);
    this.changeListener = changeListener;
    init();
  }

  public LastFilesButton(@NamedArg("text") String text) {
    this(text, null);
  }

  private void init() {
    setTooltip(new Tooltip("Load last files"));
    menu = new ContextMenu();
    lastFiles = new ArrayList<>();
    setLastFiles(lastFiles);
    // show menu on click
    this.setOnAction(e -> {
      final Bounds boundsInScreen = this.localToScreen(this.getBoundsInLocal());
      menu.show(this, boundsInScreen.getCenterX(), boundsInScreen.getCenterY());
    });
  }

  public List<File> getLastFiles() {
    return lastFiles;
  }

  @Override
  public void setLastFiles(List<File> lastFiles) {
    this.lastFiles = lastFiles;

    menu.getItems().clear();
    if (lastFiles == null || lastFiles.isEmpty()) {
      setDisable(true);
      return;
    }
    setDisable(false);

    lastFiles.forEach(file -> {
      String name = fileToString(file);
      MenuItem item = new MenuItem(name);
      item.setUserData(file);
      item.setOnAction(e -> {
        MenuItem c = (MenuItem) e.getSource();
        if (c != null) {
          changeListener.accept((File) c.getUserData());
        }
      });
      menu.getItems().add(item);
    });
  }

  @Nullable
  public File getLastFile() {
    return lastFiles != null && lastFiles.size() > 0 ? lastFiles.get(0) : null;
  }

  private String fileToString(File f) {
    return MessageFormat.format("{0} ({1})", f.getName(), f.getParent());
  }

  public void addFile(File f) {
    if (f == null) {
      return;
    }

    // add to last files if not already inserted
    lastFiles.remove(f);
    lastFiles.add(0, f);
    setLastFiles(lastFiles);
  }

  public void setChangeListener(Consumer<File> changeListener) {
    this.changeListener = changeListener;
  }
}
