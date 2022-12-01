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

package io.github.mzmine.gui.chartbasics.chartthemes;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.FontParameter;
import io.github.mzmine.parameters.parametertypes.FontSpecs;
import io.github.mzmine.parameters.parametertypes.FontSpecsComponent;
import javafx.scene.text.Font;

/**
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ChartThemeParametersSetupDialog extends ParameterSetupDialog {

  FontParameter masterFontParameter;
  private String prevMasterFont;

  public ChartThemeParametersSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    masterFontParameter = parameterSet.getParameter(ChartThemeParameters.masterFont);
    FontSpecs masterFontSpec = masterFontParameter.getValue();
    if (masterFontSpec != null)
      prevMasterFont = masterFontSpec.getFont().getName();
    else
      prevMasterFont = "";
  }

  @Override
  public void parametersChanged() {
    super.parametersChanged();
    
    FontSpecs masterFontSpec = masterFontParameter.getValue();
    if (masterFontSpec != null) {
      // if master font changed, change the other fonts, too
      if (!masterFontSpec.getFont().getName().equals(prevMasterFont)) {
        prevMasterFont = masterFontSpec.getFont().getName();

        for (Parameter p : parameterSet.getParameters()) {
          if (p instanceof FontParameter && p != masterFontParameter) {
            FontSpecs fs = (FontSpecs) p.getValue();
            // keep previous size and color, just change the actual font type
            Font newFont = new Font(masterFontSpec.getFont().getName(), fs.getFont().getSize());
            
            // TODO: is just setValue or just setValueToComponent or both needed?
            p.setValue(new FontSpecs(fs.getColor(), newFont));
            ((FontParameter) p).setValueToComponent(
                (FontSpecsComponent) parametersAndComponents.get(p.getName()),
                (FontSpecs) p.getValue());
          }
        }
      }
    }
  }
  
  
}
