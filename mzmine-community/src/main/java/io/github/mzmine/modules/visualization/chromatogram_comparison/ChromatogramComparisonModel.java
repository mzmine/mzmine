/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ChromatogramComparisonModel {

  private final ObjectProperty<@Nullable FeatureList> featureListA = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable FeatureList> featureListB = new SimpleObjectProperty<>();
  /**
   * Groups chromatograms of both lists, initialized with the chromatogram builder tolerances
   */
  private final ObjectProperty<@NotNull MZTolerance> mzTolerance = new SimpleObjectProperty<>(
      ChromatogramComparisonController.DEFAULT_TOLERANCE);
  /**
   * Data points of at least this intensity are signals, initialized with the minimum height of the
   * chromatogram builders. Null while the user enters an invalid number.
   */
  private final ObjectProperty<@Nullable Number> minSignalIntensity = new SimpleObjectProperty<>(
      0d);
  private final ObservableList<@NotNull ChromatogramGroup> groups = FXCollections.observableArrayList();
  private final ObjectProperty<@Nullable ChromatogramGroup> selectedGroup = new SimpleObjectProperty<>();
  private final ObjectProperty<@NotNull GroupFilter> groupFilter = new SimpleObjectProperty<>(
      GroupFilter.ALL);
  /**
   * Show the chromatograms of both lists as lines in one chart
   */
  private final BooleanProperty overlay = new SimpleBooleanProperty(false);
  /**
   * Show the XIC of the raw data around the m/z of the selected group with the XIC tolerance
   */
  private final BooleanProperty showXic = new SimpleBooleanProperty(false);
  private final ObjectProperty<@NotNull MZTolerance> xicTolerance = new SimpleObjectProperty<>(
      ChromatogramComparisonController.DEFAULT_XIC_TOLERANCE);
  private final ObjectProperty<@Nullable ToleranceXic> toleranceXic = new SimpleObjectProperty<>();
  /**
   * The raw data file of both lists and the scans that either builder used, in the order of the
   * file, set with the groups
   */
  private final ObjectProperty<@Nullable RawDataFile> comparisonFile = new SimpleObjectProperty<>();
  private final ObjectProperty<@NotNull List<Scan>> comparisonScans = new SimpleObjectProperty<>(
      List.of());
  private final StringProperty status = new SimpleStringProperty("");

  public @Nullable FeatureList getFeatureListA() {
    return featureListA.get();
  }

  public ObjectProperty<@Nullable FeatureList> featureListAProperty() {
    return featureListA;
  }

  public void setFeatureListA(@Nullable FeatureList featureListA) {
    this.featureListA.set(featureListA);
  }

  public @Nullable FeatureList getFeatureListB() {
    return featureListB.get();
  }

  public ObjectProperty<@Nullable FeatureList> featureListBProperty() {
    return featureListB;
  }

  public void setFeatureListB(@Nullable FeatureList featureListB) {
    this.featureListB.set(featureListB);
  }

  public @NotNull MZTolerance getMzTolerance() {
    return mzTolerance.get();
  }

  public ObjectProperty<@NotNull MZTolerance> mzToleranceProperty() {
    return mzTolerance;
  }

  public void setMzTolerance(@NotNull MZTolerance mzTolerance) {
    this.mzTolerance.set(mzTolerance);
  }

  public @Nullable Number getMinSignalIntensity() {
    return minSignalIntensity.get();
  }

  public ObjectProperty<@Nullable Number> minSignalIntensityProperty() {
    return minSignalIntensity;
  }

  public void setMinSignalIntensity(@Nullable Number minSignalIntensity) {
    this.minSignalIntensity.set(minSignalIntensity);
  }

  public ObservableList<@NotNull ChromatogramGroup> getGroups() {
    return groups;
  }

  public @Nullable ChromatogramGroup getSelectedGroup() {
    return selectedGroup.get();
  }

  public ObjectProperty<@Nullable ChromatogramGroup> selectedGroupProperty() {
    return selectedGroup;
  }

  public void setSelectedGroup(@Nullable ChromatogramGroup selectedGroup) {
    this.selectedGroup.set(selectedGroup);
  }

  public @NotNull GroupFilter getGroupFilter() {
    return groupFilter.get();
  }

  public ObjectProperty<@NotNull GroupFilter> groupFilterProperty() {
    return groupFilter;
  }

  public void setGroupFilter(@NotNull GroupFilter groupFilter) {
    this.groupFilter.set(groupFilter);
  }

  public boolean isOverlay() {
    return overlay.get();
  }

  public BooleanProperty overlayProperty() {
    return overlay;
  }

  public void setOverlay(boolean overlay) {
    this.overlay.set(overlay);
  }

  public boolean isShowXic() {
    return showXic.get();
  }

  public BooleanProperty showXicProperty() {
    return showXic;
  }

  public void setShowXic(boolean showXic) {
    this.showXic.set(showXic);
  }

  public @NotNull MZTolerance getXicTolerance() {
    return xicTolerance.get();
  }

  public ObjectProperty<@NotNull MZTolerance> xicToleranceProperty() {
    return xicTolerance;
  }

  public void setXicTolerance(@NotNull MZTolerance xicTolerance) {
    this.xicTolerance.set(xicTolerance);
  }

  public @Nullable ToleranceXic getToleranceXic() {
    return toleranceXic.get();
  }

  public ObjectProperty<@Nullable ToleranceXic> toleranceXicProperty() {
    return toleranceXic;
  }

  public void setToleranceXic(@Nullable ToleranceXic toleranceXic) {
    this.toleranceXic.set(toleranceXic);
  }

  public @Nullable RawDataFile getComparisonFile() {
    return comparisonFile.get();
  }

  public ObjectProperty<@Nullable RawDataFile> comparisonFileProperty() {
    return comparisonFile;
  }

  public void setComparisonFile(@Nullable RawDataFile comparisonFile) {
    this.comparisonFile.set(comparisonFile);
  }

  public @NotNull List<Scan> getComparisonScans() {
    return comparisonScans.get();
  }

  public ObjectProperty<@NotNull List<Scan>> comparisonScansProperty() {
    return comparisonScans;
  }

  public void setComparisonScans(@NotNull List<Scan> comparisonScans) {
    this.comparisonScans.set(comparisonScans);
  }

  public String getStatus() {
    return status.get();
  }

  public StringProperty statusProperty() {
    return status;
  }

  public void setStatus(String status) {
    this.status.set(status);
  }
}
