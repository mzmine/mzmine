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
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
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
    final FeatureListPreferences preferences = param.toPreferences();
    flist.setPreferences(preferences);
    // derived columns like the RSD are computed on demand, therefore refresh the visible cells
    FeatureTableFXUtil.updateCellsForFeatureList(flist);
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
