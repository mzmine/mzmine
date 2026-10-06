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

package io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.TagDataType;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableFilterMenu;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.BitSet;
import java.util.List;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListPreferencesTask extends AbstractFeatureListTask {

  private final @NotNull FeatureListPreferencesParameters param;
  private final @NotNull FeatureList flist;

  protected FeatureListPreferencesTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull FeatureListPreferencesParameters parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, @NotNull final FeatureList featureList) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.param = parameters;
    this.flist = featureList;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    // parameters on KEEP_AS_IS define no value, those preferences are taken from the feature list
    final FeatureListPreferences current = flist.getPreferences();
    final FeatureListPreferences preferences = param.toPreferences(current);
    if (preferences.equals(current)) {
      // nothing to apply, e.g. all parameters are on KEEP_AS_IS
      return;
    }

    final boolean rankingChanged = !current.getIonTypeRanking()
        .equals(preferences.getIonTypeRanking());
    final int currentTagCount = current.getTagLabels().size();
    final int newTagCount = preferences.getTagLabels().size();

    flist.setPreferences(preferences);

    if (newTagCount < currentTagCount) {
      trimTagValues(newTagCount);
    }

    if (rankingChanged) {
      // the ion identities of a row are stored best first, so a new ranking has to reorder them.
      // sortIonIdentities reads the ranking from the preferences that were just set
      IonNetworkLogic.sortIonIdentities(flist);
    }

    if (DesktopService.isGUI()) {
      // decision: a preference change refreshes every table showing this list and its tag filter.
      FxThread.runLater(() -> {
        for (final var table : FeatureTableFXUtil.getTablesFor(flist)) {
          if (table.getParent() instanceof BorderPane pane
              && pane.getBottom() instanceof FxFeatureTableFilterMenu filterMenu) {
            filterMenu.refreshTagLabels();
          }
          table.refresh();
        }
      });
    }
  }

  /**
   * Removes tag selections for labels that were removed from the feature-list preferences.
   */
  private void trimTagValues(final int tagCount) {
    final TagDataType tagType = DataTypes.get(TagDataType.class);
    for (final var row : flist.getRows()) {
      final BitSet tags = row.get(tagType);
      if (tags == null || tags.length() <= tagCount) {
        continue;
      }

      final BitSet trimmedTags = (BitSet) tags.clone();
      trimmedTags.clear(tagCount, trimmedTags.length());
      row.set(tagType, trimmedTags);
    }
  }

  @Override
  protected void addAppliedMethod() {
    // this module only redefines preferences and may be applied repeatedly. Avoid stacking up
    // redundant steps by dropping a trailing preferences step before the new one is added
    for (final FeatureList featureList : getProcessedFeatureLists()) {
      final List<FeatureListAppliedMethod> appliedMethods = featureList.getAppliedMethods();
      if (appliedMethods.isEmpty()) {
        continue;
      }
      final FeatureListAppliedMethod last = appliedMethods.getLast();
      if (last.getModule().getClass().equals(getModuleClass())) {
        appliedMethods.removeLast();
      }
    }
    super.addAppliedMethod();
  }

  @Override
  public String getTaskDescription() {
    return "Redefining preferences of feature list " + flist.getName();
  }
}
