/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import static io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode.CONTAINS;
import static io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode.EQUAL;
import static io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode.GREATER_EQUAL;
import static io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption.FORMULA;
import static io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption.LIPID;
import static io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption.SMARTS;
import static io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption.SMILES;
import static io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterPreset.createPreset;

import io.github.mzmine.util.presets.AbstractJsonPresetStore;
import io.github.mzmine.util.presets.KnownPresetGroup;
import io.github.mzmine.util.presets.PresetCategory;
import io.github.mzmine.util.presets.PresetGroup;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class RowTypeFilterPresetStore extends AbstractJsonPresetStore<RowTypeFilterPreset> {

  @Override
  public @NotNull PresetCategory getPresetCategory() {
    return PresetCategory.FILTERS;
  }

  @Override
  public @NotNull PresetGroup getPresetGroup() {
    return KnownPresetGroup.ROW_TYPE_FILTER_PRESET;
  }

  @Override
  public @NotNull List<RowTypeFilterPreset> createDefaults() {
    return List.of( //
        createPreset("Any lipid", LIPID, EQUAL, "C"), //
        createPreset("PC", LIPID, EQUAL, "PC"), //
        createPreset("Unsaturated lipids", LIPID, EQUAL, "C>0:>0"), //
        createPreset("Oxidized lipids", LIPID, EQUAL, "C>0:>=0;>0"), //
        createPreset("Halide (F, Cl, Br, I)", SMARTS, CONTAINS, "[#6][F,Cl,Br,I]"), //
        createPreset("Amino acid", SMARTS, CONTAINS, "[NX3,NX4+][CX4H]([*])[CX3](=[OX1])[O,N]"), //
        createPreset("Sulfate", SMARTS, CONTAINS,
            "[$([#16X4](=[OX1])(=[OX1])([OX2H,OX1H0-])[OX2][#6]),$([#16X4+2]([OX1-])([OX1-])([OX2H,OX1H0-])[OX2][#6])]"),
        createPreset("Sulfone", SMARTS, CONTAINS,
            "[$([#16X4](=[OX1])=[OX1]),$([#16X4+2]([OX1-])[OX1-])]"),
        //
        createPreset("Amide", SMILES, CONTAINS, "CNC=O"), //
        createPreset("Acid", SMILES, CONTAINS, "C(=O)OH"), //
        createPreset("OH (at least 3)", SMILES, CONTAINS, "OH.OH.OH"), //
        createPreset("PFAS CF2", SMARTS, CONTAINS, "C(F)(F)C(F)(F)C(F)(F)"), //
        createPreset("PFAS simple", FORMULA, GREATER_EQUAL, "F4"), //
        createPreset("High oxygen", FORMULA, GREATER_EQUAL, "O4")//
    );
  }


  @Override
  public RowTypeFilterPresetEditor createPresetEditor() {
    return new RowTypeFilterPresetEditor(this);
  }

}
