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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking;

import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.DeconvolutionModule;
import io.github.mzmine.parameters.ParameterSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AdapResolverModule extends DeconvolutionModule {

  public static final String NAME = "ADAP feature resolver";

  @Nonnull
  @Override
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return ADAPDetectorParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Resolves EICs into features using ADAP algorithm.";
  }
}
