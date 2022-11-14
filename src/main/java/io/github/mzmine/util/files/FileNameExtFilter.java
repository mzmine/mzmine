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

package io.github.mzmine.util.files;

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
