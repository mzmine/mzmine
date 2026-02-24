/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.projectload;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import java.io.File;
import java.util.List;
import java.util.Map;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class ProjectLoaderParameters extends SimpleParameterSet {

  protected static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mzmine project file", "*.mzmine"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FileNameParameter projectFile = new FileNameParameter("Project file",
      "File name of project to be loaded", extensions, FileSelectionType.OPEN);

  public static final BooleanParameter mergeOntoExisting = new BooleanParameter(
      "Merge onto existing", """
      If selected, the loaded project will be merged into the current one.
      When loading a standalone project, the resulting project can only be saved as a standalone 
      project, not as a referencing project.""", false);

  public static final BooleanParameter keepLibraries = new BooleanParameter(
      "Keep current spectral libraries",
      "If selected the currently loaded spectral libraries will be removed.", false);


  public ProjectLoaderParameters() {
    super(projectFile, mergeOntoExisting, keepLibraries);
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);

    if (!loadedParams.containsKey(keepLibraries.getName())) {
      setParameter(keepLibraries, false);
    }
    if (!loadedParams.containsKey(mergeOntoExisting.getName())) {
      setParameter(mergeOntoExisting, false);
    }
  }

  public static ProjectLoaderParameters create(@NotNull File file, boolean mergeOntoExisting,
      boolean keepCurrentLibraries) {
    ParameterSet parameterSet = new ProjectLoaderParameters().cloneParameterSet();
    parameterSet.getParameter(projectFile).setValue(file);
    parameterSet.getParameter(ProjectLoaderParameters.mergeOntoExisting)
        .setValue(mergeOntoExisting);
    parameterSet.getParameter(keepLibraries).setValue(keepCurrentLibraries);
    return (ProjectLoaderParameters) parameterSet;
  }

  public static ProjectLoaderParameters create(@NotNull File file) {
    return create(file, false, false);
  }
}
