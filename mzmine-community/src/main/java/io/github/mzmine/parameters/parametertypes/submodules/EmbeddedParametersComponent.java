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

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.parameters.EmbeddedParameterComponentProvider;
import io.github.mzmine.parameters.ParameterComponent;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.Nullable;

/**
 * Basis for parameter components with sub parameters
 *
 * @param <ValueType> value of parameter
 */
public abstract class EmbeddedParametersComponent<ValueType> extends BorderPane implements
    ParameterComponent<ValueType>, EmbeddedParameterComponentProvider {

  protected ParameterSetupPane paramPane;

  public EmbeddedParametersComponent(final ParameterSet parameters) {
    paramPane = new ParameterSetupPane(true, parameters, false, false, null, true, false);
  }

  public void setParameterValuesToComponents() {
    paramPane.setParameterValuesToComponents();
  }

  public ParameterSet getEmbeddedParameters() {
    return paramPane.updateParameterSetFromComponents();
  }

  public ParameterSet updateParametersFromComponent() {
    return getEmbeddedParameters();
  }


  @Override
  public @Nullable Map<String, Node> getParametersAndComponents() {
    return paramPane != null ? paramPane.getParametersAndComponents() : null;
  }

}
