/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Pair;

/**
 * This class contains methods for MS/MS lipid identifications
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class MSMSLipidTools {

  /**
   * Calculate the explained intensity of MS/MS signals by lipid fragmentation rules in %
   */
  public Double calculateMsMsScore(DataPoint[] massList, Set<LipidFragment> annotatedFragments,
      Double precursor, MZTolerance mzTolRangeMSMS) {
    Double intensityAllSignals = null;
    boolean includePrecursor = annotatedFragments.stream().anyMatch(
        lipidFragment -> lipidFragment.getRuleType().equals(LipidFragmentationRuleType.PRECURSOR));
    if (includePrecursor) {
      intensityAllSignals = Arrays.stream(massList).mapToDouble(DataPoint::getIntensity).sum();
    } else {
      intensityAllSignals = Arrays.stream(massList)
          .filter(dp -> !mzTolRangeMSMS.checkWithinTolerance(dp.getMZ(), precursor))
          .mapToDouble(DataPoint::getIntensity).sum();
    }
    Double intensityMatchedSignals = annotatedFragments.stream().map(LipidFragment::getDataPoint)
        .mapToDouble(DataPoint::getIntensity).sum();
    return (intensityMatchedSignals / intensityAllSignals);
  }

  public static Pair<Integer, Integer> getCarbonandDBEFromLipidAnnotaitonString(
      String lipidAnnotation) {
    int numberOfCarbons = 0;
    int doubleBondEquivalents = 0;

    // Define the regular expression pattern
    Pattern pattern = Pattern.compile("(\\d+):(\\d+)");
    Matcher matcher = pattern.matcher(lipidAnnotation);

    while (matcher.find()) {
      int carbonValue = Integer.parseInt(matcher.group(1));
      int doubleBondValue = Integer.parseInt(matcher.group(2));
      numberOfCarbons += carbonValue;
      doubleBondEquivalents += doubleBondValue;
    }

    return new Pair<>(numberOfCarbons, doubleBondEquivalents);
  }

}
