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

package io.github.mzmine.modules.dataprocessing.featdet_manualintegration;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

/**
 * Parameters describing a set of manual (re-)integrations produced by the integration dashboard.
 * The same parameter set is recorded in the applied method and (in a later phase) consumed by a
 * replay task, so provenance and replay share one definition.
 */
public class ManualIntegrationParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter(1, 1);

  public static final ManualIntegrationEntriesParameter entries = new ManualIntegrationEntriesParameter();

  public static final BooleanParameter applyPostProcessing = new BooleanParameter(
      "Apply latest smoothing step",
      "If enabled, the latest smoothing step of the feature list is re-applied to the extracted "
          + "chromatograms before integration, matching the dashboard's post-processing.", false);

  public static final MZToleranceParameter mzMatchTolerance = new MZToleranceParameter(
      "m/z match tolerance",
      "Tolerance used to match rows by m/z when a stored row id cannot be found (e.g. after "
          + "re-alignment).", 0.005, 10);

  public static final RTToleranceParameter rtMatchTolerance = new RTToleranceParameter(
      "RT match tolerance",
      "Tolerance used to match rows by retention time when a stored row id cannot be found.",
      new RTTolerance(0.1f, Unit.MINUTES));

  // identifies the integration dashboard session that produced these integrations, so a later commit
  // from the same session replaces its own record while a reopened dashboard adds a new one.
  public static final HiddenParameter<Integer> sessionId = new HiddenParameter<>(
      new IntegerParameter("Dashboard session id",
          "Internal id of the integration dashboard session that produced these integrations.",
          -1));

  public ManualIntegrationParameters() {
    super(flists, entries, applyPostProcessing, mzMatchTolerance, rtMatchTolerance, sessionId);
  }
}
