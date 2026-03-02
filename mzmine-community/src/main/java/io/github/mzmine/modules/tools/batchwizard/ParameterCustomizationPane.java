/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import java.util.List;
import javafx.scene.layout.BorderPane;

/**
 * UI pane for customizing batch module parameters. This is the editor component for
 * {@link io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverridesParameter}.
 *
 * <p>Acts as a thin {@link BorderPane} adapter around the MVCI stack
 * ({@link ParameterCustomizationController}, {@link ParameterCustomizationViewBuilder},
 * {@link ParameterCustomizationModel}) so that the existing
 * {@link io.github.mzmine.parameters.UserParameter} API is preserved.
 */
public class ParameterCustomizationPane extends BorderPane {

  private final ParameterCustomizationController controller;

  public ParameterCustomizationPane() {
    controller = new ParameterCustomizationController();
    setCenter(controller.buildView());
  }

  /**
   * Returns all currently configured parameter overrides.
   */
  public List<ParameterOverride> getParameterOverrides() {
    return controller.getParameterOverrides();
  }

  /**
   * Loads a list of parameter overrides into the pane (e.g. when deserialising from XML).
   */
  public void setParameterOverrides(List<ParameterOverride> overrides) {
    controller.setParameterOverrides(overrides);
  }
}
