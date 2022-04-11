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

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;

/**
 * All changes to the project are registered
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class ProjectChangeListener {

  public void featureListsChanged(ProjectChangeEvent<FeatureList> event) {

  }

  public void dataFilesChanged(ProjectChangeEvent<RawDataFile> event) {

  }

  public void librariesChanged(ProjectChangeEvent<SpectralLibrary> event) {
  }
}
