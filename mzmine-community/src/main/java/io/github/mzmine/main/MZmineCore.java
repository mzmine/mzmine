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

package io.github.mzmine.main;

import com.vdurmont.semver4j.Semver;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.gui.MZmineDesktop;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.mainwindow.UsersTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.io.SemverVersionReader;
import io.github.mzmine.util.web.ProxyChangedEvent;
import io.mzio.events.AuthRequiredEvent;
import io.mzio.events.EventService;
import io.mzio.mzmine.startup.MZmineCoreArgumentParser;
import io.mzio.users.gui.fx.LoginOptions;
import io.mzio.users.gui.fx.UsersController;
import io.mzio.users.user.CurrentUserService;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static java.util.Objects.requireNonNullElse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MZmine main class
 */
public final class MZmineCore {

  private static final Logger logger = Logger.getLogger(MZmineCore.class.getName());

  private static final MZmineCore instance = new MZmineCore();
  // the default headless desktop is returned if no other desktop is set (e.g., during start up)
  // it is also used in headless mode
  private final Map<String, MZmineModule> initializedModules = new HashMap<>();


  private MZmineCore() {
    init();
  }

  /**
   * Main method
   */
  public static void main(final String[] args) {
    try {
      printDebugInfo(args);
      final MZmineCoreArgumentParser argsParser = new MZmineCoreArgumentParser(args);
      getInstance().startUp(argsParser);

      launchBatchOrGui(args, argsParser);

    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Error during mzmine start up", ex);
      exit(null);
    }
  }


  /**
   * Loads the configuration, parses the arguments and initializes everything, but does not launch
   * the batch or gui. Note: not static so it ensures that the {@link MZmineCore#init()} method is
   * called.
   */
  public void startUp(@NotNull final MZmineCoreArgumentParser argsParser) {
    ArgsToConfigUtils.applyArgsToConfig(argsParser);

    CurrentUserService.subscribe(user -> {
      var nickname = user == null ? null : user.getNickname();
      ConfigService.getPreferences().setParameter(MZminePreferences.username, nickname);
    });

    addUserRequiredListener();

    // after loading the config and numCores
    TaskService.init(ConfigService.getConfiguration().getNumOfThreads());
  }

  /**
   * Adds a listener to prompt the user to login if he/she is not logged in.
   */
  private static void addUserRequiredListener() {
    // add event listener
    EventService.subscribe(mzEvent -> {
      if (mzEvent instanceof AuthRequiredEvent(String message)) {
        DialogLoggerUtil.showMessageDialog("Invalid user", requireNonNullElse(message, ""));

        if (DesktopService.isGUI()) {
          getDesktop().addTab(UsersTab.showTab());
        } else {
          try {
            if (DesktopService.hasTerminalInput()) {
              UsersController.getInstance()
                  .loginOrRegisterConsoleBlocking(LoginOptions.CONSOLE_ENTER_CREDENTIALS);
              if (CurrentUserService.isValid()) {
                // login was successful
                return;
              }
            }
            getDesktop().displayMessage(
                "Requires user login. Open mzmine GUI and login to a user. Then provide the user file as command line argument -user path/user.mzuser");
            System.exit(1);
          } catch (Exception ex) {
            getDesktop().displayMessage(
                "Requires user login. Open mzmine GUI and login to a user. Then provide the user file as command line argument -user path/user.mzuser");
            System.exit(1);
          }
        }
      }
      if (mzEvent instanceof ProxyChangedEvent pevent) {
        ConfigService.getPreferences().setProxy(pevent.proxy());
      }
    });
  }

  public static void printDebugInfo(String[] args) {
    Semver version = SemverVersionReader.getMZmineVersion();
    logger.info("Starting mzmine %s libraries: %s".formatted(version,
        SemverVersionReader.getMZmineProVersion()));
    /*
     * Dump the MZmine and JVM arguments for debugging purposes
     */
    final String mzmineArgsString = String.join(" ", args);
    final List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    final String jvmArgsString = String.join(" ", jvmArgs);
    final String classPathString = System.getProperty("java.class.path");
    logger.finest("mzmine arguments: %s".formatted(mzmineArgsString));
    logger.finest("Java VM arguments: %s".formatted(jvmArgsString));
    logger.finest("Java class path: %s".formatted(classPathString));

    /*
     * Report current working and temporary directory
     */
    final String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
    logger.finest("Working directory is %s".formatted(cwd));
    logger.finest(
        "Default temporary directory is %s".formatted(System.getProperty("java.io.tmpdir")));

    final File logFile = ConfigService.getConfiguration().getLogFile();
    logger.finest("Writing log file to %s".formatted(logFile.getAbsolutePath()));
  }

