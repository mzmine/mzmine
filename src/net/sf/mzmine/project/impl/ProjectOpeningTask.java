/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.impl.xstream.MZmineXStream;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.UnclosableInputStream;

/**
 * Project opening task using XStream library
 */
public class ProjectOpeningTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private MZmineXStream xstream;

    private File openFile;
    private ZipInputStream zipStream;
    private UnclosableInputStream unclosableZipStream;
    private StoredProjectDescription description;
    private File scanDataFiles[];

    private MZmineProjectImpl loadedProject;

    private int currentStage;

    private long readBytes;

    public ProjectOpeningTask(File openFile) {
        this.openFile = openFile;
        xstream = new MZmineXStream();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        String taskDescription = "Opening project " + openFile;
        switch (currentStage) {
        case 3:
            return taskDescription + " (raw data points)";
        case 4:
            return taskDescription + " (scan objects)";
        case 5:
            return taskDescription + " (peak list objects)";
        default:
            return taskDescription;
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        switch (currentStage) {
        case 3:
            if (description.getTotalNumOfScanFileBytes() == 0)
                return 0;
            else
                return (double) readBytes
                        / description.getTotalNumOfScanFileBytes();
        case 4:
            if (description.getNumOfScans() == 0)
                return 0;
            else
                return (double) xstream.getNumOfDeserializedScans()
                        / description.getNumOfScans();
        case 5:
            if (description.getNumOfPeakListRows() == 0)
                return 0;
            else
                return (double) xstream.getNumOfDeserializedRows()
                        / description.getNumOfPeakListRows();
        case 6:
            return 1f;
        default:
            return 0f;
        }
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        try {

            logger.info("Started opening project " + openFile);
            status = TaskStatus.PROCESSING;

            // Get project ZIP stream
            FileInputStream fileStream = new FileInputStream(openFile);
            zipStream = new ZipInputStream(fileStream);
            unclosableZipStream = new UnclosableInputStream(zipStream);

            // Stage 1 - load project description
            currentStage++;
            description = loadProjectDescription();

            // Stage 2 - load configuration
            currentStage++;
            loadConfiguration();

            // Stage 3 - load scan data files
            currentStage++;
            scanDataFiles = new File[description.getNumOfScanFiles()];
            for (int i = 0; i < scanDataFiles.length; i++) {
                scanDataFiles[i] = File.createTempFile("mzmine", ".scans");
                scanDataFiles[i].deleteOnExit();
                loadScanDataFile(scanDataFiles[i]);
            }

            // Stage 4 - load RawDataFile objects
            currentStage++;
            loadRawDataObjects();

            // Stage 5 - load PeakList objects
            currentStage++;
            loadPeakListObjects();

            // Stage 6 - load MZmineProjectImpl instance
            currentStage++;
            loadMZmineProject();

            // Finish and close the project ZIP file
            zipStream.close();

            // Final check for cancel
            if (status == TaskStatus.CANCELED)
                return;

            // Set the project
            ProjectManagerImpl projectManager = ProjectManagerImpl.getInstance();
            projectManager.setCurrentProject(loadedProject);

            logger.info("Finished opening project " + openFile);
            status = TaskStatus.FINISHED;

        } catch (Throwable e) {
            status = TaskStatus.ERROR;
            errorMessage = "Failed opening project: "
                    + ExceptionUtils.exceptionToString(e);
        }
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Canceling opening of project " + openFile);
        status = TaskStatus.CANCELED;
    }

    private StoredProjectDescription loadProjectDescription()
            throws IOException, ClassNotFoundException {
        zipStream.getNextEntry();
        ObjectInputStream objectStream = xstream.createObjectInputStream(unclosableZipStream);
        StoredProjectDescription description = (StoredProjectDescription) objectStream.readObject();
        objectStream.close();
        return description;
    }

    private void loadConfiguration() throws IOException {
        zipStream.getNextEntry();
        File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
        FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
        int len;
        byte buffer[] = new byte[1 << 10]; // 1 MB buffer
        while ((len = zipStream.read(buffer)) > 0) {
            if (status == TaskStatus.CANCELED)
                return;
            fileStream.write(buffer, 0, len);
        }
        fileStream.close();
        MZmineCore.loadConfiguration(tempConfigFile);
        tempConfigFile.delete();
    }

    private void loadScanDataFile(File targetFile) throws IOException,
            ClassNotFoundException {
        zipStream.getNextEntry();
        FileOutputStream fileStream = new FileOutputStream(targetFile);
        int len;
        byte buffer[] = new byte[1 << 10]; // 1 MB buffer
        while ((len = zipStream.read(buffer)) > 0) {
            if (status == TaskStatus.CANCELED)
                return;
            fileStream.write(buffer, 0, len);
            readBytes += len;
        }
        fileStream.close();
    }

    private void loadRawDataObjects() throws IOException,
            ClassNotFoundException {
        zipStream.getNextEntry();
        ObjectInputStream objectStream = xstream.createObjectInputStream(unclosableZipStream);

        int scanDataFileIndex = 0;
        for (int i = 0; i < description.getNumOfDataFiles(); i++) {
            RawDataFileImpl rawDataFile = (RawDataFileImpl) objectStream.readObject();
            File scanDataFile = scanDataFiles[scanDataFileIndex];
            scanDataFileIndex++;
            rawDataFile.setScanDataFile(scanDataFile);
        }
        objectStream.close();
    }

    private void loadPeakListObjects() throws IOException,
            ClassNotFoundException {
        zipStream.getNextEntry();
        ObjectInputStream objectStream = xstream.createObjectInputStream(unclosableZipStream);

        for (int i = 0; i < description.getNumOfPeakLists(); i++) {

            // Read the number of rows
            int numOfRows = objectStream.readInt();

            // Read the PeakListRow instances
            for (int row = 0; row < numOfRows; row++) {
                objectStream.readObject();
            }

            // Read the actual PeakList object
            objectStream.readObject();
        }
        objectStream.close();
    }

    private void loadMZmineProject() throws IOException, ClassNotFoundException {
        zipStream.getNextEntry();
        ObjectInputStream objectStream = xstream.createObjectInputStream(unclosableZipStream);
        loadedProject = (MZmineProjectImpl) objectStream.readObject();
        objectStream.close();
        // we need to update the project's location
        loadedProject.setProjectFile(openFile);
    }

}
