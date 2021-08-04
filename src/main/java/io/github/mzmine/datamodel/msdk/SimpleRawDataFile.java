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

package io.github.mzmine.datamodel.msdk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of the RawDataFile interface.
 */
public class SimpleRawDataFile implements RawDataFile {

  private @Nonnull String rawDataFileName;
  private @Nonnull Optional<File> originalRawDataFile;
  private final @Nonnull ArrayList<MsScan> scans;
  private final @Nonnull ArrayList<Chromatogram> chromatograms;

  /**
   * <p>
   * Constructor for SimpleRawDataFile.
   * </p>
   *
   * @param rawDataFileName a {@link String} object.
   * @param originalRawDataFile a {@link Optional} object.
   */
  public SimpleRawDataFile(@Nonnull String rawDataFileName,
      @Nonnull Optional<File> originalRawDataFile) {
    this.rawDataFileName = rawDataFileName;
    this.originalRawDataFile = originalRawDataFile;
    this.scans = new ArrayList<>();
    this.chromatograms = new ArrayList<>();
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@link String} object.
   */
  public @Nonnull String getName() {
    return rawDataFileName;
  }

  /**
   * {@inheritDoc}
   *
   * @param name a {@link String} object.
   */
  public void setName(@Nonnull String name) {
    Preconditions.checkNotNull(name);
    this.rawDataFileName = name;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Optional<File> getOriginalFile() {
    return originalRawDataFile;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public String getOriginalFilename() {
    if (originalRawDataFile.isPresent()) {
      return originalRawDataFile.get().getName();
    }

    return "Unknown";
  }

  /**
   * {@inheritDoc}
   *
   * @param newOriginalFile a {@link File} object.
   */
  public void setOriginalFile(@Nullable File newOriginalFile) {
    this.originalRawDataFile = Optional.ofNullable(newOriginalFile);
  }
  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<String> getMsFunctions() {
    ArrayList<String> msFunctionList = new ArrayList<>();
    synchronized (scans) {
      for (MsScan scan : scans) {
        String f = scan.getMsFunction();
        if ((f != null) && (!msFunctionList.contains(f)))
          msFunctionList.add(f);
      }
    }
    return msFunctionList;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull List<MsScan> getScans() {
    return ImmutableList.copyOf(scans);
  }

  /**
   * {@inheritDoc}
   *
   * @param scan a {@link MsScan} object.
   */
  public void addScan(@Nonnull MsScan scan) {
    Preconditions.checkNotNull(scan);
    synchronized (scans) {
      scans.add(scan);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param scan a {@link MsScan} object.
   */
  public void removeScan(@Nonnull MsScan scan) {
    Preconditions.checkNotNull(scan);
    synchronized (scans) {
      scans.remove(scan);
    }
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<Chromatogram> getChromatograms() {
    return ImmutableList.copyOf(chromatograms);
  }

  /**
   * {@inheritDoc}
   *
   * @param chromatogram a {@link Chromatogram} object.
   */
  public void addChromatogram(@Nonnull Chromatogram chromatogram) {
    Preconditions.checkNotNull(chromatogram);
    synchronized (chromatograms) {
      chromatograms.add(chromatogram);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param chromatogram a {@link Chromatogram} object.
   */
  public void removeChromatogram(@Nonnull Chromatogram chromatogram) {
    Preconditions.checkNotNull(chromatogram);
    synchronized (chromatograms) {
      chromatograms.remove(chromatogram);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void dispose() {
    // Do nothing
  }

}
