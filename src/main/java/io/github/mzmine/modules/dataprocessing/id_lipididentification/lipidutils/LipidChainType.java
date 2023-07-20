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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils;

public enum LipidChainType {

  ACYL_CHAIN("Acyl chain"), //
  ACYL_MONO_HYDROXY_CHAIN("Acyl mono hydroxy chain"),
  ALKYL_CHAIN("Alkyl chain"),//
  AMID_CHAIN("Amid chain"), //
  AMID_MONO_HYDROXY_CHAIN("Amid mono hydroxy chain"), //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN("Shpingolipid mono hydroxy backbone chain"), //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN("Shpingolipid di hydroxy backbone chain"), //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN("Shpingolipid tri hydroxy backbone chain");// ;

  private final String name;

  LipidChainType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

}
