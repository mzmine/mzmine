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

package io.github.mzmine.modules.dataanalysis.statsdashboard;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;

public class StatsDashboardModel {

  private final ObjectProperty<List<FeatureList>> flists = new SimpleObjectProperty<>();
  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>();
  private final ObjectProperty<AbundanceMeasure> abundance = new SimpleObjectProperty<>(
      AbundanceMeasure.Height);
  private final ObjectProperty<MetadataColumn<?>> metadataColumn = new SimpleObjectProperty<>();

  @NotNull
  public List<FeatureList> getFlists() {
    return requireNonNullElse(flists.get(), List.of());
  }

  public void setFlists(List<FeatureList> flists) {
    this.flists.set(flists);
  }

  public ObjectProperty<List<FeatureList>> flistsProperty() {
    return flists;
  }

  public List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  public void setSelectedRows(List<FeatureListRow> selectedRows) {
    this.selectedRows.set(selectedRows);
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public AbundanceMeasure getAbundance() {
    return abundance.get();
  }

  public void setAbundance(AbundanceMeasure abundance) {
    this.abundance.set(abundance);
  }

  public ObjectProperty<AbundanceMeasure> abundanceProperty() {
    return abundance;
  }

  public MetadataColumn<?> getMetadataColumn() {
    return metadataColumn.get();
  }

  public void setMetadataColumn(MetadataColumn<?> metadataColumn) {
    this.metadataColumn.set(metadataColumn);
  }

  public ObjectProperty<MetadataColumn<?>> metadataColumnProperty() {
    return metadataColumn;
  }
}
