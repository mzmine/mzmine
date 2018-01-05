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
import java.io.FilenameFilter;
import java.io.Serializable;

/**
 * Filter files by extentions or startsWith
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FileNameExtFilter implements FilenameFilter, Serializable {
  private static final long serialVersionUID = 1L;
  private String startsWith, ext;

  public FileNameExtFilter(String startsWith, String ext) {
    this.startsWith = startsWith.toLowerCase();
    this.ext = ext.toLowerCase();
  }

  @Override
  public boolean accept(File f, String name) {
    return ((new File(f, name)).isFile() && (ext.equals("") || name.toLowerCase().endsWith(ext))
        && (startsWith.equals("") || name.toLowerCase().startsWith(startsWith)));
  }

  public String getStartsWith() {
    return startsWith;
  }

  public void setStartsWith(String startsWith) {
    this.startsWith = startsWith;
  }

  public String getExt() {
    return ext;
  }

  public void setExt(String ext) {
    this.ext = ext;
  }

}
