/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.maths.similarity;

public class Weights {
  public static final Weights MASSBANK = new Weights("MassBank", 0.5, 2);
  public static final Weights NIST_GC = new Weights("NIST (GC)", 0.6, 3);
  public static final Weights NIST11 = new Weights("NIST11 (LC)", 0.53, 1.3);
  public static final Weights[] VALUES = new Weights[] {MASSBANK, NIST11, NIST_GC};

  private String name;
  private double mz, intensity;

  private Weights(String name, double intensity, double mz) {
    this.name = name;
    this.mz = mz;
    this.intensity = intensity;
  }

  public double getIntensity() {
    return intensity;
  }

  public double getMz() {
    return mz;
  }

  public String getName() {
    return name;
  }
}
