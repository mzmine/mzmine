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

package io.github.mzmine.modules.io.import_rawdata_imzml;


public class Coordinates {

  private int x;
  private int y;
  private int z;

  /**
   * @param x The x index position relative to the minimum x index position.
   * @param y The y index position relative to the minimum y index position.
   * @param z The z index position relative to the minimum z index position.
   */
  public Coordinates(int x, int y, int z) {
    super();
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * @return The x index position relative to the minimum x index position.
   */
  public int getX() {
    return x;
  }


  public void setX(int x) {
    this.x = x;
  }

  /**
   * @return The y index position relative to the minimum y index position.
   */
  public int getY() {
    return y;
  }


  /**
   * @param y The y index position relative to the minimum y index position.
   */
  public void setY(int y) {
    this.y = y;
  }


  /**
   * @return The z index position relative to the minimum z index position.
   */
  public int getZ() {
    return z;
  }


  /**
   * @param z The z index position relative to the minimum z index position.
   */
  public void setZ(int z) {
    this.z = z;
  }

  @Override
  public String toString() {
    return getX() + ";" + getY() + ";" + getZ() + ";";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + x;
    result = prime * result + y;
    result = prime * result + z;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Coordinates other = (Coordinates) obj;
    if (x != other.x) {
      return false;
    }
    if (y != other.y) {
      return false;
    }
    if (z != other.z) {
      return false;
    }
    return true;
  }

}
