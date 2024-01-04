package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.gui.framework.fx.features.SimpleFeatureListTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;

public class LipidAnnotationMatchTab extends SimpleFeatureListTab {

  private final LipidAnnotationMatchPane lipidAnnotationMatchPane;

  public LipidAnnotationMatchTab(final FeatureTableFX table) {
    super("Lipid matches", false, false);
    lipidAnnotationMatchPane = new LipidAnnotationMatchPane(getParentGroup());
    setContent(lipidAnnotationMatchPane);
    lipidAnnotationMatchPane.setFeatureTable(table);
  }
}
