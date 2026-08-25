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

package io.github.mzmine.datamodel.structures;

import org.jetbrains.annotations.NotNull;

/**
 * Options for
 * {@link StructureHarmonizer#harmonize(org.openscience.cdk.interfaces.IAtomContainer,
 * HarmonizationOptions)}. Use {@link #DEFAULT} unless a caller has a specific reason to keep the
 * input as it was written.
 *
 * @param normalizeFunctionalGroups apply the representation rules of {@link StructureHarmonizer},
 *                                  for example pentavalent nitro to its charge separated form and
 *                                  imidic acid to the amide tautomer
 * @param metalPolicy               how to treat metal-ligand bonds before splitting
 * @param fragmentPolicy            whether to reduce the structure to its main component
 * @param neutralizeCharges         protonate anions and deprotonate cations so that the neutral
 *                                  molecule mass is reported. Permanent charges such as quaternary
 *                                  ammonium and charge separated groups are preserved
 * @param suppressHydrogens         make explicit hydrogens implicit. Keep this on for analytes so
 *                                  generated smiles are free of explicit {@code [H]}. Turn it off
 *                                  for query structures, where an explicit hydrogen is a constraint
 *                                  the user wrote on purpose: {@code OH} asks for a hydroxyl while
 *                                  {@code O} matches any oxygen
 */
public record HarmonizationOptions(boolean normalizeFunctionalGroups,
                                   @NotNull MetalPolicy metalPolicy,
                                   @NotNull FragmentPolicy fragmentPolicy,
                                   boolean neutralizeCharges, boolean suppressHydrogens) {

  /**
   * Full harmonization. This is what {@link StructureParser} applies unless a caller asks for
   * something else.
   */
  public static final HarmonizationOptions DEFAULT = new HarmonizationOptions(true,
      MetalPolicy.KEEP_CENTRAL_IONS, FragmentPolicy.MAJOR_FRAGMENT, true, true);

  /**
   * Like {@link #DEFAULT} but keeps the protonation state of the input. Use this when the charge as
   * written carries information, for example for a structure that describes an observed ion.
   */
  public static final HarmonizationOptions KEEP_CHARGES = new HarmonizationOptions(true,
      MetalPolicy.KEEP_CENTRAL_IONS, FragmentPolicy.MAJOR_FRAGMENT, false, true);

  /**
   * Like {@link #DEFAULT} but keeps all components, including counter ions and solvates.
   */
  public static final HarmonizationOptions KEEP_FRAGMENTS = new HarmonizationOptions(true,
      MetalPolicy.KEEP_ALL, FragmentPolicy.KEEP_ALL, true, true);

  /**
   * For query structures of the smiles, inchi and smarts row filters and of
   * {@link SubstructureMatcher}.
   * <p>
   * decision: a query keeps everything the user wrote. Multiple structures in one query are a
   * feature of the filters, {@code O.O.O} asks for at least three oxygens, so main fragment
   * selection is off. Charges are part of the query. Explicit hydrogens are kept because they are a
   * constraint: {@code OH} asks for a hydroxyl while {@code O} matches any oxygen. Group
   * normalization is still applied so a query written with a pentavalent nitro matches a target
   * written charge separated.
   */
  public static final HarmonizationOptions QUERY = new HarmonizationOptions(true,
      MetalPolicy.KEEP_ALL, FragmentPolicy.KEEP_ALL, false, false);

  /**
   * Only perceive rings and aromaticity and suppress explicit hydrogens. No structural change.
   */
  public static final HarmonizationOptions NONE = new HarmonizationOptions(false,
      MetalPolicy.KEEP_ALL, FragmentPolicy.KEEP_ALL, false, true);

  /**
   * @return true if this instance would leave the connection table and charges untouched
   */
  public boolean isNoOp() {
    return !normalizeFunctionalGroups && !neutralizeCharges && metalPolicy == MetalPolicy.KEEP_ALL
        && fragmentPolicy == FragmentPolicy.KEEP_ALL;
  }
}
