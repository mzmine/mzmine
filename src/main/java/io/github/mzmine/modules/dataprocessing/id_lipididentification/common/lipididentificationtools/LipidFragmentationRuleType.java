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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools;

public enum LipidFragmentationRuleType {

  HEADGROUP_FRAGMENT, //
  HEADGROUP_FRAGMENT_NL, //
  ACYLCHAIN_FRAGMENT, //
  ACYLCHAIN_FRAGMENT_NL, //
  ACYLCHAIN_PLUS_FORMULA_FRAGMENT, //
  ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL, //
  ACYLCHAIN_MINUS_FORMULA_FRAGMENT, //
  ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
  TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT, //
  //  ALKYLCHAIN_FRAGMENT, //
//  ALKYLCHAIN_FRAGMENT_NL, //
  ALKYLCHAIN_PLUS_FORMULA_FRAGMENT, //
  //  ALKYLCHAIN_PLUS_FORMULA_FRAGMENT_NL, //
//  ALKYLCHAIN_MINUS_FORMULA_FRAGMENT, //
//  ALKYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,//
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT, //
  SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT, //
  SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT, //
  SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT, //
  AMID_CHAIN_FRAGMENT, //
  AMID_CHAIN_PLUS_FORMULA_FRAGMENT, //
  AMID_CHAIN_MINUS_FORMULA_FRAGMENT, //
  AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT, //
  AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT;

  @Override
  public String toString() {
    return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
  }

}