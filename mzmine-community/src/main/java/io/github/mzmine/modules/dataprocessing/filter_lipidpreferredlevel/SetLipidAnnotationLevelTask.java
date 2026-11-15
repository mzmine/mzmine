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

package io.github.mzmine.modules.dataprocessing.filter_lipidpreferredlevel;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.PreferredAnnotationType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetLipidAnnotationLevelTask extends AbstractFeatureListTask {

  @NotNull
  private final FeatureList flist;
  private final LipidAnnotationLevel preferredLevel;

  protected SetLipidAnnotationLevelTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, @NotNull FeatureList flist) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.flist = flist;
    totalItems = flist.getNumberOfRows();
    preferredLevel = parameters.getValue(SetLipidAnnotationLevelParameters.level);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    for (final FeatureListRow row : flist.getRows()) {
      final List<MatchedLipid> lipidMatches = row.getLipidMatches();
      if (!lipidMatches.isEmpty()) {
        final FeatureAnnotation preferredAnnotation = row.isUserPreferredAnnotation()
            ? row.getPreferredAnnotation() : null;
        MatchedLipid updatedPreferredLipid = null;
        final List<MatchedLipid> updatedMatches = new ArrayList<>(lipidMatches.size());
        for (final MatchedLipid match : lipidMatches) {
          final MatchedLipid updatedMatch = match.copyWithPreferredAnnotationLevel(preferredLevel);
          updatedMatches.add(updatedMatch);
          if (updatedPreferredLipid == null && matchesPreferredAnnotation(match,
              preferredAnnotation)) {
            updatedPreferredLipid = updatedMatch;
          }
        }
        row.setLipidAnnotations(updatedMatches);
        if (updatedPreferredLipid != null) {
          row.set(PreferredAnnotationType.class, updatedPreferredLipid);
        }
      }
      incrementFinishedItems();
    }
  }

  private static boolean matchesPreferredAnnotation(final @NotNull MatchedLipid candidate,
      final @Nullable FeatureAnnotation preferredAnnotation) {
    if (candidate == preferredAnnotation) {
      return true;
    }
    if (!(preferredAnnotation instanceof MatchedLipid preferredLipid)) {
      return false;
    }
    return Objects.equals(candidate.getLipidAnnotation().getAnnotation(),
        preferredLipid.getLipidAnnotation().getAnnotation())
        && Objects.equals(candidate.getAccurateMz(), preferredLipid.getAccurateMz())
        && Objects.equals(candidate.getIonizationType(), preferredLipid.getIonizationType())
        && Objects.equals(candidate.getMsMsScore(), preferredLipid.getMsMsScore());
  }

  @Override
  public String getTaskDescription() {
    return "Setting lipid annotation level for feature list %s".formatted(flist.getName());
  }
}
