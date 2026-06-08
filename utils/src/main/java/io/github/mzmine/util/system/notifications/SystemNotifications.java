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

package io.github.mzmine.util.system.notifications;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for displaying system tray notifications using java.awt.SystemTray. Needs
 * initialization, adds TrayIcon and notification options. Current limitations seem to be short
 * message strings that cannot really be read. No feedback on clicks on the message. Therefore, we
 * currently use notifications in mzmine.
 * <p>
 * Just here as a reference implementation for later testing in case we want to explore system
 * notifications and tray icons again.
 */
@Deprecated
public final class SystemNotifications {

  private static final Logger LOGGER = Logger.getLogger(SystemNotifications.class.getName());

  private static SystemNotifications INSTANCE;

  private final SystemTray systemTray;
  private final TrayIcon trayIcon;
  private final boolean initialized;

  public static synchronized void initialize(@NotNull String iconResourcesPath,
      @NotNull String tooltip) {
    if (INSTANCE != null) {
      throw new IllegalStateException("SystemNotification has already been initialized");
    }

    INSTANCE = new SystemNotifications(iconResourcesPath, tooltip);
  }


  /**
   * Private constructor to prevent instantiation.
   */
  private SystemNotifications(@NotNull String iconResourcesPath, @NotNull String tooltip) {
    boolean initialized = false;
    SystemTray systemTray = null;
    TrayIcon trayIcon = null;

    try {
      if (!SystemTray.isSupported()) {
        LOGGER.fine("SystemTray is not supported on this platform. Notifications in console.");
      } else {
        systemTray = SystemTray.getSystemTray();

        Image iconImage = loadIcon(iconResourcesPath);
        if (iconImage == null) {
          throw new IllegalStateException(
              "SystemTray icon %s could not be loaded. Is loaded by class.getClassLoader.getResource - so by absolute path from src dir".formatted(
                  iconResourcesPath));
        }

        trayIcon = new TrayIcon(iconImage, tooltip);
        trayIcon.setImageAutoSize(true);

        final PopupMenu popup = new PopupMenu();
        final MenuItem exit = new MenuItem("Exit mzmine");
        exit.addActionListener(_ -> System.exit(0));
        popup.add(exit);
        trayIcon.setPopupMenu(popup);

        final AtomicBoolean addSuccess = new AtomicBoolean(false);
        final SystemTray finalTray = systemTray;
        final TrayIcon finalIcon = trayIcon;
        if (finalTray != null && finalIcon != null) {
//          EventQueue.invokeAndWait(() -> {
          try {
            finalTray.add(finalIcon);
            LOGGER.info("Tray icon added successfully.");
            addSuccess.set(true); // Mark success
          } catch (AWTException e) {
            LOGGER.log(Level.SEVERE, "Failed to add TrayIcon to SystemTray", e);
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error adding TrayIcon", e);
          }
//          });
        }

        // Update initialization status based on the result from invokeAndWait
        initialized = addSuccess.get();
      }
//    } catch (InterruptedException e) {
//      LOGGER.log(Level.SEVERE,
//          "Initialization interrupted during EventQueue.invokeAndWait. Notifications in console.",
//          e);
//      initialized = false;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Unexpected error during static initialization", e);
      initialized = false;
    }

    if (initialized) {
      LOGGER.info("Static initialization completed successfully.");
    }
    this.initialized = initialized;
    this.systemTray = systemTray;
    this.trayIcon = trayIcon;
  }

  /**
   * Displays a system notification message. Checks if initialization was successful. If not, logs
   * an error and does nothing. This method is safe to call from any thread.
   *
   * @param title The notification title (mandatory).
   * @param text  The notification body text (mandatory).
   * @param type  The type of message (INFO, WARNING, ERROR, NONE).
   * @throws NullPointerException if title, text, or type is null.
   */
  public static void show(@NotNull String title, @NotNull String text, @NotNull MessageType type) {
    if (INSTANCE == null) {
      LOGGER.log(Level.WARNING,
          "SystemNotifications was not initialized yet. Will just log to console.");
      LOGGER.log(Level.INFO, "%s: %s (notification type: %s)".formatted(title, text, type));
      return;
    }
    INSTANCE._show(title, text, type);
  }

  private void _show(@NotNull String title, @NotNull String text, @NotNull MessageType type) {
    if (!initialized) {
      LOGGER.log(Level.INFO, "%s: %s (notification type: %s)".formatted(title, text, type));
      return;
    }

    // Ensure the AWT call happens on the Event Dispatch Thread (EDT)
    EventQueue.invokeLater(() -> {
      // Check again inside invokeLater
      if (trayIcon != null) {
        try {
          trayIcon.displayMessage(title, text, type);
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Error displaying system notification", e);
        }
      }
    });
  }

  /**
   * Displays an informational system notification message. Shortcut for show(title, text,
   * MessageType.INFO).
   */
  public static void showInfo(String title, String text) {
    show(title, text, MessageType.INFO);
  }

  /**
   * Displays a warning system notification message. Shortcut for show(title, text,
   * MessageType.WARNING).
   */
  public static void showWarning(String title, String text) {
    show(title, text, MessageType.WARNING);
  }

  /**
   * Displays an error system notification message. Shortcut for show(title, text,
   * MessageType.ERROR).
   */
  public static void showError(String title, String text) {
    show(title, text, MessageType.ERROR);
  }

  // --- Helper Methods ---

  private static @Nullable Image loadIcon(String path) {
    if (path != null && !path.trim().isEmpty()) {
      try {
        URL iconUrl = SystemNotifications.class.getClassLoader().getResource(path);
        if (iconUrl != null) {
          Image img = Toolkit.getDefaultToolkit().getImage(iconUrl);
          if (img != null) {
            // Check if image loaded successfully (can be tricky with Toolkit)
            // A more robust check might involve MediaTracker or ImageIO
            LOGGER.log(Level.INFO, "Attempting to load custom icon from: {0}", path);
            // Assuming success here for simplicity, Toolkit loads asynchronously
            return img;
          } else {
            LOGGER.warning("Failed to load image from URL (Toolkit returned null): " + iconUrl);
          }
        } else {
          LOGGER.warning("Custom icon resource not found at path: " + path);
        }
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error loading custom icon from " + path, e);
      }
    }
    LOGGER.info("Using default placeholder icon.");
    return null; // Return default if path is null, empty, or loading fails
  }

}
