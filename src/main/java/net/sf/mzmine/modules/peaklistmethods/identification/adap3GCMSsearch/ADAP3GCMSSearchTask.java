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
package net.sf.mzmine.modules.peaklistmethods.identification.adap3GCMSsearch;

import dulab.adap.common.distances.PurityScore;
import dulab.adap.common.parsers.CompoundInfo;
import dulab.adap.common.types.SparseVector;
import dulab.adap.datamodel.DataBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3GCMSSearchTask extends AbstractTask {
    
    private final ParameterSet parameters;
    private final PeakList peakList;
    private final DataBase dataBase;
    
    private int processedMax;
    private int processed;
    
    public ADAP3GCMSSearchTask(final ParameterSet parameters, 
            final PeakList peakList, final DataBase dataBase)
    {
        this.parameters = parameters;
        this.peakList = peakList;
        this.dataBase = dataBase;
        
        this.processed = 0;
        this.processedMax = 0;
    }

    
    @Override
    public double getFinishedPercentage() {
	return processedMax == 0 ? 0.0 : (double) processed / processedMax;
    }

    @Override
    public String getTaskDescription() {
	return "Identification of peaks in " + peakList;
    }

    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        
        this.processedMax = peakList.getNumberOfRows();
        
        double tolerance = parameters.
                getParameter(ADAP3GCMSSearchParameters.TOLERANCE)
                .getValue();
        
        List <SparseVector> result = new ArrayList <> ();
        Map <Double, Double> spectrum = new HashMap <> ();
        
        for (PeakListRow row : peakList.getRows())
        {
            if (isCanceled()) break;
            
            IsotopePattern ip = row.getPeaks()[0].getIsotopePattern();
            
            for (DataPoint dataPoint : ip.getDataPoints())
                spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());
            
            dataBase.search(spectrum, (float) tolerance, result);
            
            SparseVector userVector = new SparseVector(spectrum);
            userVector.multiply(1f / userVector.norm(), false);
            
            for (SparseVector vector : result) {
                CompoundInfo info = vector.getInfo();
                
                double purityScore = new PurityScore().call(userVector, vector);
                // Convert ADAP Score (0..1) to NIST Score (0..1000)
                purityScore = 1000 * Math.cos(Math.PI * purityScore / 2); 
                
                SimplePeakIdentity identity = new SimplePeakIdentity(
                        info.name + " (" + Double.toString(purityScore) + ")",
                        info.formula,
                        "ADAP DataBase Search",
                        info.ID,
                        null);
                
                identity.setPropertyValue("SCORE", Double.toString(purityScore));
                
                identity.setPropertyValue(PeakIdentity.PROPERTY_SPECTRUM, 
                        vector.data().toString());
                
                row.addPeakIdentity(identity, false);
            }
        
            spectrum.clear();
            result.clear();
            
            ++processed;
        }
        
        setStatus(TaskStatus.FINISHED);
    }
}
