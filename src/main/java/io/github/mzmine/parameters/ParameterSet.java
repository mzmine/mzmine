/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters;

import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javafx.beans.property.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * This class represents a general parameter set of a module. Typical module will use a
 * SimpleParameterSet instance.
 */
public interface ParameterSet extends ParameterContainer {

  public Parameter<?>[] getParameters();

  public <T extends Parameter<?>> T getParameter(T parameter);

  default <V, T extends Parameter<V>> V getValue(T parameter) {
    final T actualParam = getParameter(parameter);
    return actualParam == null ? null : actualParam.getValue();
  }

  public void loadValuesFromXML(Element element);

  public void saveValuesToXML(Element element);

  public boolean checkParameterValues(Collection<String> errorMessages);

  public ParameterSet cloneParameterSet();

  ParameterSet cloneParameterSet(boolean keepSelection);

  /**
   * This method specifies the fitness of a module to process data acquired on a ion mobility
   * spectrometry (IMS)-mass spectrometer. The default implementation returns {@link
   * IonMobilitySupport#UNTESTED}. However, overriding this method is encouraged to clarify it's
   * fitness for ion mobility data, even if it will still return {@link
   * IonMobilitySupport#UNTESTED}.
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
  public String toString();

  public ExitCode showSetupDialog(boolean valueCheckRequired);

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
  public BooleanProperty parametersChangeProperty();

  @Nullable String getOnlineHelpUrl();
}
