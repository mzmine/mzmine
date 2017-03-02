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
package net.sf.mzmine.util.adap;

import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAPInterface {
    
    public static Component getComponent(final PeakListRow row) 
    {   
        if (row.getNumberOfPeaks() == 0)
            throw new IllegalArgumentException("No peaks found");
        
        NavigableMap <Double, Double> spectrum = new TreeMap <> ();
        
        // Read Spectrum information
        IsotopePattern ip = row.getBestIsotopePattern();
        if (ip != null)
        {
            for (DataPoint dataPoint : ip.getDataPoints())
                spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());
        }
        
        // Read Chromatogram
        final Feature peak = row.getBestPeak();
        final RawDataFile dataFile = peak.getDataFile();
        
        NavigableMap <Double, Double> chromatogram = new TreeMap <> ();
        
        for (final int scan : peak.getScanNumbers()) {
            final DataPoint dataPoint = peak.getDataPoint(scan);
            if (dataPoint != null)
                chromatogram.put(dataFile.getScan(scan).getRetentionTime(), 
                        dataPoint.getIntensity());
        }
        
        return new Component(null, 
                new Peak(chromatogram, new PeakInfo()
                        .mzValue(peak.getMZ())
                        .peakID(row.getID())),
                spectrum, null);
    }
    
    public static List <Component> getComponents(final PeakList peakList)
    {
        List <Component> result = new ArrayList <> (peakList.getNumberOfRows());
        
        for (final PeakListRow row : peakList.getRows())
            result.add(getComponent(row));
        
        return result;
    }
    
    public static double[] getIntensityVector(RawDataFile dataFile)
    {
        Set <Double> setMZValues = new HashSet<> ();
        Map <Integer, Map <Double, Double>> data = new HashMap <> ();

        final int[] scanNumbers = dataFile.getScanNumbers();
        final int scanCount = scanNumbers.length;

        for (final int scanNumber : scanNumbers) {

            data.put(scanNumber, new HashMap <Double, Double> ());
            final Scan scan = dataFile.getScan(scanNumber);

            for (final DataPoint p : scan.getDataPoints()) 
            {
                final double mz = p.getMZ();
                data.get(scanNumber).put(mz, p.getIntensity());
                setMZValues.add(mz);
            }
        }

        List <Double> sortedMZValues = new ArrayList <> (setMZValues);
        java.util.Collections.sort(sortedMZValues);

        final int mzCount = sortedMZValues.size();
        
        double[] intensities = new double[scanCount * mzCount];
        
        for (int i = 0; i < mzCount; ++i) 
        {
            final double mz = sortedMZValues.get(i);

            for (int j = 0; j < scanCount; ++j) 
            {
                Map <Double, Double> c = data.get(scanNumbers[j]);

                if (c != null && c.get(mz) != null)
                    intensities[i * scanCount + j] = c.get(mz);
            }
        }
        
        return intensities;
    }
    
    public static double[] getIntensityVector(PeakList peakList) 
    {
        RawDataFile dataFile = peakList.getRawDataFile(0);
        
        final int mzCount = peakList.getNumberOfRows();
        
        final int[] scanNumbers = dataFile.getScanNumbers();
        final int scanCount = scanNumbers.length;
        
        double[] result = new double[mzCount * scanCount];
        
        for (int i = 0; i < mzCount; ++i)
        {
            Feature chromatogram = peakList.getRow(i).getBestPeak();
            
            for (int j = 0; j < scanCount; ++j)
            {
                final int scanNumber = scanNumbers[j];
                DataPoint dataPoint = chromatogram.getDataPoint(scanNumber);
                if (dataPoint != null)
                    result[i * scanCount + j] = dataPoint.getIntensity();
            }
        }
        
        return result;
    }
    
    public static double[] getMZVector(RawDataFile dataFile)
    {
        Set <Double> setMZValues = new HashSet<> ();

        final int[] scanNumbers = dataFile.getScanNumbers();

        for (final int scanNumber : scanNumbers) 
        {
            final Scan scan = dataFile.getScan(scanNumber);

            for (final DataPoint p : scan.getDataPoints())
                setMZValues.add(p.getMZ());
        }
        
        List <Double> sortedMZValues = new ArrayList <> (setMZValues);
        java.util.Collections.sort(sortedMZValues);
        
        double[] mzValues = new double[sortedMZValues.size()];
        for (int i = 0; i < sortedMZValues.size(); ++i)
            mzValues[i] = sortedMZValues.get(i);
        
        return mzValues;
    }
    
    public static double[] getMZVector(PeakList peakList)
    {   
        final int mzCount = peakList.getNumberOfRows();
        
        double[] result = new double[mzCount];
        
        for (int i = 0; i < mzCount; ++i)
            result[i] = peakList.getRow(i).getAverageMZ();
        
        return result;
    }
    
    public static double[] getRetTimeVector(RawDataFile dataFile)
    {
        final int[] scanNumbers = dataFile.getScanNumbers();
        
        double[] retTimes = new double[scanNumbers.length];
        
        for (int i = 0; i < scanNumbers.length; ++i)
            retTimes[i] = dataFile.getScan(scanNumbers[i]).getRetentionTime();
        
        return retTimes;
    }
    
    public static double[] getRetTimeVector(PeakList peakList) {
        return getRetTimeVector(peakList.getRawDataFile(0));
    }
}
