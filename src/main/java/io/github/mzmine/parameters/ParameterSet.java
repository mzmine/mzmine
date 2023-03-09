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
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

  /**
   * Version is saved to the batch file steps and compared when loaded. This version number should
   * change when parameter names change or if parameters are added or removed. Override the method
   * and increment the version.
   *
   * @return the parameter set version, 0 if unspecified (before MZmine 3.4.0)
   */
  default int getVersion() {
    return 1;
  }

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
  default <V, T extends UserParameter<V, ?>> V getEmbeddedParameterValueIfSelectedOrElseGet(
      OptionalParameter<T> parameter, @Nullable Supplier<V> defaultValueSupplier) {
    if (getValue(parameter)) {
      final UserParameter<V, ?> actualParam = getParameter(parameter).getEmbeddedParameter();
      return actualParam == null ? null : actualParam.getValue();
    }
    return defaultValueSupplier != null ? defaultValueSupplier.get() : null;
  }

  /**
   * @param defaultValue A default value. may be null.
   */
  default <V, T extends UserParameter<V, ?>> V getEmbeddedParameterValueIfSelectedOrElse(
      OptionalParameter<T> parameter, @Nullable V defaultValue) {
    if (getValue(parameter)) {
      final UserParameter<V, ?> actualParam = getParameter(parameter).getEmbeddedParameter();
      return actualParam == null ? null : actualParam.getValue();
    }
    return defaultValue;
  }


  default <T extends ParameterSet, V> ParameterSet getEmbeddedParameterValue(
      EmbeddedParameterSet<T, V> parameter) {
    return getParameter(parameter).getEmbeddedParameters();
  }

  void loadValuesFromXML(Element element);

  void saveValuesToXML(Element element);


  /**
   * Maps the names of parameters to the parameter for {@link #loadValuesFromXML(Element)}.
   * <p>
   * Extend this method to map old parameter names (maybe saved to batch files) to the parameter.
   * Only works if the old and new parameter are of the same type (save and load the parameter
   * values the same way).
   *
   * @return map of name to parameter
   */
  default Map<String, Parameter<?>> getNameParameterMap() {
    var parameters = getParameters();
    Map<String, Parameter<?>> nameParameterMap = new HashMap<>(parameters.length);
    for (final Parameter<?> p : parameters) {
      nameParameterMap.put(p.getName(), p);
    }
    return nameParameterMap;
  }


  /**
   * check all parameters. Also {@link FeatureListsParameter} and {@link RawDataFilesParameter}.
   * Those parameters cannot be checked in batch mode. Then use
   * {@link #checkParameterValues(Collection, boolean)}
   *
   * @param errorMessages will add error messages for each parameter here
   * @return true if all parameters are set correctly
   */
  default boolean checkParameterValues(Collection<String> errorMessages) {
    return checkParameterValues(errorMessages, false);
  }

  /**
   * check all parameters with the option to skip {@link FeatureListsParameter} and
   * {@link RawDataFilesParameter}. Those parameters cannot be checked in batch mode.
   *
   * @param errorMessages                       will add error messages for each parameter here
   * @param skipRawDataAndFeatureListParameters skip RawDataFile and FeatureList selections if true
   * @return true if all parameters are set correctly
   */
  boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters);

  ParameterSet cloneParameterSet();

  ParameterSet cloneParameterSet(boolean keepSelection);

  /**
   * This method specifies the fitness of a module to process data acquired on a ion mobility
   * spectrometry (IMS)-mass spectrometer. The default implementation returns
   * {@link IonMobilitySupport#UNTESTED}. However, overriding this method is encouraged to clarify
   * it's fitness for ion mobility data, even if it will still return
   * {@link IonMobilitySupport#UNTESTED}.
   *
   * @return true if module supports IMS
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
    getParameter(optParam).setValue(enabled);
    getParameter(optParam).getEmbeddedParameter().setValue(value);
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

  String getModuleNameAttribute();

  void setModuleNameAttribute(String moduleName);

  /**
   * Defines if user has to setup parameters
   *
   * @return true if there are user options
   */
  default boolean hasUserParameters() {
    return Arrays.stream(getParameters())
        .anyMatch(p -> p instanceof UserParameter<?, ?> && !(p instanceof HiddenParameter));
  }

  /**
   * @return true if parameter name is in list of parameter
   */
  default boolean hasParameter(Parameter<?> p) {
    return Arrays.stream(getParameters()).map(Parameter::getName)
        .anyMatch(name -> Objects.equals(p.getName(), name));
  }
}
