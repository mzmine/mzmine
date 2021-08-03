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
/*
 * author Owen Myers (Oweenm@gmail.com)
 */
package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking;

import io.github.mzmine.modules.MZmineModule;

public interface SNEstimatorChoice extends MZmineModule {
  /**
   * Gets if resolver requires R, if applicable
   */
  public boolean getRequiresR();

  /**
   * Gets R required packages for the resolver's method, if applicable
   */
  public String[] getRequiredRPackages();

  /**
   * Gets R required packages versions for the resolver's method, if applicable
   */
  public String[] getRequiredRPackagesVersions();

  public String getSNCode();

}
