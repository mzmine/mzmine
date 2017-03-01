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

import java.text.NumberFormat;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3LCMSSearchParameters extends SimpleParameterSet 
{    
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
    
    public static final FileNameParameter FILE_NAME = 
            new FileNameParameter("DataBase", "File containing ADAP database", "db");
    
    public static final DoubleParameter MASS_TOLERANCE =
            new DoubleParameter("Mass tolerance (Da)",
                    "Maximum difference between experimental and theoretical exact masses",
                    NumberFormat.getInstance(), 0.1);
    
    public static final DoubleParameter DISTANCE_TOLERANCE =
            new DoubleParameter("Similarity tolerance",
                    "Maximum difference between experimental and theoretical exact masses",
                    NumberFormat.getInstance(), 0.1, 0.0, 1.0);
    
    public ADAP3LCMSSearchParameters() {
        super(new Parameter[] {PEAK_LISTS, FILE_NAME, MASS_TOLERANCE, DISTANCE_TOLERANCE});
    }
}
