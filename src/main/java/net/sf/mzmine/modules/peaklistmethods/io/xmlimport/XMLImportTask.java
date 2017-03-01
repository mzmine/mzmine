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

package net.sf.mzmine.modules.peaklistmethods.io.xmlimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import com.google.common.io.CountingInputStream;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.projectmethods.projectload.PeakListOpenHandler;
import net.sf.mzmine.modules.projectmethods.projectload.version_2_0.PeakListOpenHandler_2_0;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class XMLImportTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // task variables
    private PeakListOpenHandler peakListOpenHander;
    private PeakList buildingPeakList;

    // parameter values
    private final MZmineProject project;
    private final File fileName;

    // progress
    private CountingInputStream cis;
    private long totalBytes;

    /**
     * 
     * @param parameters
     */
    public XMLImportTask(MZmineProject project, ParameterSet parameters) {
        this.project = project;
        fileName = parameters.getParameter(XMLImportParameters.filename)
                .getValue();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        super.cancel();
        if (peakListOpenHander != null)
            peakListOpenHander.cancel();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (cis != null && totalBytes > 0) {
            return (double) cis.getCount() / totalBytes;
        }
        return 0;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Loading peak list from " + fileName;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);
        logger.info("Started parsing file " + fileName);

        try {

            if ((!fileName.exists()) || (!fileName.canRead())) {
                throw new Exception(
                        "Parsing Cancelled, file does not exist or is not readable");
            }

            totalBytes = fileName.length();

            FileInputStream fis = new FileInputStream(fileName);
            cis = new CountingInputStream(fis);
            InputStream finalStream = cis;
            byte b[] = new byte[32];
            fis.read(b);
            String firstLine = new String(b);
            if (!firstLine.contains("<?xml")) {
                FileChannel fc = fis.getChannel();
                fc.position(0);
                @SuppressWarnings("resource")
                ZipInputStream zis = new ZipInputStream(cis);
                zis.getNextEntry();
                finalStream = zis;
            } else {
                FileChannel fc = fis.getChannel();
                fc.position(0);
            }

            Hashtable<String, RawDataFile> dataFilesIDMap = new Hashtable<String, RawDataFile>();
            for (RawDataFile file : project.getDataFiles()) {
                dataFilesIDMap.put(file.getName(), file);
            }

            peakListOpenHander = new PeakListOpenHandler_2_0(dataFilesIDMap);

            buildingPeakList = peakListOpenHander.readPeakList(finalStream);
            finalStream.close();

        } catch (Throwable e) {
            /* we may already have set the status to CANCELED */
            if (getStatus() == TaskStatus.PROCESSING)
                setStatus(TaskStatus.ERROR);
            setErrorMessage(e.toString());
            e.printStackTrace();
            return;
        }

        // Add new peaklist to the project or MZviewer.desktop
        project.addPeakList(buildingPeakList);

        logger.info("Finished parsing " + fileName);
        setStatus(TaskStatus.FINISHED);

    }

}
