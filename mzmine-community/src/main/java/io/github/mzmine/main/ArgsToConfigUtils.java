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

package io.github.mzmine.main;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.mzio.mzmine.startup.MZmineCoreArgumentParser;
import io.mzio.mzmine.startup.MZmineExit;
import io.mzio.users.client.UserAuthStore;
import io.mzio.users.gui.fx.LoginOptions;
import io.mzio.users.gui.fx.UsersController;
import io.mzio.users.user.CurrentUserService;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class to apply all parsed args from the
 * {@link io.mzio.mzmine.startup.MZmineCoreArgumentParser} to the {@link ConfigService}.
 */
class ArgsToConfigUtils {

  private static final Logger logger = Logger.getLogger(ArgsToConfigUtils.class.getName());

  /**
   * Lists the command line login options when no valid user is available in headless mode.
   */
  static final String CLI_LOGIN_REQUIRED_MESSAGE = """
      Requires user login. Use one of these command line arguments:
        -cli-token <token>  login with a token generated in the mzio account console, \
        recommended for automatic pipelines for online users.
        -login              login via the system internet browser, saves a user file to the users directory
        -user <file|name>   an offline user locked to this machine, use an .mzuser4 user file
      Legacy .mzuser files from older mzmine versions are not supported, login again to create a new user file.""";

  /**
   * Parses all relevant arguments from the given program arguments and initialises the
   * {@link MZmineCoreArgumentParser} instance in this class.
   *
   * @param argsParser The args parser
   */
  static void applyArgsToConfig(final MZmineCoreArgumentParser argsParser) {
    ConfigService.setTdfPseudoProfile(argsParser.isLoadTdfPseudoProfile());

    checkAndLoadArgsConfiguration(argsParser);
    TmpFileCleanup.runCleanup(); // clean old temp files in old dir

    // parse args temp dir after config was loaded, so we can override
    checkAndOverrideArgsTempDir(argsParser);
    TmpFileCleanup.runCleanup(); // clean temp files in new dir

    checkAndOverrideArgsUser(argsParser);

    checkAndOverrideArgsMemoryOption(argsParser);

    setNumThreadsOverride(argsParser);

    checkAndHandleArgsUserLoginOptions(argsParser);

    ConfigService.setIgnoreParameterWarningsInBatch(argsParser.isIgnoreParameterWarnings());
  }

  static void checkAndOverrideArgsTempDir(MZmineCoreArgumentParser argsParser) {
    final File[] tempDirectories = argsParser.getTempDirectories();
    if (tempDirectories == null) {
      return;
    }
    // set and get actual temp directories, excluding .mzmine folder
    final File[] usableDirectories = FileAndPathUtil.setTempDirs(tempDirectories);
    if (usableDirectories.length > 0) {
      ConfigService.getPreferences()
          .setParameter(MZminePreferences.tempDirectory, usableDirectories);
    }
    logger.finest(() -> "Working temporary directory is " + FileAndPathUtil.getTempDir());
  }

  static void checkAndHandleArgsUserLoginOptions(MZmineCoreArgumentParser argsParser) {
    final boolean isCliBatchProcessing = argsParser.getBatchFile() != null;

    // login user by cli-token
    if (argsParser.isCliLoginToken()) {
      commandLineLogin(isCliBatchProcessing, LoginOptions.CLI_TOKEN);
    }

    // the -login user was already logged in and saved by MZmineCoreArgumentParser
    // without a batch, -login only saves the user to the configuration and closes mzmine
    if (argsParser.isCliLogin() && !isCliBatchProcessing) {
      saveLoginToConfigAndExit();
    }
  }

  static void checkAndOverrideArgsMemoryOption(@NotNull final MZmineCoreArgumentParser argsParser) {
    KeepInMemory keepInMemory;
    try {
      var memory = argsParser.isKeepInMemory();
      if (StringUtils.hasValue(memory)) {
        keepInMemory = KeepInMemory.parse(memory);

        // set to preferences
        ConfigService.getPreferences().setParameter(MZminePreferences.memoryOption, keepInMemory);
      } else {
        keepInMemory = ConfigService.getPreferences().getParameter(MZminePreferences.memoryOption)
            .getValue();
      }
    } catch (Exception exception) {
      logger.warning("Issue while reading keep in memory option from CLI argument");
      MZmineExit.exit(1);
      return;
    }

    if (keepInMemory == null) {
      keepInMemory = KeepInMemory.NONE;
    }

    // apply memory management option
    keepInMemory.enforceToMemoryMapping();
  }

  static void checkAndOverrideArgsUser(@NotNull final MZmineCoreArgumentParser argsParser) {
    if (argsParser.getUserFile() != null) {
      return; // user was already read and locked in MZmineCoreArgumentParser
    }
    if (argsParser.isGuiMode()) {
      return; // GUI: caller will restore the user asynchronously after TaskService is ready
    }
    // Headless / CLI mode: restore synchronously so the user is set before the batch runs.
    restoreUserFromConfig(ConfigService.getPreference(MZminePreferences.username));
  }

  /**
   * Restores the previously active user identified by {@code username} by scanning all saved user
   * files. Safe to call from any thread. May do network I/O when the user file is older than 5
   * days.
   */
  static void restoreUserFromConfig(@Nullable final String username) {
    if (!StringUtils.hasValue(username)) {
      return;
    }
    try {
      UserAuthStore.readAllUserFiles().stream().filter(u -> username.equals(u.getNickname()))
          .findFirst().ifPresent(CurrentUserService::setUser);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not restore saved user: " + username, e);
    }
  }

  static void checkAndLoadArgsConfiguration(@NotNull final MZmineCoreArgumentParser argsParser) {
    // override preferences file by command line argument pref
    final File prefFile = Objects.requireNonNullElse(argsParser.getPreferencesFile(),
        MZmineConfiguration.CONFIG_FILE);
    if ("null".equals(prefFile.getName())) {
      logger.info("Preference file was set to null, not loading configuration.");
      return;
    }

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
  static void setNumThreadsOverride(@NotNull final MZmineCoreArgumentParser argsParser) {
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
  static boolean commandLineLogin(final boolean isCliBatchProcessing, LoginOptions option) {
    boolean success = false;
    try {
      logger.info("CLI user login");
      UsersController.getInstance().loginOrRegisterConsoleBlocking(option);
      success = true;
    } catch (Exception ex) {
      DesktopService.getDesktop().displayMessage(CLI_LOGIN_REQUIRED_MESSAGE);
      if (!isCliBatchProcessing) {
        MZmineExit.exit(1);
        return true;
      }
    }
    // if no batch select - that means it was only a login call.
    // save config and close mzmine
    if (success && !isCliBatchProcessing) {
      saveLoginToConfigAndExit();
      return true;
    }
    return false;
  }

  /**
   * Saves the current user to the configuration and closes mzmine. Used when a login option is
   * given without a batch file.
   */
  private static void saveLoginToConfigAndExit() {
    final String currentUserName = CurrentUserService.getUserName().orElse("");
    ConfigService.getPreferences().setParameter(MZminePreferences.username, currentUserName);
    if (!ConfigService.saveUserConfig()) {
      logger.severe(
          "Failed to save user config after login. A solution may be to delete the .mzconfig file in the system user directory /.mzmine/");
      MZmineExit.exit(1);
    } else {
      logger.info("User login successful, user configuration is saved with the new user "
          + currentUserName);
      MZmineExit.exit(0);
    }
  }

}
