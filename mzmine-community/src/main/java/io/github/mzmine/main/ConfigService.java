/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.main;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.impl.MZmineConfigurationImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.web.ProxyUtils;
import io.mzio.mzmine.startup.MZmineCoreArgumentParser;
import io.mzio.users.gui.fx.LoginOptions;
import io.mzio.users.gui.fx.UsersController;
import io.mzio.users.user.CurrentUserService;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import org.jetbrains.annotations.NotNull;

public final class ConfigService {

  private static final Logger logger = Logger.getLogger(ConfigService.class.getName());
  private static final MZmineConfiguration config = new MZmineConfigurationImpl();
  private static MZmineCoreArgumentParser argsParser;

  public static MZmineConfiguration getConfiguration() {
    return config;
  }

  public static MZminePreferences getPreferences() {
    return config.getPreferences();
  }

  public static <V, T extends Parameter<V>> V getPreference(T parameter) {
    return config.getPreferences().getValue(parameter);
  }

  public static NumberFormats getExportFormats() {
    return config.getExportFormats();
  }

  public static NumberFormats getGuiFormats() {
    return config.getGuiFormats();
  }

  public static SimpleColorPalette getDefaultColorPalette() {
    return config.getDefaultColorPalette();
  }

  public static BooleanProperty isDarkModeProperty() {
    return config.getPreferences().darkModeProperty();
  }

  public static void setDarkMode(final Boolean dark) {
    getPreferences().setDarkMode(dark);
  }

  /**
   * Save current config to user directory
   *
   * @return true if successful, false otherwise
   */
  public static boolean saveUserConfig() {
    try {
      getConfiguration().saveConfiguration(MZmineConfiguration.CONFIG_FILE);
      return true;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot save user config", e);
      return false;
    }
  }

  public static boolean isTdfPseudoProfile() {
    return argsParser != null && getArgsParser().isLoadTdfPseudoProfile();
  }


  public static boolean isTsfProfile() {
    return argsParser != null && getArgsParser().isLoadTsfProfile();
  }

  /**
   * Parses all relevant arguments from the given program arguments and initialises the
   * {@link MZmineCoreArgumentParser} instance in this class.
   *
   * @param args The program arguments.
   */
  public static void parseArgs(final String[] args) {
    MZmineCoreArgumentParser argsParser = new MZmineCoreArgumentParser(args);
    ConfigService.setArgsParser(argsParser);

    checkAndLoadArgsConfiguration(argsParser);
    TmpFileCleanup.runCleanup(); // clean old temp files in old dir

    // parse args temp dir after config was loaded, so we can override
    checkAndOverrideArgsTempDir(argsParser);
    applyTempDirFromConfiguration();
    TmpFileCleanup.runCleanup(); // clean temp files in new dir

    checkAndOverrideArgsProxy(argsParser);

    checkAndOverrideArgsUser(argsParser);

    checkAndOverrideArgsMemoryOption(argsParser);

    setNumThreadsOverride(argsParser);

    checkAndHandleArgsUserLoginOptions(argsParser);

  }

  private static void checkAndOverrideArgsTempDir(MZmineCoreArgumentParser argsParser) {
    // override temp directory
    final File tempDirectory = argsParser.getTempDirectory();
    if (tempDirectory != null) {
      // needs to be accessible
      if (FileAndPathUtil.createDirectory(tempDirectory)) {
        getPreferences().setParameter(MZminePreferences.tempDirectory, tempDirectory);
      } else {
        logger.log(Level.WARNING,
            "Cannot create or access temp file directory that was set via program argument: "
                + tempDirectory.getAbsolutePath());
      }
    }
  }

  private static void checkAndHandleArgsUserLoginOptions(MZmineCoreArgumentParser argsParser) {
    final boolean isCliBatchProcessing = argsParser.getBatchFile() != null;

    // login user by cli direct password
    if (argsParser.isCliLoginPassword()) {
      if (commandLineLogin(isCliBatchProcessing, LoginOptions.CONSOLE_ENTER_CREDENTIALS)) {
        return;
      }
    }

    // login user if cli option
    if (argsParser.isCliLogin()) {
      if (commandLineLogin(isCliBatchProcessing, LoginOptions.CONSOLE)) {
        return;
      }
    }
  }

  private static void checkAndOverrideArgsMemoryOption(
      @NotNull final MZmineCoreArgumentParser argsParser) {
    KeepInMemory keepInMemory;
    try {
      var memory = argsParser.isKeepInMemory();
      if (StringUtils.hasValue(memory)) {
        keepInMemory = KeepInMemory.parse(memory);

        // set to preferences
        getPreferences().setParameter(MZminePreferences.memoryOption, keepInMemory);
      } else {
        keepInMemory = getPreferences().getParameter(MZminePreferences.memoryOption).getValue();
      }
    } catch (Exception exception) {
      logger.warning("Issue while reading keep in memory option from CLI argument");
      System.exit(1);
      return;
    }

    if (keepInMemory == null) {
      keepInMemory = KeepInMemory.NONE;
    }

    // apply memory management option
    keepInMemory.enforceToMemoryMapping();
  }

