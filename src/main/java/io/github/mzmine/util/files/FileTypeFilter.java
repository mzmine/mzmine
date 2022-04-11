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

package io.github.mzmine.util.files;

import java.io.File;
import java.io.FileFilter;
import java.util.Objects;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FileTypeFilter extends javax.swing.filechooser.FileFilter implements FileFilter {

  private String[] extensions;
  private String extension = null;
  private String description;

  public FileTypeFilter(String extension, String description) {
    this.extension = extension;
    this.description = description;
  }

  public FileTypeFilter(String[] extensions, String description) {
    this.extensions = extensions;
    this.description = description;
  }

  public FileTypeFilter(ExtensionFilter filter, String description) {
    this.extensions = mapFilter(filter);
    this.description = description;
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

  public static String getExtensionFromFile(File file) {
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
    if (file.isDirectory()) {
      return false;
    }
    // String extfile = FilenameUtils.getExtension(file.getName());
    if (extension != null) {
      return extension.equalsIgnoreCase(FileTypeFilter.getExtensionFromFile(file));
    } else {
      String fileEx = FileTypeFilter.getExtensionFromFile(file);
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
      System.out
          .println("Save File as: " + endfile.getName() + " under " + endfile.getAbsolutePath());
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
