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

package io.github.mzmine.parameters.parametertypes;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.CompositeParametersParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelection;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelectionParameter;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class MinimumSamplesInOneMetadataGroupParameter extends
    CompositeParametersParameter<MinimumSamplesFilterConfig, MinimumSamplesInOneMetadataGroupComponent> {

  public static final String DEFAULT_NAME = "Minimum aligned samples (in one group)";
  public static final String DEFAULT_DESCRIPTION = """
      Minimum number of samples a feature was detected and aligned in a SPECIFIC GROUP in one metadata column. \
      Both the absolute threshold and a percentage is applied. The value will be rounded down to the nearest whole number.""";

  private final MinimumSamplesParameter minSamples;
  private final MetadataGroupSelectionParameter metadata = new MetadataGroupSelectionParameter();
  private final Integer minAbsolute;
  private MinimumSamplesFilterConfig value;


  public MinimumSamplesInOneMetadataGroupParameter() {
    this(DEFAULT_NAME, DEFAULT_DESCRIPTION);
  }

  public MinimumSamplesInOneMetadataGroupParameter(final String name, final String description) {
    this(name, description, MinimumSamplesFilterConfig.DEFAULT);
  }

  public MinimumSamplesInOneMetadataGroupParameter(final String name, final String description,
      final Integer minAbsolute) {
    this(name, description, MinimumSamplesFilterConfig.DEFAULT, minAbsolute);
  }

  public MinimumSamplesInOneMetadataGroupParameter(final String name, final String description,
      final MinimumSamplesFilterConfig value) {
    this(name, description, value, 0);
  }

  @Override
  protected Parameter<?>[] getInternalParameters() {
    return new Parameter[]{minSamples, metadata};
  }

  public MinimumSamplesInOneMetadataGroupParameter(final String name, final String description,
      final MinimumSamplesFilterConfig value, final Integer minAbsolute) {
    super(name, description);
    this.value = value;
    this.minAbsolute = minAbsolute;

    minSamples = new MinimumSamplesParameter("Minimum samples", "", minAbsolute);
    setValue(value);
  }

  @Override
  public MinimumSamplesInOneMetadataGroupParameter cloneParameter() {
    return new MinimumSamplesInOneMetadataGroupParameter(name, description, value, minAbsolute);
  }

  @Override
  public MinimumSamplesInOneMetadataGroupComponent createEditingComponent() {
    return new MinimumSamplesInOneMetadataGroupComponent(minSamples.createEditingComponent(),
        metadata.createEditingComponent(), value);
  }

  @Override
  public void setValueFromComponent(MinimumSamplesInOneMetadataGroupComponent comp) {
    setValue(comp.getValue());
  }

  @Override
  public void setValueToComponent(MinimumSamplesInOneMetadataGroupComponent comp,
      @Nullable MinimumSamplesFilterConfig newValue) {
    comp.setValue(newValue);
  }

  @Override
  public MinimumSamplesFilterConfig getValue() {
    return value;
  }

  @Override
  public void setValue(MinimumSamplesFilterConfig newValue) {
    this.value = newValue;
    // also set value to sub parameters
    final String columnName = requireNonNullElse(value.columnName(), "");
    final String groupStr = requireNonNullElse(value.group(), "");

    metadata.setValue(new MetadataGroupSelection(columnName, groupStr));
    minSamples.setValue(value.minSamples());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return value != null && metadata.checkValue(errorMessages) && minSamples.checkValue(
        errorMessages);
  }

  @Override
  protected void handleLoadedParameters(Map<String, Parameter<?>> loadedParameters) {
    super.handleLoadedParameters(loadedParameters);
    final MetadataGroupSelection selection = metadata.getValue();
    setValue(new MinimumSamplesFilterConfig(minSamples.getValue(), selection.columnName(),
        selection.groupStr()));
  }
}
