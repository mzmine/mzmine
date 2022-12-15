/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters;

import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * This class represents a general parameter set of a module. Typical module will use a
 * SimpleParameterSet instance.
 */
public interface ParameterSet extends ParameterContainer {

  Parameter<?>[] getParameters();

  <T extends Parameter<?>> T getParameter(T parameter);

  default <V, T extends Parameter<V>> V getValue(T parameter) {
    final T actualParam = getParameter(parameter);
    return actualParam == null ? null : actualParam.getValue();
  }

  default <V, T extends UserParameter<V, ?>> V getEmbeddedParameterValue(
      OptionalParameter<T> parameter) {
    final UserParameter<V, ?> actualParam = getParameter(parameter).getEmbeddedParameter();
    return actualParam == null ? null : actualParam.getValue();
  }

  /**
   * @param defaultValueSupplier A supplier for the default value. may be null.
   */
  default <V, T extends UserParameter<V, ?>> V getEmbeddedParameterValueIfSelectedOrElse(
      OptionalParameter<T> parameter, @Nullable Supplier<V> defaultValueSupplier) {
    if (getValue(parameter)) {
      final UserParameter<V, ?> actualParam = getParameter(parameter).getEmbeddedParameter();
      return actualParam == null ? null : actualParam.getValue();
    }
    return defaultValueSupplier != null ? defaultValueSupplier.get() : null;
  }

  default <T extends ParameterSet> ParameterSet getEmbeddedParameterValue(
      OptionalModuleParameter<T> parameter) {
    return getParameter(parameter).getEmbeddedParameters();
  }

  void loadValuesFromXML(Element element);

  void saveValuesToXML(Element element);

  boolean checkParameterValues(Collection<String> errorMessages);

  ParameterSet cloneParameterSet();

  ParameterSet cloneParameterSet(boolean keepSelection);

  /**
   * This method specifies the fitness of a module to process data acquired on a ion mobility
   * spectrometry (IMS)-mass spectrometer. The default implementation returns
   * {@link IonMobilitySupport#UNTESTED}. However, overriding this method is encouraged to clarify
   * it's fitness for ion mobility data, even if it will still return
   * {@link IonMobilitySupport#UNTESTED}.
   *
   * @return
   */
  @NotNull
  default IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.UNTESTED;
  }

  /**
   * Represent method's parameters and their values in human-readable format
   */
  @Override
  String toString();

  ExitCode showSetupDialog(boolean valueCheckRequired);

  /**
   * Set the value of a parameter
   *
   * @param parameter the parameter to change
   * @param value     the new value
   * @param <T>       Value type
   */
  default <T> void setParameter(Parameter<T> parameter, T value) {
    getParameter(parameter).setValue(value);
  }

  default <V, T extends UserParameter<V, ?>> void setParameter(OptionalParameter<T> optParam,
      boolean enabled, V value) {
    optParam.setValue(enabled);
    optParam.getEmbeddedParameter().setValue(value);
  }

  /**
   * Returns BooleanProperty which value is changed when some parameter of this ParameterSet is
   * changed. It is useful to perform operations directly dependant on the components corresponding
   * to this ParameterSet (e.g. TextField of a parameter is changed -> preview plot is updated).
   *
   * @return BooleanProperty signalizing a change of any parameter of this ParameterSet
   */
  BooleanProperty parametersChangeProperty();

  @Nullable String getOnlineHelpUrl();

  void setModuleNameAttribute(String moduleName);

  String getModuleNameAttribute();
}
