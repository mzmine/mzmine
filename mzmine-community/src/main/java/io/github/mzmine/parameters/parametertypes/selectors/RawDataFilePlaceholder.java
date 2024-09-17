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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.MemoryMapStorage;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawDataFilePlaceholder implements RawDataFile {

  private final String name;
  private final String absPath;
  @Nullable
  private final Integer fileHashCode;

  public RawDataFilePlaceholder(@NotNull final RawDataFile file) {
    name = file.getName();
    absPath = file.getAbsolutePath();
    if (file instanceof RawDataFilePlaceholder rfp) {
      if (rfp.fileHashCode == null) {
        fileHashCode = null;
      } else {
        fileHashCode = rfp.fileHashCode;
      }
    } else {
      fileHashCode = file.hashCode();
    }
  }

  public RawDataFilePlaceholder(@NotNull String name, @Nullable String absPath) {
    this(name, absPath, null);
  }

  public RawDataFilePlaceholder(@NotNull String name, @Nullable String absPath,
      @Nullable Integer fileHashCode) {
    this.name = name;
    this.absPath = absPath;
    this.fileHashCode = fileHashCode;
  }

  public @Nullable Integer getFileHashCode() {
    return fileHashCode;
  }

  /**
   * @return The first matching raw data file of the current project.
   */
  @Nullable
  public RawDataFile getMatchingFile() {
    final MZmineProject proj = ProjectService.getProjectManager().getCurrentProject();
    if (proj == null) {
      return null;
    }

    return proj.getCurrentRawDataFiles().stream().filter(this::matches).findFirst().orElse(null);
  }

  public boolean matches(@Nullable final RawDataFile file) {
    return file != null && file.getName().equals(name) && Objects.equals(absPath,
        file.getAbsolutePath()) && (fileHashCode == null || Objects.equals(fileHashCode,
        file.hashCode()));
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public @Nullable String getAbsolutePath() {
    return absPath;
  }

  @Override
  public int getNumOfScans() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public int getNumOfScans(int msLevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public int getMaxCentroidDataPoints() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public int getMaxRawDataPoints() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull int[] getMSLevels() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull List<Scan> getScanNumbers(int msLevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull Scan[] getScanNumbers(int msLevel, @NotNull Range<Float> rtRange) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public Scan binarySearchClosestScan(float rt, int mslevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public Scan binarySearchClosestScan(float rt) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public int binarySearchClosestScanIndex(final float rt) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public int binarySearchClosestScanIndex(final float rt, final int mslevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull Range<Double> getDataMZRange() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public boolean isContainsZeroIntensity() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public boolean isContainsEmptyScans() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public MassSpectrumType getSpectraType() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull Range<Float> getDataRTRange() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull Range<Double> getDataMZRange(int msLevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull Range<Float> getDataRTRange(Integer msLevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public double getDataMaxBasePeakIntensity(int msLevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public double getDataMaxTotalIonCurrent(int msLevel) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull List<PolarityType> getDataPolarity() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public Color getColorAWT() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public javafx.scene.paint.Color getColor() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public void setColor(javafx.scene.paint.Color color) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> colorProperty() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @Nullable MemoryMapStorage getMemoryMapStorage() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public void addScan(Scan newScan) throws IOException {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull ObservableList<Scan> getScans() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public void applyMassListChanged(Scan scan, MassList old, MassList masses) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public @NotNull ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    throw new UnsupportedOperationException(
        "This class is only to be used in the RawDataFilesSelection and does not support the required operation.");
  }

  @Override
  public List<OtherDataFile> getOtherDataFiles() {
    return List.of();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RawDataFilePlaceholder that = (RawDataFilePlaceholder) o;
    return Objects.equals(getName(), that.getName()) && Objects.equals(absPath, that.absPath)
        && Objects.equals(fileHashCode, that.fileHashCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), absPath, fileHashCode);
  }
}
