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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids;

/**
 * Enum that contains all lipid main classes. Each enum contains information on the name and
 * lipidCoreClass
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public enum LipidMainClasses {

  // Fatty Acyls

  FATTYACIDSANDCONJUGATES("Fatty acids and Conjugates", LipidCategories.FATTYACYLS),//
  FATTYAMIDES("Fatty amides", LipidCategories.FATTYACYLS),//
  FATTYESTERS("Fatty esters", LipidCategories.FATTYACYLS),//

  RHAMNOLIPIDS("Rhamnolipids", LipidCategories.FATTYACYLS), //

  // Glycerolipids
  MONORADYLGLYCEROLS("Monoradylglycerols", LipidCategories.GLYCEROLIPIDS), //
  DIRADYLGLYCEROLS("Diradylglycerols", LipidCategories.GLYCEROLIPIDS), //
  TRIRADYLGLYCEROLS("Triradylglycerols", LipidCategories.GLYCEROLIPIDS), //
  GLYCOSYLDIACYLGLYCEROLS("Glycosyldiacylglycerols", LipidCategories.GLYCEROLIPIDS), //
  GLYCOSYLMONOACYLGLYCEROLS("Glycosylmonoacylglycerols", LipidCategories.GLYCEROLIPIDS), //
  OTHERGLYCEROLIPIDS("Other glycerolipids", LipidCategories.GLYCEROLIPIDS), //

  // Glycerophospholipids
  PHOSPHATIDYLCHOLINE("Phosphatidylcholine", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOETHANOLAMINES("Glycerophosphoethanolamines",
      LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOSERINES("Glycerophosphoserines", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROLS("Glycerophosphoglycerols", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROPHOSPHATES("Glycerophosphoglycerophosphates",
      LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOINOSITOLS("Glycerophosphoinositols", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOINOSITOLGLYCANS("Glycerophosphoinositolglycans",
      LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHATES("Glycerophosphates", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  CARDIOLIPIN("Cardiolipin", LipidCategories.GLYCEROPHOSPHOLIPIDS), //
  CDPGLYCEROLS("CDP-Glycerols", LipidCategories.GLYCEROPHOSPHOLIPIDS), //

  // Sphingolipids
  SPHINGOIDBASES("Sphingoid bases", LipidCategories.SPHINGOLIPIDS),//
  CERAMIDES("Ceramides", LipidCategories.SPHINGOLIPIDS),//
  NEUTRALGLYCOSPHINGOLIPIDS("Neutral glycosphingolipids", LipidCategories.SPHINGOLIPIDS),//
  PHOSPHOSPHINGOLIPIDS("Phosphosphingolipids", LipidCategories.SPHINGOLIPIDS),//

  //Sterols
  STEROLS("Sterols", LipidCategories.STEROLLIPIDS);

  private final String name;
  private final LipidCategories lipidCategory;

  LipidMainClasses(String name, LipidCategories lipidCategory) {
    this.name = name;
    this.lipidCategory = lipidCategory;
  }

  public String getName() {
    return name;
  }

  public LipidCategories getLipidCategory() {
    return lipidCategory;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
