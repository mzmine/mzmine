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

package io.github.mzmine.util.files;

import java.io.File;
import java.io.FileFilter;
import java.util.Objects;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FileTypeFilter extends javax.swing.filechooser.FileFilter implements FileFilter {

  private String[] extensions;
  private String extension = null;
  private final String description;
  private final boolean allowDirectories;

  public FileTypeFilter(String extension, String description) {
    this.extension = extension;
    this.description = description;
    allowDirectories = false;
  }

  public FileTypeFilter(String[] extensions, String description) {
    this.extensions = extensions;
    this.description = description;
    allowDirectories = false;
  }

  public FileTypeFilter(ExtensionFilter filter, String description) {
    this(filter, description, false);
  }

  public FileTypeFilter(ExtensionFilter filter, String description, boolean allowDirectories) {
    this.extensions = mapFilter(filter);
    this.description = description;
    this.allowDirectories = allowDirectories;
  }

  public static String[] mapFilter(ExtensionFilter filters) {
    if (filters == null) {
      return new String[0];
    }
    return filters.getExtensions().stream().filter(Objects::nonNull)
        .map(FileTypeFilter::cleanExtension).toArray(String[]::new);
  }

  private static String cleanExtension(String ext) {
    if (ext.startsWith("*.")) {
      ext = ext.substring(2);
    } else if (ext.startsWith(".")) {
      ext = ext.substring(1);
    }
    return ext;
  }

  public static boolean hasExtensionFile(File file) {
    return getExtensionFromFile(file) != null;
  }

  public static @Nullable String getExtensionFromFile(File file) {
    String extfile = null;
    String fileName = file.getName();

    int i = fileName.lastIndexOf('.');
    int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

    if (i > p) {
      extfile = fileName.substring(i + 1);
    }
    return extfile;
  }

  public static String getFileNameWithoutExtension(File file) {
    String realName = file.getName();
    String fileName = realName;

    int i = fileName.lastIndexOf('.');
    int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

    if (i > p) {
      realName = fileName.substring(0, i);
    }
    return realName;
  }

  @Override
  public boolean accept(File file) {
    if (!allowDirectories && file.isDirectory()) {
      return false;
    }
    // String extfile = FilenameUtils.getExtension(file.getName());
    if (extension != null) {
      return extension.equalsIgnoreCase(FileTypeFilter.getExtensionFromFile(file));
    } else {
      String fileEx = FileTypeFilter.getExtensionFromFile(file);
      if (fileEx == null) {
        return false;
      }
      for (String e : extensions) {
        if (e.equalsIgnoreCase(fileEx)) {
          return true;
        }
      }
      return false;
    }
  }

  public String getDescription() {
    if (extension != null) {
      return description + String.format(" (*%s)", extension);
    } else {
      String desc = description + " (";
      for (String e : extensions) {
        desc = desc + "*" + e + ", ";
      }
      desc = desc.substring(0, desc.length() - 2) + ")";
      return desc;
    }
  }

  public File addExtensionToFileName(File file) {
    String ext = getExtensionFromFile(file);
    if (ext == null || !extension.equals(ext)) {
      // FIle Name
      String tmp = getFileNameWithoutExtension(file) + "." + extension;
      File endfile = new File(file.getParent(), tmp);
      System.out.println(
          "Save File as: " + endfile.getName() + " under " + endfile.getAbsolutePath());
      return endfile;
    }
    return file;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }
}
