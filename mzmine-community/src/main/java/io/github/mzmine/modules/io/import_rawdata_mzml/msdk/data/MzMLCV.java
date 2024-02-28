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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import io.github.msdk.io.mzml.data.MzMLCVParam;

/**
 * Controlled vocabulary (CV) values for mzML files.
 *
 * @see <a href=
 * "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo">
 * Official CV specification</a>
 */
public class MzMLCV {

  // Scan start time
  /**
   * Constant <code>MS_RT_SCAN_START="MS:1000016"</code>
   */
  public static final String MS_RT_SCAN_START = "MS:1000016"; // "scan start time"
  /**
   * Constant <code>MS_RT_RETENTION_TIME="MS:1000894"</code>
   */
  public static final String MS_RT_RETENTION_TIME = "MS:1000894"; // "retention time"
  /**
   * Constant <code>MS_RT_RETENTION_TIME_LOCAL="MS:1000895"</code>
   */
  public static final String MS_RT_RETENTION_TIME_LOCAL = "MS:1000895"; // "local retention time"
  /**
   * Constant <code>MS_RT_RETENTION_TIME_NORMALIZED="MS:1000896"</code>
   */
  public static final String MS_RT_RETENTION_TIME_NORMALIZED = "MS:1000896"; // "normalized
  // retention time"

  // MS level
  /**
   * Constant <code>cvMSLevel="MS:1000511"</code>
   */
  public static final String cvMSLevel = "MS:1000511";
  /**
   * Constant <code>cvMS1Spectrum="MS:1000579"</code>
   */
  public static final String cvMS1Spectrum = "MS:1000579";

  // m/z and charge state
  /**
   * Constant <code>cvMz="MS:1000040"</code>
   */
  public static final String cvMz = "MS:1000040";
  /**
   * Constant <code>cvChargeState="MS:1000041"</code>
   */
  public static final String cvChargeState = "MS:1000041";

  // Minutes unit. MS:1000038 is used in mzML 1.0, while UO:000003 is used in
  // mzML 1.1.0
  /**
   * Constant <code>cvUnitsMin1="MS:1000038"</code>
   */
  public static final String cvUnitsMin1 = "MS:1000038";
  /**
   * Constant <code>cvUnitsMin2="UO:0000031"</code>
   */
  public static final String cvUnitsMin2 = "UO:0000031";
  /**
   * Constant <code>cvUnitsSec="UO:0000010"</code>
   */
  public static final String cvUnitsSec = "UO:0000010";

  // Scan filter string
  /**
   * Constant <code>cvScanFilterString="MS:1000512"</code>
   */
  public static final String cvScanFilterString = "MS:1000512";

  /**
   * Constant <code>cvIonInjectTime="MS:1000927"</code>
   */
  public static final String cvIonInjectTime = "MS:1000927";

  // Precursor m/z.
  /**
   * Constant <code>cvPrecursorMz="MS:1000744"</code>
   */
  public static final String cvPrecursorMz = "MS:1000744";

  // Polarity
  /**
   * Constant <code>cvPolarityPositive="MS:1000130"</code>
   */
  public static final String cvPolarityPositive = "MS:1000130";
  /**
   * Constant <code>cvPolarityNegative="MS:1000129"</code>
   */
  public static final String cvPolarityNegative = "MS:1000129";
  /**
   * Constant <code>polarityPositiveCvParam</code>
   */
  public static final MzMLCVParam polarityPositiveCvParam = new MzMLCVParam(cvPolarityPositive, "",
      "positive scan", null);
  /**
   * Constant <code>polarityNegativeCvParam</code>
   */
  public static final MzMLCVParam polarityNegativeCvParam = new MzMLCVParam(cvPolarityNegative, "",
      "negative scan", null);

  // Centroid vs profile
  /**
   * Constant <code>cvCentroidSpectrum="MS:1000127"</code>
   */
  public static final String cvCentroidSpectrum = "MS:1000127";
  /**
   * Constant <code>cvProfileSpectrum="MS:1000128"</code>
   */
  public static final String cvProfileSpectrum = "MS:1000128";
  /**
   * Constant <code>centroidCvParam</code>
   */
  public static final MzMLCVParam centroidCvParam = new MzMLCVParam(cvCentroidSpectrum, "",
      "centroid mass spectrum", null);
  /**
   * Constant <code>profileCvParam</code>
   */
  public static final MzMLCVParam profileCvParam = new MzMLCVParam(cvProfileSpectrum, "",
      "profile spectrum", null);

