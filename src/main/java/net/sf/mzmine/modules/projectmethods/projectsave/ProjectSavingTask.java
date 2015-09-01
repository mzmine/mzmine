/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.projectmethods.projectsave;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.TransformerConfigurationException;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.projectmethods.projectload.ProjectLoaderParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.StreamCopy;

import org.xml.sax.SAXException;

public class ProjectSavingTask extends AbstractTask {

    public static final String VERSION_FILENAME = "MZMINE_VERSION";
    public static final String CONFIG_FILENAME = "configuration.xml";
    public static final String PARAMETERS_FILENAME = "User parameters.xml";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File saveFile;
    private MZmineProjectImpl savedProject;

    private RawDataFileSaveHandler rawDataFileSaveHandler;
    private PeakListSaveHandler peakListSaveHandler;
    private UserParameterSaveHandler userParameterSaveHandler;

    private final int totalSaveItems;
    private int currentStage, finishedSaveItems = 0;
    private String currentSavedObjectName;

    // This hashtable maps raw data files to their ID within the saved project
    private Hashtable<RawDataFile, String> dataFilesIDMap;

    public ProjectSavingTask(MZmineProject project, ParameterSet parameters) {
        this.savedProject = (MZmineProjectImpl) project;
        this.saveFile = parameters
                .getParameter(ProjectLoaderParameters.projectFile).getValue();
        dataFilesIDMap = new Hashtable<RawDataFile, String>();
        this.totalSaveItems = project.getDataFiles().length
                + project.getPeakLists().length;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        if (currentSavedObjectName == null)
            return "Saving project";
        return "Saving project (" + currentSavedObjectName + ")";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {

        if (totalSaveItems == 0)
            return 0.0;

        double currentItemProgress = 0.0;

        switch (currentStage) {
        case 2:
            if (rawDataFileSaveHandler != null)
                currentItemProgress = rawDataFileSaveHandler.getProgress();
            break;
        case 3:
            if (peakListSaveHandler != null)
                currentItemProgress = peakListSaveHandler.getProgress();
            break;
        case 4:
        case 5:
            return 1.0;
        default:
            return 0;
        }

        double progress = ((double) finishedSaveItems + currentItemProgress)
                / totalSaveItems;

        return progress;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {

        logger.info("Canceling saving of project to " + saveFile);

        setStatus(TaskStatus.CANCELED);

        if (rawDataFileSaveHandler != null)
            rawDataFileSaveHandler.cancel();

        if (peakListSaveHandler != null)
            peakListSaveHandler.cancel();

        if (userParameterSaveHandler != null)
            userParameterSaveHandler.cancel();

    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        try {

            logger.info("Saving project to " + saveFile);
            setStatus(TaskStatus.PROCESSING);

            // Prepare a temporary ZIP file. We create this file in the same
            // directory as the final saveFile to avoid moving between
            // filesystems in the last stage (renameTo)
            File tempFile = File.createTempFile(saveFile.getName(), ".tmp",
                    saveFile.getParentFile());
            tempFile.deleteOnExit();

            // Create a ZIP stream writing to the temporary file
            FileOutputStream tempStream = new FileOutputStream(tempFile);
            ZipOutputStream zipStream = new ZipOutputStream(tempStream);

            // Stage 1 - save version and configuration
            currentStage++;
            saveVersion(zipStream);
            saveConfiguration(zipStream);
            if (isCanceled()) {
                zipStream.close();
                tempFile.delete();
                return;
            }

            // Stage 2 - save RawDataFile objects
            currentStage++;
            saveRawDataFiles(zipStream);
            if (isCanceled()) {
                zipStream.close();
                tempFile.delete();
                return;
            }

            // Stage 3 - save PeakList objects
            currentStage++;
            savePeakLists(zipStream);
            if (isCanceled()) {
                zipStream.close();
                tempFile.delete();
                return;
            }

            // Stage 4 - save user parameters
            currentStage++;
            saveUserParameters(zipStream);
            if (isCanceled()) {
                zipStream.close();
                tempFile.delete();
                return;
            }

            // Stage 5 - finish and close the temporary ZIP file
            currentStage++;
            currentSavedObjectName = null;
            zipStream.close();

            // Final check for cancel
            if (isCanceled()) {
                tempFile.delete();
                return;
            }

            // Move the temporary ZIP file to the final location
            if (saveFile.exists() && !saveFile.delete()) {
                throw new IOException("Could not delete old file " + saveFile);
            }

            boolean renameOK = tempFile.renameTo(saveFile);
            if (!renameOK) {
                throw new IOException("Could not move the temporary file "
                        + tempFile + " to the final location " + saveFile);
            }

            // Update the location of the project
            savedProject.setProjectFile(saveFile);

            // Update the window title to reflect the new name of the project
            if (MZmineCore.getDesktop() instanceof MainWindow) {
                MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
                mainWindow.updateTitle();
            }

            logger.info("Finished saving the project to " + saveFile);

            setStatus(TaskStatus.FINISHED);

        } catch (Throwable e) {

            setStatus(TaskStatus.ERROR);

            if (currentSavedObjectName == null) {
                setErrorMessage("Failed saving the project: "
                        + ExceptionUtils.exceptionToString(e));
            } else {
                setErrorMessage("Failed saving the project. Error while saving "
                        + currentSavedObjectName + ": "
                        + ExceptionUtils.exceptionToString(e));
            }

        }
    }

    /**
     * Save the version info
     * 
     * @throws java.io.IOException
     */
    private void saveVersion(ZipOutputStream zipStream) throws IOException {

        zipStream.putNextEntry(new ZipEntry(VERSION_FILENAME));

        String MZmineVersion = MZmineCore.getMZmineVersion();

        zipStream.write(MZmineVersion.getBytes());

    }

    /**
     * Save the configuration file.
     * 
     * @throws java.io.IOException
     */
    private void saveConfiguration(ZipOutputStream zipStream)
            throws IOException {

        logger.info("Saving configuration file");

        currentSavedObjectName = "configuration";

        zipStream.putNextEntry(new ZipEntry(CONFIG_FILENAME));
        File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");

        try {
            MZmineCore.getConfiguration().saveConfiguration(tempConfigFile);
        } catch (Exception e) {
            throw new IOException("Could not save configuration"
                    + ExceptionUtils.exceptionToString(e));
        }

        FileInputStream fileStream = new FileInputStream(tempConfigFile);

        StreamCopy copyMachine = new StreamCopy();
        copyMachine.copy(fileStream, zipStream);

        fileStream.close();
        tempConfigFile.delete();
    }

    /**
     * Save the raw data files
     * 
     * @throws SAXException
     * @throws TransformerConfigurationException
     */
    private void saveRawDataFiles(ZipOutputStream zipStream) throws IOException,
            TransformerConfigurationException, SAXException {

        rawDataFileSaveHandler = new RawDataFileSaveHandler(zipStream);

        RawDataFile rawDataFiles[] = savedProject.getDataFiles();

        for (int i = 0; i < rawDataFiles.length; i++) {

            if (isCanceled())
                return;

            currentSavedObjectName = rawDataFiles[i].getName();
            rawDataFileSaveHandler
                    .writeRawDataFile((RawDataFileImpl) rawDataFiles[i], i + 1);
            dataFilesIDMap.put(rawDataFiles[i], String.valueOf(i + 1));
            finishedSaveItems++;
        }
    }

    /**
     * Save the peak lists
     * 
     * @throws SAXException
     * @throws TransformerConfigurationException
     */
    private void savePeakLists(ZipOutputStream zipStream) throws IOException,
            TransformerConfigurationException, SAXException {

        PeakList peakLists[] = savedProject.getPeakLists();

        for (int i = 0; i < peakLists.length; i++) {

            if (isCanceled())
                return;

            logger.info("Saving peak list: " + peakLists[i].getName());

            String peakListSavedName = "Peak list #" + (i + 1) + " "
                    + peakLists[i].getName();

            zipStream.putNextEntry(new ZipEntry(peakListSavedName + ".xml"));

            peakListSaveHandler = new PeakListSaveHandler(zipStream,
                    dataFilesIDMap);

            currentSavedObjectName = peakLists[i].getName();
            peakListSaveHandler.savePeakList(peakLists[i]);
            finishedSaveItems++;
        }
    }

    /**
     * Save the peak lists
     * 
     * @throws SAXException
     * @throws TransformerConfigurationException
     */
    private void saveUserParameters(ZipOutputStream zipStream)
            throws IOException, TransformerConfigurationException,
            SAXException {

        if (isCanceled())
            return;

        logger.info("Saving user parameters");

        zipStream.putNextEntry(new ZipEntry(PARAMETERS_FILENAME));

        userParameterSaveHandler = new UserParameterSaveHandler(zipStream,
                savedProject, dataFilesIDMap);

        currentSavedObjectName = "User parameters";
        userParameterSaveHandler.saveParameters();

    }

}
