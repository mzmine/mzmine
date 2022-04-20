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

package io.github.mzmine.main;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.impl.MZmineConfigurationImpl;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.project.impl.ProjectManagerImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.impl.TaskControllerImpl;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.javafx.FxThreadUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
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
  private final Desktop defaultHeadlessDesktop = new HeadLessDesktop();
  private final List<MemoryMapStorage> storageList = Collections.synchronizedList(
      new ArrayList<>());
  private final Map<Class<?>, MZmineModule> initializedModules = new Hashtable<>();
  private TaskControllerImpl taskController;
  private MZmineConfiguration configuration;
  private Desktop desktop;
  private ProjectManagerImpl projectManager;
  private boolean headLessMode = true;
  private boolean tdfPseudoProfile = false;
  // batch exit code is only set if run in headless mode with batch file
  private ExitCode batchExitCode = null;

  private MZmineCore() {
    init();
  }

  /**
   * Main method
   */
  public static void main(final String[] args) {
    try {
      logger.info("Starting MZmine " + getMZmineVersion());
      /*
       * Dump the MZmine and JVM arguments for debugging purposes
       */
      final String mzmineArgsString = String.join(" ", args);
      final List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
      final String jvmArgsString = String.join(" ", jvmArgs);
      final String classPathString = System.getProperty("java.class.path");
      logger.finest("MZmine arguments: " + mzmineArgsString);
      logger.finest("Java VM arguments: " + jvmArgsString);
      logger.finest("Java class path: " + classPathString);

      /*
       * Report current working and temporary directory
       */
      final String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
      logger.finest("Working directory is " + cwd);
      logger.finest("Default temporary directory is " + System.getProperty("java.io.tmpdir"));

      // Remove old temporary files on a new thread
      Thread cleanupThread = new Thread(new TmpFileCleanup()); // check regular temp dir
      cleanupThread.setPriority(Thread.MIN_PRIORITY);
      cleanupThread.start();

      MZmineArgumentParser argsParser = new MZmineArgumentParser();
      argsParser.parse(args);
      getInstance().tdfPseudoProfile = argsParser.isLoadTdfPseudoProfile();

      // override preferences file by command line argument pref
      final File prefFile = Objects.requireNonNullElse(argsParser.getPreferencesFile(),
          MZmineConfiguration.CONFIG_FILE);

      boolean updateTempDir = false;
      // Load configuration
      if (prefFile.exists() && prefFile.canRead()) {
        try {
          getInstance().configuration.loadConfiguration(prefFile);
          updateTempDir = true;
        } catch (Exception e) {
          logger.log(Level.WARNING,
              "Error while reading configuration " + prefFile.getAbsolutePath(), e);
        }
      } else {
        logger.log(Level.WARNING, "Cannot read configuration " + prefFile.getAbsolutePath());
      }

      // override temp directory
      final File tempDirectory = argsParser.getTempDirectory();
      if (tempDirectory != null) {
        // needs to be accessible
        if (FileAndPathUtil.createDirectory(tempDirectory)) {
          getInstance().configuration.getPreferences()
              .setParameter(MZminePreferences.tempDirectory, tempDirectory);
          updateTempDir = true;
        } else {
          logger.log(Level.WARNING,
              "Cannot create or access temp file directory that was set via program argument: "
                  + tempDirectory.getAbsolutePath());
        }
      }

      // set temp directory
      if (updateTempDir) {
        setTempDirToPreference();
      }

      KeepInMemory keepInMemory = argsParser.isKeepInMemory();
      if (keepInMemory != null) {
        // set to preferences
        getInstance().configuration.getPreferences()
            .setParameter(MZminePreferences.memoryOption, keepInMemory);
      } else {
        keepInMemory = getInstance().configuration.getPreferences()
            .getParameter(MZminePreferences.memoryOption).getValue();
      }

      // apply memory management option
      keepInMemory.enforceToMemoryMapping();

      // batch mode defined by command line argument
      File batchFile = argsParser.getBatchFile();
      boolean keepRunningInHeadless = argsParser.isKeepRunningAfterBatch();

      getInstance().headLessMode = (batchFile != null || keepRunningInHeadless);
      // If we have no arguments, run in GUI mode, otherwise run in batch mode
      if (!getInstance().headLessMode) {
        try {
          logger.info("Starting MZmine GUI");
          Application.launch(MZmineGUI.class, args);
        } catch (Throwable e) {
          e.printStackTrace();
          logger.log(Level.SEVERE, "Could not initialize GUI", e);
          System.exit(1);
        }
      } else {
        getInstance().desktop = getInstance().defaultHeadlessDesktop;

        // Tracker
        GoogleAnalyticsTracker GAT = new GoogleAnalyticsTracker("MZmine Loaded (Headless mode)",
            "/JAVA/Main/GUI");
        Thread gatThread = new Thread(GAT);
        gatThread.setPriority(Thread.MIN_PRIORITY);
        gatThread.start();

        if (batchFile != null) {
          // load batch
          if ((!batchFile.exists()) || (!batchFile.canRead())) {
            logger.severe("Cannot read batch file " + batchFile);
            System.exit(1);
          }

          // run batch file
          getInstance().batchExitCode = BatchModeModule.runBatch(
              getInstance().projectManager.getCurrentProject(), batchFile, Instant.now());
        }

        // option to keep MZmine running after the batch is finished
        // currently used to test - maybe useful to provide an API to access more data or to run other modules on demand
        if (!keepRunningInHeadless) {
          exit();
        }
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Error during MZmine start up", ex);
      exit();
    }
  }

  public static MZmineCore getInstance() {
    return instance;
  }

  /**
   * Exit MZmine (usually used in headless mode)
   */
  public static void exit() {
    if (instance.batchExitCode == ExitCode.OK || instance.batchExitCode == null) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  @NotNull
  public static TaskController getTaskController() {
    return instance.taskController;
  }

  /**
   * The current desktop or a default headless desktop (e.g., during app startup).
   *
   * @return the current desktop or the default headless desktop if still during app startup
   */
  @NotNull
  public static Desktop getDesktop() {
    return instance.desktop == null ? instance.defaultHeadlessDesktop : instance.desktop;
  }

  public static void setDesktop(Desktop desktop) {
    assert desktop != null;
    getInstance().desktop = desktop;
  }

  @NotNull
  public static ProjectManager getProjectManager() {
    assert getInstance().projectManager != null;
    return getInstance().projectManager;
  }

  @NotNull
  public static MZmineConfiguration getConfiguration() {
    assert getInstance().configuration != null;
    return getInstance().configuration;
  }

  /**
   * Returns the instance of a module of given class
   */
  @SuppressWarnings("unchecked")
  public synchronized static <ModuleType extends MZmineModule> ModuleType getModuleInstance(
      Class<ModuleType> moduleClass) {

    ModuleType module = (ModuleType) getInstance().initializedModules.get(moduleClass);

    if (module == null) {

      try {

        logger.finest("Creating an instance of the module " + moduleClass.getName());

        // Create instance and init module
        module = moduleClass.getDeclaredConstructor().newInstance();

        // Add to the module list
        getInstance().initializedModules.put(moduleClass, module);

      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Could not start module " + moduleClass, e);
        e.printStackTrace();
        return null;
      }
    }

    return module;
  }

  public static Collection<MZmineModule> getAllModules() {
    return getInstance().initializedModules.values();
  }

  public static RawDataFile createNewFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new RawDataFileImpl(name, absPath, storage);
  }

  public static IMSRawDataFile createNewIMSFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new IMSRawDataFileImpl(name, absPath, storage);
  }

  public static ImagingRawDataFile createNewImagingFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new ImagingRawDataFileImpl(name, absPath, storage);
  }

  @NotNull
  public static Semver getMZmineVersion() {
    try {
      ClassLoader myClassLoader = MZmineCore.class.getClassLoader();
      InputStream inStream = myClassLoader.getResourceAsStream("mzmineversion.properties");
      if (inStream == null) {
        return new Semver("3-SNAPSHOT", SemverType.LOOSE);
      }
      Properties properties = new Properties();
      properties.load(inStream);
      String versionString = properties.getProperty("version.semver");
      if ((versionString == null) || (versionString.startsWith("$"))) {
        return new Semver("3-SNAPSHOT", SemverType.LOOSE);
      }
      Semver version = new Semver(versionString, SemverType.LOOSE);
      // for now add beta here - jpackage does not work with -beta at version
      version = version.withSuffix("beta");
      return version;
    } catch (Exception e) {
      e.printStackTrace();
      return new Semver("3-SNAPSHOT", SemverType.LOOSE);
    }
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

    // Usage Tracker
    GoogleAnalyticsTracker GAT = new GoogleAnalyticsTracker(module.getName(),
        "/JAVA/" + module.getName());
    Thread gatThread = new Thread(GAT);
    gatThread.setPriority(Thread.MIN_PRIORITY);
    gatThread.start();

    // Run the module
    final List<Task> newTasks = new ArrayList<>();
    final MZmineProject currentProject = getInstance().projectManager.getCurrentProject();
    final Instant date = Instant.now();
    logger.finest(() -> "Module " + module.getName() + " called at " + date.toString());
    module.runModule(currentProject, parameters, newTasks, date);
    getInstance().taskController.addTasks(newTasks.toArray(new Task[0]));

    return newTasks;
    // Log module run in audit log
    // AuditLogEntry auditLogEntry = new AuditLogEntry(module, parameters,
    // newTasks);
    // currentProject.logProcessingStep(auditLogEntry);
  }

  private static void setTempDirToPreference() {
    final File tempDir = getConfiguration().getPreferences()
        .getParameter(MZminePreferences.tempDirectory).getValue();
    if (tempDir == null) {
      logger.warning(() -> "Invalid temporary directory.");
      return;
    }

    if (!tempDir.exists()) {
      if (!tempDir.mkdirs()) {
        logger.warning(() -> "Could not create temporary directory " + tempDir.getAbsolutePath());
        return;
      }
    }

    if (tempDir.isDirectory()) {
      System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());
      logger.finest(() -> "Working temporary directory is " + System.getProperty("java.io.tmpdir"));
      // check the new temp dir for old files.
      Thread cleanupThread2 = new Thread(new TmpFileCleanup());
      cleanupThread2.setPriority(Thread.MIN_PRIORITY);
      cleanupThread2.start();
    }
  }

  /**
   * @return headless mode or JavaFX GUI
   */
  public static boolean isHeadLessMode() {
    return getInstance().headLessMode;
  }

  /**
   * @param r runnable to either run directly or on the JavaFX thread
   */
  public static void runLater(Runnable r) {
    if (isHeadLessMode() || Platform.isFxApplicationThread()) {
      r.run();
    } else {
      Platform.runLater(r);
    }
  }

  /**
   * Simulates Swing's invokeAndWait(). Based on https://news.kynosarges.org/2014/05/01/simulating-platform-runandwait/
   */
  public static void runOnFxThreadAndWait(Runnable r) {
    FxThreadUtil.runOnFxThreadAndWait(r);
  }

  public static void registerStorage(MemoryMapStorage storage) {
    getInstance().storageList.add(storage);
  }

  public static List<MemoryMapStorage> getStorageList() {
    return getInstance().storageList;
  }

  protected void init() {
    // In the beginning, set the default locale to English, to avoid
    // problems with conversion of numbers etc. (e.g. decimal separator may
    // be . or , depending on the locale)
    Locale.setDefault(new Locale("en", "US"));
    // initialize by default with all in memory
    MemoryMapStorage.setStoreAllInRam(true);

    logger.fine("Loading core classes..");
    // Create instance of configuration
    configuration = new MZmineConfigurationImpl();

    // Create instances of core modules
    projectManager = new ProjectManagerImpl();
    taskController = new TaskControllerImpl();

    logger.fine("Initializing core classes..");

    projectManager.initModule();
    taskController.initModule();
  }

  public boolean isTdfPseudoProfile() {
    return tdfPseudoProfile;
  }
}
