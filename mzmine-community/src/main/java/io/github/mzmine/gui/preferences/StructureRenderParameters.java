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

package io.github.mzmine.gui.preferences;

import io.github.mzmine.modules.visualization.molstructure.Structure2DRenderConfig;
import io.github.mzmine.modules.visualization.molstructure.Structure2DRenderConfig.Sizing;
import io.github.mzmine.parameters.RestoreDefaults;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Uses {@link ParameterSetParameter} to allow additional parameters that are extra compared to the
 * ones defined for {@link Structure2DRenderConfig}
 */
public class StructureRenderParameters extends SimpleParameterSet implements RestoreDefaults {

  public static final ComboParameter<Structure2DRenderConfig.Sizing> mode = new ComboParameter<>(
      "Mode",
      "Draw structures either fitted to the pane size or with fixed bond length and label sizes (will be zoomed if too large for pane)",
      List.of(Sizing.FIXED_SIZES_IN_BOUNDS, Sizing.FIT_TO_SIZE), Sizing.FIXED_SIZES_IN_BOUNDS);

  public static final DoubleParameter bondLength = new DoubleParameter("Bond length",
      "Bond length, default: %.1f".formatted(Structure2DRenderConfig.DEFAULT_BOND_LENGTH),
      new DecimalFormat("0.00"), Structure2DRenderConfig.DEFAULT_BOND_LENGTH, 0.01, null);

  public static final DoubleParameter baseZoom = new DoubleParameter("Base zoom factor",
      "Base zoom factor, some structure visualizers will add another zoom factor, default: %.1f".formatted(
          Structure2DRenderConfig.DEFAUlT_ZOOM), new DecimalFormat("0.00"),
      Structure2DRenderConfig.DEFAUlT_ZOOM, 0.01, null);


  public StructureRenderParameters() {
    super(mode, bondLength, baseZoom);
  }

  /**
   * @return the render config with the base zoom
   */
  public Structure2DRenderConfig createConfig() {
    final Sizing sizing = this.getValue(mode);
    final double bonds = this.getValue(bondLength);
    final double zoom = this.getValue(baseZoom);
    return new Structure2DRenderConfig(sizing, zoom, bonds);
  }

  @Override
  public void restoreDefaults() {
    setParameter(mode, Sizing.getDefault());
    setParameter(bondLength, Structure2DRenderConfig.DEFAULT_BOND_LENGTH);
    setParameter(baseZoom, Structure2DRenderConfig.DEFAUlT_ZOOM);
  }

}
