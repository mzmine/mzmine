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

package net.sf.mzmine.modules.tools.kovats;

import java.text.MessageFormat;
import java.util.stream.IntStream;

public class KovatsValues {
  public enum KovatsIndex {
    C1, C2, C3, C4, C5, C6, C7, C8, C9, C10, C11, C12, C13, C14, C15, C16, C17, C18, C19, C20, C21, C22, C23, C24, C25, C26, C27, C28, C29, C30, C31, C32, C33, C34, C35, C36, C37, C38, C39, C40, C41, C42, C43, C44, C45, C46, C47, C48, C49, C50;
    private String[] names = new String[] {"Methane", "Ethane", "Propane", "Butane", "Pentane",
        "Hexane", "Heptane", "Octane", "Nonane", "Decane", "Undecane", "Dodecane", "Tridecane",
        "Tetradecane", "Pentadecane", "Hexadecane", "Heptadecane", "Octadecane", "Nonadecane",
        "Icosane", "Henicosane", "Docosane", "Tricosane", "Tetracosane", "Pentacosane",
        "Hexacosane", "Heptacosane", "Octacosane", "Nonacosane", "Triacontane", "Hentriacontane",
        "Dotriacontane", "Tritriacontane", "Tetratriacontane", "Pentatriacontane",
        "Hexatriacontane", "Heptatriacontane", "Octatriacontane", "Nonatriacontane", "Tetracontane",
        "Hentetracontane", "Dotetracontane", "Tritetracontane", "Tetratetracontane",
        "Pentatetracontane", "Hexatetracontane", "Heptatetracontane", "Octatetracontane",
        "Nonatetracontane", "Pentacontane"};

    public static KovatsIndex getByCarbon(int c) {
      return KovatsIndex.values()[c - 1];
    }

    public String getAlkaneName() {
      return names[this.ordinal()];
    }

    public String getShortName() {
      return name();
    }

    public String getCombinedName() {
      return names[this.ordinal()] + " (" + name() + ")";
    }

    @Override
    public String toString() {
      return getCombinedName();
    }

    public int getNumCarbon() {
      return this.ordinal() + 1;
    }

    /**
     * Kovats indexes from a carbons to b carbons (inclusive)
     * 
     * @param cFirst
     * @param cLastInclusive
     * @return
     */
    public static KovatsIndex[] getRange(int cFirst, int cLastInclusive) {
      cFirst = Math.max(cFirst, 1);
      cLastInclusive = Math.min(cLastInclusive, KovatsIndex.values().length);
      if (cLastInclusive < cFirst)
        return new KovatsIndex[] {getByCarbon(cFirst)};
      else {
        return IntStream.rangeClosed(cFirst, cLastInclusive).mapToObj(KovatsIndex::getByCarbon)
            .toArray(KovatsIndex[]::new);
      }
    }

    public static KovatsIndex getByShortName(String c) {
      return getByCarbon(Integer.parseInt(c.substring(1)));
    }

    public static KovatsIndex getByString(String name) {
      // short name?
      if (name.length() <= 3 && name.toLowerCase().startsWith("c")) {
        try {
          return getByShortName(name);
        } catch (Exception e) {
        }
      }
      // else full name
      // AlkaneName (C20)
      String c = name.split("\\(|\\)")[1];
      return getByShortName(c);
    }

    /**
     * Formula of this alkane or of fragment ion with -H
     * 
     * @param subtractH
     * @return
     */
    public String getFormula(boolean subtractH) {
      int c = getNumCarbon();
      int h = c * 2 + 2 - (subtractH ? 1 : 0);
      return MessageFormat.format("C{0}H{1}", c, h);
    }
  }

  //
  private final KovatsIndex ki;
  private double retentionTime;

  public KovatsValues(KovatsIndex index, double retentionTime) {
    this.ki = index;
    this.retentionTime = retentionTime;
  }

  public KovatsIndex getIndex() {
    return ki;
  }

  public double getRetentionTime() {
    return retentionTime;
  }

  public void setRetentionTime(double retentionTime) {
    this.retentionTime = retentionTime;
  }

}
