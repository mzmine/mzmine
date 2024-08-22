/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.other_correlationdashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;

public class CorrelationDashboardModel {

  private ObjectProperty<@Nullable ModularFeatureList> featureList = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable FeatureListRow> selectedRow = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable OtherFeature> selectedOtherRawTrace = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable OtherFeature> selectedOtherFeature = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable RawDataFile> selectedRawDataFile = new SimpleObjectProperty<>();

  private DoubleProperty uvToMsRtOffset = new SimpleDoubleProperty();

  private ObjectProperty<ChromatogramPlotController> uvPlotController = new SimpleObjectProperty<>(
      new ChromatogramPlotController());
  private ObjectProperty<ChromatogramPlotController> msPlotController = new SimpleObjectProperty<>(
      new ChromatogramPlotController());
  private ObjectProperty<ChromatogramPlotController> correlationPlotController = new SimpleObjectProperty<>(
      new ChromatogramPlotController());

  public @Nullable FeatureListRow getSelectedRow() {
    return selectedRow.get();
  }

  public void setSelectedRow(@Nullable FeatureListRow selectedRow) {
    this.selectedRow.set(selectedRow);
  }

  public ObjectProperty<@Nullable FeatureListRow> selectedRowProperty() {
    return selectedRow;
  }

  public @Nullable OtherFeature getSelectedOtherRawTrace() {
    return selectedOtherRawTrace.get();
  }

  public void setSelectedOtherRawTrace(@Nullable OtherFeature selectedOtherRawTrace) {
    this.selectedOtherRawTrace.set(selectedOtherRawTrace);
  }

  public ObjectProperty<@Nullable OtherFeature> selectedOtherRawTraceProperty() {
    return selectedOtherRawTrace;
  }

  public @Nullable OtherFeature getSelectedOtherFeature() {
    return selectedOtherFeature.get();
  }

  public void setSelectedOtherFeature(@Nullable OtherFeature selectedOtherFeature) {
    this.selectedOtherFeature.set(selectedOtherFeature);
  }

  public ObjectProperty<@Nullable OtherFeature> selectedOtherFeatureProperty() {
    return selectedOtherFeature;
  }

  public @Nullable RawDataFile getSelectedRawDataFile() {
    return selectedRawDataFile.get();
  }

  public void setSelectedRawDataFile(@Nullable RawDataFile selectedRawDataFile) {
    this.selectedRawDataFile.set(selectedRawDataFile);
  }

  public ObjectProperty<@Nullable RawDataFile> selectedRawDataFileProperty() {
    return selectedRawDataFile;
  }

  public double getUvToMsRtOffset() {
    return uvToMsRtOffset.get();
  }

  public void setUvToMsRtOffset(double uvToMsRtOffset) {
    this.uvToMsRtOffset.set(uvToMsRtOffset);
  }

  public DoubleProperty uvToMsRtOffsetProperty() {
    return uvToMsRtOffset;
  }

  public ChromatogramPlotController getUvPlotController() {
    return uvPlotController.get();
  }

  public void setUvPlotController(ChromatogramPlotController uvPlotController) {
    this.uvPlotController.set(uvPlotController);
  }

  public ObjectProperty<ChromatogramPlotController> uvPlotControllerProperty() {
    return uvPlotController;
  }

  public ChromatogramPlotController getMsPlotController() {
    return msPlotController.get();
  }

  public void setMsPlotController(ChromatogramPlotController msPlotController) {
    this.msPlotController.set(msPlotController);
  }

  public ObjectProperty<ChromatogramPlotController> msPlotControllerProperty() {
    return msPlotController;
  }

  public ChromatogramPlotController getCorrelationPlotController() {
    return correlationPlotController.get();
  }

  public void setCorrelationPlotController(ChromatogramPlotController correlationPlotController) {
    this.correlationPlotController.set(correlationPlotController);
  }

  public ObjectProperty<ChromatogramPlotController> correlationPlotControllerProperty() {
    return correlationPlotController;
  }

  public @Nullable ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public ObjectProperty<@Nullable ModularFeatureList> featureListProperty() {
    return featureList;
  }

  public void setFeatureList(@Nullable ModularFeatureList featureList) {
    this.featureList.set(featureList);
  }
}
