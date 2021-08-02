/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk;

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
