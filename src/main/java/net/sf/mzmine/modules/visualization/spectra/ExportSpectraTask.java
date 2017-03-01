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

package net.sf.mzmine.modules.visualization.spectra;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Range;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.datastore.DataPointStore;
import io.github.msdk.datamodel.datastore.DataPointStoreFactory;
import io.github.msdk.datamodel.files.FileType;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.msspectra.MsSpectrumType;
import io.github.msdk.datamodel.rawdata.IsolationInfo;
import io.github.msdk.datamodel.rawdata.MsFunction;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.io.mzml.MzMLFileExportMethod;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Exports a spectrum to a file.
 *
 */
public class ExportSpectraTask extends AbstractTask {

    // Logger
    private static final Logger LOG = Logger
            .getLogger(ExportSpectraTask.class.getName());

    private final File exportFile;
    private final Scan scan;
    private final String extension;

    private int progress;
    private int progressMax;

    /**
     * Create the task.
     *
     * @param currentScan
     *            scan to export.
     * @param file
     *            file to save scan data to.
     */
    public ExportSpectraTask(final Scan currentScan, final File file,
            final String ext) {
        scan = currentScan;
        exportFile = file;
        extension = ext;
        progress = 0;
        progressMax = 0;
    }

    @Override
    public String getTaskDescription() {
        return "Exporting spectrum # " + scan.getScanNumber() + " for "
                + scan.getDataFile().getName();
    }

    @Override
    public double getFinishedPercentage() {
        return progressMax == 0 ? 0.0
                : (double) progress / (double) progressMax;
    }

    @Override
    public void run() {

        // Update the status of this task
        setStatus(TaskStatus.PROCESSING);

        // Handle text export below
        try {

            // Handle mzML export
            if (extension.equals("mzML")) {
                exportmzML();
            } else {
                // Handle text export
                exportText();
            }
            // Success
            LOG.info("Exported spectrum # " + scan.getScanNumber() + " for "
                    + scan.getDataFile().getName());

            setStatus(TaskStatus.FINISHED);

        } catch (Throwable t) {

            LOG.log(Level.SEVERE, "Spectrum export error", t);
            setStatus(TaskStatus.ERROR);
            setErrorMessage(t.getMessage());
        }
    }

    /**
     * Export the chromatogram - text formats
     *
     * @throws IOException
     *             if there are i/o problems.
     */
    public void exportText() throws IOException {

        // Open the writer - append data if file already exists
        final BufferedWriter writer = new BufferedWriter(
                new FileWriter(exportFile, true));
        try {
            // Write Header row
            switch (extension) {
            case "txt":
                writer.write("Name: Scan#: " + scan.getScanNumber() + ", RT: "
                        + scan.getRetentionTime() + " min");
                writer.newLine();
                break;
            case "mgf":
                writer.write("BEGIN IONS");
                writer.newLine();
                writer.write("PEPMASS=" + scan.getPrecursorMZ());
                writer.newLine();
                writer.write("CHARGE=" + scan.getPrecursorCharge());
                writer.newLine();
                writer.write("Title: Scan#: " + scan.getScanNumber() + ", RT: "
                        + scan.getRetentionTime() + " min");
                writer.newLine();
                writer.newLine();
                break;
            case "msp":
                break;
            }

            // Write the data points
            DataPoint[] dataPoints = scan.getDataPoints();
            final int itemCount = dataPoints.length;
            progressMax = itemCount;

            for (int i = 0; i < itemCount; i++) {

                // Write data point row
                writer.write(dataPoints[i].getMZ() + " "
                        + dataPoints[i].getIntensity());
                writer.newLine();

                progress = i + 1;
            }

            // Write footer row
            if (extension.equals("mgf")) {
                writer.newLine();
                writer.write("END IONS");
                writer.newLine();
            }

            writer.newLine();
        } catch (Exception e) {
            throw (new IOException(e));
        } finally {

            // Close
            writer.close();
        }
    }

    /**
     * Export the chromatogram - mzML format
     *
     * @throws IOException
     *             if there are i/o problems.
     */

    public void exportmzML() throws MSDKException {

        // Initialize objects
        DataPointStore store = DataPointStoreFactory.getMemoryDataStore();
        RawDataFile inputFile = MSDKObjectBuilder.getRawDataFile(
                "MZmine2 mzML export", exportFile, FileType.MZML, store);

        // Get data from MZmine2 style scan
        Integer scanNum = scan.getScanNumber();
        Integer msLevel = scan.getMSLevel();
        DataPoint[] dp = scan.getDataPoints();
        PolarityType polarity = scan.getPolarity();
        Double precursorMZ = scan.getPrecursorMZ();
        String scanDefinition = scan.getScanDefinition();
        Integer precursorCharge = scan.getPrecursorCharge();

        // GUI progress bar updating
        progressMax = dp.length;

        // Initialize MSDK style Scan
        MsFunction dummyFunction = MSDKObjectBuilder.getMsFunction(msLevel);
        MsScan MSDKscan = MSDKObjectBuilder.getMsScan(store, scanNum,
                dummyFunction);

        // Iterate & convert from MZmine2 style to MSDK style
        double mzValues[]=new double[dp.length];
        float intensityValues[]=new float[dp.length];
        for (int i = 0; i < dp.length; i++) {
            mzValues[i]=dp[i].getMZ();
            intensityValues[i]=(float) dp[i].getIntensity();
            // GUI progress bar updating
            progress += 1;
        }

        // Put the data in the scan
        MSDKscan.setDataPoints(mzValues, intensityValues, mzValues.length);

        // Parse if data is profile vs centroid
        MassSpectrumType t = scan.getSpectrumType();
        if (t == MassSpectrumType.CENTROIDED)
            MSDKscan.setSpectrumType(MsSpectrumType.CENTROIDED);
        else
            MSDKscan.setSpectrumType(MsSpectrumType.PROFILE);

        // Parse polarity of data from mzMine2 style to MSDK style
        if (polarity.equals(PolarityType.POSITIVE))
            MSDKscan.setPolarity(
                    io.github.msdk.datamodel.rawdata.PolarityType.POSITIVE);
        else if (polarity.equals(PolarityType.NEGATIVE))
            MSDKscan.setPolarity(
                    io.github.msdk.datamodel.rawdata.PolarityType.NEGATIVE);
        else
            MSDKscan.setPolarity(
                    io.github.msdk.datamodel.rawdata.PolarityType.UNKNOWN);

        // Parse precursor from mzMine2 style to MSDK style
        if (!precursorMZ.equals(0f)) {
            List<IsolationInfo> MSDKprecursor = MSDKscan.getIsolations();
            IsolationInfo MSDKisolationInfo = MSDKObjectBuilder
                    .getIsolationInfo(Range.singleton(precursorMZ), null,
                            precursorMZ, precursorCharge, null);
            MSDKprecursor.add(MSDKisolationInfo);
        }

        // Parse scanDefinition to MSDK style
        MSDKscan.setScanDefinition(scanDefinition);

        inputFile.addScan(MSDKscan);

        // Actually write to disk
        MzMLFileExportMethod method = new MzMLFileExportMethod(inputFile,
                exportFile);
        method.execute();
    }
}
