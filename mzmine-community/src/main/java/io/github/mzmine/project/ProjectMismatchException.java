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
import org.jetbrains.annotations.NotNull;

/**
 * Thrown if data is evaluated against a project that is not the current project shown in the GUI,
 * but the evaluation depends on the GUI, e.g., the items selected in the main window.
 */
public class ProjectMismatchException extends RuntimeException {

  public ProjectMismatchException(@NotNull final String message) {
    super(message);
  }

  /**
   * Selections of the main window only refer to the current project.
   *
   * @param project   the project the selection is evaluated against
   * @param selection describes what was selected, e.g., raw data files
   * @throws ProjectMismatchException if project is not the current project
   */
  public static void requireCurrentProject(@NotNull final MZmineProject project,
      @NotNull final String selection) throws ProjectMismatchException {
    if (project != ProjectService.getProject()) {
      throw new ProjectMismatchException("""
          Cannot use the %s selected in the main window, because they belong to the current project \
          but are evaluated against a different project, e.g., in a batch on another project. \
          Select specific %s or use another selection type instead.""".formatted(selection,
          selection));
    }
  }
}
