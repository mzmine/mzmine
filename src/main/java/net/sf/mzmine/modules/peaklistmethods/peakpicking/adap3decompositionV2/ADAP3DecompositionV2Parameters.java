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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import java.text.NumberFormat;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV2Parameters extends SimpleParameterSet {
    
    public static final PeakListsParameter PEAK_LISTS = 
            new PeakListsParameter();
    
    public static final DoubleParameter MIN_MODEL_STN =
            new DoubleParameter("Minimum Signal-to-Noise ratio",
                    "Minimum Signal-to-Noise ratio that the model peak can have",
                    NumberFormat.getNumberInstance(), 100.0);
    
    public static final DoubleParameter MIN_MODEL_SHARPNESS =
            new DoubleParameter("Minimim Shapness",
                    "Minimum sharpness that the model peak can have",
                    NumberFormat.getNumberInstance(), 10.0);
    
    public static final DoubleParameter CLUSTER_THRESHOLD =
            new DoubleParameter("Cluster Threshold",
                    "Shape-similarity threshold used to cluster similar peaks",
                    NumberFormat.getNumberInstance(), 0.7);
    
    public static final DoubleParameter SPECTRUM_THRESHOLD =
            new DoubleParameter("Spectrum Threshold",
                    "Shape-similarity threshold used to build a spectrum",
                    NumberFormat.getNumberInstance(), 0.8);
    
    public static final DoubleParameter MERGE_THRESHOLD =
            new DoubleParameter("Merge Threshold",
                    "Peaks with the spectrum-similarity above this threshold, will be merged",
                    NumberFormat.getNumberInstance(), 0.95);
    
    public static final IntegerParameter NUM_ISOTOPES =
            new IntegerParameter("Number of Isotopes", 
                    "Minimum number of isotopes in each isotopic pattern of a spectrum",
                    2);
    
    public static final DoubleParameter ISOTOPE_DISTANCE =
            new DoubleParameter("Isotope Distance", 
                    "Maximum distance between two isotopes in an isotopic pattern",
                    NumberFormat.getNumberInstance(), 2.5);
    
    public static final StringParameter SUFFIX = new StringParameter("Suffix",
	    "This string is added to peak list name as suffix", "ADAP-3 Peak Decomposition");
    
    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, original chromatogram will be removed and only the deconvolved version remains");
    
    public ADAP3DecompositionV2Parameters() {
	super(new Parameter[] {PEAK_LISTS, MIN_MODEL_STN, MIN_MODEL_SHARPNESS,
            CLUSTER_THRESHOLD, SPECTRUM_THRESHOLD, MERGE_THRESHOLD, 
            NUM_ISOTOPES, ISOTOPE_DISTANCE, SUFFIX, AUTO_REMOVE});
    }
}
