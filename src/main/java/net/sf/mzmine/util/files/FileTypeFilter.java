/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.files;

import java.io.File;
import javax.swing.filechooser.FileFilter;


/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FileTypeFilter extends FileFilter {

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

  @Override
  public boolean accept(File file) {
    if (file.isDirectory()) {
      return true;
    }
    // String extfile = FilenameUtils.getExtension(file.getName());
    if (extension != null)
      return extension.equalsIgnoreCase(FileTypeFilter.getExtensionFromFile(file));
    else {
      String fileEx = FileTypeFilter.getExtensionFromFile(file);
      for (String e : extensions)
        if (e.equalsIgnoreCase(fileEx))
          return true;
      return false;
    }
  }

  public String getDescription() {
    if (extension != null)
      return description + String.format(" (*%s)", extension);
    else {
      String desc = description + " (";
      for (String e : extensions)
        desc = desc + "*" + e + ", ";
      desc = desc.substring(0, desc.length() - 2) + ")";
      return desc;
    }
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

  public File addExtensionToFileName(File file) {
    // Wenn eine Extension vorliegt schauen ob sie richtig ist
    String ext = getExtensionFromFile(file);
    if (ext == null || !extension.equals(ext)) {
      // FIle Name
      String tmp = getFileNameWithoutExtension(file) + "." + extension;
      // EXT von File l�schen und neu anf�gen
      File endfile = new File(file.getParent(), tmp);
      System.out
          .println("Save File as: " + endfile.getName() + " under " + endfile.getAbsolutePath());
      return endfile;
    }
    // ansonsten das file zur�ckgeben
    return file;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }
}
