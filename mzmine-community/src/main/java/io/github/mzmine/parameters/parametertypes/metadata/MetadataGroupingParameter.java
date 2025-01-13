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

package io.github.mzmine.parameters.parametertypes.metadata;

import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.parameters.parametertypes.StringValueParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataGroupingParameter extends StringValueParameter<MetadataGroupingComponent> {

  private final List<AvailableTypes> types;

  public MetadataGroupingParameter(final String name, final String description) {
    // extract columnName titles from metadata
    this(name, description, AvailableTypes.values());
  }

  public MetadataGroupingParameter(final String name, final String description,
      final String defaultValue) {
    this(name, description, defaultValue, AvailableTypes.values());
  }

  public MetadataGroupingParameter(final String name, final String description,
      @NotNull AvailableTypes... types) {
    this(name, description, "", types);
  }

  public MetadataGroupingParameter(final String name, final String description,
      final String defaultValue, @NotNull AvailableTypes... types) {
    // extract columnName titles from metadata
    super(name, description, defaultValue);
    this.types = List.of(types);
  }

  public MetadataGroupingParameter() {
    this("Metadata grouping",
        "Group based on metadata columnName. Open sample metadata from the 'Project' menu, "
        + "\nadd metadata, and open this dialog again.");
  }

  @Override
  public MetadataGroupingComponent createEditingComponent() {
    return new MetadataGroupingComponent(types);
  }

  @Override
  public void setValueFromComponent(final MetadataGroupingComponent comp) {
    setValue(comp.getValue());
  }

  @Override
  public void setValueToComponent(final MetadataGroupingComponent comp,
      @Nullable final String newValue) {
    comp.setValue(newValue);
  }

  @Override
  public MetadataGroupingParameter cloneParameter() {
    MetadataGroupingParameter copy = new MetadataGroupingParameter(name, description, getValue(),
        types.toArray(AvailableTypes[]::new));
    return copy;
  }
}
