/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.localmaxima.LocalMaxMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.recursive.RecursiveMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.wavelet.WaveletMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massfilters.MassFilter;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massfilters.nofilter.NoFilter;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massfilters.shoulderpeaksfilter.ShoulderPeaksFilter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class ChromatogramBuilderParameters extends SimpleParameterSet {

	private static final MassDetector massDetectors[] = {
			new CentroidMassDetector(), new ExactMassDetector(),
			new LocalMaxMassDetector(), new RecursiveMassDetector(),
			new WaveletMassDetector() };

	private static final MassFilter massFilters[] = { new NoFilter(),
			new ShoulderPeaksFilter() };

	// All parameters

	public static final ModuleComboParameter<MassDetector> massDetector = new ModuleComboParameter<MassDetector>(
			"Mass detector", "Mass detector description", massDetectors);

	public static final ModuleComboParameter<MassFilter> massFilter = new ModuleComboParameter<MassFilter>(
			"Mass filter", "Mass filter description", massFilters);

	public static final NumberParameter minimumTimeSpan = new NumberParameter(
			"Min time span",
			"Minimum acceptable time span of connected string of m/z peaks",
			MZmineCore.getRTFormat());

	public static final NumberParameter minimumHeight = new NumberParameter(
			"Min height", "Minimum top intensity of the chromatogram",
			MZmineCore.getIntensityFormat());

	public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
			"m/z tolerance",
			"Maximum allowed distance in M/Z between data points in successive spectrums");

	public static final StringParameter suffix = new StringParameter("Suffix",
			"This string is added to filename as suffix", "peaklist");

	public ChromatogramBuilderParameters() {
		super(new UserParameter[] { massDetector, massFilter, minimumTimeSpan,
				minimumHeight, mzTolerance, suffix });
	}

}
