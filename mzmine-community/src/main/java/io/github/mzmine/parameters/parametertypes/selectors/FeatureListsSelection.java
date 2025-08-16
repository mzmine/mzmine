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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.TextUtils;
import io.github.mzmine.util.io.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class FeatureListsSelection implements Cloneable {

  private static final Logger logger = Logger.getLogger(FeatureListsSelection.class.getName());
  private FeatureListsSelectionType selectionType = FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS;
  private FeatureListPlaceholder[] specificFeatureLists;
  private String namePattern;
  private FeatureListPlaceholder[] batchLastFeatureLists;

  // the actual selection
  private volatile FeatureListPlaceholder[] evaluatedSelection = null;


  /**
   * Uses specific feature lists
   *
   * @param flists specific feature lists
   */
  public FeatureListsSelection(ModularFeatureList... flists) {
    this();
    specificFeatureLists = FeatureListPlaceholder.of(flists);
    selectionType = FeatureListsSelectionType.SPECIFIC_FEATURELISTS;
  }

  public FeatureListsSelection(FeatureListsSelectionType selectionType) {
    this();
    this.selectionType = selectionType;
  }

  public FeatureListsSelection() {
  }

  /**
   * The actually evaluated selection.
   *
   * @return placeholder as the actual feature lists may be already removed from project and memory
   */
  public FeatureListPlaceholder[] getEvaluatedSelection() {
    return Arrays.copyOf(evaluatedSelection, evaluatedSelection.length);
  }

  public void resetEvaluatedSelection() {
    if (evaluatedSelection != null) {
      logger.finest(
          () -> "Resetting featurelist selection. Previously evaluated files: " + Arrays.toString(
              evaluatedSelection));
    }
    evaluatedSelection = null;
  }


  @NotNull
  public ModularFeatureList[] getMatchingFeatureLists() {

    final ModularFeatureList[] matchingFlists = switch (selectionType) {

      case GUI_SELECTED_FEATURELISTS -> Stream.of(MZmineCore.getDesktop().getSelectedPeakLists())
          .map(ModularFeatureList.class::cast).toArray(ModularFeatureList[]::new);
      case ALL_FEATURELISTS ->
          ProjectService.getProjectManager().getCurrentProject().getCurrentFeatureLists()
              .toArray(ModularFeatureList[]::new);
      case SPECIFIC_FEATURELISTS -> {
        if (specificFeatureLists == null) {
          yield new ModularFeatureList[0];
        }
        yield FeatureListPlaceholder.getMatchingFeatureListFilterNull(specificFeatureLists);
      }
      case NAME_PATTERN -> {
        if (Strings.isNullOrEmpty(namePattern)) {
          yield new ModularFeatureList[0];
        }
        ArrayList<ModularFeatureList> matchingFeatureLists = new ArrayList<>();
        ModularFeatureList allFeatureLists[] = ProjectService.getProjectManager()
            .getCurrentProject().getCurrentFeatureLists().toArray(ModularFeatureList[]::new);

        for (ModularFeatureList pl : allFeatureLists) {

          final String plName = pl.getName();

          final String regex = TextUtils.createRegexFromWildcards(namePattern);

          if (plName.matches(regex)) {
            if (matchingFeatureLists.contains(pl)) {
              continue;
            }
            matchingFeatureLists.add(pl);
          }
        }
        yield matchingFeatureLists.toArray(new ModularFeatureList[0]);
      }
      case BATCH_LAST_FEATURELISTS -> {
        if (batchLastFeatureLists == null) {
          yield new ModularFeatureList[0];
        }
        yield FeatureListPlaceholder.getMatchingFeatureListFilterNull(batchLastFeatureLists);
      }
    };

    assert matchingFlists != null;

    evaluatedSelection = FeatureListPlaceholder.of(matchingFlists);
    return matchingFlists;
  }

  public FeatureListsSelectionType getSelectionType() {
    return selectionType;
  }

  public void setSelectionType(FeatureListsSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public FeatureList[] getSpecificFeatureLists() {
    return FeatureListPlaceholder.getMatchingFeatureListFilterNull(specificFeatureLists);
  }

  public void setSpecificFeatureLists(FeatureList[] specificFeatureLists) {
    this.specificFeatureLists = FeatureListPlaceholder.of(toModular(specificFeatureLists));
  }

  public String getNamePattern() {
    return namePattern;
  }

  public void setNamePattern(String namePattern) {
    this.namePattern = namePattern;
  }

  public void setBatchLastFeatureLists(FeatureList[] batchLastFeatureLists) {
    this.batchLastFeatureLists = FeatureListPlaceholder.of(toModular(batchLastFeatureLists));
  }

  public ModularFeatureList[] toModular(FeatureList[] flist) {
    return Stream.of(flist).map(ModularFeatureList.class::cast).toArray(ModularFeatureList[]::new);
  }

  public FeatureListsSelection clone() {
    return clone(true);
  }

  public @NotNull FeatureListsSelection clone(boolean keepSelection) {
    FeatureListsSelection newSelection = new FeatureListsSelection();
    newSelection.selectionType = selectionType;
    newSelection.namePattern = namePattern;
    // avoid memory leak use placeholder with weak references
    if (keepSelection) {
      newSelection.specificFeatureLists = specificFeatureLists;
      newSelection.batchLastFeatureLists = batchLastFeatureLists;
      newSelection.evaluatedSelection = evaluatedSelection;
    }
    return newSelection;
  }

  public String toString() {
    if (evaluatedSelection != null) {
      // write as json list for easier parsing
      return selectionType + ", " + JsonUtils.writeStringOrElse(
          Arrays.stream(evaluatedSelection).map(FeatureListPlaceholder::getName).toList(), "[]");
    }
    return selectionType + ", Evaluation not executed.";
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof FeatureListsSelection that)) {
      return false;
    }

    return selectionType == that.selectionType && Arrays.equals(specificFeatureLists,
        that.specificFeatureLists) && Objects.equals(namePattern, that.namePattern)
        && Arrays.equals(batchLastFeatureLists, that.batchLastFeatureLists) && Arrays.equals(
        evaluatedSelection, that.evaluatedSelection);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(selectionType);
    result = 31 * result + Arrays.hashCode(specificFeatureLists);
    result = 31 * result + Objects.hashCode(namePattern);
    result = 31 * result + Arrays.hashCode(batchLastFeatureLists);
    result = 31 * result + Arrays.hashCode(evaluatedSelection);
    return result;
  }
}
