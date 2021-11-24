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

package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectraIdentificationResultsModule implements MZmineModule {

  public static final String MODULE_NAME = "Local spectral libraries search results";

  public static void showNewTab(List<ModularFeatureListRow> rows) {
    List<SpectralDBFeatureIdentity> spectralID =
        rows.stream().flatMap(row -> row.getSpectralLibraryMatches().stream()).toList();
    if (!spectralID.isEmpty()) {
      SpectraIdentificationResultsWindowFX window = new SpectraIdentificationResultsWindowFX();
      window.addMatches(spectralID);
      window.setTitle("Matched " + spectralID.size() + " compounds for feature list rows "
          + rows.stream().map(row -> String.valueOf(row.getID())).collect(Collectors.joining(", ")));
      window.show();
    }
  }

  @Override
  public @NotNull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nullable
  Class<? extends ParameterSet> getParameterSetClass() {
    return SpectraIdentificationResultsParameters.class;
  }
}
