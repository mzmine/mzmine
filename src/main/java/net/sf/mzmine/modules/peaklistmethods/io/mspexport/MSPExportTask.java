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

package net.sf.mzmine.modules.peaklistmethods.io.mspexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class MSPExportTask extends AbstractTask 
{
    private final PeakList[] peakLists;
    private final File fileName;
    private final String plNamePattern = "{}";
    private final boolean fractionalMZ;
    private final String roundMode;
    
    MSPExportTask(ParameterSet parameters) 
    {
        this.peakLists = parameters.getParameter(MSPExportParameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();
        
        this.fileName = parameters.getParameter(MSPExportParameters.FILENAME)
                .getValue();
        
        this.fractionalMZ = parameters.getParameter(MSPExportParameters.FRACTIONAL_MZ)
                .getValue();
        
        this.roundMode = parameters.getParameter(MSPExportParameters.ROUND_MODE)
                .getValue();
    }
    
    public double getFinishedPercentage() {
        return 0.0;
    }

    public String getTaskDescription() {
        return "Exporting peak list(s) " 
                + Arrays.toString(peakLists) + " to MSP file(s)";
    }
    
    public void run() 
    {
        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        /*// Total number of rows
        for (PeakList peakList: peakLists) {
            totalRows += peakList.getNumberOfRows();
        }*/

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
            
            String name = row.toString();
                if (name != null) writer.write("Name: " + name + newLine);
            
            PeakIdentity identity = row.getPreferredPeakIdentity();
            if (identity != null) 
            {
//                String name = identity.getName();
//                if (name != null) writer.write("Name: " + name + newLine);
                
                String formula = identity.getPropertyValue(
                        PeakIdentity.PROPERTY_FORMULA);
                if (formula != null) writer.write("Formula: " + formula + newLine);
                
                String id = identity.getPropertyValue(PeakIdentity.PROPERTY_ID);
                if (id != null) writer.write("Comments: " + id + newLine);
            }
            
            String rowID = Integer.toString(row.getID());
            if (rowID != null) writer.write("DB#: " + rowID + newLine);
            
            DataPoint[] dataPoints = ip.getDataPoints();
            
            if (!fractionalMZ)
                dataPoints = integerDataPoints(dataPoints, roundMode);
            
            String numPeaks = Integer.toString(dataPoints.length);
            if (numPeaks != null) writer.write("Num Peaks: " + numPeaks + newLine);
            
            for (DataPoint point : dataPoints)
            {
                String line = Double.toString(point.getMZ()) + " "
                        + Double.toString(point.getIntensity());
                writer.write(line + newLine);
            }
            
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
                case MSPExportParameters.ROUND_MODE_SUM:
                    integerDataPoints.put(mz, prevIntensity + intensity);
                    break;
                    
                case MSPExportParameters.ROUND_MODE_MAX:
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
