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
