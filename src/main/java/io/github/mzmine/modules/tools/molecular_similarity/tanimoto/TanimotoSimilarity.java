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

package io.github.mzmine.modules.tools.molecular_similarity.tanimoto;

import java.util.BitSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.cdk.smiles.SmilesParser;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TanimotoSimilarity {

  private final SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
  private final Fingerprinter fingerprinter = new Fingerprinter();

  public double forSmiles(String a, String b) throws CDKException {
    // maybe need to add explicit hydrogens etc?
    IAtomContainer mola = sp.parseSmiles(a);
    IAtomContainer molb = sp.parseSmiles(b);
    return forMol(mola, molb);
  }

  private double forMol(IAtomContainer a, IAtomContainer b) throws CDKException {
    final BitSet fpa = fingerprinter.getFingerprint(a);
    final BitSet fpb = fingerprinter.getFingerprint(b);
    return Tanimoto.calculate(fpa, fpb);
  }

}
