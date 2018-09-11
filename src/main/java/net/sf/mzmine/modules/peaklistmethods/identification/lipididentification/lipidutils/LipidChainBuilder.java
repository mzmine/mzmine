/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils;

/**
 * This class contains a method to build radyl chains for lipids.
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidChainBuilder {

  LipidChainBuilder() {

  }

  /**
   * This method builds radyl chains for lipids based on the user set parameters chain length, chain
   * double bonds, number of acyl chains and number of alky chains
   */
  public String calculateChainFormula(final int chainLength, final int chainDoubleBonds,
      final int numberOfAcylChains, final int numberOfAlkylChains) {
    String chainFormula = null;
    if (chainLength > 0) { // +1 H for CH3 last CH3 group
      final int numberOfHydrogens = (1 * numberOfAcylChains + 1 * numberOfAlkylChains)// +1H for las
                                                                                      // CH3 group
          + (chainLength * 2 - chainDoubleBonds * 2) // double bond correction
          - 2 * numberOfAcylChains; // remove 2 H for C in acyl group
      final int numberOfCarbons = chainLength - numberOfAcylChains;
      // correctNumberOfCarbons(chainLength, numberOfAcylChains, numberOfAlkylChains);
      chainFormula = "C" + numberOfCarbons + 'H' + numberOfHydrogens;
    }
    return chainFormula;
  }

}
