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

package io.github.mzmine.modules.visualization.molstructure;

import java.awt.Color;
import java.io.Serializable;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.renderer.color.IAtomColorer;

public class MZmine2DAtomColors implements IAtomColorer, Serializable {

  private static final long serialVersionUID = 1641485932719499462L;
  // bluish green for all other like Na Li Fe B
  private static final Color OTHER_ELEMENTS_METALS_COLOR = new Color(0x02845D);

  @Override
  @Nullable
  public Color getAtomColor(IAtom atom) {
    Elements elem = Elements.ofString(atom.getSymbol());
    if (elem == Elements.Unknown) {
      elem = Elements.ofNumber(atom.getAtomicNumber());
    }

//    final int group = elem.group();

    return switch (elem) {
      // only used when stereochemistry H are added, gray to put in background
      case Carbon -> Color.BLACK; // or use null for default color
      case Hydrogen -> new Color(0x706F6F);
      case Silicon -> new Color(0x3C3C3B);
      // N and P both blue for same group
      case Nitrogen -> new Color(0x2E5697); // blue
      case Phosphorus -> new Color(0x2482A8); // sky blue
      // O and S both violet redish for same group
//      case Oxygen -> new Color(0xFF0000); // reddish purple
      case Oxygen -> new Color(0xC02B84); // reddish purple
      case Sulfur, Selenium -> new Color(0x4D2A6D); // dark violet
      // Halogens group 7 all orange
      case Fluorine -> new Color(0xEF8B27); // orange
      case Chlorine, Bromine, Iodine -> new Color(0xD26027); // darker orange
      // metals and other elements all green, should be different enough to black bond lines
      default -> OTHER_ELEMENTS_METALS_COLOR;
    };
  }

}
