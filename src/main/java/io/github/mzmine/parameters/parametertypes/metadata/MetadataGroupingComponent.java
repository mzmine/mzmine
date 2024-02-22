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

package io.github.mzmine.parameters.parametertypes.metadata;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.PropertyComponent;
import java.util.List;
import javafx.beans.property.Property;
import javafx.scene.control.TextField;
import org.controlsfx.control.textfield.TextFields;

public class MetadataGroupingComponent extends TextField implements PropertyComponent<String> {

  private final List<AvailableTypes> availableTypes;

  public MetadataGroupingComponent() {
    this(AvailableTypes.values());
  }

  public MetadataGroupingComponent(AvailableTypes[] availableTypes) {
    this(List.of(availableTypes));
  }

  public MetadataGroupingComponent(List<AvailableTypes> types) {
    super();
    this.availableTypes = types;
    final String[] columns = MZmineCore.getProjectMetadata().getColumns().stream()
        .filter(col -> availableTypes.contains(col.getType())).map(MetadataColumn::getTitle)
        .toArray(String[]::new);
    TextFields.bindAutoCompletion(this, columns);
  }

  @Override
  public Property<String> valueProperty() {
    return this.textProperty();
  }
}
