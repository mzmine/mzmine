/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_processing;

import java.util.Stack;
import javax.annotation.Nullable;

/**
 * A boolean stack. The numbers of true and false values are counted. If a maximum size is set, the
 * stack cannot exceed that size. If the size would be exceeded, an element is removed
 * automatically.
 */
class CountingBooleanStack extends Stack<Boolean> {

  private final Integer maxSize;
  private int numTrue = 0;
  private int numFalse = 0;


  public CountingBooleanStack() {
    this(null);
  }

  public CountingBooleanStack(@Nullable Integer maxSize) {
    super();
    this.maxSize = maxSize;
  }

  @Override
  public Boolean push(Boolean item) {
    numTrue = item ? numTrue + 1 : numTrue;
    numFalse = !item ? numFalse + 1 : numFalse;

    if (maxSize != null && maxSize < super.size() + 1) {
      Boolean removed = remove(0);
      numTrue = removed ? numTrue - 1 : numTrue;
      numFalse = !removed ? numFalse - 1 : numFalse;
    }

    return super.push(item);
  }

  @Override
  public synchronized Boolean pop() {
    Boolean item = super.pop();
    numTrue = item ? numTrue - 1 : numTrue;
    numFalse = !item ? numFalse - 1 : numFalse;
    return item;
  }

  /**
   * @return The maximum size, if set.
   */
  public Integer getMaxSize() {
    return maxSize;
  }

  public int getNumTrue() {
    return numTrue;
  }

  public int getNumFalse() {
    return numFalse;
  }

  @Override
  public synchronized Boolean remove(int index) {
    return super.remove(index);
  }
}
