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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ExternalRowHtmlVisualizerModel {

  private final StringProperty masstFile = new SimpleStringProperty(
      "D:/git/mzmine3/mzmine-community/src/test/resources/modules/id_masst_meta/combined.html");
  private final ObjectProperty<FeatureListRow> selectedRow = new SimpleObjectProperty<>();

  public FeatureListRow getSelectedRow() {
    return selectedRow.get();
  }

  public ObjectProperty<FeatureListRow> selectedRowProperty() {
    return selectedRow;
  }

  public void setSelectedRow(final FeatureListRow selectedRow) {
    this.selectedRow.set(selectedRow);
  }

  public String getMasstFile() {
    return masstFile.get();
  }

  public void setMasstFile(final String masstFile) {
    this.masstFile.set(masstFile);
  }

  public StringProperty masstFileProperty() {
    return masstFile;
  }
}
