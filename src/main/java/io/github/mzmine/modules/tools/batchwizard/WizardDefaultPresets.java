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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ChromatographyDefaults;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.ImsDefaults;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.MsInstrumentDefaults;
import io.github.mzmine.modules.tools.batchwizard.WizardPreset.WizardPart;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WizardDefaultPresets {

  /**
   * Create list of presets for every {@link WizardPart}
   *
   * @return map of part and list of presets
   */
  public static Map<WizardPart, List<WizardPreset>> createPresets() {
    return Arrays.stream(WizardPart.values())
        .collect(Collectors.toMap(part -> part, WizardDefaultPresets::createPresets));
  }

  /**
   * @param part part of BatchWizard
   * @return list of presets for part
   */
  public static List<WizardPreset> createPresets(WizardPart part) {
    var parameters = MZmineCore.getConfiguration().getModuleParameters(BatchWizardModule.class)
        .cloneParameterSet();

    ParameterSetParameter parameterPart = part.getParameterSetParameter();
    ParameterSet partParameters = parameters.getParameter(parameterPart).getEmbeddedParameters();

    return switch (part) {
      case DATA_IMPORT -> List.of(new WizardPreset("Data", part, partParameters));
      case SAMPLE_INTRODUCTION_CHROMATOGRAPHY ->
          Arrays.stream(ChromatographyDefaults.values()).map(WizardPreset::new).toList();
      case IMS -> Arrays.stream(ImsDefaults.values()).map(WizardPreset::new).toList();
      case MS -> Arrays.stream(MsInstrumentDefaults.values()).map(WizardPreset::new).toList();
      case FILTER -> List.of(new WizardPreset("Filter", part, partParameters));
      case EXPORT -> List.of(new WizardPreset("Export", part, partParameters));
    };
  }

}
