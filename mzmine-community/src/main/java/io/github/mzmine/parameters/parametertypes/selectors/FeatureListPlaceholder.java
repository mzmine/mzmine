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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.collections.CollectionUtils;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListPlaceholder {

  private final WeakReference<ModularFeatureList> featureList;
  private final String name;

  public FeatureListPlaceholder() {
    this(null);
  }

  public FeatureListPlaceholder(@Nullable final ModularFeatureList flist) {
    name = flist == null ? null : flist.getName();
    featureList = new WeakReference<>(flist);
  }

  public static FeatureListPlaceholder[] of(
      @Nullable final ModularFeatureList @Nullable [] flists) {
    if (flists == null) {
      return new FeatureListPlaceholder[0];
    }
    return Arrays.stream(flists).map(FeatureListPlaceholder::new)
        .toArray(FeatureListPlaceholder[]::new);
  }

  public static List<FeatureListPlaceholder> of(
      @Nullable final List<? extends ModularFeatureList> flists) {
    if (flists == null) {
      return List.of();
    }
    return flists.stream().map(FeatureListPlaceholder::new).toList();
  }

  /**
   * Same length as input but may contain null if featureLists are not in memory anymore
   *
   * @return array of matching feature lists. If the feature list was removed from the project and
   * already garbage collected this list may contain null values
   */
  public static @Nullable ModularFeatureList @NotNull [] getMatchingFeatureListOrNull(
      @NotNull FeatureListPlaceholder @Nullable [] flists) {
    if (flists == null) {
      return new ModularFeatureList[0];
    }
    return Arrays.stream(flists).map(FeatureListPlaceholder::getMatchingFeatureList)
        .toArray(ModularFeatureList[]::new);
  }

  /**
   * Same length as input but may contain null if featureLists are not in memory anymore
   *
   * @return list of matching feature lists. If the feature list was removed from the project and
   * already garbage collected this list may contain null values
   */
  public static @NotNull List<@Nullable ModularFeatureList> getMatchingFeatureListOrNull(
      @Nullable List<@NotNull FeatureListPlaceholder> flists) {
    if (flists == null) {
      return List.of();
    }
    // need to allow null values
    return flists.stream().map(FeatureListPlaceholder::getMatchingFeatureList)
        .collect(CollectionUtils.toArrayList());
  }

  /**
   * Non null matching feature lists. This may be an empty array if no feature list is in memory
   *
   * @return array of matching feature lists. null filtered
   */
  public static @NotNull ModularFeatureList @NotNull [] getMatchingFeatureListFilterNull(
      @NotNull FeatureListPlaceholder @Nullable [] flists) {
    if (flists == null) {
      return new ModularFeatureList[0];
    }
    return Arrays.stream(flists).map(FeatureListPlaceholder::getMatchingFeatureList)
        .filter(Objects::nonNull).toArray(ModularFeatureList[]::new);
  }

  /**
   * Non null matching feature lists. This may be an empty array if no feature list is in memory
   *
   * @return list of matching feature lists. null filtered
   */
  public static @NotNull List<@NotNull ModularFeatureList> getMatchingFeatureListFilterNull(
      List<FeatureListPlaceholder> flists) {
    // need to allow null values
    return flists.stream().map(FeatureListPlaceholder::getMatchingFeatureList)
        .filter(Objects::nonNull).toList();
  }

  /**
   * @return The first matching raw data file of the current project.
   */
  @Nullable
  public ModularFeatureList getMatchingFeatureList() {
    final ModularFeatureList flist = featureList.get();
    if (flist != null) {
      return flist;
    }
    final MZmineProject proj = ProjectService.getProject();
    if (proj == null) {
      return null;
    }
    return proj.getCurrentFeatureLists().stream().filter(ModularFeatureList.class::isInstance)
        .map(ModularFeatureList.class::cast).filter(this::matches).findFirst().orElse(null);
  }

  public boolean matches(@Nullable final FeatureList flist) {
    return flist != null && flist.getName().equals(name);
  }

  @Override
  public String toString() {
    return name;
  }

  public String getName() {
    return name;
  }

}
