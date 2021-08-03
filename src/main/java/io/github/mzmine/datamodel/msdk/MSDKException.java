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

package io.github.mzmine.datamodel.msdk;

/**
 * MSDK exception class.
 */
public class MSDKException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * <p>
   * Constructor for MSDKException.
   * </p>
   *
   * @param msg a {@link String} object.
   */
  public MSDKException(String msg) {
    super(msg);
  }

  /**
   * <p>
   * Constructor for MSDKException.
   * </p>
   *
   * @param exception a {@link Throwable} object.
   */
  public MSDKException(Throwable exception) {
    super(exception);
  }
  
  /**
   * <p>
   * Constructor for MSDKException.
   * </p>
   *
   * @param exception a {@link Throwable} object.
   */
  public MSDKException(String msg, Throwable exception) {
    super(msg, exception);
  }

}
