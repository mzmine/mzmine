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

import org.jetbrains.annotations.Nullable;

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
