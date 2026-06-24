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

import io.github.mzmine.datamodel.structures.StructureInputType;
import io.github.mzmine.datamodel.structures.StructureParser;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.similarity.Tanimoto;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TanimotoSimilarity {

  private static final Logger logger = Logger.getLogger(TanimotoSimilarity.class.getName());
  private final Fingerprinter fingerprinter = new Fingerprinter();

  @Nullable
  public Float forSmiles(String a, String b) throws CDKException {
    // maybe need to add explicit hydrogens etc?
    var mola = StructureParser.silent().parseStructure(a, StructureInputType.SMILES);
    var molb = StructureParser.silent().parseStructure(b, StructureInputType.SMILES);
    if (mola == null || molb == null) {
      return null;
    }

    return forMol(mola.structure(), molb.structure());
  }

  @NotNull
  private Float forMol(@NotNull IAtomContainer a, @NotNull IAtomContainer b) throws CDKException {
    final BitSet fpa = fingerprinter.getFingerprint(a);
    final BitSet fpb = fingerprinter.getFingerprint(b);
    return Tanimoto.calculate(fpa, fpb);
  }

  public @Nullable Double forMols(@NotNull List<IAtomContainer> mols) {
    if (mols.size() < 2) {
      return null;
    }
    try {
      final Fingerprinter fingerprinter = new Fingerprinter();
      final BitSet[] fps = new BitSet[mols.size()];
      for (int i = 0; i < mols.size(); i++) {
        fps[i] = fingerprinter.getFingerprint(mols.get(i));
      }
      double sum = 0;
      int pairs = 0;
      for (int i = 0; i < fps.length; i++) {
        for (int j = i + 1; j < fps.length; j++) {
          sum += Tanimoto.calculate(fps[i], fps[j]);
          pairs++;
        }
      }
      return pairs == 0 ? null : sum / pairs;
    } catch (CDKException e) {
      logger.log(Level.WARNING, "Failed to compute structural similarity for annotation agreement",
          e);
      return null;
    }
  }
}