  /**
   * @param args       the program arguments, required to launch the gui.
   * @param argsParser Args parser for easy access to e.g. the batch file.
   */
  public static void launchBatchOrGui(String[] args, MZmineCoreArgumentParser argsParser) {
    // batch mode defined by command line argument
    final File batchFile = argsParser.getBatchFile();
    final boolean isCliBatchProcessing = batchFile != null;
    final boolean keepRunningInHeadless = argsParser.isKeepRunningAfterBatch();
    final boolean headLessMode = (isCliBatchProcessing || keepRunningInHeadless);

    // If we have no arguments, run in GUI mode, otherwise run in batch mode
    if (!headLessMode) {
      launchGui(args);
      return;
    }

    // set headless desktop globally
    DesktopService.setDesktop(new HeadLessDesktop());

    // ask for login if terminal input is available
    if (DesktopService.hasTerminalInput() && CurrentUserService.isInvalid()) {
      // requires user
      try {
        logger.info("User login required.");
        UsersController.getInstance()
            .loginOrRegisterConsoleBlocking(LoginOptions.CONSOLE_ENTER_CREDENTIALS);
      } catch (Exception ex) {
        getDesktop().displayMessage(
            "Requires user login. Open mzmine GUI and login to a user. Then provide the user file as command line argument -user path/user.mzuser");
      }
      if (CurrentUserService.isInvalid()) {
        logger.warning(
            "No valid user. Please login via the GUI or CLI or provide a user via command line argument -user path/user.mzuser");
        System.exit(1);
      }
    }

    Task batchTask = null;
    if (batchFile != null) {
      // load batch
      if ((!batchFile.exists()) || (!batchFile.canRead())) {
        logger.severe("Cannot read batch file " + batchFile);
        exit(null);
      }

      // change input in batch?
      final String outBaseFile = argsParser.getOutBaseFile();
      final File[] overrideDataFiles = argsParser.getOverrideDataFiles();
      final File overrideMetadataFile = argsParser.getMetadataFile();
      final File[] overrideSpectralLibraryFiles = argsParser.getOverrideSpectralLibrariesFiles();

      // run batch file
      batchTask = BatchModeModule.runBatchFile(ProjectService.getProject(), batchFile,
          overrideDataFiles, overrideMetadataFile, overrideSpectralLibraryFiles, outBaseFile,
          Instant.now());
    }

    // option to keep MZmine running after the batch is finished
    // currently used to test - maybe useful to provide an API to access more data or to run other modules on demand
    if (!keepRunningInHeadless) {
      exit(batchTask);
    }

  }

  private static void launchGui(String[] args) {
    try {
      logger.info("Starting mzmine GUI");
      FxThread.setIsFxInitialized(true);
      Application.launch(MZmineGUI.class, args);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not applyArgsToConfig GUI", e);
      System.exit(1);
    }
    System.exit(0);
  }

  public static MZmineCore getInstance() {
    return instance;
  }

