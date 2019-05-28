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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats;

/**
 * Specific values for GNPS entries (fields that only allow these values)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GnpsValues {

  public enum CompoundSource {
    Lysate, Isolated, Commercial, Crude, Other;
  }

  public enum Polarity {
    Positive, Negative;
  }

  public enum Instrument {
    qTof, QQQ, Ion_Trap("Ion Trap"), Hybrid_FT("Hybrid FT"), Orbitrap, ToF;

    private final String value;

    private Instrument() {
      this.value = null;
    }

    private Instrument(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value != null ? value : super.toString();
    }
  }

  public enum IonSource {
    LC_ESI("LC-ESI"), DI_ESI("DI-ESI"), EI, APCI, ESI;

    private final String value;

    private IonSource() {
      this.value = null;
    }

    private IonSource(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value != null ? value : super.toString();
    }
  }
}
