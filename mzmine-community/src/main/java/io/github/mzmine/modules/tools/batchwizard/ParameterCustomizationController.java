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

import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.tools.batchwizard.ParameterCustomizationModel.OverrideKey;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * MVCI controller for the parameter customization pane. Owns all business logic and updates the
 * {@link ParameterCustomizationModel}. The {@link ParameterCustomizationViewBuilder} creates the UI
 * and binds it to the model.
 */
public class ParameterCustomizationController extends FxController<ParameterCustomizationModel> {

  private final ParameterCustomizationViewBuilder viewBuilder;

  public ParameterCustomizationController() {
    super(new ParameterCustomizationModel());
    viewBuilder = new ParameterCustomizationViewBuilder(model);
  }

  @Override
  protected @NotNull FxViewBuilder<ParameterCustomizationModel> getViewBuilder() {
    return viewBuilder;
  }

  // --- Public API (used by ParameterCustomizationPane adapter) ---

  public List<ParameterOverride> getParameterOverrides() {
    return new ArrayList<>(model.getOverrides().values());
  }

  public void setParameterOverrides(List<ParameterOverride> overrides) {
    onGuiThread(() -> {
      model.getOverrides().clear();
      for (ParameterOverride override : overrides) {
        model.getOverrides().put(
            new OverrideKey(override.moduleClassName(), override.parameterWithValue().getName(),
                override.scope()), override);
      }
    });
  }
}
