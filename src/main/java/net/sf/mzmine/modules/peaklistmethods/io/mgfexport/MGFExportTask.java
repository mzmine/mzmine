/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.io.mgfexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class MGFExportTask extends AbstractTask 
{
    private final PeakList[] peakLists;
    private final File fileName;
    private final String plNamePattern = "{}";
    private final boolean fractionalMZ;
    private final String roundMode;
    
    MGFExportTask(ParameterSet parameters) 
    {
        this.peakLists = parameters.getParameter(MGFExportParameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();
        
        this.fileName = parameters.getParameter(MGFExportParameters.FILENAME)
                .getValue();
        
        this.fractionalMZ = parameters.getParameter(MGFExportParameters.FRACTIONAL_MZ)
                .getValue();
        
        this.roundMode = parameters.getParameter(MGFExportParameters.ROUND_MODE)
                .getValue();
    }
    
    public double getFinishedPercentage() {
        return 0.0;
    }

    public String getTaskDescription() {
        return "Exporting peak list(s) " 
                + Arrays.toString(peakLists) + " to MGF file(s)";
    }
    
    public void run() 
    {
        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        // Process peak lists
        for (PeakList peakList: peakLists) {

            // Filename
            File curFile = fileName;
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

            try {
                exportPeakList(peakList, writer, curFile);
            } catch (IOException e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Error while writing into file " + curFile +
                        ": " + e.getMessage());
                return;
            }

            // Cancel?
            if (isCanceled()) {
                return;
            }

            // Close file
            try {
                writer.close();
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not close file " + curFile);
                return;
            }

            // If peak list substitution pattern wasn't found, 
            // treat one peak list only
            if (!substitute)
                break;
        }

        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);
    }

    private void exportPeakList(PeakList peakList, FileWriter writer, File curFile)
            throws IOException
    {
        final String newLine = System.lineSeparator();
        
        for (PeakListRow row : peakList.getRows())
        {
            IsotopePattern ip = row.getBestIsotopePattern();
            if (ip == null) continue;
            
            writer.write("BEGIN IONS" + newLine);
            
            String rowID = Integer.toString(row.getID());
            if (rowID != null) writer.write("FEATURE_ID=" + rowID + newLine);

            // Find mass of the highest peak
            double maxHeight = 0.0;
            Double mass = null;
            for (Feature peak : row.getPeaks()) {
                double height = peak.getHeight();
                if (height > maxHeight) {
                    maxHeight = height;
                    mass = peak.getMZ();
                }
            }

            if (mass != null) writer.write("PEPMASS=" + mass + newLine);
            
            String retTimeInSeconds = Double.toString(row.getAverageRT() * 60);
            if (retTimeInSeconds != null) 
                writer.write("RTINSECONDS=" + retTimeInSeconds + newLine);
            
            if (rowID != null) writer.write("SCANS=" + rowID + newLine);
            
            writer.write("MSLEVEL=2" + newLine);
            writer.write("CHARGE=1+" + newLine);
            
            DataPoint[] dataPoints = ip.getDataPoints();
            
            if (!fractionalMZ)
                dataPoints = integerDataPoints(dataPoints, roundMode);
            
            for (DataPoint point : dataPoints)
            {
                String line = Double.toString(point.getMZ()) + " "
                        + Double.toString(point.getIntensity());
                writer.write(line + newLine);
            }
            
            writer.write("END IONS" + newLine);
            
            writer.write(newLine);
        }
    }
    
    private DataPoint[] integerDataPoints(final DataPoint[] dataPoints, 
            final String mode)
    {
        int size = dataPoints.length;
        
        Map <Double, Double> integerDataPoints = new HashMap <> ();
        
        for (int i = 0; i < size; ++i)
        {
            double mz = (double) Math.round(dataPoints[i].getMZ());
            double intensity = dataPoints[i].getIntensity();
            Double prevIntensity = integerDataPoints.get(mz);
            if (prevIntensity == null) prevIntensity = 0.0;
            
            switch (mode) 
            {
                case MGFExportParameters.ROUND_MODE_SUM:
                    integerDataPoints.put(mz, prevIntensity + intensity);
                    break;
                    
                case MGFExportParameters.ROUND_MODE_MAX:
                    integerDataPoints.put(mz, Math.max(prevIntensity, intensity));
                    break;
            }
        }
        
        DataPoint[] result = new DataPoint[integerDataPoints.size()];
        int count = 0;
        for (Entry <Double, Double> e : integerDataPoints.entrySet())
            result[count++] = new SimpleDataPoint(e.getKey(), e.getValue());
        
        return result;
    }
}
