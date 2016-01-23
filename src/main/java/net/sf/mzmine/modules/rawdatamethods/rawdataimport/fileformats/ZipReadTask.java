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

package net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataFileType;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportModule;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.StreamCopy;

public class ZipReadTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final File file;
    private final @Nonnull MZmineProject project;
    private final RawDataFileType fileType;

    private StreamCopy copy;

    public ZipReadTask(@Nonnull MZmineProject project, File fileToOpen,
            RawDataFileType fileType) {
        this.project = project;
        this.file = fileToOpen;
        this.fileType = fileType;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        return copy == null ? 0 : copy.getProgress();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // Update task status
        setStatus(TaskStatus.PROCESSING);
        logger.info("Started decompressing file " + file);

        try {

            // Name of the uncompressed file
            String newName = file.getName();
            if (newName.toLowerCase().endsWith(".zip")
                    || newName.toLowerCase().endsWith(".gz")) {
                newName = FilenameUtils.removeExtension(newName);
            }
            long decompressedSize = 0;

            // Create decompressing stream
            FileInputStream fis = new FileInputStream(file);
            InputStream is;
            switch (fileType) {
            case ZIP:
                ZipInputStream zis = new ZipInputStream(fis);
                ZipEntry entry = zis.getNextEntry();
                newName = entry.getName();
                decompressedSize = entry.getSize();
                if (decompressedSize < 0)
                    decompressedSize = 0;
                is = zis;
                break;
            case GZIP:
                is = new GZIPInputStream(fis);
                break;
            default:
                setErrorMessage("Cannot decompress file type: " + fileType);
                setStatus(TaskStatus.ERROR);
                return;
            }

            File tmpDir = Files.createTempDirectory("mzmine").toFile();

            File tmpFile = new File(tmpDir, newName);
            logger.finest("Decompressing to file " + tmpFile);
            tmpFile.deleteOnExit();
            tmpDir.deleteOnExit();
            FileOutputStream ous = new FileOutputStream(tmpFile);

            // Decompress the contents
            copy = new StreamCopy();
            copy.copy(is, ous, decompressedSize);

            // Close the streams
            is.close();
            ous.close();

            // Run the import module on the decompressed file
            RawDataImportModule importModule = MZmineCore
                    .getModuleInstance(RawDataImportModule.class);
            ParameterSet parameters = MZmineCore.getConfiguration()
                    .getModuleParameters(RawDataImportModule.class)
                    .cloneParameterSet();
            parameters.getParameter(RawDataImportParameters.fileNames)
                    .setValue(new File[] { tmpFile });
            List<Task> newTasks = new ArrayList<>();
            importModule.runModule(project, parameters, newTasks);
            MZmineCore.getTaskController()
                    .addTasks(newTasks.toArray(new Task[0]));

        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not open file " + file.getPath(),
                    e);
            setErrorMessage(ExceptionUtils.exceptionToString(e));
            setStatus(TaskStatus.ERROR);
            return;
        }

        logger.info("Finished decompressing " + file);

        // Update task status
        setStatus(TaskStatus.FINISHED);

    }

    public String getTaskDescription() {
        return "Decompressing file " + file;
    }

    @Override
    public void cancel() {
        if (copy != null)
            copy.cancel();
        super.cancel();
    }

}
