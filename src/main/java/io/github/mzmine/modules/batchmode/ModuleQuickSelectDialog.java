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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ModuleQuickSelectDialog extends Stage {

  private static ModuleQuickSelectDialog instance;

  private final BatchModuleTreePane moduleTree;

  private ModuleQuickSelectDialog() {
    Stage mainWindow = MZmineCore.getDesktop().getMainWindow();
    initOwner(mainWindow);
    // cannot use modal when closing on lost focus
//    initModality(Modality.APPLICATION_MODAL);
    initStyle(StageStyle.UNDECORATED);

    moduleTree = new BatchModuleTreePane(true);
    BorderPane root = new BorderPane(moduleTree);
    Scene scene = new Scene(root);
    scene.getStylesheets().addAll(mainWindow.getScene().getStylesheets());
    this.setScene(scene);

    scene.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        hide();
        event.consume();
      }
    });
    // auto close when clicked somewhere
    focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
      if (!isNowFocused) {
        hide();
      }
    });

    moduleTree.setOnAddModuleEventHandler(this::runModuleAndHide);
    moduleTree.setCloseButtonEventHandler(this::hide);
    moduleTree.setMinWidth(600);
    moduleTree.setPrefWidth(600);

  }

  public static synchronized ModuleQuickSelectDialog getInstance() {
    if (instance == null) {
      instance = new ModuleQuickSelectDialog();
    }
    return instance;
  }

  public static void openQuickSearch() {
    getInstance().showAndWait();
  }

  private void runModuleAndHide(final MZmineRunnableModule module) {
    moduleTree.clearSearchText();
    hide();
    MZmineCore.setupAndRunModule(module.getClass());
  }

  @Override
  public void showAndWait() {
    if (isShowing()) {
      return;
    }
    getScene().getStylesheets()
        .setAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    centerOnScreen();
    moduleTree.focusSearchField();
    super.show();
  }

}
