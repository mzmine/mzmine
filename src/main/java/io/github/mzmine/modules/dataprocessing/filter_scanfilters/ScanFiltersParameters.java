/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.filter_scanfilters;

import io.github.mzmine.modules.dataprocessing.filter_scanfilters.mean.MeanFilter;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.resample.ResampleFilter;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.roundresample.RndResampleFilter;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.savitzkygolay.SGFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class ScanFiltersParameters extends SimpleParameterSet {

  public static final ScanFilter rawDataFilters[] =
      {new SGFilter(), new MeanFilter(), new ResampleFilter(), new RndResampleFilter()};

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelect =
      new ScanSelectionParameter(new ScanSelection(1));

  public static final StringParameter suffix =
      new StringParameter("Suffix", "This string is added to filename as suffix", "filtered");

  public static final ModuleComboParameter<ScanFilter> filter =
      new ModuleComboParameter<ScanFilter>("Filter", "Raw data filter", rawDataFilters, rawDataFilters[0]);

  public static final BooleanParameter autoRemove =
      new BooleanParameter("Remove source file after filtering",
          "If checked, original file will be removed and only filtered version remains");

  public ScanFiltersParameters() {
    super(new Parameter[] {dataFiles, scanSelect, suffix, filter, autoRemove},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_raw_data/filter-scan-by-scan.html");
  }

}
