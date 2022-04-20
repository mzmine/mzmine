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

package io.github.mzmine.modules.io.projectload;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used during project import to cache {@link io.github.mzmine.datamodel.impl.StoredMobilityScan}s.
 * Since mobility scans are created on demand of every call to {@link Frame#getMobilityScans()},
 * multiple duplicate instances would be created during project import, for every loaded {@link
 * io.github.mzmine.datamodel.featuredata.IonSpectrumSeries}, every {@link
 * io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType}, and so on. This class
 * contains {@link CachedIMSFrame}s over regular frames. These cached frames will once create and
 * retain instances of {@link io.github.mzmine.datamodel.impl.StoredMobilityScan}s so the same
 * instance can be used throughout the loading process.
 *
 * @author SteffenHeu https://github.com/SteffenHeu
 */
public class CachedIMSRawDataFile implements IMSRawDataFile {

  private final IMSRawDataFile originalFile;
  private final ObservableList<CachedIMSFrame> cachedFrames;

  public CachedIMSRawDataFile(IMSRawDataFile file) {
    if (file instanceof CachedIMSRawDataFile) {
      throw new IllegalArgumentException("File is already cached.");
    }

    originalFile = file;
    cachedFrames = FXCollections.observableArrayList();
    file.getFrames().forEach(f -> cachedFrames.add(new CachedIMSFrame(f)));
  }

  @Override
  public @Nullable Frame getFrame(int frameNum) {
    return cachedFrames.get(frameNum);
  }

  @Override
  public @NotNull List<? extends Frame> getFrames() {
    return cachedFrames;
  }

  @Override
  public @NotNull List<? extends Frame> getFrames(int msLevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull List<? extends Frame> getFrames(int msLevel, Range<Float> rtRange) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public String setNameNoChecks(@NotNull String name) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public int getNumberOfFrames() {
    return cachedFrames.size();
  }

  @Override
  public @NotNull List<Scan> getFrameNumbers(int msLevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull List<Scan> getFrameNumbers(int msLevel, @NotNull Range<Float> rtRange) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull Range<Double> getDataMobilityRange() {
    return originalFile.getDataMobilityRange();
  }

  @Override
  public @NotNull Range<Double> getDataMobilityRange(int msLevel) {
    return originalFile.getDataMobilityRange(msLevel);
  }

  @Override
  public @Nullable Frame getFrameAtRt(double rt) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable Frame getFrameAtRt(double rt, int msLevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull MobilityType getMobilityType() {
    return originalFile.getMobilityType();
  }

  @Override
  public @NotNull String getName() {
    return originalFile.getName();
  }

  @Override
  public String setName(@NotNull String name) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull StringProperty nameProperty() {
    return new SimpleStringProperty(getName());
  }

  @Override
  public @Nullable String getAbsolutePath() {
    return originalFile.getAbsolutePath();
  }

  @Override
  public int getNumOfScans() {
    return cachedFrames.size();
  }

  @Override
  public int getNumOfScans(int msLevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public int getMaxCentroidDataPoints() {
    return originalFile.getMaxCentroidDataPoints();
  }

  @Override
  public int getMaxRawDataPoints() {
    return originalFile.getMaxRawDataPoints();
  }

  @Override
  public @NotNull int[] getMSLevels() {
    return originalFile.getMSLevels();
  }

  @Override
  public @NotNull List<Scan> getScanNumbers(int msLevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull Scan[] getScanNumbers(int msLevel, @NotNull Range<Float> rtRange) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public Scan getScanNumberAtRT(float rt, int mslevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public Scan getScanNumberAtRT(float rt) {
    throw new UnsupportedOperationException("Unsupported during project load.");

  }

  @Override
  public @NotNull Range<Double> getDataMZRange() {
    return originalFile.getDataMZRange();
  }

  @Override
  public @NotNull Range<Float> getDataRTRange() {
    return originalFile.getDataRTRange();
  }

  @Override
  public @NotNull Range<Double> getDataMZRange(int msLevel) {
    return originalFile.getDataMZRange(1);
  }

  @Override
  public @NotNull Range<Float> getDataRTRange(Integer msLevel) {
    return originalFile.getDataRTRange(msLevel);
  }

  @Override
  public double getDataMaxBasePeakIntensity(int msLevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public double getDataMaxTotalIonCurrent(int msLevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull List<PolarityType> getDataPolarity() {
    return originalFile.getDataPolarity();
  }

  @Override
  public Color getColorAWT() {
    return originalFile.getColorAWT();
  }

  @Override
  public javafx.scene.paint.Color getColor() {
    return originalFile.getColor();
  }

  @Override
  public void setColor(javafx.scene.paint.Color color) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> colorProperty() {
    return new SimpleObjectProperty<>(getColor());
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable MemoryMapStorage getMemoryMapStorage() {
    return originalFile.getMemoryMapStorage();
  }

  @Override
  public void addScan(Scan newScan) throws IOException {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public void setRTRange(int msLevel, Range<Float> rtRange) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public void setMZRange(int msLevel, Range<Double> mzRange) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public ObservableList<Scan> getScans() {
    return (ObservableList<Scan>) (ObservableList<? extends Scan>) cachedFrames;
  }

  @Override
  public void applyMassListChanged(Scan scan, MassList old, MassList masses) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return originalFile.getAppliedMethods();
  }

  public RawDataFile getOriginalFile() {
    return originalFile;
  }

  @Override
  public @Nullable CCSCalibration getCCSCalibration() {
    return null;
  }

  @Override
  public void setCCSCalibration(@Nullable CCSCalibration calibration) {

  }

  @Override
  public DoubleImmutableList getSegmentMobilities(int segment) {
    return originalFile.getSegmentMobilities(segment);
  }

  @Override
  public int addMobilityValues(double[] mobilities) {
    return originalFile.addMobilityValues(mobilities);
  }
}