  /**
   * Exit MZmine (usually used in headless mode)
   */
  public static void exit(final @Nullable Task batchTask) {
    if (isHeadLessMode() && FxThread.isFxInitialized()) {
      // fx might be initialized for graphics export in headless mode - shut it down
      // in GUI mode it is shut down automatically
      Platform.exit();
    }
    if (batchTask != null && batchTask.isFinished()) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  @NotNull
  public static TaskController getTaskController() {
    return TaskService.getController();
  }

  /**
   * The current desktop or a default headless desktop (e.g., during app startup).
   *
   * @return the current desktop or the default headless desktop if still during app startup
   */
  @NotNull
  public static MZmineDesktop getDesktop() {
    if (DesktopService.getDesktop() instanceof MZmineDesktop mZmineDesktop) {
      return mZmineDesktop;
    }
    throw new IllegalStateException("Desktop was not initialized. Requires mzmineDesktop");
  }

  @NotNull
  public static MZmineConfiguration getConfiguration() {
    return ConfigService.getConfiguration();
  }

  /**
   * Returns the instance of a module of given class
   */
  @SuppressWarnings("unchecked")
  public synchronized static <ModuleType extends MZmineModule> ModuleType getModuleInstance(
      Class<ModuleType> moduleClass) {
    if (moduleClass == null) {
      return null;
    }

    ModuleType module = (ModuleType) getInstance().initializedModules.get(moduleClass.getName());

    if (module == null) {

      try {

        logger.finest("Creating an instance of the module " + moduleClass.getName());

        // Create instance and init module
        module = moduleClass.getDeclaredConstructor().newInstance();

        // Add to the module list
        getInstance().initializedModules.put(moduleClass.getName(), module);

      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Could not start module " + moduleClass, e);
        return null;
      }
    }

    return module;
  }

  public static Collection<MZmineModule> getAllModules() {
    return getInstance().initializedModules.values();
  }

  /**
   * Show setup dialog and run module if okay
   *
   * @param moduleClass the module class
   */
  public static ExitCode setupAndRunModule(
      final Class<? extends MZmineRunnableModule> moduleClass) {
    return setupAndRunModule(moduleClass, null, null);
  }

  /**
   * Show setup dialog and run module if okay
   *
   * @param moduleClass the module class
   * @param onFinish    callback for all tasks finished
   * @param onError     callback for error
   */
  public static ExitCode setupAndRunModule(final Class<? extends MZmineRunnableModule> moduleClass,
      @Nullable Runnable onFinish, @Nullable Runnable onError) {
    return setupAndRunModule(moduleClass, onFinish, onError, null);
  }

  /**
   * Show setup dialog and run module if okay
   *
   * @param moduleClass the module class
   * @param onFinish    callback for all tasks finished
   * @param onError     callback for error
   * @param onCancel    callback for cancelled tasks
   */
  public static ExitCode setupAndRunModule(final Class<? extends MZmineRunnableModule> moduleClass,
      @Nullable Runnable onFinish, @Nullable Runnable onError, @Nullable Runnable onCancel) {
    // throw exception on headless mode
    if (isHeadLessMode()) {
      throw new IllegalStateException(
          "Cannot setup parameters in headless mode. This needs the parameter setup dialog");
    }

    MZmineModule module = MZmineCore.getModuleInstance(moduleClass);

    if (module == null) {
      MZmineCore.getDesktop().displayMessage("Cannot find module of class " + moduleClass);
      return ExitCode.ERROR;
    }

    ParameterSet moduleParameters = ConfigService.getConfiguration()
        .getModuleParameters(moduleClass);

    logger.info("Setting parameters for module " + module.getName());
    moduleParameters.setModuleNameAttribute(module.getName());

    try {
      ExitCode exitCode = moduleParameters.showSetupDialog(true);
      if (exitCode != ExitCode.OK) {
        return exitCode;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
    logger.finest("Starting module " + module.getName() + " with parameters " + parametersCopy);
    List<Task> tasks = MZmineCore.runMZmineModule(moduleClass, parametersCopy);

    if (onError != null || onFinish != null || onCancel != null) {
      AllTasksFinishedListener.registerCallbacks(tasks, true, onFinish, onError, onCancel);
    }
    return ExitCode.OK;
  }

  public static RawDataFile createNewFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new RawDataFileImpl(name, absPath, storage);
  }

  public static ImagingRawDataFile createNewImagingFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new ImagingRawDataFileImpl(name, absPath, storage);
  }

  /**
   * Standard method to run modules in MZmine
   *
   * @param moduleClass the module class to run
   * @param parameters  the parameter set
   * @return a list of created tasks that were added to the controller
   */
  public static List<Task> runMZmineModule(
      @NotNull Class<? extends MZmineRunnableModule> moduleClass,
      @NotNull ParameterSet parameters) {

    MZmineRunnableModule module = getModuleInstance(moduleClass);

    // Run the module
    final List<Task> newTasks = new ArrayList<>();
    final MZmineProject currentProject = ProjectService.getProject();
    final Instant date = Instant.now();
    logger.finest(() -> "Module " + module.getName() + " called at " + date.toString());
    module.runModule(currentProject, parameters, newTasks, date);
    TaskService.getController().addTasks(newTasks.toArray(new Task[0]));

    return newTasks;
    // Log module run in audit log
    // AuditLogEntry auditLogEntry = new AuditLogEntry(module, parameters,
    // newTasks);
    // currentProject.logProcessingStep(auditLogEntry);
  }


  /**
   * @return headless mode or JavaFX GUI
   */
  public static boolean isHeadLessMode() {
    return DesktopService.isHeadLess();
  }

  /**
   * @return currently just negates {@link #isHeadLessMode()}
   */
  public static boolean isGUI() {
    return !isHeadLessMode();
  }

  /**
   * Use {@link ProjectService#getMetadata()} instead.
   */
  @Deprecated
  public static @NotNull MetadataTable getProjectMetadata() {
    return ProjectService.getMetadata();
  }

  private void init() {
    // In the beginning, set the default locale to English, to avoid
    // problems with conversion of numbers etc. (e.g. decimal separator may
    // be . or , depending on the locale)
    Locale.setDefault(new Locale("en", "US"));
    // initialize by default with all in memory
    MemoryMapStorage.setStoreAllInRam(true);

    logger.fine("Initializing core classes..");
  }
}
