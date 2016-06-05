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
import java.io.FileWriter;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
    private PeakList[] peakLists;
    private String plNamePattern = "{}";
    private PeakListSaveHandler[] peakListSaveHandlers;
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

        this.peakLists = parameters.getParameter(XMLExportParameters.peakLists)
                .getValue().getMatchingPeakLists();
        
        this.peakListSaveHandlers = new PeakListSaveHandler[this.peakLists.length];
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        double percentage = 0.0;
        for (PeakListSaveHandler peakListSaveHandler : peakListSaveHandlers) {
            if (peakListSaveHandler != null)
                percentage += peakListSaveHandler.getProgress();
        }
        return percentage / (double) peakListSaveHandlers.length;
    }

    public void cancel() {
        super.cancel();
        for (PeakListSaveHandler peakListSaveHandler : peakListSaveHandlers) {
            if (peakListSaveHandler != null)
                peakListSaveHandler.cancel();
        }
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Exporting peak list(s) " + Arrays.toString(peakLists)
                + " to MPL file(s)";
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        // Process peak lists
        for (int i = 0; i < peakLists.length; i++) {

            PeakList peakList = peakLists[i];
            
            File curFile = fileName;
            try {

                // Filename
                if (substitute) {
                    // Cleanup from illegal filename characters
                    String cleanPlName = peakList.getName().replaceAll(
                            "[^a-zA-Z0-9.-]", "_");
                    // Substitute
                    String newFilename = fileName.getPath().replaceAll(
                            Pattern.quote(plNamePattern), cleanPlName);
                    curFile = new File(newFilename);
                }

                // Open file
                FileWriter writer;
                try {
                    writer = new FileWriter(curFile);
                } catch (Exception e) {
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage("Could not open file " + curFile 
                            + " for writing.");
                    return;
                }

                logger.info("Started saving peak list " + peakList.getName());

                // write the saving file
                FileOutputStream fos = new FileOutputStream(curFile);
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

                PeakListSaveHandler peakListSaveHandler = new PeakListSaveHandler(finalStream,
                        dataFilesIDMap);                
                peakListSaveHandlers[i] = peakListSaveHandler;

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
    
}
