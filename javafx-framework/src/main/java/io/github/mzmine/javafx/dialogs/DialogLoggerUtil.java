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

package io.github.mzmine.javafx.dialogs;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.JavaFxDesktop;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.NotificationService.NotificationType;
import io.github.mzmine.javafx.util.FxTextUtils;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DialogLoggerUtil {

  private static final Logger logger = Logger.getLogger(DialogLoggerUtil.class.getName());

  public static void showErrorDialog(String title, String message) {
    showErrorDialog(null, title, message);
  }

  public static void showWarningDialog(String title, String message) {
    showWarningDialog(null, title, message);
  }

  public static void showErrorDialog(final @Nullable Window owner, String title, String message) {
    showDialog(AlertType.ERROR, owner, title, message, true);
  }

  public static void showWarningDialog(final @Nullable Window owner, String title, String message) {
    showDialog(AlertType.WARNING, owner, title, message, true);
  }

  /**
   * Logs the message and shows a dialog when in GUI mode
   */
  public static void showMessageDialog(String title, String message) {
    showMessageDialog(title, true, message);
  }

  /**
   * Logs the message and shows a dialog when in GUI mode
   */
  public static void showMessageDialog(String title, Node content) {
    showMessageDialog(title, true, content);
  }

  /**
   * Logs the message and shows a dialog when in GUI mode
   */
  public static void showMessageDialog(String title, boolean modal, Node content) {
    // log text content
    String message = FxTextUtils.extractText(content);
    if (!message.isBlank()) {
      logger.info(title + ": " + message);
    }

    if (DesktopService.isHeadLess()) {
      return;
    }

    if (modal) {
      FxThread.runOnFxThreadAndWait(() -> {
        createAlert(AlertType.INFORMATION, null, title, content).showAndWait();
      });
    } else {
      // non blocking
      FxThread.runLater(() -> {
        createAlert(AlertType.INFORMATION, null, title, content).show();
      });
    }
  }

  /**
   * Logs the message and shows a dialog when in GUI mode
   */
  public static void showMessageDialog(String title, boolean modal, String message) {
    logger.info(title + ": " + message);
    if (DesktopService.isHeadLess()) {
      return;
    }

    if (modal) {
      FxThread.runOnFxThreadAndWait(() -> {
        createAlert(AlertType.INFORMATION, null, title, message).showAndWait();
      });
    } else {
      // non blocking
      FxThread.runLater(() -> {
        createAlert(AlertType.INFORMATION, null, title, message).show();
      });
    }
  }

  /**
   * @return main window if desktop is a {@link JavaFxDesktop}
   */
  @Nullable
  public static Stage getMainWindow() {
    if (DesktopService.getDesktop() instanceof JavaFxDesktop fx) {
      return fx.getMainWindow();
    }
    return null;
  }

  /**
   * @return the currently focused window
   */
  public static Optional<Window> getFocusedWindow() {
    return Window.getWindows().stream().filter(Window::isFocused).findFirst();
  }

  /**
   * Set owner to the focused window or if not available to mainWindow. This sets the style sheets,
   * icons, and lets dialogs spawn in the center of that window.
   */
  public static void applyFocusedWindowStyle(final Dialog<?> dialog) {
    final Window parentWindow = getFocusedWindow().orElseGet(DialogLoggerUtil::getMainWindow);
    if (parentWindow == null) {
      return;
    }
    dialog.initOwner(parentWindow);
  }

  /**
   * @return true if yes was clicked, false on No and on cancel
   */
  public static boolean showDialogYesNo(String title, String message) {
    return showDialogYesNo(AlertType.CONFIRMATION, title, message);
  }

  /**
   * @return true if yes was clicked, false on No and on cancel
   */
  public static boolean showDialogYesNo(AlertType type, String title, String message) {
    return showDialog(type, title, message, ButtonType.YES, ButtonType.NO).map(
        res -> res == ButtonType.YES).orElse(false);
  }

  /**
   * @param type    usually uses {@link AlertType#CONFIRMATION}
   * @param title   title of dialog and also repeated in dialog header
   * @param message message in box wrapped
   * @param buttons Either use predefined buttons or define new buttons with
   *                {@link ButtonType#ButtonType(String, ButtonData)} (String, ButtonData)}requires
   *                a {@link ButtonData#CANCEL_CLOSE} or {@link ButtonData#NO} button that will also
   *                trigger on X close button. Otherwise X button will not work
   * @return optional button type. Easy to use the ButtonData or title to match the button
   */
  public static Optional<ButtonType> showDialog(AlertType type, String title, String message,
      @Nullable ButtonType... buttons) {
    return showDialog(type, title, message, true, buttons);
  }

  /**
   * @param type          usually uses {@link AlertType#CONFIRMATION}
   * @param title         title of dialog and also repeated in dialog header
   * @param message       message in box wrapped
   * @param blockingModal open dialog and block
   * @return optional button type. Easy to use the ButtonData or title to match the button
   */
  public static Optional<ButtonType> showDialog(AlertType type, String title, String message,
      boolean blockingModal) {
    return showDialog(type, null, title, message, blockingModal);
  }

  /**
   * @param type          usually uses {@link AlertType#CONFIRMATION}
   * @param title         title of dialog and also repeated in dialog header
   * @param message       message in box wrapped
   * @param blockingModal open dialog and block
   * @return optional button type. Easy to use the ButtonData or title to match the button
   */
  public static Optional<ButtonType> showDialog(AlertType type, final @Nullable Window owner,
      String title, String message, boolean blockingModal) {
    return showDialog(type, owner, title, message, blockingModal, new ButtonType[0]);
  }

  /**
   * @param type          usually uses {@link AlertType#CONFIRMATION}
   * @param title         title of dialog and also repeated in dialog header
   * @param message       message in box wrapped
   * @param blockingModal open dialog and block
   * @param buttons       Either use predefined buttons or define new buttons with
   *                      {@link ButtonType#ButtonType(String, ButtonData)} (String,
   *                      ButtonData)}requires a {@link ButtonData#CANCEL_CLOSE} or
   *                      {@link ButtonData#NO} button that will also trigger on X close button.
   *                      Otherwise X button will not work
   * @return optional button type. Easy to use the ButtonData or title to match the button
   */
  public static Optional<ButtonType> showDialog(AlertType type, String title, String message,
      boolean blockingModal, @Nullable ButtonType... buttons) {
    return showDialog(type, null, title, message, blockingModal, buttons);
  }

  /**
   * @param type          usually uses {@link AlertType#CONFIRMATION}
   * @param title         title of dialog and also repeated in dialog header
   * @param message       message in box wrapped
   * @param blockingModal open dialog and block
   * @param buttons       Either use predefined buttons or define new buttons with
   *                      {@link ButtonType#ButtonType(String, ButtonData)} (String,
   *                      ButtonData)}requires a {@link ButtonData#CANCEL_CLOSE} or
   *                      {@link ButtonData#NO} button that will also trigger on X close button.
   *                      Otherwise X button will not work
   * @return optional button type. Easy to use the ButtonData or title to match the button
   */
  public static Optional<ButtonType> showDialog(AlertType type, final @Nullable Window owner,
      String title, String message, boolean blockingModal, @Nullable ButtonType... buttons) {
    if (DesktopService.isHeadLess()) {
      logger.info(title + ": " + message);
      return Optional.empty();
    }
    if (blockingModal) {
      final AtomicReference<Optional<ButtonType>> result = new AtomicReference<>();
      FxThread.runOnFxThreadAndWait(() -> {
        var alert = createAlert(type, owner, title, message, buttons);
        result.set(alert.showAndWait());
      });
      return result.get();
    } else {
      // non blocking
      FxThread.runLater(() -> {
        createAlert(type, owner, title, message, buttons).show();
      });
      return Optional.empty();
    }
  }

  /**
   * Internal method to create an alert. use {@link #showDialog}
   */
  private static @NotNull Alert createAlert(final AlertType type, final @Nullable Window owner,
      final String title, final String message, @Nullable final ButtonType... buttons) {
    // seems like a good size for the dialog message when an old batch is loaded into new version
    Text label = new Text(message);
    label.setWrappingWidth(415);
    HBox box = new HBox(label);
    box.setPadding(new Insets(5));
    return createAlert(type, owner, title, box, buttons);
  }

  /**
   * Internal method to create an alert. use {@link #showDialog}
   */
  private static @NotNull Alert createAlert(final AlertType type, final @Nullable Window owner,
      final String title, final Node content, @Nullable final ButtonType... buttons) {
    Alert alert = new Alert(type, "", buttons);
    if (owner == null) {
      applyFocusedWindowStyle(alert);
    } else {
      alert.initOwner(owner);
    }

    alert.setTitle(title);
    alert.setHeaderText(title);
    alert.getDialogPane().setContent(content);
    return alert;
  }

  /**
   * shows a message dialog just for a few given milliseconds
   */
  public static void showMessageDialogForTime(String title, String message) {
    showMessageDialogForTime(title, message, 3500);
  }

  public static void showMessageDialogForTime(String title, String message, long timeMillis) {
    showDialogForTime(title, message, timeMillis, AlertType.INFORMATION);
  }

  public static void showDialogForTime(String title, String message, final AlertType type) {
    showDialogForTime(title, message, 3500, type);
  }

  public static void showDialogForTime(String title, String message, long timeMillis,
      final AlertType type) {
    FxThread.runLater(() -> {
      if (type == AlertType.WARNING || type == AlertType.ERROR) {
        logger.warning(title + ": " + message);
      } else {
        logger.info(title + ": " + message);
      }

      if (DesktopService.isHeadLess()) {
        return;
      }
      var alert = createAlert(type, null, title, message);
      alert.show();

      PauseTransition delay = new PauseTransition(Duration.millis(timeMillis));
      delay.setOnFinished(_ -> alert.hide());
      delay.play();
    });
  }


  /**
   * @return the selected option or null if cancelled
   */
  @Nullable
  public static <T> T showAndWaitChoiceDialog(T selected, T[] options, String title,
      String content) {
    return showAndWaitChoiceDialog(selected, List.of(options), title, content);
  }

  /**
   * @return the selected option or null if cancelled
   */
  @Nullable
  public static <T> T showAndWaitChoiceDialog(T selected, List<T> options, String title,
      String content) {
    ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(selected, options);
    // applies style and more
    applyFocusedWindowStyle(choiceDialog);
    choiceDialog.setTitle(title);
    choiceDialog.setContentText(content);
    choiceDialog.showAndWait();
    return choiceDialog.getResult();
  }

  public static TextInputDialog createTextInputDialog(@NotNull String title, @NotNull String header,
      @NotNull String content) {
    TextInputDialog dialog = new TextInputDialog();
    // applies style and more
    applyFocusedWindowStyle(dialog);
    dialog.setTitle(title);
    dialog.setHeaderText(header);
    dialog.setContentText(content);
    return dialog;
  }

  public static void showNotification(@NotNull NotificationType type, @NotNull String title,
      @NotNull String message) {
    logger.info(() -> title + ": " + message);
    NotificationService.show(type, title, message);
  }

  public static void showInfoNotification(@NotNull String title, @NotNull String message) {
    showNotification(NotificationType.INFO, title, message);
  }

  public static void showWarningNotification(@NotNull String title, @NotNull String message) {
    showNotification(NotificationType.WARNING, title, message);
  }

  public static void showErrorNotification(@NotNull String title, @NotNull String message) {
    showNotification(NotificationType.ERROR, title, message);
  }

  public static void showPlainNotification(@NotNull String title, @NotNull String message) {
    showNotification(NotificationType.PLAIN, title, message);
  }
}
