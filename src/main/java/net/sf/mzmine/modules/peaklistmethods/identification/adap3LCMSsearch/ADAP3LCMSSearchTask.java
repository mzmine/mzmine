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
package net.sf.mzmine.modules.peaklistmethods.identification.adap3LCMSsearch;

import dulab.adap.common.parsers.CompoundInfo;
import dulab.adap.common.types.SparseVector;
import dulab.adap.datamodel.LCMSDataBase;
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
public class ADAP3LCMSSearchTask extends AbstractTask {
    
    private final ParameterSet parameters;
    private final PeakList peakList;
    private final LCMSDataBase dataBase;
    
    private int processedMax;
    private int processed;
    
    public ADAP3LCMSSearchTask(final ParameterSet parameters, 
            final PeakList peakList, final LCMSDataBase dataBase)
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
        
        double massTolerance = parameters
                .getParameter(ADAP3LCMSSearchParameters.MASS_TOLERANCE)
                .getValue();
        
        double distanceTolerance = parameters
                .getParameter(ADAP3LCMSSearchParameters.DISTANCE_TOLERANCE)
                .getValue();
        
        Map <Double, Double> spectrum = new HashMap <> ();
        
        for (PeakListRow row : peakList.getRows())
        {
            System.out.println(row.getID());
            
            if (isCanceled()) break;
            
            IsotopePattern ip = row.getBestIsotopePattern();
            
            if (ip == null) continue;
            
            for (DataPoint dataPoint : ip.getDataPoints())
                spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());
            
            List <SparseVector> compounds = dataBase.search(
                    row.getAverageMZ() - 1.0078246, spectrum, massTolerance, distanceTolerance); // substract exact mass of one proton
            
            boolean preferred = true;
            
            for (SparseVector vector : compounds) {
                CompoundInfo info = vector.getInfo();
                String name = info.name.split("\n")[0];
                
                SimplePeakIdentity identity = new SimplePeakIdentity(
                        //name.length() > 0 ? name : info.ID + "; " + info.formula,
                        info.ID + "; " + info.formula,
                        info.formula,
                        "ADAP LC-MS DataBase Search",
                        info.ID,
                        null);

                identity.setPropertyValue(PeakIdentity.PROPERTY_SPECTRUM, 
                        vector.data().toString());
                
                row.addPeakIdentity(identity, preferred);
                preferred = false;
            }
        
            spectrum.clear();
            
            ++processed;
        }
        
        setStatus(TaskStatus.FINISHED);
    }
}
