/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.external_row_html;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.StringUtils;
import java.io.File;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ExternalRowHtmlVisualizerModel {

  private final StringProperty externalFolder = new SimpleStringProperty("");

  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>();
  private final ObservableValue<FeatureListRow> selectedRow;
  private final ObservableValue<String> selectedRowFullId;

  /**
   * Choices of external html files matching the selected row
   */
  private final ObservableList<String> htmlChoices = FXCollections.observableArrayList();
  /**
   * Defines the simple html without the rowID
   */
  private final StringProperty selectedHtml = new SimpleStringProperty();
  /**
   * Defines the full external html file: folder/selectedRowFullID selectedHTML
   */
  private final Property<File> selectedFullHtml = new SimpleObjectProperty<>();

  public ExternalRowHtmlVisualizerModel() {
    selectedRow = PropertyUtils.firstElementProperty(selectedRows);
    selectedRowFullId = selectedRow.map(FeatureUtils::rowToFullId).orElse("");
  }

  public FeatureListRow getSelectedRow() {
    return selectedRow.getValue();
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  public ObservableValue<FeatureListRow> selectedRowProperty() {
    return selectedRow;
  }

  public String getExternalFolder() {
    return externalFolder.get();
  }

  public void setExternalFolder(final String externalFolder) {
    this.externalFolder.set(externalFolder);
  }

  public StringProperty externalFolderProperty() {
    return externalFolder;
  }

  public String getSelectedRowFullId() {
    return selectedRowFullId.getValue();
  }

  public ObservableValue<String> selectedRowFullIdProperty() {
    return selectedRowFullId;
  }

  public ObservableList<String> getHtmlChoices() {
    return htmlChoices;
  }

  public File getExternalFolderAsFile() {
    return StringUtils.isBlank(externalFolder.get()) ? null : new File(externalFolder.get());
  }

  public String getSelectedHtml() {
    return selectedHtml.get();
  }

  public StringProperty selectedHtmlProperty() {
    return selectedHtml;
  }

  public File getSelectedFullHtml() {
    return selectedFullHtml.getValue();
  }

  public Property<File> selectedFullHtmlProperty() {
    return selectedFullHtml;
  }

  public void setSelectedRows(List<? extends FeatureListRow> selectedRows) {
    this.selectedRows.set(List.copyOf(selectedRows));
  }
}