  // Total Ion Current
  /**
   * Constant <code>cvTIC="MS:1000285"</code>
   */
  public static final String cvTIC = "MS:1000285";

  // m/z range
  /**
   * Constant <code>cvLowestMz="MS:1000528"</code>
   */
  public static final String cvLowestMz = "MS:1000528";
  /**
   * Constant <code>cvHighestMz="MS:1000527"</code>
   */
  public static final String cvHighestMz = "MS:1000527";

  // Scan window range

  /**
   * Constant <code>cvScanWindowUpperLimit="MS:1000500"</code>
   */
  public static final String cvScanWindowUpperLimit = "MS:1000500";
  /**
   * Constant <code>cvScanWindowLowerLimit="MS:1000501"</code>
   */
  public static final String cvScanWindowLowerLimit = "MS:1000501";

  // Chromatograms
  /**
   * Constant <code>cvChromatogramTIC="MS:1000235"</code>
   */
  public static final String cvChromatogramTIC = "MS:1000235";
  /**
   * Constant <code>cvChromatogramMRM_SRM="MS:1001473"</code>
   */
  public static final String cvChromatogramMRM_SRM = "MS:1001473";
  /**
   * Constant <code>cvChromatogramSIC="MS:1000627"</code>
   */
  public static final String cvChromatogramSIC = "MS:1000627";
  /**
   * Constant <code>cvChromatogramBPC="MS:1000628"</code>
   */
  public static final String cvChromatogramBPC = "MS:1000628";

  // Activation
  /// activation methods
  /**
   * Constant <code>cvActivationEnergy="MS:1000045"</code>
   */
  public static final String cvActivationEnergy = "MS:1000045";
  public static final String cvPercentCollisionEnergy = "MS:1000138";
  public static final String cvActivationEnergy2 = "MS:1000509";
  /// activation energies
  /**
   * Constant <code>cvActivationCID="MS:1000133"</code>
   */
  public static final String cvActivationCID = "MS:1000133";
  public static final String cvElectronCaptureDissociation = "MS:1000250";
  public static final String cvHighEnergyCID = "MS:1000422";
  public static final String cvLowEnergyCID = "MS:1000433";

  // Isolation
  /**
   * Constant <code>cvIsolationWindowTarget="MS:1000827"</code>
   */
  public static final String cvIsolationWindowTarget = "MS:1000827";
  /**
   * Constant <code>cvIsolationWindowLowerOffset="MS:1000828"</code>
   */
  public static final String cvIsolationWindowLowerOffset = "MS:1000828";
  /**
   * Constant <code>cvIsolationWindowUpperOffset="MS:1000829"</code>
   */
  public static final String cvIsolationWindowUpperOffset = "MS:1000829";

  // Data arrays
  /**
   * Constant <code>cvMzArray="MS:1000514"</code>
   */
  public static final String cvMzArray = "MS:1000514";
  /**
   * Constant <code>cvIntensityArray="MS:1000515"</code>
   */
  public static final String cvIntensityArray = "MS:1000515";
  /**
   * Constant <code>cvRetentionTimeArray="MS:1000595"</code>
   */
  public static final String cvRetentionTimeArray = "MS:1000595";

  // UV spectrum, actually "electromagnetic radiation spectrum"
  /**
   * Constant <code>cvUVSpectrum="MS:1000804"</code>
   */
  public static final String cvUVSpectrum = "MS:1000804";

  // Intensity array unit
  /**
   * Constant <code>cvUnitsIntensity1="MS:1000131"</code>
   */
  public static final String cvUnitsIntensity1 = "MS:1000131";

  // Ion mobility
  // <cvParam cvRef="MS" accession="MS:1002476" name="ion mobility drift time" value="4.090608"
  // unitCvRef="UO" unitAccession="UO:0000028" unitName="millisecond"/>
  public static final String cvMobilityDriftTime = "MS:1002476";
  public static final String cvMobilityDriftTimeUnit = "UO:0000028";
  // <cvParam cvRef="MS" accession="MS:1002815" name="inverse reduced ion mobility"
  // value="1.572618927197" unitCvRef="MS" unitAccession="MS:1002814" unitName="volt-second per
  // square centimeter"/>
  public static final String cvMobilityInverseReduced = "MS:1002815";
  public static final String cvMobilityInverseReducedUnit = "MS:1002814";
}
