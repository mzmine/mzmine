/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
