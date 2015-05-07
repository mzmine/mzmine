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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.xmlexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.projectmethods.projectsave.PeakListSaveHandler;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class XMLExportTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private PeakList peakList;
    private PeakListSaveHandler peakListSaveHandler;
    public static DateFormat dateFormat = new SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss");

    // parameter values
    private File fileName;
    private boolean compression;

    /**
     * @param peakList
     * @param parameters
     */
    public XMLExportTask(ParameterSet parameters) {

        fileName = parameters.getParameter(XMLExportParameters.filename)
                .getValue();
        compression = parameters.getParameter(XMLExportParameters.compression)
                .getValue();

        this.peakList = parameters.getParameter(XMLExportParameters.peakList)
                .getValue().getMatchingPeakLists()[0];

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (peakListSaveHandler == null)
            return 0;
        return peakListSaveHandler.getProgress();
    }

    public void cancel() {
        super.cancel();
        if (peakListSaveHandler != null)
            peakListSaveHandler.cancel();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Saving peak list " + peakList + " to " + fileName;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        try {

            setStatus(TaskStatus.PROCESSING);
            logger.info("Started saving peak list " + peakList.getName());

            // write the saving file
            FileOutputStream fos = new FileOutputStream(fileName);
            OutputStream finalStream = fos;

            if (compression) {
                @SuppressWarnings("resource")
                ZipOutputStream zos = new ZipOutputStream(fos);
                zos.setLevel(9);
                zos.putNextEntry(new ZipEntry(fileName.getName()));
                finalStream = zos;
            }

            Hashtable<RawDataFile, String> dataFilesIDMap = new Hashtable<RawDataFile, String>();
            for (RawDataFile file : peakList.getRawDataFiles()) {
                dataFilesIDMap.put(file, file.getName());
            }

            peakListSaveHandler = new PeakListSaveHandler(finalStream,
                    dataFilesIDMap);

            peakListSaveHandler.savePeakList(peakList);

            finalStream.close();

        } catch (Exception e) {
            /* we may already have set the status to CANCELED */
            if (getStatus() == TaskStatus.PROCESSING) {
                setStatus(TaskStatus.ERROR);
            }
            setErrorMessage(e.toString());
            e.printStackTrace();
            return;
        }

        logger.info("Finished saving " + peakList.getName());
        setStatus(TaskStatus.FINISHED);
    }

}
