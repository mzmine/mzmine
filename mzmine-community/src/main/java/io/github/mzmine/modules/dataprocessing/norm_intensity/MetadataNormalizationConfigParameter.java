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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.modules.dataprocessing.norm_intensity.MetadataNormalizationConfig.Mode;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.parameters.CompositeParametersParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataNormalizationConfigParameter extends
    CompositeParametersParameter<MetadataNormalizationConfig, MetadataNormalizationConfigComponent> {

  private final MetadataGroupingParameter metadataCol;

  private final ComboParameter<MetadataNormalizationConfig.Mode> mode;

  public MetadataNormalizationConfigParameter(String name, String description) {
    this(name, description, MetadataNormalizationConfig.getDefault());
  }

  public MetadataNormalizationConfigParameter(String name, String description,
      @NotNull MetadataNormalizationConfig defaultVal) {
    super(name, description);
    mode = new ComboParameter<>("mode", "", MetadataNormalizationConfig.Mode.values(),
        Mode.multiply);
    metadataCol = new MetadataGroupingParameter("metadata column", "", AvailableTypes.NUMBER);

    setValue(defaultVal);
  }

  @Override
  protected Parameter<?>[] getInternalParameters() {
    return new Parameter[]{mode, metadataCol};
  }

  @Override
  public MetadataNormalizationConfigComponent createEditingComponent() {
    return new MetadataNormalizationConfigComponent(mode.createEditingComponent(),
        metadataCol.createEditingComponent());
  }

  @Override
  public void setValueFromComponent(MetadataNormalizationConfigComponent comp) {
    setValue(comp.getValue());
  }

  @Override
  public void setValueToComponent(MetadataNormalizationConfigComponent comp,
      @Nullable MetadataNormalizationConfig newValue) {
    comp.setValue(requireNonNullElse(newValue, MetadataNormalizationConfig.getDefault()));
  }

  @Override
  @NotNull
  public MetadataNormalizationConfig getValue() {
    return new MetadataNormalizationConfig(metadataCol.getValue(), mode.getValue());
  }

  @Override
  public void setValue(@Nullable MetadataNormalizationConfig newValue) {
    newValue = requireNonNullElse(newValue, MetadataNormalizationConfig.getDefault());
    mode.setValue(newValue.mode());
    metadataCol.setValue(newValue.metadataColumn());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (getValue().metadataColumn().isBlank()) {
      errorMessages.add("Metadata column cannot be blank");
      return false;
    }

    return true;
  }

  @Override
  @NotNull
  public MetadataNormalizationConfigParameter cloneParameter() {
    return new MetadataNormalizationConfigParameter(getName(), getDescription(), getValue());
  }
}
