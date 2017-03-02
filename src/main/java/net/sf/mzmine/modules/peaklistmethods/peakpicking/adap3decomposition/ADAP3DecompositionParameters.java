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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decomposition;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ParameterSetParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionParameters extends SimpleParameterSet {
    
    public static final PeakListsParameter PEAK_LISTS = 
            new PeakListsParameter();
    
    public static final ParameterSetParameter TIC_WINDOW =
            new ParameterSetParameter("TIC Window Detection Parameters", 
                    "Parameters for ADAP-3 TIC Window Detection", 
                    new ADAP3TICWindowDetectionParameters());
    
    public static final ParameterSetParameter EIC_DECOMPOSITION =
            new ParameterSetParameter("EIC Decomposition Parameters",
                    "Parameters for ADAP-3 Peak Decomposition",
                    new ADAP3EICDecompositionParameters());
    
    public static final ParameterSetParameter OPTIMIZATION =
            new ParameterSetParameter("Optimization Parameters",
                    "Parameters for ADAP-3 spectrum building",
                    new ADAP3OptimizationParameters());
    
    public static final StringParameter SUFFIX = new StringParameter("Suffix",
	    "This string is added to peak list name as suffix", "ADAP-3 Peak Decomposition");
    
    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, original chromatogram will be removed and only the deconvolved version remains");
    
    public ADAP3DecompositionParameters() {
	super(new Parameter[] {PEAK_LISTS, TIC_WINDOW, EIC_DECOMPOSITION,
            OPTIMIZATION, SUFFIX, AUTO_REMOVE});
    }
}
