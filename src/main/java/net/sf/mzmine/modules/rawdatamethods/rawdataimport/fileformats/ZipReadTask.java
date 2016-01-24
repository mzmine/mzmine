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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;

import com.google.common.io.Files;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataFileType;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataFileTypeDetector;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportModule;
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

    private File tmpDir, tmpFile;
    private StreamCopy copy = null;
    private Task decompressedOpeningTask = null;

    public ZipReadTask(@Nonnull MZmineProject project, File fileToOpen,
            RawDataFileType fileType) {
        this.project = project;
        this.file = fileToOpen;
        this.fileType = fileType;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // Update task status
        setStatus(TaskStatus.PROCESSING);
        logger.info("Started opening compressed file " + file);

        try {

            // Name of the uncompressed file
            String newName = file.getName();
            if (newName.toLowerCase().endsWith(".zip")
                    || newName.toLowerCase().endsWith(".gz")) {
                newName = FilenameUtils.removeExtension(newName);
            }

            // Create decompressing stream
            FileInputStream fis = new FileInputStream(file);
            InputStream is;
            long decompressedSize = 0;
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
                decompressedSize = (long) (file.length() * 1.5); // Ballpark a
                                                                 // decompressedFile
                                                                 // size so the
                                                                 // GUI can show
                                                                 // progress
                if (decompressedSize < 0)
                    decompressedSize = 0;
                break;
            default:
                setErrorMessage("Cannot decompress file type: " + fileType);
                setStatus(TaskStatus.ERROR);
                return;
            }

            tmpDir = Files.createTempDir();
            tmpFile = new File(tmpDir, newName);
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

            if (isCanceled())
                return;

            // Find the type of the decompressed file
            RawDataFileType fileType = RawDataFileTypeDetector
                    .detectDataFileType(tmpFile);
            logger.finest("File " + tmpFile + " type detected as " + fileType);

            if (fileType == null) {
                setErrorMessage(
                        "Could not determine the file type of file " + newName);
                setStatus(TaskStatus.ERROR);
                return;
            }

            // Run the import module on the decompressed file
            RawDataFileWriter newMZmineFile = MZmineCore.createNewFile(newName);
            decompressedOpeningTask = RawDataImportModule.createOpeningTask(
                    fileType, project, tmpFile, newMZmineFile);

            if (decompressedOpeningTask == null) {
                setErrorMessage("File type " + fileType + " of file " + newName
                        + " is not supported.");
                setStatus(TaskStatus.ERROR);
                return;
            }

            // Run the underlying task
            decompressedOpeningTask.run();

            // Delete the temporary folder
            tmpFile.delete();
            tmpDir.delete();

            if (isCanceled())
                return;

        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not open file " + file.getPath(),
                    e);
            setErrorMessage(ExceptionUtils.exceptionToString(e));
            setStatus(TaskStatus.ERROR);
            return;
        }

        logger.info("Finished opening compressed file " + file);

        // Update task status
        setStatus(TaskStatus.FINISHED);

    }

    public String getTaskDescription() {
        if (decompressedOpeningTask != null)
            return decompressedOpeningTask.getTaskDescription();
        else
            return "Decompressing file " + file;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (decompressedOpeningTask != null)
            return (decompressedOpeningTask.getFinishedPercentage() / 2.0)
                    + 0.5; // Reports 50% to 100%
        if (copy != null) {
            // Reports up to 50%. In case of .gz files, the uncompressed size
            // was only estimated, so we make sure the progress bar doesn't go
            // over 100%
            double copyProgress = copy.getProgress() / 2.0;
            if (copyProgress > 1.0)
                copyProgress = 1.0;
            return copyProgress;
        }
        return 0.0;
    }

    @Override
    public void cancel() {
        super.cancel();
        if (decompressedOpeningTask != null)
            decompressedOpeningTask.cancel();
        if (copy != null)
            copy.cancel();
        if ((tmpFile != null) && (tmpFile.exists()))
            tmpFile.delete();
        if ((tmpDir != null) && (tmpDir.exists()))
            tmpDir.delete();
    }

}
