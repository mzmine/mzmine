package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import java.lang.ref.WeakReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListsPlaceholder {

  private final String name;
  private final WeakReference<FeatureList> featureList;

  public FeatureListsPlaceholder(@NotNull final FeatureList featureList) {
    name = featureList.getName();
    this.featureList = new WeakReference<>(featureList);
  }

  @Nullable
  public FeatureList getMatchingFeatureList() {
    var flist = featureList.get();
    if (flist != null) {
      return flist;
    }

    final MZmineProject proj = MZmineCore.getProjectManager().getCurrentProject();
    if (proj == null) {
      return null;
    }
    return proj.getCurrentFeatureLists().stream().filter(this::matches).findFirst().orElse(null);
  }

  private boolean matches(@Nullable final FeatureList featureList) {
    return featureList != null && this.featureList.refersTo(featureList) && featureList.getName()
        .equals(name);
  }


  public @NotNull String getName() {
    return name;
  }

  public WeakReference<FeatureList> getFeatureList() {
    return featureList;
  }


}
