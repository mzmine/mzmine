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

package io.github.mzmine.modules.tools.siriusapi.modules.export;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.FeatureUtils;
import java.util.List;

public class SiriusApiExportRowsParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final OptionalParameter<StringParameter> rowIds = new OptionalParameter<>(
      new StringParameter("Row IDs", """
          If selected, only specific features will be exported. Otherwise, the complete feature list is exported.
          The ids of the rows to run CSI:FingerID for.
          Specify row IDs as ranges or lists: 1,5-8,10"""),
      false);

  public SiriusApiExportRowsParameters() {
    super(flist, rowIds);
  }

  public static SiriusApiExportRowsParameters of(List<? extends FeatureListRow> rows) {
    final String ids = FeatureUtils.rowsToIdString(rows);

    final ModularFeatureList featureList = (ModularFeatureList) rows.stream()
        .map(r -> r.getFeatureList()).findFirst().get();

    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(SiriusApiExportRowsModule.class).cloneParameterSet();

    parameters.setParameter(flist, new FeatureListsSelection(featureList));
    parameters.setParameter(rowIds, true, ids);
    return (SiriusApiExportRowsParameters) parameters;
  }

  public static SiriusApiExportRowsParameters of(ModularFeatureList flist) {
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(SiriusApiExportRowsModule.class).cloneParameterSet();

    parameters.setParameter(SiriusApiExportRowsParameters.flist, new FeatureListsSelection(flist));
    parameters.setParameter(rowIds, false, "");
    return (SiriusApiExportRowsParameters) parameters;
  }

}
