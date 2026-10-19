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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

public class FeatureListSummaryModel {

  /// the feature list this summary shows, null while a raw data file is shown
  private final ObjectProperty<@Nullable ModularFeatureList> featureList = new SimpleObjectProperty<>();

  private final StringProperty title = new SimpleStringProperty("None selected");
  private final StringProperty numRowsLabel = new SimpleStringProperty("Number of Rows");
  private final StringProperty numRows = new SimpleStringProperty("");
  private final StringProperty numAnnotatedLabel = new SimpleStringProperty("Annotated rows");
  private final StringProperty numAnnotated = new SimpleStringProperty("");
  private final StringProperty createdLabel = new SimpleStringProperty("Date created");
  private final StringProperty created = new SimpleStringProperty("");

  /// single observable list bound to the applied methods view, updated via setAll
  private final ObservableList<FeatureListAppliedMethod> appliedMethods = FXCollections.observableArrayList();

  public @Nullable ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public void setFeatureList(@Nullable ModularFeatureList featureList) {
    this.featureList.set(featureList);
  }

  public ObjectProperty<@Nullable ModularFeatureList> featureListProperty() {
    return featureList;
  }

  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public String getNumRowsLabel() {
    return numRowsLabel.get();
  }

  public void setNumRowsLabel(String numRowsLabel) {
    this.numRowsLabel.set(numRowsLabel);
  }

  public StringProperty numRowsLabelProperty() {
    return numRowsLabel;
  }

  public String getNumRows() {
    return numRows.get();
  }

  public void setNumRows(String numRows) {
    this.numRows.set(numRows);
  }

  public StringProperty numRowsProperty() {
    return numRows;
  }

  public String getNumAnnotatedLabel() {
    return numAnnotatedLabel.get();
  }

  public void setNumAnnotatedLabel(String numAnnotatedLabel) {
    this.numAnnotatedLabel.set(numAnnotatedLabel);
  }

  public StringProperty numAnnotatedLabelProperty() {
    return numAnnotatedLabel;
  }

  public String getNumAnnotated() {
    return numAnnotated.get();
  }

  public void setNumAnnotated(String numAnnotated) {
    this.numAnnotated.set(numAnnotated);
  }

  public StringProperty numAnnotatedProperty() {
    return numAnnotated;
  }

  public String getCreatedLabel() {
    return createdLabel.get();
  }

  public void setCreatedLabel(String createdLabel) {
    this.createdLabel.set(createdLabel);
  }

  public StringProperty createdLabelProperty() {
    return createdLabel;
  }

  public String getCreated() {
    return created.get();
  }

  public void setCreated(String created) {
    this.created.set(created);
  }

  public StringProperty createdProperty() {
    return created;
  }

  public ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return appliedMethods;
  }
}
