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

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.dataprocessing.norm_intensity.MetadataNormalizationConfig.Mode;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingComponent;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;

public class MetadataNormalizationConfigComponent extends HBox {

  private final ComboComponent<Mode> modeCombo;
  private final MetadataGroupingComponent metadataCol;

  public MetadataNormalizationConfigComponent(ComboComponent<Mode> modeCombo,
      MetadataGroupingComponent metadataCol) {
    super(FxLayout.DEFAULT_SPACE, modeCombo, metadataCol);
    FxLayout.applyDefaults(this, Insets.EMPTY);
    this.modeCombo = modeCombo;
    this.metadataCol = metadataCol;
  }

  public MetadataNormalizationConfig getValue() {
    return new MetadataNormalizationConfig(metadataCol.getValue(), modeCombo.getValue());
  }

  public void setValue(@NotNull MetadataNormalizationConfig newValue) {
    modeCombo.setValue(newValue.mode());
    metadataCol.setValue(newValue.metadataColumn());
  }
}
