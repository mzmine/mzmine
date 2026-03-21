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

import io.github.mzmine.util.presets.PresetStore;
import io.github.mzmine.util.presets.manage.SimplePresetEditor;

/**
 * This is the editor shown in the preset management tab. It uses the {@link RowTypeFilterComponent}
 * and adds an apply button to it.
 */
public class RowTypeFilterPresetEditor extends SimplePresetEditor<RowTypeFilterPreset> {

  private final RowTypeFilterComponent comp;

  public RowTypeFilterPresetEditor(PresetStore store) {
    super(store);

    comp = RowTypeFilterParameter.createDefaultEditingComponent(false);
    getChildren().addFirst(comp);

    comp.valueProperty().subscribe((nv) -> {
      setModifiedPreset(RowTypeFilterPreset.createPreset("placeholder", nv));
    });
  }

  @Override
  protected void setPresetInComponent(RowTypeFilterPreset originalPreset) {
    comp.setValue(originalPreset == null ? null : originalPreset.filter());
  }
}
