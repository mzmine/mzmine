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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.util.exceptions;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.ScanUtils;

public class MissingMassListException extends RuntimeException {

  public MissingMassListException(Scan scan) {
    this("", scan);
  }

  public MissingMassListException(String message, Scan scan) {
    super("Missing mass list in scan " + ScanUtils.scanToString(scan, true) + ". " + message);
  }

  /**
   * This constructor is only to be used when no direct scan object is present. (e.g., {@link
   * io.github.mzmine.datamodel.impl.MobilityScanStorage}) Use other constructors by default.
   *
   * @param message Error message
   */
  public MissingMassListException(String message) {
    super(message);
  }

}
