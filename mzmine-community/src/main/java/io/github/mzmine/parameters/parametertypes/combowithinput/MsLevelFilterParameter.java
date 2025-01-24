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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import org.w3c.dom.Element;

public class MsLevelFilterParameter extends
    ComboWithInputParameter<Options, MsLevelFilter, IntegerParameter> {

  public MsLevelFilterParameter() {
    this(Options.values(), MsLevelFilter.ALL_LEVELS);
  }

  public MsLevelFilterParameter(Options[] options, MsLevelFilter defaultValue) {
    super(new IntegerParameter("MS level filter",
            "Only select MS of defined levels. MS1, MS2, MSn (all >1), or specific levels entered as number.",
            3, true, 1, 100000), //
        options, Options.SPECIFIC_LEVEL, defaultValue);
  }

  public MsLevelFilterParameter(String desc, Options[] options, MsLevelFilter defaultValue) {
    super(new IntegerParameter("MS level filter", desc, 3, true, 1, 100000), //
        options, Options.SPECIFIC_LEVEL, defaultValue);
  }

  @Override
  public MsLevelFilter createValue(final Options option, final IntegerParameter embeddedParameter) {
    return new MsLevelFilter(option, embeddedParameter.getValue());
  }


  @Override
  public MsLevelFilterParameter cloneParameter() {
    return new MsLevelFilterParameter(choices.toArray(Options[]::new), value);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    // used to be an IntegerParameter - also support a simple integer
    try {
      final String selected = xmlElement.getAttribute("selected");
      if (selected == null || selected.isBlank()) {
        final String numString = xmlElement.getTextContent();
        if (numString != null && !numString.isEmpty()) {
          int oldParameterValue = Integer.parseInt(numString);
          setValue(MsLevelFilter.of(oldParameterValue));
          return;
        }
      }
    } catch (Exception e) {
      // silent as this is only for compatibility to old parameter
    }

    // otherwise load the new parameter
    super.loadValueFromXML(xmlElement);
  }
}
