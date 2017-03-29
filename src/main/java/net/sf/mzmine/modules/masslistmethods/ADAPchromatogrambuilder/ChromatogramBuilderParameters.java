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
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package net.sf.mzmine.modules.masslistmethods.ADAPchromatogrambuilder;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ChromatogramBuilderParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    public static final MassListParameter massList = new MassListParameter();

    public static final DoubleParameter minimumScanSpan = new DoubleParameter(
            "Min group size in # of scans",
            "Minimum scan span over which some peak in the chromatogram must have (continuous) points above the noise level\n"
                    + "to be recognized as a chromatogram.\n"
                    + "The optimal value depends on the chromatography system setup. The best way to set this parameter\n"
                    + "is by studying the raw data and determining what is the typical time span of chromatographic peaks.",
            MZmineCore.getConfiguration().getRTFormat());

    public static final DoubleParameter minimumHeight = new DoubleParameter(
            "Min height",
            "Minimum intensity of the highest data point in the chromatogram. If chromatogram height is below this level, it is discarded.",
            MZmineCore.getConfiguration().getIntensityFormat());

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final StringParameter suffix = new StringParameter("Suffix",
            "This string is added to filename as suffix", "chromatograms");
    //Owen Edit
    public static final DoubleParameter IntensityThresh2 = new DoubleParameter(
            "Group intensity threshold",
            "This parameter is the intensity value for wich intensities greater than this value can contribute to the minimumScanSpan count.",
            MZmineCore.getConfiguration().getIntensityFormat());
    
    public static final DoubleParameter startIntensity = new DoubleParameter(
            "Min highest intensity",
            "Points below this intensity will not be considered in starting a new chromatogram",
            MZmineCore.getConfiguration().getIntensityFormat());
    // End Owen Edit

    public ChromatogramBuilderParameters() {
        super(new Parameter[] { dataFiles, scanSelection, massList,
                minimumScanSpan,IntensityThresh2, startIntensity, mzTolerance, suffix });
    }

}
