/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.impl.ProjectManagerImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Contains the project manager and current project. There is always just one project loaded
 */
public final class ProjectService {

  @NotNull
  public static ProjectManager getProjectManager() {
    return ProjectManagerImpl.getInstance();
  }

  /**
   * @return the current project
   */
  public static @NotNull MZmineProject getProject() {
    return getProjectManager().getCurrentProject();
  }

  public static @NotNull MetadataTable getMetadata() {
    return getProject().getProjectMetadata();
  }
}
