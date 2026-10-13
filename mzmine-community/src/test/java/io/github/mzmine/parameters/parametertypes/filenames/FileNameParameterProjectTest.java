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

package io.github.mzmine.parameters.parametertypes.filenames;

import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.util.XMLUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Relative file paths are saved and resolved against the project that is passed to the XML methods
 * and not against the current project.
 */
class FileNameParameterProjectTest {

  @TempDir
  Path tempDir;

  @Test
  void relativePathUsesGivenProject() throws IOException, ParserConfigurationException {
    // the current project has no project file, so it cannot create relative paths
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());

    final MZmineProjectImpl savedProject = createProject("saved");
    final File savedData = Files.createFile(tempDir.resolve("saved").resolve("data.csv")).toFile();

    final FileNameParameter parameter = new FileNameParameter("file", "file",
        FileSelectionType.OPEN);
    parameter.setValue(savedData);

    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement("parameter");
    document.appendChild(element);
    parameter.saveValueToXML(savedProject, element);

    // the project was moved: the absolute path is gone, only the relative path still resolves
    Files.delete(savedData.toPath());
    final MZmineProjectImpl movedProject = createProject("moved");
    final File movedData = Files.createFile(tempDir.resolve("moved").resolve("data.csv")).toFile();

    final FileNameParameter loaded = new FileNameParameter("file", "file", FileSelectionType.OPEN);
    loaded.loadValueFromXML(movedProject, element);

    Assertions.assertEquals(movedData.getCanonicalFile(), loaded.getValue().getCanonicalFile());
  }

  private MZmineProjectImpl createProject(String folder) throws IOException {
    // assumption: relative paths are resolved from the project file path, which is a directory
    // here so that the parent reference also resolves on file systems that check each segment
    final File projectFile = Files.createDirectories(
        tempDir.resolve(folder).resolve("project.mzmine")).toFile();
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.setProjectFile(projectFile);
    return project;
  }
}
