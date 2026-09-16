/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonEnvelopeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 * The single complete configuration of a detection run: elements, tolerance, charge range, the
 * optional gates, the optional FWHM refinement and the envelope model parameters.
 * <p>
 * Simplified options (see {@link AutomaticIsotopeFinderModule}) expose a few of these and fill the
 * rest via {@link #setAll}, so there is only one algorithm implementation to maintain.
 */
public class CarbonModelAlgorithmParameters extends SimpleParameterSet {

  // single source of truth: used to build the parameters below AND by createDefault()/setAll(), so
  // callers never depend on the possibly-overwritten value of a shared static template
  public static final List<Element> DEFAULT_ELEMENTS = List.of(new Element("H"), new Element("C"),
      new Element("N"), new Element("O"), new Element("S"));
  public static final ElementDetectionMode DEFAULT_ELEMENT_DETECTION_MODE = ElementDetectionMode.USER_PLUS_AUTO;
  public static final MZTolerance DEFAULT_MZ_TOLERANCE = MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA;
  public static final int DEFAULT_MAX_CHARGE = 3;
  public static final boolean DEFAULT_REQUIRE_C13 = false;
  public static final boolean DEFAULT_EXPLAINABLE_SIGNALS_ONLY = false;
  public static final boolean DEFAULT_FWHM_REFINE = false;

  public static final ElementsParameter elements = new ElementsParameter("Chemical elements",
      "Chemical elements whose major stable isotopes are considered. Heavy isotopes (e.g. S, Cl, Br) "
          + "also widen the expected intensity bounds.");

  public static final ComboParameter<ElementDetectionMode> elementDetectionMode = new ComboParameter<>(
      "Element auto-detection",
      "Infers which heavy elements are present from the detected pattern and uses the inferred atom "
          + "counts to refine the plausible intensity bounds of the carbon model envelope. "
          + "User-defined + auto-detect (default) combines the chosen elements with the inferred "
          + "ones. User-defined elements only takes heavy-isotope bounds from the chosen elements "
          + "with a crude atom-count estimate. Auto-detect heavy elements infers popular heavy "
          + "elements (Cl, Br, S, Si) from the pattern and uses the detected atom counts.",
      ElementDetectionMode.values(), DEFAULT_ELEMENT_DETECTION_MODE);

  // decision: what the simplified "automatic" option shares is this TEXT, not the parameter
  // instances. An instance carries its value and both option sets live in the configuration at once,
  // so sharing instances would make them share one value.
  public static final String MAX_CHARGE_NAME = "Maximum charge of isotope m/z";
  public static final String MAX_CHARGE_DESCRIPTION =
      "Maximum possible charge of the isotope distribution. Charges 1..maxCharge are evaluated and "
          + "the most probable charge is selected; other highly probable charges are flagged.";
  public static final String REQUIRE_C13_NAME = "Require 13C isotope peak";
  public static final String REQUIRE_C13_DESCRIPTION = """
      If enabled, a charge is only accepted when the signals form a gap-free ladder on the \
      charge-adjusted 13C grid through the detected pattern. Features without such a ladder are \
      skipped (useful to suppress noise / heavy-isotope-only artifacts).
      Note that this also TRUNCATES the reported pattern: it stops at the first missing 13C \
      position, even if further signals exist beyond the gap. Molecules whose pattern is \
      dominated by an intense +2 comb (Cl/Br/Cu) are allowed to use every second 13C position \
      instead.
      Additionally, when the base peak is the monoisotopic, its M+1/M relative intensity must \
      be roughly plausible for the carbon count the mass implies. The lower bound is \
      deliberately far below the carbon-model minimum so that heteroatom-rich, carbon-poor \
      molecules are not rejected; mid-envelope patterns without a visible monoisotopic (e.g. \
      proteins) are exempt from this ratio check.""";

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      ToleranceType.FEATURE_TO_SCAN, DEFAULT_MZ_TOLERANCE.getMzTolerance(),
      DEFAULT_MZ_TOLERANCE.getPpmTolerance());

  public static final IntegerParameter maxCharge = new IntegerParameter(MAX_CHARGE_NAME,
      MAX_CHARGE_DESCRIPTION, DEFAULT_MAX_CHARGE, true, 1, 1000);

  public static final BooleanParameter requireC13 = new BooleanParameter(REQUIRE_C13_NAME,
      REQUIRE_C13_DESCRIPTION, DEFAULT_REQUIRE_C13);

  public static final BooleanParameter explainableSignalsOnly = new BooleanParameter(
      "Only keep explainable signals", """
      If enabled, a detected signal is only reported when it sits on the charge-adjusted 13C grid \
      or its mass defect matches a combination of isotopes of the selected elements (37Cl, 81Br, \
      34S, 29/30Si, 15N, 2H, 18O, ...). Noise or a co-eluting compound's peak that happens to fall \
      at an offset the pattern reaches is then dropped instead of being reported as an isotope \
      signal. A signal is never dropped where nothing at its offset is explainable, nor when it is \
      the most intense signal at its offset.
      Off by default, because it is a trade rather than a strict win: measured over the synthetic \
      benchmark corpus it lowers the noise leak (0.0174 to 0.0162) but also pattern completeness \
      (recall 0.9931 to 0.9909, F1 0.9938 to 0.9927), since a blended fine-structure centroid can \
      land between two isotope defects. Charge detection is unaffected. Enable it when a clean \
      pattern matters more than completeness - e.g. before formula prediction, which otherwise has \
      to explain signals that are not isotopes.""", DEFAULT_EXPLAINABLE_SIGNALS_ONLY);

  public static final OptionalModuleParameter<FwhmRefineParameters> fwhmRefine = new OptionalModuleParameter<>(
      "Refine across FWHM scans",
      "Detect on the most intense scan, then refine relative intensities and recover fine structure "
          + "across the scans within the feature FWHM (instead of pre-merging the scans).",
      new FwhmRefineParameters(), DEFAULT_FWHM_REFINE);

  public static final ParameterSetParameter<CarbonEnvelopeParameters> envelope = new ParameterSetParameter<>(
      "Carbon model envelope",
      "Parameters of the predicted 13C envelope: the carbon count is estimated from the searched mass "
          + "and drives the expected relative intensities used to score charges and bound the pattern.",
      new CarbonEnvelopeParameters());

  public CarbonModelAlgorithmParameters() {
    super(new Parameter[]{elements, elementDetectionMode, isotopeMzTolerance, maxCharge, requireC13,
        explainableSignalsOnly, fwhmRefine, envelope});
  }

  /**
   * An independent parameter set with every value actively set to its default. Prefer it over the
   * plain constructor, which stores the shared static templates ({@link SimpleParameterSet} does not
   * clone) whose values may have been overwritten by a config load or the GUI.
   */
  public static @NotNull CarbonModelAlgorithmParameters createDefault() {
    final CarbonModelAlgorithmParameters params = (CarbonModelAlgorithmParameters) new CarbonModelAlgorithmParameters().cloneParameterSet();
    params.setAll(DEFAULT_ELEMENTS, DEFAULT_ELEMENT_DETECTION_MODE, DEFAULT_MZ_TOLERANCE,
        DEFAULT_MAX_CHARGE, DEFAULT_REQUIRE_C13, DEFAULT_EXPLAINABLE_SIGNALS_ONLY,
        DEFAULT_FWHM_REFINE, CarbonEnvelopeParameters.createDefault());
    return params;
  }

  /**
   * Actively set every value, so a simplified option can map its few parameters onto the full setup.
   * Only call this on a cloned instance - see {@link #createDefault()}.
   *
   * @param envelopeParameters the envelope model parameters; values are copied.
   */
  public void setAll(@NotNull final List<Element> elementsValue,
      @NotNull final ElementDetectionMode detectionMode, @NotNull final MZTolerance tolerance,
      final int maxChargeValue, final boolean requireC13Value, final boolean explainableOnly,
      final boolean refineAcrossFwhm, @NotNull final CarbonEnvelopeParameters envelopeParameters) {
    setParameter(elements, List.copyOf(elementsValue));
    setParameter(elementDetectionMode, detectionMode);
    setParameter(isotopeMzTolerance, tolerance);
    setParameter(maxCharge, maxChargeValue);
    setParameter(requireC13, requireC13Value);
    setParameter(explainableSignalsOnly, explainableOnly);
    getParameter(fwhmRefine).setValue(refineAcrossFwhm);
    getParameter(envelope).setValue(envelopeParameters);
  }
}
