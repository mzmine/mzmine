package io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport;


public class Coordinates {

  private int x;
  private int y;
  private int z;

  public Coordinates(int x, int y, int z) {
    super();
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public int getX() {
    return x;
  }


  public void setX(int x) {
    this.x = x;
  }


  public int getY() {
    return y;
  }


  public void setY(int y) {
    this.y = y;
  }


  public int getZ() {
    return z;
  }


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
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Coordinates other = (Coordinates) obj;
    if (x != other.x)
      return false;
    if (y != other.y)
      return false;
    if (z != other.z)
      return false;
    return true;
  }

}
