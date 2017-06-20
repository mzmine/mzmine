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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution;

import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.baseline.BaselinePeakDetector;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking.ADAPDetector;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.centwave.CentWaveDetector;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetector;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.noiseamplitude.NoiseAmplitudePeakDetector;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay.SavitzkyGolayPeakDetector;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;

public class DeconvolutionParameters extends SimpleParameterSet {

    private static final PeakResolver[] RESOLVERS = {
	    new BaselinePeakDetector(), new NoiseAmplitudePeakDetector(),
	    new SavitzkyGolayPeakDetector(), new MinimumSearchPeakDetector(),
	    new CentWaveDetector(), new ADAPDetector()};

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter("Suffix",
	    "This string is added to peak list name as suffix", "deconvoluted");

    public static final ModuleComboParameter<PeakResolver> PEAK_RESOLVER = new ModuleComboParameter<PeakResolver>(
	    "Algorithm", "Peak recognition description", RESOLVERS);

    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, original chromatogram will be removed and only the deconvolved version remains");
    public static final OptionalParameter<DoubleParameter> mzRangeMSMS = new OptionalParameter<>(new DoubleParameter(
    	 		"m/z range for MS2 scan pairing (Da)",
    	  		"M/z range: Will work only if ticked.\n"
    	  					+ "Maximum allowed difference between the m/z value of MS1 scan and the m/z value of precursor ion of MS2 scan (in Daltons) to be\n"
    	  					+ "considered belonging to the same feature. If not activated, the m/z tolerance set above will be used.\n"));
     public static final OptionalParameter<DoubleParameter> RetentionTimeMSMS = new OptionalParameter<>(new DoubleParameter(
    		  		"RT range for MS2 scan pairing (min)",
    		   		"RT range: Will work only if ticked.\n"+
    	    		"Maximum allowed difference between the retention time value of MS1 scan and the retention time value of the MS2 scan (in min) to be\n"
    		  		+ "considered belonging to the same feature. If not activated, the pairing of MS1 scan with the corresponding MS2 scan\n"
    	  			+ "will be done on the full retention time range of the chromatogram."));
    		 
    public DeconvolutionParameters() {
	super(
		new Parameter[] { PEAK_LISTS, SUFFIX, PEAK_RESOLVER,mzRangeMSMS,RetentionTimeMSMS,
			AUTO_REMOVE });
    }
}
