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

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters;

import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.mean.MeanFilter;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.resample.ResampleFilter;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.roundresample.RndResampleFilter;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.savitzkygolay.SGFilter;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class ScanFiltersParameters extends SimpleParameterSet {

    public static final ScanFilter rawDataFilters[] = { new SGFilter(),
            new MeanFilter(), new ResampleFilter(), new RndResampleFilter() };

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final StringParameter suffix = new StringParameter("Suffix",
            "This string is added to filename as suffix", "filtered");

    public static final ModuleComboParameter<ScanFilter> filter = new ModuleComboParameter<ScanFilter>(
            "Filter", "Raw data filter", rawDataFilters);

    public static final BooleanParameter autoRemove = new BooleanParameter(
            "Remove source file after filtering",
            "If checked, original file will be removed and only filtered version remains");

    public ScanFiltersParameters() {
        super(new Parameter[] { dataFiles, suffix, filter, autoRemove });
    }

}
