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
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.TwCCSCalibration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WatersImsCalibrationReader {

  private WatersImsCalibrationReader() {
  }

  private static final Logger logger = Logger.getLogger(WatersImsCalibrationReader.class.getName());

  public static final String WATERS_MOB_CAL = "mob_cal.csv";
  public static final String WATERS_EXTERN_INF = "_extern.inf";

  private static final Pattern coefficientPattern = Pattern.compile(
      "(\\*\\sCoefficient:\\s)(\\d+.\\d+)");
  private static final Pattern exponentPattern = Pattern.compile("(\\*\\sExponent:\\s)(\\d+.\\d+)");
  /*t0 pattern has been updated to accept negative t0 values*/
  private static final Pattern t0Pattern = Pattern.compile("(\\*\\st0:\\s)(-?\\d+(\\.\\d+)?)");
  private static final Pattern edcPattern = Pattern.compile(
      "(EDC Delay Coefficient)(\\s+)(\\d+.\\d+)");
  private static final Pattern edcLowPattern = Pattern.compile(
      "(Transfer.EDCCoefficientLow.Setting)(\\s+)(\\d+.\\d+)");
  private static final Pattern edcHighPattern = Pattern.compile(
      "(Transfer.EDCCoefficientHigh.Setting)(\\s+)(\\d+.\\d+)");

  public static CCSCalibration readCalibrationFile(@NotNull final File file)
      throws RuntimeException {
    final File calFile = findCalibrationFilePath(file);

    if (calFile == null) {
      throw new IllegalArgumentException("Cannot find calibration file " + file.getAbsolutePath());
    }

    String strCoefficient = null;
    String strExponent = null;
    String strT0 = null;
    try (FileReader reader = new FileReader(calFile)) {
      BufferedReader r = new BufferedReader(reader);

      String s;
      while ((s = r.readLine()) != null) {
        final Matcher coefficientMatcher = coefficientPattern.matcher(s);
        final Matcher exponentMatcher = exponentPattern.matcher(s);
        final Matcher t0Matcher = t0Pattern.matcher(s);
        if (coefficientMatcher.matches()) {
          strCoefficient = coefficientMatcher.group(2);
        } else if (exponentMatcher.matches()) {
          strExponent = exponentMatcher.group(2);
        } else if (t0Matcher.matches()) {
          strT0 = t0Matcher.group(2);
        }
        if (strCoefficient != null && strExponent != null && strT0 != null) {
          break;
        }
      }
      r.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw new IllegalArgumentException("Cannot read calibration file " + file.getAbsolutePath());
    }

    if (strCoefficient == null || strExponent == null || strT0 == null) {
      throw new IllegalArgumentException(String.format(
          "Calibration file did not contain expected parameters. (coeff = %s, exponent = %s, t0 = %s)",
          strCoefficient, strExponent, strT0));
    }
    final double coeff = Double.parseDouble(strCoefficient);
    final double exp = Double.parseDouble(strExponent);
    final double t0 = Double.parseDouble(strT0);

    final File externInf = new File(file.getParentFile(), WATERS_EXTERN_INF);

    if (!file.exists() || !file.canRead()) {
      throw new IllegalArgumentException("Cannot find or read extern.inf file.");
    }

    String strEdc = null;
    String strEdcLow = null;
    String strEdcHigh = null;
    try (FileReader reader = new FileReader(externInf)) {
      BufferedReader r = new BufferedReader(reader);
      String s;

      while ((s = r.readLine()) != null) {
        final Matcher matcher = edcPattern.matcher(s);
        if (matcher.matches()) {
          strEdc = matcher.group(3);
        }

        final Matcher matcherLow = edcLowPattern.matcher(s);
        if (matcherLow.matches()) {
          strEdcLow = matcherLow.group(3);
        }
        final Matcher matcherHigh = edcHighPattern.matcher(s);
        if (matcherHigh.matches()) {
          strEdcHigh = matcherHigh.group(3);
        }
      }

      r.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw new IllegalStateException("Error while reading extern.inf file.");
    }

    if (strEdc != null) {
      final double edc = Double.parseDouble(strEdc);
      return new TwCCSCalibration(coeff, exp, t0, edc);
    } else if (strEdcLow != null && strEdcHigh != null) {
      final double edcLow = Double.parseDouble(strEdcLow);
      final double edcHigh = Double.parseDouble(strEdcHigh);
      return new TwCCSCalibration(coeff, exp, t0, (edcLow + edcHigh) / 2d);
    } else {
      throw new IllegalStateException("Did not find value EDC Delay Coefficient in extern.inf");
    }
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
      if (file.getName().endsWith(".raw")) {
        calibrationFile = new File(file.getAbsolutePath() + File.separator + WATERS_MOB_CAL);
        if (!calibrationFile.exists()) {
          logger.warning(() -> "Waters raw " + file.getAbsolutePath() + " is not calibrated. File "
              + file.toPath().relativize(calibrationFile.toPath()) + " does not exist.");
          return null;
        }
      } else {
        logger.warning(() -> "Invalid folder path." + file.getAbsolutePath());
        return null;
      }
    } else if (file.isFile() && file.getName().equals(WATERS_MOB_CAL)) {
      calibrationFile = file;
    } else {
      logger.warning(() -> "Invalid calibration file path." + file.getAbsolutePath());
      return null;
    }
    return calibrationFile;
  }
}
