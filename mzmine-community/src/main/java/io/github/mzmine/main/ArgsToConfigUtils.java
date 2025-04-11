package io.github.mzmine.main;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.web.ProxyUtils;
import io.mzio.mzmine.startup.MZmineCoreArgumentParser;
import io.mzio.users.gui.fx.LoginOptions;
import io.mzio.users.gui.fx.UsersController;
import io.mzio.users.user.CurrentUserService;
import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class to apply all parsed args from the
 * {@link io.mzio.mzmine.startup.MZmineCoreArgumentParser} to the {@link ConfigService}.
 */
class ArgsToConfigUtils {

  private static final Logger logger = Logger.getLogger(ArgsToConfigUtils.class.getName());

  /**
   * Parses all relevant arguments from the given program arguments and initialises the
   * {@link MZmineCoreArgumentParser} instance in this class.
   *
   * @param argsParser The args parser
   */
  static void applyArgsToConfig(final MZmineCoreArgumentParser argsParser) {
    ConfigService.setTsfProfile(argsParser.isLoadTsfProfile());
    ConfigService.setTdfPseudoProfile(argsParser.isLoadTdfPseudoProfile());

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

  static void checkAndOverrideArgsTempDir(MZmineCoreArgumentParser argsParser) {
    // override temp directory
    final File tempDirectory = argsParser.getTempDirectory();
    if (tempDirectory != null) {
      // needs to be accessible
      if (FileAndPathUtil.createDirectory(tempDirectory)) {
        ConfigService.getPreferences().setParameter(MZminePreferences.tempDirectory, tempDirectory);
      } else {
        logger.log(Level.WARNING,
            "Cannot create or access temp file directory that was set via program argument: "
                + tempDirectory.getAbsolutePath());
      }
    }
  }

  static void checkAndHandleArgsUserLoginOptions(MZmineCoreArgumentParser argsParser) {
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
      System.exit(1);
      return;
    }

    if (keepInMemory == null) {
      keepInMemory = KeepInMemory.NONE;
    }

    // apply memory management option
    keepInMemory.enforceToMemoryMapping();
  }

  static void checkAndOverrideArgsUser(@NotNull final MZmineCoreArgumentParser argsParser) {
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

  static void checkAndOverrideArgsProxy(@NotNull final MZmineCoreArgumentParser argsParser) {
    //set proxy to config
    if (argsParser.getFullProxy() != null) {
      // proxy was already set
      ConfigService.getPreferences().setProxy(ProxyUtils.getSelectedSystemProxy());
    }
  }

  static void checkAndLoadArgsConfiguration(@NotNull final MZmineCoreArgumentParser argsParser) {
    // override preferences file by command line argument pref
    final File prefFile = Objects.requireNonNullElse(argsParser.getPreferencesFile(),
        MZmineConfiguration.CONFIG_FILE);
    if("null".equals(prefFile.getName())){
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

  static void applyTempDirFromConfiguration() {
    final File tempDir = ConfigService.getConfiguration().getPreferences()
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
}
