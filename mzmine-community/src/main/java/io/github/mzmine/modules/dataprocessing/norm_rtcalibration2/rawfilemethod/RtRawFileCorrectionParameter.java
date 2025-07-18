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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.rawfilemethod;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.AbstractRtCorrectionFunction;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.RawFileRtCorrectionModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.RtCorrectionFunctions;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RtRawFileCorrectionParameter implements
    UserParameter<List<AbstractRtCorrectionFunction>, Label> {

  private static final Logger logger = Logger.getLogger(
      RtRawFileCorrectionParameter.class.getName());
  private static final String rtCorrectionFunctionElement = "rtCorrectionFunction";
  private static final String rtCorrectionModuleAttribute = "correctionModule";

  @NotNull
  private final List<AbstractRtCorrectionFunction> calibrationFunctions = new ArrayList<>();

  @Override
  public String getDescription() {
    return "Stores RT calibration functions";
  }

  @Override
  public Label createEditingComponent() {
    return FxLabels.newLabel("This module is only used for project save/load");
  }

  @Override
  public void setValueFromComponent(Label label) {
    // empty
  }

  @Override
  public void setValueToComponent(Label label,
      @Nullable List<AbstractRtCorrectionFunction> newValue) {
    // empty
  }

  @Override
  public UserParameter<List<AbstractRtCorrectionFunction>, Label> cloneParameter() {
    final RtRawFileCorrectionParameter rtRawFileCorrectionParameter = new RtRawFileCorrectionParameter();
    rtRawFileCorrectionParameter.setValue(List.copyOf(calibrationFunctions));
    return rtRawFileCorrectionParameter;
  }


  @Override
  public String getName() {
    return "Rt correction functions";
  }

  @Override
  @NotNull
  public List<AbstractRtCorrectionFunction> getValue() {
    return calibrationFunctions;
  }

  @Override
  public void setValue(@Nullable List<AbstractRtCorrectionFunction> newValue) {
    calibrationFunctions.clear();
    if (newValue != null) {
      calibrationFunctions.addAll(newValue);
    }
  }


  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return calibrationFunctions != null;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    this.calibrationFunctions.clear();

    final NodeList correctionFunctionsList = xmlElement.getElementsByTagName(
        rtCorrectionFunctionElement);
    for (int i = 0; i < correctionFunctionsList.getLength(); i++) {
      final Element correctionFunctionElement = (Element) correctionFunctionsList.item(i);

      final String correctionModuleId = correctionFunctionElement.getAttribute(
          rtCorrectionModuleAttribute);
      final String fileName = correctionFunctionElement.getAttribute(CONST.XML_RAW_FILE_ELEMENT);
      final RawDataFilePlaceholder file = new RawDataFilePlaceholder(fileName, null, null);

      final RtCorrectionFunctions corrFunction = ModuleOptionsEnum.parse(
          RtCorrectionFunctions.class, correctionModuleId).orElseThrow(
          () -> new IllegalArgumentException(
              "Invalid id for RT correction function " + correctionModuleId + ". Cannot par"));

      final RawFileRtCorrectionModule corrFunctionModule = corrFunction.getModuleInstance();

      try {
        final AbstractRtCorrectionFunction corrFunctionInstance = corrFunctionModule.loadFromXML(
            correctionFunctionElement, file); calibrationFunctions.add(corrFunctionInstance);
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, "Error while loading RT correction function for file " + fileName,
            e);
      }

    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {

    final Document doc = xmlElement.getOwnerDocument();
    for (AbstractRtCorrectionFunction func : calibrationFunctions) {
      final Element correctionFunctionElement = doc.createElement(rtCorrectionFunctionElement);

      final RawDataFile file = func.getRawDataFile();
      if (file == null) {
        continue; // file not loaded anymore
      }

      correctionFunctionElement.setAttribute(CONST.XML_RAW_FILE_ELEMENT, file.getName());
      correctionFunctionElement.setAttribute(rtCorrectionModuleAttribute,
          func.getRtCalibrationModule().getUniqueID());
      func.saveToXML(correctionFunctionElement);

      xmlElement.appendChild(correctionFunctionElement);
    }
  }
}