  private static void checkAndOverrideArgsUser(@NotNull final MZmineCoreArgumentParser argsParser) {
    if (argsParser.getUserFile() == null) {
      // listen for user changes so that the latest user is saved
      String username = ConfigService.getPreference(MZminePreferences.username);
      // this will set the current user to CurrentUserService
      // loads all users already logged in from the user folder
      if (StringUtils.hasValue(username)) {
        UsersController.getInstance().setCurrentUserByName(username);
      }
    }
  }

  private static void checkAndOverrideArgsProxy(
      @NotNull final MZmineCoreArgumentParser argsParser) {
    //set proxy to config
    if (argsParser.getFullProxy() != null) {
      // proxy was already set
      getPreferences().setProxy(ProxyUtils.getSelectedSystemProxy());
    }
  }

  private static void checkAndLoadArgsConfiguration(
      @NotNull final MZmineCoreArgumentParser argsParser) {
    // override preferences file by command line argument pref
    final File prefFile = Objects.requireNonNullElse(argsParser.getPreferencesFile(),
        MZmineConfiguration.CONFIG_FILE);

    // Load configuration
    if (prefFile.exists() && prefFile.canRead()) {
      try {
        ConfigService.getConfiguration().loadConfiguration(prefFile, true);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error while reading configuration " + prefFile.getAbsolutePath(),
            e);
      }
    } else {
      logger.log(Level.WARNING, "Cannot read configuration " + prefFile.getAbsolutePath());
    }
  }

  /**
   * Set number of cores to automatic or to fixed number
   */
  public static void setNumThreadsOverride(@NotNull final MZmineCoreArgumentParser argsParser) {
    final String numCores = argsParser.getNumCores();
    if (numCores != null) {
      // set to preferences
      var parameter = ConfigService.getPreferences().getParameter(MZminePreferences.numOfThreads);
      if (numCores.equalsIgnoreCase("auto") || numCores.equalsIgnoreCase("automatic")) {
        parameter.setAutomatic(true);
      } else {
        try {
          parameter.setValue(Integer.parseInt(numCores));
        } catch (Exception ex) {
          logger.log(Level.SEVERE,
              "Cannot parse command line argument threads (int) set to " + numCores);
          throw new IllegalArgumentException("numCores was set to " + numCores, ex);
        }
      }
    }
  }

  /**
   * @param isCliBatchProcessing
   * @param option
   * @return true if application finished
   */
  private static boolean commandLineLogin(final boolean isCliBatchProcessing, LoginOptions option) {
    boolean success = false;
    try {
      logger.info("CLI user login");
      UsersController.getInstance().loginOrRegisterConsoleBlocking(option);
      success = true;
    } catch (Exception ex) {
      DesktopService.getDesktop().displayMessage(
          "Requires user login. Open mzmine GUI and login to a user. Then provide the user file as command line argument -user path/user.mzuser");
      if (!isCliBatchProcessing) {
        System.exit(1);
        return true;
      }
    }
    // if no batch select - that means it was only a login call.
    // save config and close mzmine
    if (success && !isCliBatchProcessing) {
      String currentUserName = CurrentUserService.getUserName().orElse("");
      ConfigService.getPreferences().setParameter(MZminePreferences.username, currentUserName);
      if (!ConfigService.saveUserConfig()) {
        logger.severe(
            "Failed to save user config after login. A solution may be to delete the .mzconfig file in the system user directory /.mzmine/");
        System.exit(1);
        return true;
      } else {
        logger.info("User login successful, user configuration is saved with the new user "
            + currentUserName);
        System.exit(0);
        return true;
      }
    }
    return false;
  }

  public static MZmineCoreArgumentParser getArgsParser() {
    if (argsParser == null) {
      throw new IllegalStateException("MZmineCoreArgumentParser not yet initialized");
    }
    return argsParser;
  }

  private static void setArgsParser(@NotNull final MZmineCoreArgumentParser argsParser) {
    ConfigService.argsParser = argsParser;
  }

  private static void applyTempDirFromConfiguration() {
    final File tempDir = getConfiguration().getPreferences()
        .getParameter(MZminePreferences.tempDirectory).getValue();
    if (tempDir == null) {
      logger.warning(
          () -> "Invalid temporary directory. Defaulting to system temp directory. %s".formatted(
              System.getProperty("java.io.tmpdir")));
      return;
    }

    if (!tempDir.exists()) {
      if (!tempDir.mkdirs()) {
        logger.warning(
            () -> "Could not create temporary directory %s. Defaulting to system temp directory. %s".formatted(
                tempDir.getAbsolutePath(), System.getProperty("java.io.tmpdir")));
        return;
      }
    }

    if (tempDir.isDirectory()) {
      FileAndPathUtil.setTempDir(tempDir.getAbsoluteFile());
      logger.finest(() -> "Default temporary directory is %s".formatted(
          System.getProperty("java.io.tmpdir")));
      logger.finest(() -> "Working temporary directory is %s".formatted(
          FileAndPathUtil.getTempDir().toString()));
    }
  }

  public static void openTempPreferences() {
    MZminePreferences pref = MZmineCore.getConfiguration().getPreferences();
    FxThread.runLater(() -> pref.showSetupDialog(true, "temp"));
  }
}
