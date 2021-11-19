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

package io.github.mzmine.parameters.impl;

public enum IonMobilitySupport {
  /**
   * Module requires mobility data to work.
   */
  ONLY,

  /**
   * Module can be used to process non-ims and ims data.
   */
  SUPPORTED,

  /**
   * Module has not been tested with ion mobility data, but should work.
   */
  UNTESTED,

  /**
   * Module has been tested with ion mobility data and shows certain restrictions when processing
   * that data. A specific warning message should be displayed by overriding {@link
   * SimpleParameterSet#getRestrictedIonMobilitySupportMessage()}.
   */
  RESTRICTED,

  /**
   * Module does not support ion mobility data and will produce wrong results.
   */
  UNSUPPORTED,
}
