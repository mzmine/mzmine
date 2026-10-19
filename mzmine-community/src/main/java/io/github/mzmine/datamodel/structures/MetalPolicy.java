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

/**
 * Controls how bonds between a metal and its ligands are treated before a structure is split into
 * fragments. Databases and spectral libraries frequently write salts without the fragment separator
 * (for example {@code C(N[Na])=O} instead of {@code C(N)=O.[Na+]}), so those bonds have to be cut
 * before {@link FragmentPolicy#MAJOR_FRAGMENT} can strip the counter ion. Genuine coordination
 * complexes such as heme, chlorophyll or cobalamin must survive that step.
 */
public enum MetalPolicy {

  /**
   * Cut salt-like metal bonds but keep chelated central ions. A metal bond is considered salt-like
   * when the metal is not part of a ring, has no bond to carbon, and is either an alkali/alkaline
   * earth metal or bound to a single hetero atom donor. See
   * {@link StructureHarmonizer#isSaltLikeMetal}.
   */
  KEEP_CENTRAL_IONS,

  /**
   * Cut every metal-heteroatom bond. This reproduces the normalization that standard InChI applies
   * internally and therefore also separates the iron of heme or the magnesium of chlorophyll, which
   * {@link FragmentPolicy#MAJOR_FRAGMENT} then strips away.
   */
  DISCONNECT_ALL,

  /**
   * Never cut metal bonds. Structures are only split at bonds that were already absent, i.e. at the
   * {@code .} separator of the input.
   */
  KEEP_ALL
}
