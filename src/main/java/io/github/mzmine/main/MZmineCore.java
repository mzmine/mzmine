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
import java.util.Collection;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;

import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.impl.HeadLessDesktop;
import io.github.mzmine.gui.impl.MainWindow;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.NewVersionCheck.CheckType;
import io.github.mzmine.main.impl.MZmineConfigurationImpl;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.ProjectManagerImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.impl.TaskControllerImpl;
import io.github.mzmine.util.ExitCode;

/**
 * MZmine main class
 */
public final class MZmineCore {

    private static Logger logger = Logger.getLogger(MZmineCore.class.getName());

    private static TaskControllerImpl taskController;
    private static MZmineConfiguration configuration;
    private static Desktop desktop;
    private static ProjectManagerImpl projectManager;

    private static Map<Class<?>, MZmineModule> initializedModules = new Hashtable<Class<?>, MZmineModule>();

    /**
     * Main method
     */
    @SuppressWarnings("unchecked")
    public static void main(String args[]) {

        // In the beginning, set the default locale to English, to avoid
        // problems with conversion of numbers etc. (e.g. decimal separator may
        // be . or , depending on the locale)
        Locale.setDefault(new Locale("en", "US"));

        logger.info("Starting MZmine " + getMZmineVersion());

        // Remove old temporary files, if we find any
        TmpFileCleanup.removeOldTemporaryFiles();

        logger.fine("Loading core classes..");

        // create instance of configuration
        configuration = new MZmineConfigurationImpl();

        // create instances of core modules
        projectManager = new ProjectManagerImpl();
        taskController = new TaskControllerImpl();

        logger.fine("Initializing core classes..");

        projectManager.initModule();
        taskController.initModule();

        logger.fine("Loading modules");

        for (Class<?> moduleClass : MZmineModulesList.MODULES) {

            try {

                logger.finest("Loading module " + moduleClass.getName());

                // Create instance and init module
                MZmineModule moduleInstance = (MZmineModule) moduleClass
                        .newInstance();

                // Add to the module list
                initializedModules.put(moduleClass, moduleInstance);

                // Create an instance of parameter set
                Class<? extends ParameterSet> parameterSetClass = moduleInstance
                        .getParameterSetClass();
                ParameterSet parameterSetInstance = parameterSetClass
                        .newInstance();

                // Add the parameter set to the configuration
                configuration.setModuleParameters(
                        (Class<MZmineModule>) moduleClass,
                        parameterSetInstance);

            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Could not load module " + moduleClass,
                        e);
                e.printStackTrace();
                continue;
            }

        }

        // If we have no arguments, run in GUI mode, otherwise run in batch mode
        if (args.length == 0) {

            // Create the Swing GUI in the event-dispatching thread, as is
            // generally recommended
            Runnable desktopInit = new Runnable() {
                @Override
                public void run() {

                    logger.fine("Initializing GUI");
                    MainWindow mainWindow = new MainWindow();
                    desktop = mainWindow;
                    mainWindow.initModule();

                    // Activate project - bind it to the desktop's project tree
                    MZmineProjectImpl currentProject = (MZmineProjectImpl) projectManager
                            .getCurrentProject();
                    currentProject.activateProject();

                    // add desktop menu icon
                    for (Class<?> moduleClass : MZmineModulesList.MODULES) {
                        MZmineModule module = initializedModules
                                .get(moduleClass);
                        if (module instanceof MZmineRunnableModule) {

                            mainWindow.getMainMenu().addMenuItemForModule(
                                    (MZmineRunnableModule) module);
                        }
                    }
                };

            };

            try {
                SwingUtilities.invokeAndWait(desktopInit);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not initialize GUI", e);
                e.printStackTrace();
                System.exit(1);
            }

        } else {
            desktop = new HeadLessDesktop();
        }

