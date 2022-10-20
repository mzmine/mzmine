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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.external;

import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.DriftTubeCCSCalibration;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class AgilentImsCalibrationReader {

  private static final Logger logger = Logger.getLogger(
      AgilentImsCalibrationReader.class.getName());

  private static final String AGILENT_ACQDATA = "AcqData";
  private static final String AGILENT_CALIBRATION_FILE = "OverrideImsCal.xml";

  private AgilentImsCalibrationReader() {}

  public static CCSCalibration readCalibrationFile(@NotNull final File file)
      throws RuntimeException {
    final File calFile = findCalibrationFilePath(file);

    if (calFile == null) {
      throw new IllegalArgumentException("Cannot find calibration file " + file.getAbsolutePath());
    }

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    DocumentBuilder dBuilder;
    Document calibration;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
      calibration = dBuilder.parse(calFile);
    } catch (ParserConfigurationException | IOException | SAXException e) {
      logger.log(Level.WARNING, "Cannot parse calibration file. %s".formatted(e.getMessage()), e);
      throw new IllegalArgumentException(
          "Cannot parse calibration file " + calFile.getAbsolutePath());
    }

    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();

    final String sampleName = calFile.getParentFile().getParentFile().getName();
    try {
      XPathExpression driftGasExpression = xpath.compile(
          "//OverrideImsCalibration/SingleFieldCcsCalibration/DriftGas");
      Element gasElement = (Element) driftGasExpression.evaluate(calibration, XPathConstants.NODE);
      logger.finest(() -> "Calibration: " + sampleName + " DriftGas: " + gasElement.getTextContent()
          + " mass=" + gasElement.getAttribute("mass"));
      if (!gasElement.getTextContent().equals("N2")) {
        throw new IllegalArgumentException(
            "CCS calibration is not supported for drift gases other than nitrogen.");
      }

      XPathExpression tfixExpresison = xpath.compile(
          "//OverrideImsCalibration/SingleFieldCcsCalibration/TFix");
      Element tfixElement = (Element) tfixExpresison.evaluate(calibration, XPathConstants.NODE);
      logger.finest(() -> "Calibration: " + sampleName + " tfix: " + tfixElement.getTextContent());
      final double tfix = Double.parseDouble(tfixElement.getTextContent());

      XPathExpression betaExpression = xpath.compile(
          "//OverrideImsCalibration/SingleFieldCcsCalibration/Beta");
      Element betaElement = (Element) betaExpression.evaluate(calibration, XPathConstants.NODE);
      logger.finest(() -> "Calibration: " + sampleName + " beta: " + betaElement.getTextContent());
      final double beta = Double.parseDouble(betaElement.getTextContent());

      return new DriftTubeCCSCalibration(beta, tfix, -1, -1);

    } catch (XPathExpressionException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    } throw new IllegalStateException("Cannot evaluate calibration file.");
  }

  /**
   * Finds the calibration file from a file path. The path can be the .d directory, the AcqData
   * directory or the OverrideImsCal.xml.
   *
   * @param file The initial file path.
   * @return The path to the OverrideImsCal.xml.
   */
  @Nullable
  private static File findCalibrationFilePath(@NotNull File file) {
    if (!file.exists()) {
      logger.warning(
          () -> "Cannot read calibration file. File does not exist. " + file.getAbsolutePath());
      return null;
    }

    final File calibrationFile;
    if (file.isDirectory()) {
      if (file.getName().endsWith(".d")) {
        calibrationFile = new File(
            file.getAbsolutePath() + File.separator + AGILENT_ACQDATA + File.separator
                + AGILENT_CALIBRATION_FILE);
        if (!calibrationFile.exists()) {
          logger.warning(() -> "Agilent raw " + file.getAbsolutePath() + " is not calibrated. File "
              + file.toPath().relativize(calibrationFile.toPath()) + " does not exist.");
          return null;
        }
      } else if (file.getName().endsWith(AGILENT_ACQDATA)) {
        calibrationFile = new File(
            file.getAbsolutePath() + File.separator + AGILENT_CALIBRATION_FILE);
        if (!calibrationFile.exists()) {
          logger.warning(() -> "Agilent raw " + file.getAbsolutePath() + " is not calibrated. File "
              + file.toPath().relativize(calibrationFile.toPath()) + " does not exist.");
          return null;
        }
      } else {
        logger.warning(() -> "Invalid calibration file path." + file.getAbsolutePath());
        return null;
      }
    } else if (file.isFile() && file.getName().equals(AGILENT_CALIBRATION_FILE)) {
      calibrationFile = file;
    } else {
      logger.warning(() -> "Invalid calibration file path." + file.getAbsolutePath());
      return null;
    }
    return calibrationFile;
  }
}
