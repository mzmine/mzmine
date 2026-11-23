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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * m/z tolerance that is either estimated from the data ({@link AutoOrCustomOption#AUTO}) or defined
 * by the user ({@link AutoOrCustomOption#CUSTOM}). The module that uses this parameter performs the
 * estimation, see {@link MZToleranceOrAuto#tolerance()} for the stored value.
 * <p>
 * Replaces a plain {@link MZToleranceParameter}, whose saved values are still loaded as custom
 * tolerance, see {@link #loadValueFromXML(Element)}.
 */
public class MZToleranceOrAutoParameter extends
    ComboWithInputParameter<AutoOrCustomOption, MZToleranceOrAuto, MZToleranceParameter> {

  /**
   * @param embeddedParameter defines name, description and the default custom tolerance
   * @param defaultOption     the selected option by default
   */
  public MZToleranceOrAutoParameter(@NotNull final MZToleranceParameter embeddedParameter,
      @NotNull final AutoOrCustomOption defaultOption) {
    this(embeddedParameter, new MZToleranceOrAuto(defaultOption, embeddedParameter.getValue()));
  }

  public MZToleranceOrAutoParameter(@NotNull final MZToleranceParameter embeddedParameter,
      @NotNull final MZToleranceOrAuto defaultValue) {
    super(embeddedParameter, AutoOrCustomOption.values(), AutoOrCustomOption.CUSTOM, defaultValue);
  }

  @Override
  public MZToleranceOrAuto createValue(final AutoOrCustomOption option,
      final MZToleranceParameter embeddedParameter) {
    return new MZToleranceOrAuto(option, embeddedParameter.getValue());
  }

  @Override
  public MZToleranceOrAutoParameter cloneParameter() {
    final MZToleranceOrAutoParameter clone = new MZToleranceOrAutoParameter(
        embeddedParameter.cloneParameter(), value);
    clone.setValue(value);
    return clone;
  }

  /**
   * A plain {@link MZToleranceParameter} saved the tolerance without the selected attribute, e.g.,
   * the ADAP chromatogram builder. Such values are loaded as custom tolerance.
   */
  @Override
  public void loadValueFromXML(final Element xmlElement) {
    if (!xmlElement.getAttribute("selected").isEmpty()) {
      super.loadValueFromXML(xmlElement);
      return;
    }
    if (xmlElement.getElementsByTagName("absolutetolerance").getLength() == 0
        && xmlElement.getElementsByTagName("ppmtolerance").getLength() == 0) {
      // nothing saved, keep the current value
      return;
    }
    embeddedParameter.loadValueFromXML(xmlElement);
    final MZTolerance legacy = embeddedParameter.getValue();
    if (legacy != null) {
      setValue(MZToleranceOrAuto.custom(legacy));
    }
  }
}
