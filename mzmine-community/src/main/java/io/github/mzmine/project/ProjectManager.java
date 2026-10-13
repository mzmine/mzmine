/*
 * Copyright (c) 2004-2023 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.project;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;

/**
 * Project manager
 */
public interface ProjectManager {

  /**
   * Contract: Should not be used during raw data import to retrieve a list of possible raw data
   * files. The list should be provided as a parameter to the
   * {@link io.github.mzmine.datamodel.features.types.DataType#loadFromXML(XMLStreamReader,
   * MZmineProject, ModularFeatureList, ModularFeatureListRow, ModularFeature, RawDataFile)}
   * method.
   *
   * @return The current project.
   */
  @NotNull MZmineProject getCurrentProject();

  void setCurrentProject(@NotNull MZmineProject newProject);

  /**
   * Removes all files and feature lists and creates a new project. Spectral libraries are kept.
   */
  void clearProject();

  /**
   * Creates a new empty project that keeps the spectral libraries of the given project and replaces
   * the given project by it via {@link #replaceProject(MZmineProject, MZmineProject)}.
   *
   * @return the new empty project
   */
  @NotNull MZmineProject clearProject(@NotNull MZmineProject project);

  /**
   * Replaces a project by a new project. The data files of the old project are closed. If the old
   * project is the current project, the new project becomes the current project. Otherwise, the
   * current project is not changed.
   */
  void replaceProject(@NotNull MZmineProject oldProject, @NotNull MZmineProject newProject);
}
