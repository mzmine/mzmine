/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.projectload;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used during project import to cache
 * {@link io.github.mzmine.datamodel.impl.StoredMobilityScan}s. Since mobility scans are created on
 * demand of every call to {@link Frame#getMobilityScans()}, multiple duplicate instances would be
 * created during project import, for every loaded
 * {@link io.github.mzmine.datamodel.featuredata.IonSpectrumSeries}, every
 * {@link io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType}, and so on. This
 * class contains {@link CachedIMSFrame}s over regular frames. These cached frames will once create
 * and retain instances of {@link io.github.mzmine.datamodel.impl.StoredMobilityScan}s so the same
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
  public Scan binarySearchClosestScan(float rt, int mslevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public Scan binarySearchClosestScan(float rt) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public int binarySearchClosestScanIndex(final float rt) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public int binarySearchClosestScanIndex(final float rt, final int mslevel) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull Range<Double> getDataMZRange() {
    return originalFile.getDataMZRange();
  }

  @Override
  public boolean isContainsZeroIntensity() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public boolean isContainsEmptyScans() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public MassSpectrumType getSpectraType() {
    throw new UnsupportedOperationException("Unsupported during project load.");
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
  public @NotNull ObservableList<Scan> getScans() {
    return (ObservableList<Scan>) (ObservableList<? extends Scan>) cachedFrames;
  }

  @Override
  public void applyMassListChanged(Scan scan, MassList old, MassList masses) {
    // do nothing
  }

  @Override
  public @NotNull ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return originalFile.getAppliedMethods();
  }

  @Override
  public List<OtherDataFile> getOtherDataFiles() {
    return List.of();
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
