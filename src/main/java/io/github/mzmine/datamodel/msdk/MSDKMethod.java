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

package io.github.mzmine.datamodel.msdk;

import javax.annotation.Nullable;

/**
 * This interface represents a method or algorithm of MSDK.
 *
 * @param <ResultType> Type of object that represents the result of this method. If the method has
 *        no result, Void can be used as a special case.
 */
public interface MSDKMethod<ResultType> {

  /**
   * Returns a number in the interval 0 to 1.0, representing the portion of the task that has
   * completed, or null if the algorithm has not yet started.
   *
   * @return Finished percentage (0.0 to 1.0, inclusive), or null.
   */
  @Nullable
  Float getFinishedPercentage();

  /**
   * Performs the algorithm. This method may throw MSDKException or MSDKRuntimeException if error
   * occurs.
   *
   * @throws MSDKException On any error
   * @return the result of this algorithm, or null
   */
  @Nullable
  ResultType execute() throws MSDKException;

  /**
   * Returns the result of this algorithm, or null.
   *
   * @return a ResultType object.
   */
  @Nullable
  ResultType getResult();

  /**
   * Cancel a running algorithm. This method can be called from any thread.
   */
  void cancel();

}
