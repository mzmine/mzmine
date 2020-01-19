package io.github.mzmine.gui.chartbasics.chartthemes;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.FontParameter;
import io.github.mzmine.parameters.parametertypes.FontSpecs;
import io.github.mzmine.parameters.parametertypes.FontSpecsComponent;
import javafx.scene.text.Font;

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
