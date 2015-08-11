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

package net.sf.mzmine.modules.projectmethods.projectload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
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
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.StreamCopy;

import org.xml.sax.SAXException;

import com.google.common.io.CountingInputStream;

public class ProjectOpeningTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File openFile;
    private MZmineProjectImpl newProject;

    private RawDataFileOpenHandler rawDataFileOpenHandler;
    private PeakListOpenHandler peakListOpenHandler;
    private UserParameterOpenHandler userParameterOpenHandler;

    private CountingInputStream cis;
    private long totalBytes;
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
        if (cis != null && totalBytes > 0) {
            return (double) cis.getCount()/totalBytes;
        }
        return 0;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

	try {
            // Check if existing raw data files are present
            ProjectManager projectManager = MZmineCore.getProjectManager();
            if (projectManager.getCurrentProject().getDataFiles().length > 0) {
                int dialogResult = JOptionPane
                        .showConfirmDialog(
                                null,
                                "Loading the project will replace the existing raw data files and peak lists. Do you want to proceed?",
                                "Warning", JOptionPane.YES_NO_OPTION);

                if (dialogResult != JOptionPane.YES_OPTION) {
                    cancel();
                    return;
                }
            }

            logger.info("Started opening project " + openFile);
            setStatus(TaskStatus.PROCESSING);

            // Create a new project
            newProject = new MZmineProjectImpl();
            newProject.setProjectFile(openFile);

    	    // Close all windows related to previous project
    	    GUIUtils.closeAllWindows();

    	    // Replace the current project with the new one
    	    projectManager.setCurrentProject(newProject);

    	    // ZIP file size
    	    totalBytes = openFile.length();

    	    // Get project ZIP stream
    	    FileInputStream fis = new FileInputStream(openFile);
    	    cis = new CountingInputStream(fis);
    	    MyZIS zis = new MyZIS(cis);
    	    ZipFile zipFile = new ZipFile(openFile);

            // Stage 1 - check version and load configuration
            loadVersion(zis);
            loadConfiguration(zis);
            if (isCanceled()) {
                zis.closeZIS();
    	        return;
    	    }

            // Stage 2 - load raw data files and peak lists
           loadData(zis, zipFile);
            if (isCanceled()) {
                zis.closeZIS();
    	        return;
    	    }

            // Stage 3 - load user parameters
            loadUserParameters(zis);
            if (isCanceled()) {
                zis.closeZIS();
    	        return;
            }

            // Stage 4 - finish and close the project ZIP file
            zis.closeZIS();

            // Final check for cancel
            if (isCanceled())
                return;

            logger.info("Finished opening project " + openFile);

            setStatus(TaskStatus.FINISHED);

	} catch (Throwable e) {

	    // If project opening was canceled, parser was stopped by a
	    // SAXException which can be safely ignored
	    if (isCanceled())
		return;

	    setStatus(TaskStatus.ERROR);
	    e.printStackTrace();
	    setErrorMessage("Failed opening project: "
		    + ExceptionUtils.exceptionToString(e));
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
    private void loadVersion(ZipInputStream zis) throws IOException {

	logger.info("Checking project version");

        ZipEntry versionEntry = findFile(ProjectSavingTask.VERSION_FILENAME,
                zis);

	if (versionEntry == null) {
	    throw new IOException(
		    "This file is not valid MZmine 2 project. It does not contain version information.");
	}

	Pattern versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)");

	BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
        String projectVersionString = reader.readLine();
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
	    MZmineCore.getDesktop().displayMessage(
		    MZmineCore.getDesktop().getMainWindow(), warning);
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
    private void loadConfiguration(ZipInputStream zis) throws IOException {

	logger.info("Loading configuration file");

	ZipEntry configEntry = findFile(ProjectSavingTask.CONFIG_FILENAME, zis);

	if (configEntry == null) {
	    throw new IOException(
		    "This file is not valid MZmine 2 project. It does not contain configuration data.");
	}

	InputStream configInputStream = zis;
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

    private void loadData(ZipInputStream zis, ZipFile zipFile)
            throws IOException, ParserConfigurationException, SAXException,
            InstantiationException, IllegalAccessException {

        logger.info("Loading raw data files and peak lists");

        Pattern rawFilePattern = Pattern.compile("Raw data file #([\\d]+) (.*)\\.xml$");
        Pattern peakListPattern = Pattern.compile("Peak list #([\\d]+) (.*)\\.xml$");

        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {

            // Canceled
            if (isCanceled())
                return;

            String entryName = entry.getName();
            Matcher rawFileMatcher = rawFilePattern.matcher(entryName);
            Matcher peakListMatcher = peakListPattern.matcher(entryName);

            if (rawFileMatcher.matches()) {
                String fileID = rawFileMatcher.group(1);
                currentLoadedObjectName = rawFileMatcher.group(2);

                String scansFileName = entryName.replaceFirst("\\.xml$",
                        ".scans");

                ZipEntry scansEntry = zipFile.getEntry(scansFileName);

                RawDataFile newFile = rawDataFileOpenHandler.readRawDataFile(
                        zipFile, scansEntry, entry);
                newProject.addFile(newFile);
                dataFilesIDMap.put(fileID, newFile);
            }

            else if (peakListMatcher.matches()) {

                currentLoadedObjectName = peakListMatcher.group(2);

                InputStream peakListStream = zis;

                PeakList newPeakList = peakListOpenHandler
                        .readPeakList(peakListStream);

                newProject.addPeakList(newPeakList);
            }
        }

    }

    private void loadUserParameters(ZipInputStream zis) throws IOException,
	    ParserConfigurationException, SAXException, InstantiationException,
	    IllegalAccessException {

	// Older versions of MZmine had no parameter saving
	if (userParameterOpenHandler == null)
	    return;

	logger.info("Loading user parameters");

	ZipEntry entry = findFile("User parameters.xml",zis);

	// If there are no parameters, just ignore
	if (entry == null)
	    return;

	currentLoadedObjectName = "User parameters";

	InputStream userParamStream = zis;

	userParameterOpenHandler.readUserParameters(userParamStream);

    }

    public ZipEntry findFile(String name, ZipInputStream zis)
    {
        ZipEntry entry;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    return entry;
                }
             }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

}
