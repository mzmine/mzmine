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

package io.github.mzmine.project;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import javax.xml.stream.XMLStreamReader;

/**
 * Project manager
 */
public interface ProjectManager {

  /**
   * Contract: Should not be used during raw data import to retrieve a list of possible raw data
   * files. The list should be provided as a parameter to the {@link io.github.mzmine.datamodel.features.types.DataType#loadFromXML(XMLStreamReader,
   * ModularFeatureList, ModularFeatureListRow, ModularFeature, RawDataFile)} method.
   *
   * @return The current project.
   */
  public MZmineProject getCurrentProject();

  public void setCurrentProject(MZmineProject newProject);

}
