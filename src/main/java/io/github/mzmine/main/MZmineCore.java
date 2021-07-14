/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.main;

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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  private static TaskControllerImpl taskController;
  private static MZmineConfiguration configuration;
  private static Desktop desktop;
  private static ProjectManagerImpl projectManager;
  private static final List<MemoryMapStorage> storageList = Collections
      .synchronizedList(new ArrayList<>());

  private static final Map<Class<?>, MZmineModule> initializedModules =
      new Hashtable<>();
  private static boolean headLessMode = false;
  // batch exit code is only set if run in headless mode with batch file
  private static ExitCode batchExitCode = null;

  /**
   * Main method
   */
  public static void main(final String[] args) {
    // In the beginning, set the default locale to English, to avoid
    // problems with conversion of numbers etc. (e.g. decimal separator may
    // be . or , depending on the locale)
    Locale.setDefault(new Locale("en", "US"));

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

    logger.fine("Loading core classes..");

    // Create instance of configuration
    configuration = new MZmineConfigurationImpl();

    // Create instances of core modules
    projectManager = new ProjectManagerImpl();
    taskController = new TaskControllerImpl();

    logger.fine("Initializing core classes..");

    projectManager.initModule();
    taskController.initModule();

    MZmineArgumentParser argsParser = new MZmineArgumentParser();
    argsParser.parse(args);

    // keep all in memory? (features, scans, ... in RAM instead of MemoryMapStorage
    switch(argsParser.isKeepInRam()) {
      case NONE -> {
        // nothing in RAM
      }
      case ALL -> MemoryMapStorage.setStoreAllInRam(true);
      case FEATURES -> MemoryMapStorage.setStoreFeaturesInRam(true);
      case MASS_LISTS -> MemoryMapStorage.setStoreMassListsInRam(true);
      case RAW_SCANS ->  MemoryMapStorage.setStoreRawFilesInRam(true);
      case MASSES_AND_FEATURES -> {
        MemoryMapStorage.setStoreMassListsInRam(true);
        MemoryMapStorage.setStoreFeaturesInRam(true);
      }
    }

    // override preferences file by command line argument pref
    File prefFile = argsParser.getPreferencesFile();
    if (prefFile == null) {
      prefFile = MZmineConfiguration.CONFIG_FILE;
    }

    // Load configuration
    if (prefFile.exists() && prefFile.canRead()) {
      try {
        configuration.loadConfiguration(prefFile);
        setTempDirToPreference();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // batch mode defined by command line argument
    File batchFile = argsParser.getBatchFile();
    boolean keepRunningInHeadless = argsParser.isKeepRunningAfterBatch();

    headLessMode = false;
    // If we have no arguments, run in GUI mode, otherwise run in batch mode
    if (batchFile == null && !keepRunningInHeadless) {
      try {
        logger.info("Starting MZmine GUI");
        Application.launch(MZmineGUI.class, args);
      } catch (Throwable e) {
        e.printStackTrace();
        logger.log(Level.SEVERE, "Could not initialize GUI", e);
        System.exit(1);
      }

    } else {
      headLessMode = true;
      desktop = new HeadLessDesktop();

      // Tracker
      GoogleAnalyticsTracker GAT =
          new GoogleAnalyticsTracker("MZmine Loaded (Headless mode)", "/JAVA/Main/GUI");
      Thread gatThread = new Thread(GAT);
      gatThread.setPriority(Thread.MIN_PRIORITY);
      gatThread.start();

      if(batchFile!=null) {
        // load batch
        if ((!batchFile.exists()) || (!batchFile.canRead())) {
          logger.severe("Cannot read batch file " + batchFile);
          System.exit(1);
        }

        // run batch file
        batchExitCode = BatchModeModule.runBatch(projectManager.getCurrentProject(),
            batchFile);
      }

      // option to keep MZmine running after the batch is finished
      // currently used to test - maybe useful to provide an API to access more data or to run other modules on demand
      if (!keepRunningInHeadless) {
        exit();
      }
    }
  }

  /**
   * Exit MZmine (usually used in headless mode)
   */
  public static void exit() {
    if (batchExitCode == ExitCode.OK || batchExitCode == null) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }


  @NotNull
  public static TaskController getTaskController() {
    return taskController;
  }

  /**
   * May return null during application startup when desktop is not ready yet.
   */
  @Nullable
  public static Desktop getDesktop() {
    return desktop;
  }

  public static void setDesktop(Desktop desktop) {
    assert desktop != null;
    MZmineCore.desktop = desktop;
  }

  @NotNull
  public static ProjectManager getProjectManager() {
    assert projectManager != null;
    return projectManager;
  }

  @NotNull
  public static MZmineConfiguration getConfiguration() {
    assert configuration != null;
    return configuration;
  }

  /**
   * Returns the instance of a module of given class
   */
  @SuppressWarnings("unchecked")
  public synchronized static <ModuleType extends MZmineModule> ModuleType getModuleInstance(
      Class<ModuleType> moduleClass) {

    ModuleType module = (ModuleType) initializedModules.get(moduleClass);

    if (module == null) {

      try {

        logger.finest("Creating an instance of the module " + moduleClass.getName());

        // Create instance and init module
        module = moduleClass.getDeclaredConstructor().newInstance();

        // Add to the module list
        initializedModules.put(moduleClass, module);

      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Could not start module " + moduleClass, e);
        e.printStackTrace();
        return null;
      }
    }

    return module;
  }

  public static Collection<MZmineModule> getAllModules() {
    return initializedModules.values();
  }

  public static RawDataFile createNewFile(String name, MemoryMapStorage storage)
      throws IOException {
    return new RawDataFileImpl(name, storage);
  }

  public static IMSRawDataFile createNewIMSFile(String name, MemoryMapStorage storage)
      throws IOException {
    return new IMSRawDataFileImpl(name, storage);
  }

  public static ImagingRawDataFile createNewImagingFile(String name, MemoryMapStorage storage)
      throws IOException {
    return new ImagingRawDataFileImpl(name, storage);
  }

  @NotNull
  public static String getMZmineVersion() {
    try {
      ClassLoader myClassLoader = MZmineCore.class.getClassLoader();
      InputStream inStream = myClassLoader.getResourceAsStream("mzmineversion.properties");
      if (inStream == null) {
        return "0.0";
      }
      Properties properties = new Properties();
      properties.load(inStream);
      String version = properties.getProperty("mzmine.version");
      if ((version == null) || (version.startsWith("$"))) {
        return "0.0";
      }
      return version;
    } catch (Exception e) {
      e.printStackTrace();
      return "0.0";
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
    GoogleAnalyticsTracker GAT =
        new GoogleAnalyticsTracker(module.getName(), "/JAVA/" + module.getName());
    Thread gatThread = new Thread(GAT);
    gatThread.setPriority(Thread.MIN_PRIORITY);
    gatThread.start();

    // Run the module
    final List<Task> newTasks = new ArrayList<>();
    final MZmineProject currentProject = projectManager.getCurrentProject();
    module.runModule(currentProject, parameters, newTasks);
    taskController.addTasks(newTasks.toArray(new Task[0]));

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
    return headLessMode;
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

  public static void registerStorage(MemoryMapStorage storage) {
    storageList.add(storage);
  }

  public static List<MemoryMapStorage> getStorageList() {
    return storageList;
  }
}
