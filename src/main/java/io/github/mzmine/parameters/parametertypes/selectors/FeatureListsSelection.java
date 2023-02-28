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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureListsSelection implements Cloneable {
  private static final Logger logger = Logger.getLogger(FeatureListsSelection.class.getName());

  private FeatureListsSelectionType selectionType = FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS;
  private ModularFeatureList[] specificFeatureLists;
  private String namePattern;
  private ModularFeatureList[] batchLastFeatureLists;
  private FeatureListsPlaceholder[] evaluatedSelection = null;

  /**
   * Uses specific feature lists
   * @param flists specific feature lists
   */
  public FeatureListsSelection(ModularFeatureList... flists) {
    this();
    specificFeatureLists = flists;
    selectionType = FeatureListsSelectionType.SPECIFIC_FEATURELISTS;
  }
  public FeatureListsSelection(FeatureListsSelectionType selectionType) {
    this();
    this.selectionType = selectionType;
  }
  public FeatureListsSelection() {
  }

  public FeatureListsPlaceholder[] getEvaluationResult() {
    if (evaluatedSelection != null) {
      return Arrays.copyOf(evaluatedSelection, evaluatedSelection.length);
    }
    throw new IllegalStateException("Feature list selection has not been evaluated.");
  }
  public ModularFeatureList[] getMatchingFeatureLists() {
    if (evaluatedSelection != null) {
      var value = Arrays.stream(evaluatedSelection).map(FeatureListsPlaceholder::getMatchingFeatureList)
          .toArray(ModularFeatureList[]::new);
      for (var raw : value) {
        if (raw == null) {
          throw new IllegalStateException(
              "Feature list selection points to a missing file (maybe it was removed after first evaluation).");
        }
      }
      return value;
    }

    ModularFeatureList[] matchingFeatureLists;
    switch (selectionType) {

      case GUI_SELECTED_FEATURELISTS ->
          matchingFeatureLists = Stream.of(MZmineCore.getDesktop().getSelectedPeakLists())
            .map(ModularFeatureList.class::cast).toArray(ModularFeatureList[]::new);
      case ALL_FEATURELISTS ->
          matchingFeatureLists = MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists()
            .toArray(ModularFeatureList[]::new);
      case SPECIFIC_FEATURELISTS ->
          matchingFeatureLists = (specificFeatureLists == null) ? new ModularFeatureList[0] : specificFeatureLists;
      case NAME_PATTERN -> {
        if (Strings.isNullOrEmpty(namePattern)) {
          matchingFeatureLists = new ModularFeatureList[0];
        } else {
          ArrayList<ModularFeatureList> featureLists = new ArrayList<>();
          ModularFeatureList allFeatureLists[] = MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists().toArray(ModularFeatureList[]::new);

          for (ModularFeatureList pl : allFeatureLists) {

            final String plName = pl.getName();

            final String regex = TextUtils.createRegexFromWildcards(namePattern);

            if (plName.matches(regex)) {
              if (featureLists.contains(pl)) {
                continue;
              }
              featureLists.add(pl);
            }
          }
          matchingFeatureLists = featureLists.toArray(new ModularFeatureList[0]);
        }
      }
      case BATCH_LAST_FEATURELISTS ->
        matchingFeatureLists =  (batchLastFeatureLists == null) ?new ModularFeatureList[0] : batchLastFeatureLists;
      default -> throw new IllegalStateException("Unexpected value: " + selectionType);
    }
    evaluatedSelection = new FeatureListsPlaceholder[matchingFeatureLists.length];
    for (int i = 0; i < matchingFeatureLists.length; i++) {
      ModularFeatureList modularFeatureList = matchingFeatureLists[i];
      evaluatedSelection[i] = new FeatureListsPlaceholder(modularFeatureList);
    }
    logger.finest(
        () -> "Setting file selection. Evaluated files: " + Arrays.toString(evaluatedSelection));

    return matchingFeatureLists;

  }

  public FeatureListsSelectionType getSelectionType() {
    return selectionType;
  }

  public void setSelectionType(FeatureListsSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public FeatureList[] getSpecificFeatureLists() {
    return specificFeatureLists;
  }

  public void setSpecificFeatureLists(FeatureList[] specificFeatureLists) {
    this.specificFeatureLists = toModular(specificFeatureLists);
  }

  public String getNamePattern() {
    return namePattern;
  }

  public void setNamePattern(String namePattern) {
    this.namePattern = namePattern;
  }

  public void setBatchLastFeatureLists(FeatureList[] batchLastFeatureLists) {
    this.batchLastFeatureLists = toModular(batchLastFeatureLists);
  }

  public ModularFeatureList[] toModular(FeatureList[] flist) {
    return Stream.of(flist).map(ModularFeatureList.class::cast).toArray(ModularFeatureList[]::new);
  }

  public FeatureListsSelection clone() {
    FeatureListsSelection newSelection = new FeatureListsSelection();
    newSelection.selectionType = selectionType;
    newSelection.specificFeatureLists = specificFeatureLists;
    newSelection.namePattern = namePattern;
    return newSelection;
  }

  public String toString() {
    StringBuilder str = new StringBuilder();
    FeatureList pls[] = getMatchingFeatureLists();
    for (int i = 0; i < pls.length; i++) {
      if (i > 0)
        str.append("\n");
      str.append(pls[i].getName());
    }
    return str.toString();
  }
}
