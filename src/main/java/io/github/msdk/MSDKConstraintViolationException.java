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
 * <p>
 * MSDKConstraintViolationException class.
 * </p>
 *
 * @author nilshoffmann
 */
public class MSDKConstraintViolationException extends MSDKRuntimeException {

  /**
   * <p>
   * Constructor for MSDKConstraintViolationException.
   * </p>
   *
   * @param msg a {@link String} object.
   */
  public MSDKConstraintViolationException(String msg) {
    super(msg);
  }

  /**
   * <p>
   * Constructor for MSDKConstraintViolationException.
   * </p>
   *
   * @param exception a {@link Throwable} object.
   */
  public MSDKConstraintViolationException(Throwable exception) {
    super(exception);
  }

}
