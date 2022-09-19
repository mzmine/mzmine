/*
 * Copyright 2006-2022 The MZmine Development Team
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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class LibraryBatchGenerationParameters extends SimpleParameterSet {


  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final IntegerParameter minSignals = new IntegerParameter("Min signals",
      "Minimum signals in a masslist (all other masslists are discarded)", 3);

  public static final OptionalModuleParameter<HandleChimericMsMsParameters> handleChimerics = new OptionalModuleParameter<>(
      "Handle chimeric spectra",
      "Options to identify and handle chimeric spectra with multiple MS1 signals in the precusor ion selection",
      new HandleChimericMsMsParameters(), true);

  public static final ComboParameter<ScanSelector> scanExport = new ComboParameter<>("Export scans",
      "Select scans to export", ScanSelector.values(), ScanSelector.ALL);

  public static final FileNameParameter file = new FileNameParameter("Export file",
      "Local library file", FileSelectionType.SAVE);

  public static final ComboParameter<SpectralLibraryExportFormats> exportFormat = new ComboParameter<>(
      "Export format", "format to export", SpectralLibraryExportFormats.values(),
      SpectralLibraryExportFormats.json);

  public static final SubModuleParameter<LibaryMetadataParameters> metadata = new SubModuleParameter<>(
      "Metadata", "Metadata for all entries", new LibaryMetadataParameters());

  public LibraryBatchGenerationParameters() {
    super(new Parameter[]{flists, file, minSignals, scanExport, exportFormat, metadata,
        handleChimerics});
  }

}
