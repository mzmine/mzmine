/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.id_nist;

import java.io.File;
import java.util.Collection;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 * Holds NIST MS Search parameters.
 *
 * @author $Author$
 * @version 2.0
 */
public class NistMsSearchParameters extends SimpleParameterSet {

  /**
   * Feature lists to operate on.
   */
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  /**
   * NIST MS Search path.
   */
  public static final DirectoryParameter NIST_MS_SEARCH_DIR =
      new DirectoryParameter("NIST MS Search directory",
          "Full path of the directory containing the NIST MS Search executable (nistms$.exe)");
  /**
   * Match factor cut-off.
   */
  public static final IntegerParameter MIN_MATCH_FACTOR = new IntegerParameter("Min. match factor",
      "The minimum match factor (0 .. 1000) that search hits must have", 700, 0, 1000);

  /**
   * Reverse match factor cut-off.
   */
  public static final IntegerParameter MIN_REVERSE_MATCH_FACTOR =
      new IntegerParameter("Min. reverse match factor",
          "The minimum reverse match factor (0 .. 1000) that search hits must have", 700, 0, 1000);

  // NIST MS Search executable.
  private static final String NIST_MS_SEARCH_EXE = "nistms$.exe";

  /**
   * Construct the parameter set.
   */
  public NistMsSearchParameters() {
    super(new Parameter[] {PEAK_LISTS, NIST_MS_SEARCH_DIR, MIN_MATCH_FACTOR,
        MIN_REVERSE_MATCH_FACTOR});
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages) {

    // Unsupported OS.
    if (!isWindows()) {
      errorMessages.add("NIST MS Search is only supported on Windows operating systems.");
      return false;
    }

    boolean result = super.checkParameterValues(errorMessages);

    // NIST MS Search home directory and executable.
    final File executable = getNistMsSearchExecutable();

    // Executable missing.
    if (executable == null || !executable.exists()) {

      errorMessages.add("NIST MS Search executable (" + NIST_MS_SEARCH_EXE
          + ") not found.  Please set the to the full path of the directory containing the NIST MS Search executable.");
      result = false;
    }

    return result;
  }

  /**
   * Gets the full path to the NIST MS Search executable.
   *
   * @return the path.
   */
  public File getNistMsSearchExecutable() {

    final File dir = getParameter(NIST_MS_SEARCH_DIR).getValue();
    return dir == null ? null : new File(dir, NIST_MS_SEARCH_EXE);
  }

  /**
   * Is this a Windows OS?
   *
   * @return true/false if the os.name property does/doesn't contain "Windows".
   */
  private static boolean isWindows() {

    return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
  }
}
