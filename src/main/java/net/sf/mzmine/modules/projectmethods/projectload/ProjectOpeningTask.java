/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.projectmethods.projectload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JInternalFrame;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_0.PeakListOpenHandler_2_0;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_0.RawDataFileOpenHandler_2_0;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_3.PeakListOpenHandler_2_3;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_3.RawDataFileOpenHandler_2_3;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_3.UserParameterOpenHandler_2_3;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_5.PeakListOpenHandler_2_5;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_5.RawDataFileOpenHandler_2_5;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_5.UserParameterOpenHandler_2_5;
import net.sf.mzmine.modules.projectmethods.projectsave.ProjectSavingTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.ProjectManager;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.StreamCopy;

import org.xml.sax.SAXException;

import de.schlichtherle.truezip.zip.ZipEntry;
import de.schlichtherle.truezip.zip.ZipFile;

public class ProjectOpeningTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File openFile;
    private MZmineProjectImpl newProject;

    private RawDataFileOpenHandler rawDataFileOpenHandler;
    private PeakListOpenHandler peakListOpenHandler;
    private UserParameterOpenHandler userParameterOpenHandler;

    private int currentStage;
    private String currentLoadedObjectName;

    // This hashtable maps stored IDs to raw data file objects
    private Hashtable<String, RawDataFile> dataFilesIDMap;

    public ProjectOpeningTask(ParameterSet parameters) {
        this.openFile = parameters.getParameter(
                ProjectLoaderParameters.projectFile).getValue();
        dataFilesIDMap = new Hashtable<String, RawDataFile>();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        if (currentLoadedObjectName == null)
            return "Opening project " + openFile;
        return "Opening project " + openFile + " (" + currentLoadedObjectName
                + ")";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        switch (currentStage) {
        case 2:
            if (rawDataFileOpenHandler == null)
                return 0;
            return rawDataFileOpenHandler.getProgress();
        case 3:
            if (peakListOpenHandler == null)
                return 0;
            return peakListOpenHandler.getProgress();
        case 4:
            if (userParameterOpenHandler == null)
                return 0;
            return userParameterOpenHandler.getProgress();
        case 5:
            return 1;
        default:
            return 0;
        }
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        try {

            logger.info("Started opening project " + openFile);
            setStatus(TaskStatus.PROCESSING);

            // Create a new project
            newProject = new MZmineProjectImpl();
            newProject.setProjectFile(openFile);

            // Get project ZIP stream
            ZipFile zipFile = new ZipFile(openFile);

            // Stage 1 - check version and load configuration
            currentStage++;
            loadVersion(zipFile);
            loadConfiguration(zipFile);
            if (isCanceled()) {
                zipFile.close();
                return;
            }

            // Stage 2 - load raw data files
            currentStage++;
            loadRawDataFiles(zipFile);
            if (isCanceled()) {
                zipFile.close();
                return;
            }

            // Stage 3 - load peak lists
            currentStage++;
            loadPeakLists(zipFile);
            if (isCanceled()) {
                zipFile.close();
                return;
            }

            // Stage 4 - load user parameters
            currentStage++;
            loadUserParameters(zipFile);
            if (isCanceled()) {
                zipFile.close();
                return;
            }

            // Stage 5 - finish and close the project ZIP file
            currentStage++;
            zipFile.close();

            // Final check for cancel
            if (isCanceled())
                return;

            // Close all open frames related to previous project
            JInternalFrame frames[] = MZmineCore.getDesktop()
                    .getInternalFrames();
            for (JInternalFrame frame : frames) {
                // Use doDefailtCloseAction() instead of dispose() to protect
                // the TaskProgressWindow from disposing
                frame.doDefaultCloseAction();
            }

            // Replace the current project with the new one
            ProjectManager projectManager = MZmineCore.getProjectManager();
            projectManager.setCurrentProject(newProject);

            logger.info("Finished opening project " + openFile);

            setStatus(TaskStatus.FINISHED);

        } catch (Throwable e) {

            // If project opening was canceled, parser was stopped by a
            // SAXException which can be safely ignored
            if (isCanceled())
                return;

            setStatus(TaskStatus.ERROR);
            e.printStackTrace();
            errorMessage = "Failed opening project: "
                    + ExceptionUtils.exceptionToString(e);
        }
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {

        logger.info("Canceling opening of project " + openFile);

        setStatus(TaskStatus.CANCELED);

        if (rawDataFileOpenHandler != null)
            rawDataFileOpenHandler.cancel();

        if (peakListOpenHandler != null)
            peakListOpenHandler.cancel();

    }

    /**
     * Load the version info from the ZIP file and checks whether such version
     * can be opened with this MZmine
     */
    private void loadVersion(ZipFile zipFile) throws IOException {

        logger.info("Checking project version");

        ZipEntry versionEntry = zipFile
                .getEntry(ProjectSavingTask.VERSION_FILENAME);

        if (versionEntry == null) {
            throw new IOException(
                    "This file is not valid MZmine 2 project. It does not contain version information.");
        }

        Pattern versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)");

        InputStream versionInputStream = zipFile.getInputStream(versionEntry);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                versionInputStream));
        String projectVersionString = reader.readLine();
        reader.close();

        String mzmineVersionString = MZmineCore.getMZmineVersion();

        Matcher m = versionPattern.matcher(mzmineVersionString);
        if (!m.find()) {
            throw new IOException("Invalid MZmine version "
                    + mzmineVersionString);
        }
        int mzmineMajorVersion = Integer.valueOf(m.group(1));
        int mzmineMinorVersion = Integer.valueOf(m.group(2));

        m = versionPattern.matcher(projectVersionString);
        if (!m.find()) {
            throw new IOException("Invalid project version "
                    + projectVersionString);
        }
        int projectMajorVersion = Integer.valueOf(m.group(1));
        int projectMinorVersion = Integer.valueOf(m.group(2));

        // Check if project was saved with an old version
        if (projectMajorVersion == 1) {
            throw new IOException(
                    "This project was saved with an old version (MZmine "
                            + projectVersionString
                            + ") and it cannot be opened in MZmine "
                            + mzmineVersionString);
        }

        // Check if the project version is 2.0 to 2.2
        if ((projectMajorVersion == 2) && (projectMinorVersion <= 2)) {
            rawDataFileOpenHandler = new RawDataFileOpenHandler_2_0();
            peakListOpenHandler = new PeakListOpenHandler_2_0(dataFilesIDMap);
            return;
        }

        // Check if the project version is 2.3 to 2.4
        if ((projectMajorVersion == 2) && (projectMinorVersion <= 4)) {
            rawDataFileOpenHandler = new RawDataFileOpenHandler_2_3();
            peakListOpenHandler = new PeakListOpenHandler_2_3(dataFilesIDMap);
            userParameterOpenHandler = new UserParameterOpenHandler_2_3(
                    newProject, dataFilesIDMap);
            return;
        }

        // Check if project was saved with a newer version
        if ((projectMajorVersion > mzmineMajorVersion)
                || ((projectMajorVersion == mzmineMajorVersion) && (projectMinorVersion > mzmineMinorVersion))) {
            String warning = "Warning: this project was saved with a newer version of MZmine ("
                    + projectVersionString
                    + "). Opening this project in MZmine "
                    + mzmineVersionString
                    + " may result in errors or loss of information.";
            MZmineCore.getDesktop().displayMessage(warning);
        }

        // Default opening handler for MZmine 2.5 and higher
        rawDataFileOpenHandler = new RawDataFileOpenHandler_2_5();
        peakListOpenHandler = new PeakListOpenHandler_2_5(dataFilesIDMap);
        userParameterOpenHandler = new UserParameterOpenHandler_2_5(newProject,
                dataFilesIDMap);

    }

    /**
     * Load the configuration file from the project zip file
     */
    private void loadConfiguration(ZipFile zipFile) throws IOException {

        logger.info("Loading configuration file");

        ZipEntry configEntry = zipFile
                .getEntry(ProjectSavingTask.CONFIG_FILENAME);

        if (configEntry == null) {
            throw new IOException(
                    "This file is not valid MZmine 2 project. It does not contain configuration data.");
        }

        InputStream configInputStream = zipFile.getInputStream(configEntry);
        File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
        FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
        StreamCopy copyMachine = new StreamCopy();
        copyMachine.copy(configInputStream, fileStream);
        fileStream.close();

        try {
            MZmineCore.getConfiguration().loadConfiguration(tempConfigFile);
        } catch (Exception e) {
            logger.warning("Could not load configuration from the project: "
                    + ExceptionUtils.exceptionToString(e));
        }

        tempConfigFile.delete();
    }

    private void loadRawDataFiles(ZipFile zipFile) throws IOException,
            ParserConfigurationException, SAXException, InstantiationException,
            IllegalAccessException {

        logger.info("Loading raw data files");

        Pattern filePattern = Pattern
                .compile("Raw data file #([\\d]+) (.*)\\.xml$");

        Enumeration zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {

            // Canceled
            if (isCanceled())
                return;

            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            String entryName = entry.getName();
            Matcher fileMatcher = filePattern.matcher(entryName);

            if (fileMatcher.matches()) {
                String fileID = fileMatcher.group(1);
                currentLoadedObjectName = fileMatcher.group(2);

                String scansFileName = entryName.replaceFirst("\\.xml$",
                        ".scans");
                ZipEntry scansEntry = zipFile.getEntry(scansFileName);
                RawDataFile newFile = rawDataFileOpenHandler.readRawDataFile(
                        zipFile, scansEntry, entry);
                newProject.addFile(newFile);
                dataFilesIDMap.put(fileID, newFile);
            }

        }

    }

    private void loadPeakLists(ZipFile zipFile) throws IOException,
            ParserConfigurationException, SAXException, InstantiationException,
            IllegalAccessException {

        logger.info("Loading peak lists");

        Pattern filePattern = Pattern
                .compile("Peak list #([\\d]+) (.*)\\.xml$");

        Enumeration zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {

            // Canceled
            if (isCanceled())
                return;

            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            String entryName = entry.getName();

            Matcher fileMatcher = filePattern.matcher(entryName);

            if (fileMatcher.matches()) {

                currentLoadedObjectName = fileMatcher.group(2);

                InputStream peakListStream = zipFile.getInputStream(entry);

                PeakList newPeakList = peakListOpenHandler
                        .readPeakList(peakListStream);

                newProject.addPeakList(newPeakList);
            }

        }

    }

    private void loadUserParameters(ZipFile zipFile) throws IOException,
            ParserConfigurationException, SAXException, InstantiationException,
            IllegalAccessException {

        // Older versions of MZmine had no parameter saving
        if (userParameterOpenHandler == null)
            return;

        logger.info("Loading user parameters");

        ZipEntry entry = zipFile.getEntry("User parameters.xml");

        // If there are no parameters, just ignore
        if (entry == null)
            return;

        currentLoadedObjectName = "User parameters";

        InputStream userParamStream = zipFile.getInputStream(entry);

        userParameterOpenHandler.readUserParameters(userParamStream);

    }

    public Object[] getCreatedObjects() {
        ArrayList<Object> newObjects = new ArrayList<Object>();
        for (RawDataFile file : newProject.getDataFiles()) {
            newObjects.add(file);
        }
        for (PeakList peakList : newProject.getPeakLists()) {
            newObjects.add(peakList);
        }
        return newObjects.toArray();
    }
}
