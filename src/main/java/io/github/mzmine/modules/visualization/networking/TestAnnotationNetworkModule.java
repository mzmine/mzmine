/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.visualization.networking;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class TestAnnotationNetworkModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Test MS annotations networks";
  private static final String MODULE_DESCRIPTION =
      "Visualise the results of the MS annotation module";

  @Override
  public @Nonnull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    ModularFeatureList flist = new ModularFeatureList("Test", null, project.getDataFiles()[0]);
    flist.addFeatureType(new MZType());
    flist.addFeatureType(new RTType());
    flist.addFeatureType(new HeightType());
    flist.addFeatureType(new AreaType());

    FeatureListRow row = createRow(flist, 1);
    FeatureListRow row2 = createRow(flist, 2);
    FeatureListRow row3 = createRow(flist, 3);
    flist.addRow(row);
    flist.addRow(row2);
    flist.addRow(row3);

    flist.addRowsRelationship(row, row2, new R2RSpectralSimilarity(row, row2, Type.MS2_COSINE_SIM, new SpectralSimilarity(0.9, 12)));
    flist.addRowsRelationship(row2, row3, new R2RSpectralSimilarity(row2, row3, Type.MS2_COSINE_SIM, new SpectralSimilarity(0.8, 2)));

      FeatureNetworkTab f = new FeatureNetworkTab(flist, false,
          true, true, true);
      MZmineCore.getDesktop().addTab(f);
      return ExitCode.OK;
  }

  @Nonnull
  private ModularFeatureListRow createRow(ModularFeatureList flist, int i) {
    ModularFeatureListRow row = new ModularFeatureListRow(flist, i);
    ModularFeature f = new ModularFeature(flist);
    f.setMZ(200d);
    f.setRT(12.f);
    f.setHeight(1000);
    f.setArea(3000);
    row.addFeature(flist.getRawDataFile(0), f);
    return row;
  }

  @Override
  public @Nonnull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return TestAnnotationNetworkParameters.class;
  }
}
