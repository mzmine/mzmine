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
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RtCalibrationFunction;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.ParsingUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RtRawFileCorrectionParameter implements
    UserParameter<List<RtCalibrationFunction>, Label> {

  private static final Logger logger = Logger.getLogger(
      RtRawFileCorrectionParameter.class.getName());

  @NotNull
  private final List<RtCalibrationFunction> calibrationFunctions = new ArrayList<>();

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
  public void setValueToComponent(Label label, @Nullable List<RtCalibrationFunction> newValue) {
    // empty
  }

  @Override
  public UserParameter<List<RtCalibrationFunction>, Label> cloneParameter() {
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
  public List<RtCalibrationFunction> getValue() {
    return calibrationFunctions;
  }

  @Override
  public void setValue(@Nullable List<RtCalibrationFunction> newValue) {
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

    final NodeList calibrationFunctionsList = xmlElement.getElementsByTagName(
        "calibrationFunction");
    for (int i = 0; i < calibrationFunctionsList.getLength(); i++) {
      final Element calibrationFunction = (Element) calibrationFunctionsList.item(i);
      final String fileName = calibrationFunction.getAttribute(CONST.XML_RAW_FILE_ELEMENT);
      final RawDataFile file = new RawDataFilePlaceholder(fileName, null, null);

      try {
        final PolynomialSplineFunction polynomialSplineFunction = ParsingUtils.loadSplineFunctionFromParentXmlElement(
            calibrationFunction);
        calibrationFunctions.add(new RtCalibrationFunction(file, polynomialSplineFunction));
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }

    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {

    final Document doc = xmlElement.getOwnerDocument();
    for (RtCalibrationFunction func : calibrationFunctions) {
      final Element calibrationFunctionElement = doc.createElement("calibrationFunction");
      final RawDataFile file = func.getRawDataFile();
      if (file == null) {
        continue; // file not loaded anymore
      }

      calibrationFunctionElement.setAttribute(CONST.XML_RAW_FILE_ELEMENT, file.getName());
      final Element spline = ParsingUtils.createSplineFunctionXmlElement(doc,
          func.getSplineFunction());
      calibrationFunctionElement.appendChild(spline);
      xmlElement.appendChild(calibrationFunctionElement);
    }
  }
}
