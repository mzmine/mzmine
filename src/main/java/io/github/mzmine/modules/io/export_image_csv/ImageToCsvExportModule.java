/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.export_image_csv;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageToCsvExportModule implements MZmineModule {

  public static void showExportDialog(Collection<ModularFeatureListRow> rows, @NotNull Instant moduleCallDate) {

    final List<ModularFeature> features = rows.stream()
        .flatMap(ModularFeatureListRow::streamFeatures)
        .filter(f -> f.getFeatureStatus() != FeatureStatus.UNKNOWN).toList();

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ImageToCsvExportModule.class);

    MZmineCore.runLater(() -> {
      ExitCode code = param.showSetupDialog(true);
      if (code == ExitCode.OK) {
        MZmineCore.getTaskController().addTask(new ImageToCsvExportTask(param, features, moduleCallDate));
      }
    });
  }

  @NotNull
  @Override
  public String getName() {
    return "Image to csv export";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return ImageToCsvExportParameters.class;
  }
}
