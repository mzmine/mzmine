package io.github.mzmine.modules.visualization.spectra.multimsms;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MultiMsMsTab extends MZmineTab {

  public MultiMsMsTab(List<ModularFeatureListRow> rows, Collection<RawDataFile> rawDataFiles,
      RawDataFile rawDataFile) {
    super("Multiple MS/MS (" + rows.size() + ")", false, false);

    MultiMSMSPane content = new MultiMSMSPane();
    content.setData(rows.toArray(new FeatureListRow[0]), rawDataFiles.toArray(new RawDataFile[0]),
        rawDataFile, true, SortingProperty.MZ, SortingDirection.Ascending);
    setContent(content);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.EMPTY_LIST;
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.EMPTY_LIST;
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
