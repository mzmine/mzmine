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

package io.github.mzmine.modules.visualization.molstructure;

import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.modules.visualization.molstructure.Structure2DRenderConfig.Sizing;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import java.text.DecimalFormat;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Uses {@link ParameterSetParameter} to allow additional parameters that are extra compared to the
 * ones defined for {@link Structure2DRenderConfig}
 */
public class StructureRenderParameters extends SimpleParameterSet {

  public static final ComboParameter<Structure2DRenderConfig.Sizing> mode = new ComboParameter<>(
      "Mode",
      "Draw structures either fitted to the pane size or with fixed bond length and label sizes (will be zoomed if too large for pane)",
      List.of(Sizing.FIXED_SIZES_IN_BOUNDS, Sizing.FIT_TO_SIZE), Sizing.FIXED_SIZES_IN_BOUNDS);

  public static final DoubleParameter bondLength = new DoubleParameter("Bond length",
      "Bond length, default: %.1f".formatted(Structure2DRenderConfig.DEFAULT_BOND_LENGTH),
      new DecimalFormat("0.00"), Structure2DRenderConfig.DEFAULT_BOND_LENGTH, 0.01, null);

  public static final DoubleParameter baseZoom = new DoubleParameter("Zoom factor",
      "Zoom factor to enlarge the structure, default: %.1f".formatted(
          Structure2DRenderConfig.DEFAUlT_ZOOM), new DecimalFormat("0.00"),
      Structure2DRenderConfig.DEFAUlT_ZOOM, 0.01, null);


  public StructureRenderParameters() {
    super(mode, bondLength, baseZoom);
  }

  /**
   *
   * @param config sets all values
   * @param clone  clones the parameterset if true
   * @return this or the clone with the new config
   */
  @NotNull
  public StructureRenderParameters setAll(@NotNull Structure2DRenderConfig config, boolean clone) {
    StructureRenderParameters params =
        clone ? (StructureRenderParameters) this.cloneParameterSet() : this;
    params.setParameter(mode, config.mode());
    params.setParameter(bondLength, config.bondLength());
    params.setParameter(baseZoom, config.zoom());
    return params;
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
  public @NotNull List<ModulePreset> createDefaultPresets() {
    // defaults that work in all of mzmine and in the reports
    // the reports are exported with different DPI than the screen
    // so there is a factor applied later in the ReportingTask to zoom in more
    return List.of( //
        new ModulePreset("mzmine (default)", StructureRenderModule.UNIQUE_ID,
            setAll(Structure2DRenderConfig.DEFAULT_CONFIG, true)), //
        new ModulePreset("mzmine (large molecules, shorter bonds)", StructureRenderModule.UNIQUE_ID,
            setAll(new Structure2DRenderConfig(1, 15), true)) //
    );
  }
}