        // load configuration
        if (MZmineConfiguration.CONFIG_FILE.exists()
                && MZmineConfiguration.CONFIG_FILE.canRead()) {
            try {
                configuration
                        .loadConfiguration(MZmineConfiguration.CONFIG_FILE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // if we have GUI, show it now
        if (desktop.getMainWindow() != null
                && !(desktop instanceof HeadLessDesktop)) {
            // update the size and position of the main window
            ParameterSet paramSet = configuration.getPreferences();
            WindowSettingsParameter settings = paramSet
                    .getParameter(MZminePreferences.windowSetttings);
            settings.applySettingsToWindow(desktop.getMainWindow());

            // add last project menu items
            if (desktop instanceof MainWindow) {
                ((MainWindow) desktop).createLastUsedProjectsMenu(
                        configuration.getLastProjects());
                // listen for changes
                configuration.getLastProjectsParameter()
                        .addFileListChangedListener(list -> {
                            // new list of last used projects
                            Desktop desk = getDesktop();
                            if (desk instanceof MainWindow) {
                                ((MainWindow) desk)
                                        .createLastUsedProjectsMenu(list);
                            }
                        });
            }

            // show the GUI
            logger.info("Showing main window");
            desktop.getMainWindow().setVisible(true);

            // show the welcome message
            desktop.setStatusBarText("Welcome to MZmine 2!");

            // Check for updated version
            NewVersionCheck NVC = new NewVersionCheck(CheckType.DESKTOP);
            Thread nvcThread = new Thread(NVC);
            nvcThread.setPriority(Thread.MIN_PRIORITY);
            nvcThread.start();

            // Tracker
            GoogleAnalyticsTracker GAT = new GoogleAnalyticsTracker(
                    "MZmine Loaded (GUI mode)", "/JAVA/Main/GUI");
            Thread gatThread = new Thread(GAT);
            gatThread.setPriority(Thread.MIN_PRIORITY);
            gatThread.start();

            // register shutdown hook only if we have GUI - we don't want to
            // save configuration on exit if we only run a batch
            ShutDownHook shutDownHook = new ShutDownHook();
            Runtime.getRuntime().addShutdownHook(shutDownHook);
        }

        // if arguments were specified (= running without GUI), run the batch
        // mode
        if (args.length > 0 && desktop instanceof HeadLessDesktop) {

            // Tracker
            GoogleAnalyticsTracker GAT = new GoogleAnalyticsTracker(
                    "MZmine Loaded (Headless mode)", "/JAVA/Main/GUI");
            Thread gatThread = new Thread(GAT);
            gatThread.setPriority(Thread.MIN_PRIORITY);
            gatThread.start();

            File batchFile = new File(args[0]);
            if ((!batchFile.exists()) || (!batchFile.canRead())) {
                logger.severe("Cannot read batch file " + batchFile);
                System.exit(1);
            }
            ExitCode exitCode = BatchModeModule
                    .runBatch(projectManager.getCurrentProject(), batchFile);
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

    // Removed @Nonnull
    // Function returns null when called from logger (line 94 of this file)
    // @Nonnull
    public static Desktop getDesktop() {
        return desktop;
    }

    @Nonnull
    public static ProjectManager getProjectManager() {
        return projectManager;
    }

    @Nonnull
    public static MZmineConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the instance of a module of given class
     */
    @SuppressWarnings("unchecked")
    public static <ModuleType> ModuleType getModuleInstance(
            Class<ModuleType> moduleClass) {
        return (ModuleType) initializedModules.get(moduleClass);
    }

    public static Collection<MZmineModule> getAllModules() {
        return initializedModules.values();
    }

    public static RawDataFileWriter createNewFile(String name)
            throws IOException {
        return new RawDataFileImpl(name);
    }

    @Nonnull
    public static String getMZmineVersion() {
        try {
            ClassLoader myClassLoader = MZmineCore.class.getClassLoader();
            InputStream inStream = myClassLoader
                    .getResourceAsStream("mzmineversion.properties");
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

}
