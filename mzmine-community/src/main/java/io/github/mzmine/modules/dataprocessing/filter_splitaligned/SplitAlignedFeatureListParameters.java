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

package io.github.mzmine.modules.dataprocessing.filter_splitaligned;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class SplitAlignedFeatureListParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final OptionalParameter<MetadataGroupingParameter> grouping = new OptionalParameter<>(
      new MetadataGroupingParameter("Split by metadata group",
          "If selected, files of the same metadata group will be split into the same feature list. Otherwise, each file will create an individual feature list."), false);

  public SplitAlignedFeatureListParameters() {
    super("https://mzmine.github.io/mzmine_documentation/module_docs/filter_splitaligned/split_aligned_feature_list.html", flists, grouping);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    final @NotNull ModularFeatureList[] selectedFlists = getValue(flists).getMatchingFeatureLists();

    if (selectedFlists.length > 0) {
      final String nonAlignedLists = Arrays.stream(selectedFlists)
          .filter(flist -> flist.getNumberOfRawDataFiles() <= 1).map(ModularFeatureList::getName)
          .collect(Collectors.joining(", "));
      if (!nonAlignedLists.isBlank()) {
        errorMessages.add(
            "Feature list(s) %s is/are not aligned feature lists.".formatted(nonAlignedLists));
        return false;
      }
    }

    return superCheck;
  }
}
