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

package io.github.mzmine.datamodel.identities.iontype.project_io;

import io.github.mzmine.datamodel.identities.io.IonLibraryIO;
import io.github.mzmine.datamodel.identities.io.LoadedIonLibrary;

/**
 * Element and attribute names of the ion identity network file that {@link IonNetworksSaver} writes
 * and {@link IonNetworksLoader} reads. Never change these, they are part of the project format.
 * <p>
 * The file starts with the library of all ion types it uses, so that each ion only needs to
 * reference one by index instead of repeating its parts. The rest is row major: a row lists its ion
 * identities in the order it holds them, so the first one is its best ion, and each ion names the
 * network it belongs to.
 */
final class IonNetworkXml {

  static final String NETWORKS_ELEMENT = "ionnetworks";
  /**
   * The distinct ion types used by the ions of this file, written before everything else.
   * <p>
   * The content is the JSON of a {@code StorableIonLibrary} as produced by {@link IonLibraryIO}, so
   * every {@code IonPart} definition is stored once and the ion types only reference it by id and
   * count. JSON rather than XML because the surrounding file is written with a streaming writer
   * while {@code IonLibraryIO}'s XML API works on a DOM.
   * <p>
   * An ion references an ion type by its position in this library, which the loader gets back from
   * {@link LoadedIonLibrary#ionTypesByIndex()}. The index is whatever the file says, it is never
   * re-derived from a sorting, so changing any sorting cannot silently repoint old files at the
   * wrong ion types.
   */
  static final String ION_LIBRARY_ELEMENT = "ionlibrary";
  /**
   * Declares a network and its consensus formulas. The members are listed with the rows.
   */
  static final String NETWORK_ELEMENT = "ionnetwork";
  static final String NETWORK_ID_ATTR = "id";
  /**
   * Formulas of the neutral molecule that the whole network describes.
   */
  static final String CONSENSUS_FORMULAS_ELEMENT = "consensus_formulas";
  static final String ROW_ELEMENT = "row";
  static final String ROW_ID_ATTR = "id";
  /**
   * One ion identity of a row. Document order within a row is the order of its ion identities.
   */
  static final String ION_ELEMENT = "ion";
  static final String ION_NETWORK_ATTR = "network";
  /**
   * Index of the ion type inside {@link #ION_LIBRARY_ELEMENT}.
   */
  static final String ION_TYPE_REF_ATTR = "iontype";
  /**
   * Formulas of a single ion identity.
   */
  static final String ION_FORMULAS_ELEMENT = "ion_formulas";

  private IonNetworkXml() {
  }
}
