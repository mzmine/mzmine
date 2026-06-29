/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.similarity.Tanimoto;

/**
 * Computes molecular fingerprints and Tanimoto similarities between structures. A single instance
 * can be shared across threads: the underlying CDK {@link IFingerprinter} is kept in a
 * {@link ThreadLocal} because some implementations (e.g. circular / PubChem fingerprinters) keep
 * mutable state during calculation and are not thread-safe.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TanimotoSimilarity {

  private static final Logger logger = Logger.getLogger(TanimotoSimilarity.class.getName());

  private final @NotNull FingerprintType fingerprintType;
  // one fingerprinter per thread - safe to share this TanimotoSimilarity instance across threads
  private final ThreadLocal<IFingerprinter> fingerprinter;

  public TanimotoSimilarity() {
    // historic default for the annotation-agreement check
    this(FingerprintType.DAYLIGHT_1024);
  }

  public TanimotoSimilarity(@NotNull final FingerprintType fingerprintType) {
    this.fingerprintType = fingerprintType;
    this.fingerprinter = ThreadLocal.withInitial(fingerprintType::createFingerprinter);
  }

  /**
   * Maximum Tanimoto similarity between any pair of fingerprints from the two lists. Returns null
   * when either list is empty or no valid pair could be compared.
   *
   * @param a fingerprints of the first row's structures
   * @param b fingerprints of the second row's structures
   * @return the best-scoring {@link StructureFingerprintScore}, or null if no comparison succeeded
   */
  @Nullable
  public static StructureFingerprintScore maxTanimoto(@NotNull final List<StructureFingerprint> a,
      @NotNull final List<StructureFingerprint> b) {
    StructureFingerprintScore best = null;
    for (final StructureFingerprint sfpa : a) {
      for (final StructureFingerprint sfpb : b) {
        try {
          final float sim = Tanimoto.calculate(sfpa.fingerprint(), sfpb.fingerprint());
          if (best == null || sim > best.similarity()) {
            best = new StructureFingerprintScore(sfpa, sfpb, sim);
            // perfect match – no need to check further pairs
            if (Float.compare(sim, 1f) == 0) {
              return best;
            }
          }
        } catch (CDKException e) {
          // fingerprints of different bit length cannot be compared - skip this pair
          logger.log(Level.FINE, () -> "Failed to compute Tanimoto similarity: " + e.getMessage());
        }
      }
    }
    return best;
  }

  @NotNull
  public FingerprintType getFingerprintType() {
    return fingerprintType;
  }

  @Nullable
  public Float forSmiles(final String a, final String b) throws CDKException {
    // maybe need to add explicit hydrogens etc?
    final var mola = StructureParser.silent().parseStructure(a, StructureInputType.SMILES);
    final var molb = StructureParser.silent().parseStructure(b, StructureInputType.SMILES);
    if (mola == null || molb == null) {
      return null;
    }

    return forMol(mola.structure(), molb.structure());
  }

  @NotNull
  private Float forMol(@NotNull final IAtomContainer a, @NotNull final IAtomContainer b)
      throws CDKException {
    final BitSet fpa = computeFingerprint(a);
    final BitSet fpb = computeFingerprint(b);
    return Tanimoto.calculate(fpa, fpb);
  }

  /**
   * Computes the bit fingerprint for the structure using the configured {@link FingerprintType}.
   *
   * @return the fingerprint as a {@link BitSet} or null if the structure could not be fingerprinted
   */
  @Nullable
  public BitSet getFingerprint(@NotNull final IAtomContainer mol) {
    try {
      return computeFingerprint(mol);
    } catch (CDKException | RuntimeException e) {
      logger.log(Level.FINE, () -> "Failed to compute fingerprint: " + e.getMessage());
      return null;
    }
  }

  @NotNull
  private BitSet computeFingerprint(@NotNull final IAtomContainer mol) throws CDKException {
    return fingerprinter.get().getBitFingerprint(mol).asBitSet();
  }

  public @Nullable Double forMols(@NotNull final List<IAtomContainer> mols) {
    if (mols.size() < 2) {
      return null;
    }
    try {
      final BitSet[] fps = new BitSet[mols.size()];
      for (int i = 0; i < mols.size(); i++) {
        fps[i] = computeFingerprint(mols.get(i));
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
