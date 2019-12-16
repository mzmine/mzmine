/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.impl.MZmineConfigurationImpl;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.ProjectManagerImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.impl.TaskControllerImpl;
import io.github.mzmine.util.ExitCode;
import javafx.application.Application;

/**
 * MZmine main class
 */
public final class MZmineCore {

  private static Logger logger = Logger.getLogger(MZmineCore.class.getName());

  private static TaskControllerImpl taskController;
  private static MZmineConfiguration configuration;
  private static Desktop desktop;
  private static ProjectManagerImpl projectManager;

  private static Map<Class<?>, MZmineModule> initializedModules =
      new Hashtable<Class<?>, MZmineModule>();

  /**
   * Main method
   */
  public static void main(String args[]) {

    // In the beginning, set the default locale to English, to avoid
    // problems with conversion of numbers etc. (e.g. decimal separator may
    // be . or , depending on the locale)
    Locale.setDefault(new Locale("en", "US"));

    /*
     * Configure the logging properties before we start logging
     */
    MZmineLogging.configureLogging();

    logger.info("Starting MZmine " + getMZmineVersion());

    /*
     * Report current working directory
     */
    final String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
    logger.finest("Working directory is " + cwd);

    // Remove old temporary files on a new thread
    Thread cleanupThread = new Thread(new TmpFileCleanup());
    cleanupThread.setPriority(Thread.MIN_PRIORITY);
    cleanupThread.start();

    logger.fine("Loading core classes..");

    // create instance of configuration
    configuration = new MZmineConfigurationImpl();

    // create instances of core modules
    projectManager = new ProjectManagerImpl();
    taskController = new TaskControllerImpl();

    logger.fine("Initializing core classes..");

    projectManager.initModule();
    taskController.initModule();

    // Load configuration
    if (MZmineConfiguration.CONFIG_FILE.exists() && MZmineConfiguration.CONFIG_FILE.canRead()) {
      try {
        configuration.loadConfiguration(MZmineConfiguration.CONFIG_FILE);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Activate project - bind it to the desktop's project tree
    MZmineProjectImpl currentProject = (MZmineProjectImpl) projectManager.getCurrentProject();
    currentProject.activateProject();

    // If we have no arguments, run in GUI mode, otherwise run in batch mode
    if (args.length == 0) {
      try {
        logger.info("Starting MZmine GUI");
        Application.launch(MZmineGUI.class, args);
      } catch (Throwable e) {
        e.printStackTrace();
        logger.log(Level.SEVERE, "Could not initialize GUI", e);
        System.exit(1);
      }

    } else {
      desktop = new HeadLessDesktop();

      // Tracker
      GoogleAnalyticsTracker GAT =
          new GoogleAnalyticsTracker("MZmine Loaded (Headless mode)", "/JAVA/Main/GUI");
      Thread gatThread = new Thread(GAT);
      gatThread.setPriority(Thread.MIN_PRIORITY);
      gatThread.start();

      File batchFile = new File(args[0]);
      if ((!batchFile.exists()) || (!batchFile.canRead())) {
        logger.severe("Cannot read batch file " + batchFile);
        System.exit(1);
      }
      ExitCode exitCode = BatchModeModule.runBatch(projectManager.getCurrentProject(), batchFile);
      if (exitCode == ExitCode.OK)
        System.exit(0);
      else
        System.exit(1);

    }

  }

  @Nonnull
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

  @Nonnull
  public static void setDesktop(Desktop desktop) {
    assert desktop != null;
    MZmineCore.desktop = desktop;
  }

  @Nonnull
  public static ProjectManager getProjectManager() {
    assert projectManager != null;
    return projectManager;
  }

  @Nonnull
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
        module = (ModuleType) moduleClass.getDeclaredConstructor().newInstance();

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

  public static RawDataFileWriter createNewFile(String name) throws IOException {
    return new RawDataFileImpl(name);
  }

  @Nonnull
  public static String getMZmineVersion() {
    try {
      ClassLoader myClassLoader = MZmineCore.class.getClassLoader();
      InputStream inStream = myClassLoader.getResourceAsStream("mzmineversion.properties");
      if (inStream == null)
        return "0.0";
      Properties properties = new Properties();
      properties.load(inStream);
      String version = properties.getProperty("mzmine.version");
      if ((version == null) || (version.startsWith("$")))
        return "0.0";
      return version;
    } catch (Exception e) {
      e.printStackTrace();
      return "0.0";
    }
  }

  public static void runMZmineModule(@Nonnull Class<? extends MZmineRunnableModule> moduleClass,
      @Nonnull ParameterSet parameters) {

    MZmineRunnableModule module = (MZmineRunnableModule) getModuleInstance(moduleClass);

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

    // Log module run in audit log
    // AuditLogEntry auditLogEntry = new AuditLogEntry(module, parameters,
    // newTasks);
    // currentProject.logProcessingStep(auditLogEntry);

  }

}
