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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.util.Map;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.Nullable;

public abstract class GeneralResolverParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX = new StringParameter("Suffix",
      "This string is added to feature list name as suffix", "resolved");

  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public static final OptionalModuleParameter<GroupMS2SubParameters> groupMS2Parameters = new OptionalModuleParameter<>(
      "MS/MS scan pairing", "Set MS/MS scan pairing parameters.", new GroupMS2SubParameters(),
      true);

  public static final ComboParameter<ResolvingDimension> dimension = new ComboParameter<>(
      "Dimension", "Select the dimension to be resolved.",
      FXCollections.observableArrayList(ResolvingDimension.values()),
      ResolvingDimension.RETENTION_TIME);

  public static final IntegerParameter MIN_NUMBER_OF_DATAPOINTS = new IntegerParameter(
      "Minimum scans (data points)", "Minimum number of data points on a feature", 3, true);

  public static final BooleanParameter CLASSIFY_FEATURES = new BooleanParameter("Classify feature shapes",
      "Employs a machine learning model to distinguish feature shapes.", false);

  public GeneralResolverParameters(
      Parameter[] parameters) {
    this(parameters, null);
  }

  public GeneralResolverParameters(Parameter[] parameters, String url) {
    super(parameters, url);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading
    // will work fine
    nameParameterMap.put("Min # of data points", MIN_NUMBER_OF_DATAPOINTS);
    return nameParameterMap;
  }

  @Nullable
  public abstract Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist);

  @Override
  public int getVersion() {
    return 2;
  }
}
