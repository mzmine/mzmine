/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.javafx.components.factories;

import javafx.scene.text.Text;

/**
 * Citation style PLOS one
 */
public enum ArticleReferences {
  MZMINE3("Schmid R, Heuckeroth S, Korf A et al. Nat Biotechnol. 2023;41: 447–449.",
      "https://www.nature.com/articles/s41587-023-01690-2"),

  GNPS("Wang M, Carver JJ et al. Nat Biotechnol. 2016;34: 828–837.",
      "https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.html"),

  FBMN("Nothias L-F, Petras D, Schmid R et al. Nat Methods. 2020;17: 905–908.",
      "https://www.nature.com/articles/s41592-020-0933-6"),

  IIMN("Schmid R, Petras D, Nothias L-F et al. Nat Commun. 2021;12: 3832.",
      "https://www.nature.com/articles/s41467-021-23953-9"),

  MASST("Wang M, Jarmusch A, Vargas F et al. Nat Biotechnol. 2020;38: 23–26.",
      "https://doi.org/10.1038/s41587-019-0375-9"),

  SIRIUS4("Dührkop K, Fleischauer M, Ludwig M et al. Nat Methods. 2019;16: 299–302.",
      "http://dx.doi.org/10.1038/s41592-019-0344-8"),

  MS2DEEPSCORE(
      "de Jonge N, Joas D, Truong LJ, van der Hooft JJJ, Huber F bioRxiv 2024.03.25.586580",
      "https://doi.org/10.1101/2024.03.25.586580"),

  DREAMS(
      "Bushuiev R, Bushuiev A, Samusevich R, Brungs C, Sivic J, Pluskal T chemRxiv 10.26434/chemrxiv-2023-kss3r-v2",
      "https://doi.org/10.26434/chemrxiv-2023-kss3r-v2");

  private final String content;
  private final String link;

  ArticleReferences(final String content, final String link) {
    this.content = content;
    this.link = link;
  }

  public String getContent() {
    return content;
  }

  public String getLink() {
    return link;
  }

  public Text hyperlinkText() {
    return FxTexts.hyperlinkText(content, link);
  }

}
