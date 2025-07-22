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
  public static final String cvChromatogramSIM = "MS:1001472";
  /**
   * Constant <code>cvChromatogramSIC="MS:1000627"</code>
   */
  public static final String cvChromatogramSIC = "MS:1000627";
  /**
   * Constant <code>cvChromatogramBPC="MS:1000628"</code>
   */
  public static final String cvChromatogramBPC = "MS:1000628";

  public static final String cvChromatogramElectromagneticRadiation = "MS:1000811";
  public static final String cvChromatogramAbsorption = "MS:1000812";
  public static final String cvChromatogramEmission = "MS:1000813";
  public static final String cvChromatogramIonCurrent = "MS:1000810";
  public static final String cvChromatogramPressure = "MS:1003019";
  public static final String cvChromatogramFlowRate = "MS:1003020";
  /**
   * Constant <code>cvActivationEnergy="MS:1000045"</code>
   */
  public static final String cvActivationEnergy = "MS:1000045";
  // Activation
  /// activation methods
  public static final String cvPercentCollisionEnergy = "MS:1000138";
  public static final String cvActivationEnergy2 = "MS:1000509";
  /**
   * Constant <code>cvActivationCID="MS:1000133"</code>
   */
  public static final String cvActivationCID = "MS:1000133";
  /// activation energies
  public static final String cvElectronCaptureDissociation = "MS:1000250";
  public static final String cvHighEnergyCID = "MS:1000422"; // HCD
  public static final String cvLowEnergyCID = "MS:1000433";

  /**
   * EAD electron activated dissociation
   * <pre>
   *               <activation>
   *                 <cvParam cvRef="MS" accession="MS:1003294" name="electron activated dissociation" value=""/>
   *                 <cvParam cvRef="MS" accession="MS:1003410" name="electron beam energy" value="10.0" unitCvRef="UO" unitAccession="UO:0000266" unitName="electronvolt"/>
   *                 <cvParam cvRef="MS" accession="MS:1000045" name="collision energy" value="12.0" unitCvRef="UO" unitAccession="UO:0000266" unitName="electronvolt"/>
   *               </activation>
   * </pre>
   */
  public static final String cvActivationModeEAD = "MS:1003294"; // the method
  public static final String cvElectronBeamEnergyEAD = "MS:1003410"; // the energy

  /**
   * Constant <code>cvIsolationWindowTarget="MS:1000827"</code>
   */
  public static final String cvIsolationWindowTarget = "MS:1000827";
  /**
   * Constant <code>cvIsolationWindowLowerOffset="MS:1000828"</code>
   */
  public static final String cvIsolationWindowLowerOffset = "MS:1000828";

  // Isolation
  /**
   * Constant <code>cvIsolationWindowUpperOffset="MS:1000829"</code>
   */
  public static final String cvIsolationWindowUpperOffset = "MS:1000829";
  /**
   * Constant <code>cvMzArray="MS:1000514"</code>
   */
  public static final String cvMzArray = "MS:1000514";
  /**
   * Constant <code>cvIntensityArray="MS:1000515"</code>
   */
  public static final String cvIntensityArray = "MS:1000515";

  // Data arrays
  /**
   * Constant <code>cvRetentionTimeArray="MS:1000595"</code>
   */
  public static final String cvRetentionTimeArray = "MS:1000595";
  public static final String cvWavelengthArray = "MS:1000617";
  /**
   * Constant <code>cvUVSpectrum="MS:1000804"</code>
   */
  public static final String cvUVSpectrum = "MS:1000804";
  // UV spectrum, actually "electromagnetic radiation spectrum"
  public static final String cvFluorescenceDetector = "MS:1002308";
  public static final String cvLowestObservedWavelength = "MS:1000619";
  public static final String cvhighestObservedWavelength = "MS:1000618";
  public static final String cvUnitsNanometer = "UO:0000018";
  /**
   * Constant <code>cvUnitsIntensity1="MS:1000131"</code>
   */
  public static final String cvUnitsIntensity1 = "MS:1000131";
  public static final String cvUnitsIntensity2 = "MS:1000814";

  public static final String cvUnitsGram = "UO:0000021";
  public static final String cvUnitsMilliliter = "UO:0000098";
  public static final String cvUnitsGramsPerLiter = "UO:0000175";
  public static final String cvUnitsPPM = "UO:0000169";
  public static final String cvUnitsTesla = "UO:0000228";
  public static final String cvUnitsMeter = "UO:0000008";
  public static final String cvUnitsHertz = "UO:0000106";
  public static final String cvUnitsAbsorbance = "UO:0000269";
  public static final String cvUnitsElectronVolt = "UO:0000266";
  public static final String cvUnitsVolt = "UO:0000218";
  public static final String cvUnitsVoltPerMeter = "UO:0000268";
  public static final String cvUnitsMicroLiterPerMinute = "UO:0000271";
  public static final String cvUnitsPascal = "UO:0000110";
  public static final String cvUnitsDegreeKelvin = "UO:0000012";
  public static final String cvUnitsDegreeCelsius = "UO:0000027";
  public static final String cvUnitsMicrometer = "UO:0000017";
  public static final String cvUnitsJoule = "UO:0000112";
  public static final String cvUnitsNanosecond = "UO:0000150";
  public static final String cvUnitsPercent = "UO:0000187";
  public static final String cvUnitsDegree = "UO:0000185";
  public static final String cvUnitsDalton = "UO:0000221";
  public static final String cvUnitsKiloDalton = "UO:0000222";
  public static final String cvUnitsPartsPerNotationUnit = "UO:0000166";
  public static final String cvUnitsCount = "UO:0000189";
  public static final String cvUnitsSquareAngstrom = "UO:0000324";
  public static final String cvUnitsAreaUnit = "UO:0000047";
  public static final String cvUnitsDimensionlessUnit = "UO:0000186";


  // Intensity array unit
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


  /**
   * CV values that are specific for a detector and can be used to derive the actual detector.
   */
  public enum DetectorCVs {
    UV_SPECTRUM(cvUVSpectrum), FLUORESCENCE(cvFluorescenceDetector);

    String accession;

    DetectorCVs(String accession) {
      this.accession = accession;
    }

    public String getAccession() {
      return accession;
    }
  }

}
