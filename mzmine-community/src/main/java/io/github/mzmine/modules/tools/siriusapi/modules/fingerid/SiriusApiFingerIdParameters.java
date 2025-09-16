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

package io.github.mzmine.modules.tools.siriusapi.modules.fingerid;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.OptOutParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.FeatureUtils;
import java.util.List;
import java.util.Map;

public class SiriusApiFingerIdParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final OptionalParameter<StringParameter> rowIds = new OptionalParameter<>(
      new StringParameter("Row IDs",
          "The ids of the rows to run Sirius for. If not selected, the whole feature list will be processed."));

  public static final HiddenParameter<Map<String, Boolean>> countWarningOptOut = new HiddenParameter<>(
      new OptOutParameter("Feature count warning", ""));

  public SiriusApiFingerIdParameters() {
    super(flist, rowIds, countWarningOptOut);
  }

  public static SiriusApiFingerIdParameters of(List<? extends FeatureListRow> rows) {
    final String ids = FeatureUtils.rowsToIdString(rows);

    final ModularFeatureList featureList = (ModularFeatureList) rows.stream()
        .map(r -> r.getFeatureList()).findFirst().get();

    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(SiriusApiFingerIdModule.class).cloneParameterSet();

    parameters.setParameter(flist, new FeatureListsSelection(featureList));
    parameters.setParameter(rowIds, true, ids);
    return (SiriusApiFingerIdParameters) parameters;
  }

  public static SiriusApiFingerIdParameters of(ModularFeatureList flist) {
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(SiriusApiFingerIdModule.class).cloneParameterSet();

    parameters.setParameter(SiriusApiFingerIdParameters.flist, new FeatureListsSelection(flist));
    parameters.setParameter(rowIds, false, "");
    return (SiriusApiFingerIdParameters) parameters;
  }
}
