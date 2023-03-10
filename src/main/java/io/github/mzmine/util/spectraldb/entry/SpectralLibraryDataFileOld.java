/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.spectraldb.entry;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.util.MemoryMapStorage;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectralLibraryDataFileOld implements RawDataFile {

  @NotNull
  private final SpectralLibrary library;
  private final ObservableList<FeatureListAppliedMethod> appliedMethods = FXCollections.observableArrayList();
//  private final int maxDataPoints;
//  private final int[] msLevels;
  private final ObservableList<Scan> scans;

  public SpectralLibraryDataFileOld(@NotNull SpectralLibrary library) {
    this.library = library;

//    maxDataPoints = library.stream().mapToInt(MassSpectrum::getNumberOfDataPoints).max().orElse(0);
//    msLevels = library.stream().map(SpectralLibraryEntry::getMsLevel).filter(Objects::nonNull)
//        .mapToInt(Integer::intValue).distinct().toArray();

    var entries = library.getEntries();
//    List<Scan> scans = IntStream.range(0, entries.size())
//        .mapToObj(i -> (Scan) new LibraryEntryWrappedScan(this, entries.get(i), i))
//         need to be sorted by RT even if no RT set
//        .sorted(Comparator.comparingDouble(Scan::getRetentionTime)).toList();
    this.scans = FXCollections.observableArrayList();
  }

  @Override
  public @NotNull String getName() {
    return library.getName();
  }

  @Override
  public String setName(@NotNull final String name) {
    // do not set name keep instead
    return getName();
  }

  @Override
  public @Nullable String getAbsolutePath() {
    return library.getPath().getAbsolutePath();
  }

  @Override
  public int getNumOfScans() {
    return library.getNumEntries();
  }

  @Override
  public int getNumOfScans(final int msLevel) {
    return (int) library.stream().map(SpectralLibraryEntry::getMsLevel).filter(Objects::nonNull).count();
  }

  @Override
  public int getMaxCentroidDataPoints() {
//    return maxDataPoints;
    return 0;
  }

  @Override
  public int getMaxRawDataPoints() {
    return 0;
  }

  @Override
  public @NotNull int[] getMSLevels() {
    return null;
  }

  @Override
  public @NotNull List<Scan> getScanNumbers(final int msLevel) {
    return scans.stream().filter(s -> s.getMSLevel() == msLevel).toList();
  }

  @Override
  public @NotNull Scan[] getScanNumbers(final int msLevel, @NotNull final Range<Float> rtRange) {
    return scans.stream().filter(s -> {
      var rt = s.getRetentionTime();
      return rt > 0 && rtRange.contains(rt);
    }).toArray(Scan[]::new);
  }

  @Override
  public @Nullable Scan binarySearchClosestScan(final float rt, final int mslevel) {
    return null;
  }

  @Override
  public @Nullable Scan binarySearchClosestScan(final float rt) {
    return null;
  }

  @Override
  public int binarySearchClosestScanIndex(final float rt) {
    return 0;
  }

  @Override
  public int binarySearchClosestScanIndex(final float rt, final int mslevel) {
    return 0;
  }

  @Override
  public @NotNull Range<Double> getDataMZRange() {
    return null;
  }

  @Override
  public boolean isContainsZeroIntensity() {
    return false;
  }

  @Override
  public boolean isContainsEmptyScans() {
    return false;
  }

  @Override
  public MassSpectrumType getSpectraType() {
    return null;
  }

  @Override
  public @NotNull Range<Float> getDataRTRange() {
    return null;
  }

  @Override
  public @NotNull Range<Double> getDataMZRange(final int msLevel) {
    return null;
  }

  @Override
  public @NotNull Range<Float> getDataRTRange(final Integer msLevel) {
    return null;
  }

  @Override
  public double getDataMaxBasePeakIntensity(final int msLevel) {
    return 0;
  }

  @Override
  public double getDataMaxTotalIonCurrent(final int msLevel) {
    return 0;
  }

  @Override
  public @NotNull List<PolarityType> getDataPolarity() {
    return null;
  }

  @Override
  public Color getColorAWT() {
    return null;
  }

  @Override
  public javafx.scene.paint.Color getColor() {
    return null;
  }

  @Override
  public void setColor(final javafx.scene.paint.Color color) {

  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> colorProperty() {
    return null;
  }

  @Override
  public void close() {

  }

  @Override
  public @Nullable MemoryMapStorage getMemoryMapStorage() {
    return null;
  }

  @Override
  public void addScan(final Scan newScan) throws IOException {

  }

  @Override
  public String setNameNoChecks(@NotNull final String name) {
    return null;
  }

  @Override
  public @NotNull ObservableList<Scan> getScans() {
    return scans;
  }

  @Override
  public void applyMassListChanged(final Scan scan, final MassList old, final MassList masses) {
  }

  @Override
  public @NotNull ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return appliedMethods;
  }

  @Override
  public StringProperty nameProperty() {
    return new SimpleStringProperty(getName());
  }
}
