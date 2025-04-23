/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.ImmutableList;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.io.projectsave.RawDataFileSaveHandler;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 */
public class FileNamesParameter implements UserParameter<File[], FileNamesComponent> {

  public static final String XML_RELATIVE_PATH_ATTRIBUTE = "relative_path";
  public static final Function<File[], File[]> DEFAULT_ALL_FILES_MAPPER = files -> Arrays.stream(
      files).filter(Predicate.not(File::isDirectory)).toArray(File[]::new);
  ;

  private final String name;
  private final String description;
  private final Path defaultDir;
  private File[] value;
  private final List<ExtensionFilter> filters;
  // this mapper is applied when all * files button is clicked. Input is all files and directories
  // matching the filter and the function may apply transformation like Bruker path validation
  // Default just filters out directories
  protected final @NotNull Function<File[], File[]> allFilesMapper;

  @Nullable
  protected final String dragPrompt;

  public FileNamesParameter(String name) {
    this(name, "", List.of(), null);
  }

  public FileNamesParameter(String name, String description, List<ExtensionFilter> filters) {
    this(name, description, filters, null, null, null, DEFAULT_ALL_FILES_MAPPER);
  }

  public FileNamesParameter(String name, String description, List<ExtensionFilter> filters,
      @Nullable String dragPrompt) {
    this(name, description, filters, dragPrompt, DEFAULT_ALL_FILES_MAPPER);
  }

  public FileNamesParameter(String name, String description, List<ExtensionFilter> filters,
      @Nullable String dragPrompt, @NotNull Function<File[], File[]> allFilesMapper) {
    this(name, description, filters, null, null, dragPrompt, allFilesMapper);
  }

  public FileNamesParameter(String name, String description, List<ExtensionFilter> filters,
      Path defaultDir, @Nullable File[] defaultFiles, @Nullable String dragPrompt,
      @NotNull Function<File[], File[]> allFilesMapper) {
    this.name = name;
    this.description = description;
    this.filters = ImmutableList.copyOf(filters);
    this.defaultDir = defaultDir;
    this.dragPrompt = dragPrompt;
    this.allFilesMapper = allFilesMapper;
    setValue(defaultFiles);
  }

  public Path getDefaultDir() {
    return defaultDir;
  }

  public List<ExtensionFilter> getFilters() {
    return filters;
  }

  /**
   *
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   *
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public FileNamesComponent createEditingComponent() {
    return new FileNamesComponent(filters, defaultDir, dragPrompt, allFilesMapper);
  }

  @Override
  @NotNull
  public File[] getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable File[] value) {
    this.value = requireNonNullElse(value, new File[0]);
  }

  @Override
  public FileNamesParameter cloneParameter() {
    FileNamesParameter copy = new FileNamesParameter(name, description, filters, defaultDir,
        getValue(), dragPrompt, allFilesMapper);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(FileNamesComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(FileNamesComponent component, @Nullable File[] newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList list = xmlElement.getElementsByTagName("file");
    File[] newFiles = new File[list.getLength()];
    final MZmineProject project = ProjectService.getProject();

    for (int i = 0; i < list.getLength(); i++) {
      Element nextElement = (Element) list.item(i);

      final File absFile = new File(nextElement.getTextContent());
      final String relPathAttr = nextElement.getAttribute(XML_RELATIVE_PATH_ATTRIBUTE);
      final File relFile = project.resolveRelativePathToFile(relPathAttr);

      if (!absFile.exists() && (relFile != null && relFile.exists())) {
        newFiles[i] = relFile;
      } else {
        newFiles[i] = absFile;
      }
    }
    this.value = newFiles;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    final MZmineProject project = ProjectService.getProject();

    for (File f : value) {
      Element newElement = parentDocument.createElement("file");
      newElement.setTextContent(f.getPath());

      if (!f.toString().contains(RawDataFileSaveHandler.DATA_FILES_PREFIX)) {
        final Path relativePath = project.getRelativePath(f.toPath());
        if (relativePath != null) {
          newElement.setAttribute(XML_RELATIVE_PATH_ATTRIBUTE, relativePath.toString());
        }
      }
      xmlElement.appendChild(newElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if (that == null) {
      return false;
    }
    if (!(that instanceof FileNamesParameter thatParam)) {
      return false;
    }

    File[] thatValue = thatParam.getValue();
    return Arrays.equals(value, thatValue);
  }

  @Override
  public Priority getComponentVgrowPriority() {
    return Priority.SOMETIMES;
  }

  public int numFiles() {
    return value != null ? value.length : 0;
  }
}
