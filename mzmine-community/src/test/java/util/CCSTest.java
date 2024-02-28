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

package util;

import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.DriftTubeCCSCalibration;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.TwCCSCalibration;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.external.AgilentImsCalibrationReader;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.external.WatersImsCalibrationReader;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.CCSCalibrant;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CCSTest {

  private static Logger logger = Logger.getLogger(CCSTest.class.getName());

  @Disabled("Needs test file?")
  @Test
  void testTims() {
    final TDFUtils tdfUtils = new TDFUtils();
    logger.info("CCS: " + tdfUtils.calculateCCS(1 / 1.376e-4, 1L, 322) + "");
    logger.info("CCS: " + CCSUtils.calcCCSFromTimsMobility(0.882, 2, 601.97) + "");

  }

  @Test
  void testSingleFieldCalibration() {
    // numbers taken from:
    //https://www.agilent.com/cs/library/usermanuals/public/D0003136_IM-Lipidomics_Workflow.pdf page 24
    CCSCalibrant mz622 = new CCSCalibrant(622d, 27.37f, 622.0290, 27.37f, 202.86f, 1);
    CCSCalibrant mz922 = new CCSCalibrant(922d, 33.08f, 922.0098, 33.08f, 243.91f, 1);
//    CCSCalibrant mz1022 = new CCSCalibrant(1022d, 38.41f, 1022d, 38.41f, ???, 1);
    CCSCalibrant mz1222 = new CCSCalibrant(1222d, 38.41f, 1221.9906, 38.41f, 283.11f, 1);
    CCSCalibrant mz1522 = new CCSCalibrant(1522d, 43.26f, 1521.9715, 43.26f, 318.91f, 1);

    final SimpleRegression driftTimeMzRegression = CCSUtils.getDriftTimeMzRegression(
        List.of(mz622, mz922, mz1222, mz1522));

    assert driftTimeMzRegression != null;

    logger.info("Slope: " + driftTimeMzRegression.getSlope() + "\tIntercept: "
        + driftTimeMzRegression.getIntercept() + "\tR²: " + driftTimeMzRegression.getRSquare()
        + "\tPoints: " + driftTimeMzRegression.getN());

    CCSCalibration dtimsSfCal = new DriftTubeCCSCalibration(driftTimeMzRegression);

    logger.info(dtimsSfCal.toString());
    logger.info(String.valueOf(dtimsSfCal.getCCS(1221.9901, 1, 38.41f)));
    Assertions.assertTrue(Math.abs(283.11f - dtimsSfCal.getCCS(1221.9906, 1, 38.41f)) < 20f);
  }

  @Test
  void testTIMSCalibration() {
    // numbers taken from:
    //https://www.agilent.com/cs/library/usermanuals/public/D0003136_IM-Lipidomics_Workflow.pdf page 24
    CCSCalibrant mz322 = new CCSCalibrant(622d, 0.7353f, 622.0290, 0.7363f, 153.73f, 1);
    CCSCalibrant mz622 = new CCSCalibrant(622d, 0.9937f, 622.0290, 0.9915f, 202.86f, 1);
    CCSCalibrant mz922 = new CCSCalibrant(922d, 1.1956f, 922.0098, 1.1986f, 243.91f, 1);

    final SimpleRegression timsRegression = CCSUtils.getDriftTimeMzRegression(
        List.of(mz322, mz622, mz922));
    logger.info(
        "Slope: " + timsRegression.getSlope() + "\tIntercept: " + timsRegression.getIntercept()
            + "\tR²: " + timsRegression.getRSquare() + "\tPoints: " + timsRegression.getN());

    CCSCalibration timsCalibration = new DriftTubeCCSCalibration(timsRegression);

    logger.info(timsCalibration.toString());
    logger.info(String.valueOf(timsCalibration.getCCS(1221.9901, 1, 1.3943f)));

    // looks like we can also use the linear calibration for DTIMS for TIMS measurements.
  }

  @Test
  void testAgilentCalReader() {
    String str = CCSTest.class.getClassLoader().getResource("ccscaltest/OverrideImsCal.xml")
        .getFile();

    final File calFile = new File(str);
    CCSCalibration cal = AgilentImsCalibrationReader.readCalibrationFile(calFile);
    Assertions.assertEquals(
        new DriftTubeCCSCalibration(0.146486318643186468476, -0.049872168413168131861, -1, -1),
        cal);
  }

  @Test
  void testWatersCalReader() {
    String str = CCSTest.class.getClassLoader().getResource("ccscaltest/mob_cal.csv").getFile();

    final File calFile = new File(str);
    CCSCalibration cal = WatersImsCalibrationReader.readCalibrationFile(calFile);
    Assertions.assertEquals(new TwCCSCalibration(208.037, 0.798364, 2, 1.3500), cal);

    final CCSCalibration cal2 = new TwCCSCalibration(460.428, 0.522466, 0.935198,
        (1.41 + 1.57) / 2d);

    Assertions.assertTrue(Math.abs(cal2.getCCS(607.2707, 1, 7.128f) - 265.2) < 5f);
    Assertions.assertTrue(Math.abs(cal2.getCCS(378.2056, 1, 3.432f) - 192.5) < 5f);
    Assertions.assertTrue(Math.abs(cal2.getCCS(150.0576, 1, 0.924f) - 131.5) < 5f);
  }

  /*@Test
  void ook0calc() {
    TDFUtils tdf = new TDFUtils();

    System.out.println("pos");

    System.out.println(tdf.calculateOok0(121.3, 1, 118.086255));
    System.out.println(tdf.calculateOok0(153.73, 1, 322.048123));
    System.out.println(tdf.calculateOok0(202.96, 1, 622.028961));
    System.out.println(tdf.calculateOok0(243.64, 1, 922.009799));
    System.out.println(tdf.calculateOok0(282.2, 1, 1221.990637));
    System.out.println(tdf.calculateOok0(316.96, 1, 1521.971475));
    System.out.println(tdf.calculateOok0(351.25, 1, 1821.952313));
    System.out.println(tdf.calculateOok0(383.03, 1, 2121.93315));
    System.out.println(tdf.calculateOok0(412.96, 1, 2421.913988));
    System.out.println(tdf.calculateOok0(441.21, 1, 2721.894826));

    System.out.println("negative");

    System.out.println(tdf.calculateOok0(140.04, 1, 301.998139));
    System.out.println(tdf.calculateOok0(180.77, 1, 601.978977));
    System.out.println(tdf.calculateOok0(255.34, 1, 1033.98811));
    System.out.println(tdf.calculateOok0(284.76, 1, 1333.968947));
    System.out.println(tdf.calculateOok0(319.03, 1, 1633.949785));
    System.out.println(tdf.calculateOok0(352.55, 1, 1933.930623));
    System.out.println(tdf.calculateOok0(380.74, 1, 2233.911461));
    System.out.println(tdf.calculateOok0(412.99, 1, 2533.892299));
    System.out.println(tdf.calculateOok0(432.62, 1, 2833.873137));
  }*/
}
