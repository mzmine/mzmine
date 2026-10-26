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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fxinitializer.InitJavaFX;
import io.github.mzmine.main.ConfigService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class DirectoryListComponentTest {

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
  }

  @Test
  void compactPreviewAndPopupActionsShareTheOrderedValue() throws Exception {
    runOnFxAndWait(() -> {
      var component = new DirectoryListComponent();
      File first = new File("C:\\data\\mzmine-temp");
      File second = new File("D:\\data\\mzmine-temp");
      File third = new File("E:\\data\\mzmine-temp");
      component.setValue(new File[]{first, second, third});
      var changes = new AtomicInteger();
      component.valueProperty().addListener((_, _, _) -> changes.incrementAndGet());
      CustomTextField preview = (CustomTextField) component.getDecorationTarget();
      Label more = (Label) preview.getRight();
      var root = new VBox(8, new Label("Temporary file directory"), component);
      root.setPadding(new Insets(12));
      var stage = new Stage();
      PopOver popup = null;
      try {
        Platform.setImplicitExit(false);
        stage.setScene(new Scene(root, 620, 90));
        ConfigService.getConfiguration().getTheme().apply(stage.getScene().getStylesheets());
        stage.show();
        root.applyCss();
        root.layout();

        assertEquals(first.getPath(), preview.getText());
        assertFalse(preview.isEditable());
        assertEquals("(2 more)", more.getText());
        assertTrue(more.isDisabled());
        assertTrue(more.isManaged());
        assertTrue(component.getHeight() <= 48, "The collapsed editor should occupy one row");
        assertTrue(component.lookup("#add-directory") instanceof Button);
        assertEquals(preview.getHeight(), ((Button) component.lookup("#add-directory")).getHeight(),
            0.5);
        writeSnapshot(root, "compact");

        preview.fireEvent(
            new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
        popup = (PopOver) Window.getWindows().stream()
            .filter(window -> window instanceof PopOver && window.isShowing()).findFirst()
            .orElseThrow();
        popup.getScene().getRoot().applyCss();
        popup.getScene().getRoot().layout();
        ListView<?> list = (ListView<?>) popup.getScene().getRoot().lookup("#directory-list");
        assertEquals(3, list.getItems().size());
        assertTrue(button(list, 0, "move-up").isDisabled());
        assertTrue(button(list, 2, "move-down").isDisabled());
        writeSnapshot(popup.getScene().getRoot(), "popup");
        for (int i = 0; i < list.getItems().size(); i++) {
          for (String id : new String[]{"move-up", "move-down", "remove-directory"}) {
            Button action = button(list, i, id);
            ListCell<?> cell = (ListCell<?>) action.getParent().getParent();
            var bounds = cell.sceneToLocal(action.localToScene(action.getBoundsInLocal()));
            assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= cell.getHeight(),
                "The " + id + " button must fit inside its row: " + bounds + ", height="
                    + cell.getHeight() + ", padding=" + action.getPadding());
          }
        }

        // Editing secondary entries must notify the parameter dialog despite an unchanged preview.
        button(list, 2, "move-up").fire();
        assertArrayEquals(new File[]{first, third, second}, component.getValue());
        assertEquals(first.getPath(), preview.getText());
        assertEquals(1, changes.get());
        assertTrue(popup.isShowing());

        button(list, 0, "move-down").fire();
        assertArrayEquals(new File[]{third, first, second}, component.getValue());
        assertEquals(third.getPath(), preview.getText());
        button(list, 0, "remove-directory").fire();
        assertArrayEquals(new File[]{first, second}, component.getValue());
        assertEquals("(1 more)", more.getText());
        button(list, 1, "remove-directory").fire();
        assertArrayEquals(new File[]{first}, component.getValue());
        assertFalse(more.isManaged());
        button(list, 0, "remove-directory").fire();
        assertEquals(0, component.getValue().length);
        assertEquals("", preview.getText());
        assertTrue(popup.isShowing());
        assertEquals(5, changes.get());

        component.addDirectory(first);
        component.addDirectory(first);
        assertArrayEquals(new File[]{first}, component.getValue());
        assertEquals(6, changes.get());
      } finally {
        if (popup != null) {
          popup.hide();
        }
        stage.hide();
      }
    });
  }

  @Test
  void componentAndParameterExchangeCopiesAndNullClearsThePreview() throws Exception {
    runOnFxAndWait(() -> {
      var parameter = new DirectoryListParameter("Directories", "");
      var component = parameter.createEditingComponent();
      File first = new File("C:\\temp");
      File[] directories = {first};
      parameter.setValueToComponent(component, directories);
      directories[0] = new File("D:\\temp");
      parameter.setValueFromComponent(component);
      assertArrayEquals(new File[]{first}, parameter.getValue());

      component.getValue()[0] = directories[0];
      assertArrayEquals(new File[]{first}, component.getValue());
      parameter.setValueToComponent(component, null);
      assertEquals(0, component.getValue().length);
      assertEquals("", ((CustomTextField) component.getDecorationTarget()).getText());
    });
  }

  private static Button button(ListView<?> list, int index, String id) {
    list.applyCss();
    list.layout();
    ListCell<?> cell = list.lookupAll(".list-cell").stream().filter(
            node -> node instanceof ListCell<?> candidate && !candidate.isEmpty()
                && candidate.getIndex() == index).map(node -> (ListCell<?>) node).findFirst()
        .orElseThrow();
    return (Button) cell.lookup("#" + id);
  }

  private static void writeSnapshot(Node node, String name) {
    try {
      Path output = Path.of("build", "reports", "directory-list", name + ".png");
      Files.createDirectories(output.getParent());
      ImageIO.write(SwingFXUtils.fromFXImage(node.snapshot(null, null), null), "png",
          output.toFile());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private static void runOnFxAndWait(Runnable action) throws Exception {
    var task = new FutureTask<Void>(action, null);
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (true) {
      try {
        Platform.runLater(task);
        break;
      } catch (IllegalStateException toolkitNotInitialized) {
        if (System.nanoTime() > deadline) {
          throw toolkitNotInitialized;
        }
        Thread.sleep(50);
      }
    }
    task.get(30, TimeUnit.SECONDS);
  }
}
