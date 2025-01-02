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

package io.github.mzmine.parameters.impl;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * for parameter classes that internally use a SimpleParameterSet and want to use functionality like
 * clone etc
 */
public abstract class ComposedParameterSet implements ParameterSet {

  protected abstract ParameterSet getParamSet();

  /**
   * Needed for cloneParameter which
   *
   * @param newParameters the new set of parameters
   */
  protected abstract void setParamSet(ParameterSet newParameters);

  @Override
  public void setSkipSensitiveParameters(final boolean skipSensitiveParameters) {
    getParamSet().setSkipSensitiveParameters(skipSensitiveParameters);
  }

  @Override
  public Parameter<?>[] getParameters() {
    return getParamSet().getParameters();
  }

  @Override
  public <T extends Parameter<?>> T getParameter(final T parameter) {
    return getParamSet().getParameter(parameter);
  }

  @Override
  public Map<String, Parameter<?>> loadValuesFromXML(final Element element) {
    return getParamSet().loadValuesFromXML(element);
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams) {
    getParamSet().handleLoadedParameters(loadedParams);
  }

  @Override
  public void saveValuesToXML(final Element element) {
    getParamSet().saveValuesToXML(element);
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages) {
    return getParamSet().checkParameterValues(errorMessages);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    return getParamSet().checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters);
  }

  @Override
  public ParameterSet cloneParameterSet() {
    setParamSet(getParamSet().cloneParameterSet());
    return getParamSet();
  }

  @Override
  public ParameterSet cloneParameterSet(final boolean keepSelection) {
    setParamSet(getParamSet().cloneParameterSet(keepSelection));
    return getParamSet();
  }

  @Override
  public ExitCode showSetupDialog(final boolean valueCheckRequired) {
    return getParamSet().showSetupDialog(valueCheckRequired);
  }

  @Override
  public BooleanProperty parametersChangeProperty() {
    return getParamSet().parametersChangeProperty();
  }

  @Override
  public @Nullable String getOnlineHelpUrl() {
    return getParamSet().getOnlineHelpUrl();
  }

  @Override
  public String getModuleNameAttribute() {
    return getParamSet().getModuleNameAttribute();
  }

  @Override
  public void setModuleNameAttribute(final String moduleName) {
    getParamSet().setModuleNameAttribute(moduleName);
  }
}
