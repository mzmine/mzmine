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

package io.github.mzmine.modules.io.export_features_gnps.masst;

/**
 * Available databases to search by masst. Selection of the database in GNPS is with the enum value
 * names - not toString.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum MasstDatabase {
  // value names equal the GNPS values
  ALL("ALL (Non-redundant MS/MS)"), GNPS("GNPS"), METABOLOMICSWORKBENCH(
      "Metabolomics Workbench"), METABOLIGHTS("Metabolights");

  private final String title;

  MasstDatabase(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }

  public String getGnpsValue() {
    return name();
  }
}
