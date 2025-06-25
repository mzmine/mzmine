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

package io.github.mzmine.modules.dataprocessing.id_addmanualcomp;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedXMLParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.FeatureListUtils;
import java.util.List;

public class ManualCompoundAnnotationParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final StringParameter rowIds = new StringParameter("Row IDs",
      "The ids of the rows to be annotated.");

  public static final EmbeddedXMLParameter annotations = new EmbeddedXMLParameter("Annotation XML",
      "XML code of the annotations to load.");

  public ManualCompoundAnnotationParameters() {
    super(flist, rowIds, annotations);
  }

  public static ManualCompoundAnnotationParameters of(ModularFeatureList flist,
      ModularFeatureListRow row, List<FeatureAnnotation> annotations) {
    final String xmlString = FeatureAnnotation.toXMLString(annotations, flist, row);

    final ParameterSet parameterSet = new ManualCompoundAnnotationParameters().cloneParameterSet();
    parameterSet.setParameter(ManualCompoundAnnotationParameters.flist,
        new FeatureListsSelection(flist));
    parameterSet.setParameter(rowIds, FeatureListUtils.rowsToIdString(List.of(row)));
    parameterSet.setParameter(ManualCompoundAnnotationParameters.annotations, xmlString);
    return (ManualCompoundAnnotationParameters) parameterSet;
  }
}
