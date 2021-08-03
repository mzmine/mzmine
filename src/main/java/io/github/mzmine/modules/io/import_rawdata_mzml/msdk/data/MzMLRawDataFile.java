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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import com.google.common.collect.ImmutableList;
import io.github.mzmine.datamodel.msdk.Chromatogram;
import io.github.mzmine.datamodel.msdk.FileType;
import io.github.mzmine.datamodel.msdk.MsScan;
import io.github.mzmine.datamodel.msdk.RawDataFile;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * MzMLRawDataFile class.
 * </p>
 *
 */
public class MzMLRawDataFile implements RawDataFile {

  private static final @NotNull FileType fileType = FileType.MZML;

  private final File sourceFile;

  private final @NotNull List<String> msFunctions;
  private final @NotNull List<MsScan> msScans;
  private final @NotNull List<Chromatogram> chromatograms;

  private @NotNull String defaultInstrumentConfiguration;
  private @NotNull String defaultDataProcessingScan;
  private @NotNull String defaultDataProcessingChromatogram;

  private @NotNull String name;

  /**
   * <p>
   * Constructor for MzMLRawDataFile.
   * </p>
   *
   * @param sourceFile a {@link File} object.
   * @param msFunctions a {@link List} object.
   * @param msScans a {@link List} object.
   * @param chromatograms a {@link List} object.
   */
  @SuppressWarnings("null")
  public MzMLRawDataFile(File sourceFile, List<String> msFunctions, List<MsScan> msScans,
      List<Chromatogram> chromatograms) {
    this.sourceFile = sourceFile;
    this.name = sourceFile != null ? sourceFile.getName() : null;
    this.msFunctions = msFunctions;
    this.msScans = msScans;
    this.chromatograms = chromatograms;
    this.defaultInstrumentConfiguration = "unknown";
    this.defaultDataProcessingScan = "unknown";
    this.defaultDataProcessingChromatogram = "unknown";
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public String getName() {
    return name;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<File> getOriginalFile() {
    return Optional.ofNullable(sourceFile);
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public FileType getRawDataFileType() {
    return fileType;
  }

  /** {@inheritDoc} */
  @SuppressWarnings("null")
  @Override
  @NotNull
  public List<String> getMsFunctions() {
    return ImmutableList.copyOf(msFunctions);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("null")
  @Override
  @NotNull
  public List<MsScan> getScans() {
    return ImmutableList.copyOf(msScans);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("null")
  @Override
  @NotNull
  public List<Chromatogram> getChromatograms() {
    return ImmutableList.copyOf(chromatograms);
  }

  public String getDefaultInstrumentConfiguration() {
    return defaultInstrumentConfiguration;
  }

  public void setDefaultInstrumentConfiguration(String defaultInstrumentConfiguration) {
    this.defaultInstrumentConfiguration = defaultInstrumentConfiguration;
  }

  public String getDefaultDataProcessingScan() {
    return defaultDataProcessingScan;
  }

  public void setDefaultDataProcessingScan(String defaultDataProcessingScan) {
    this.defaultDataProcessingScan = defaultDataProcessingScan;
  }

  public String getDefaultDataProcessingChromatogram() {
    return defaultDataProcessingChromatogram;
  }

  public void setDefaultDataProcessingChromatogram(String defaultDataProcessingChromatogram) {
    this.defaultDataProcessingChromatogram = defaultDataProcessingChromatogram;
  }

  /** {@inheritDoc} */
  @Override
  public void dispose() {}

}
