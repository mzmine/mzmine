/*
 * (C) Copyright 2015-2016 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import com.google.common.collect.ImmutableList;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.FileType;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;
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
  private @NotNull String startTimeStamp;

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
    this.startTimeStamp = "";
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

  public @NotNull String getStartTimeStamp() {
    return startTimeStamp;
  }

  public void setStartTimeStamp(String startTimeStamp) {
    this.startTimeStamp = startTimeStamp;
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
