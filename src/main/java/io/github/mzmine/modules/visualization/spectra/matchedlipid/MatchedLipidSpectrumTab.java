package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import java.util.Collection;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.mainwindow.MZmineTab;

public class MatchedLipidSpectrumTab extends MZmineTab {

  public MatchedLipidSpectrumTab(String matchedLipids, LipidSpectrumChart lipidSpectrumChart) {
    super(matchedLipids);
    setContent(lipidSpectrumChart);
  }

  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    // TODO Auto-generated method stub

  }


}
