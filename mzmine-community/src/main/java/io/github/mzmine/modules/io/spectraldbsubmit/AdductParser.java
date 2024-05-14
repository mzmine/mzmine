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

package io.github.mzmine.modules.io.spectraldbsubmit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdductParser {

  /**
   * 
   * @param adduct
   * @return The formatted adduct or an empty String if input was wrong
   */
  public static String parse(String adduct) {
    final String old = adduct;
    if (adduct == null || adduct.isEmpty())
      return "";

    // delete all spaces
    adduct = adduct.replaceAll(" ", "");

    // valid?
    if (!isValidAdduct(adduct))
      return "";

    // ends with single charge? remove
    if (!adduct.equals("M+") && adduct.endsWith("+"))
      adduct = adduct.substring(0, adduct.length() - 1);

    // first -FRAGMENTS then +ADDUCT
    adduct = orderAdduct(adduct);
    return adduct;
  }

  /**
   * no spaces, charge can be left out for +1 charge <br>
   * xM-FRAG+ADDUCT+CHARGE <br>
   * 2M+H <br>
   * M+2H+2 (for doubly charged) <br>
   * M-H2O+Na <br>
   * M+Cl- <br>
   * 
   * @param adduct
   * @return
   */
  private static boolean isValidAdduct(String adduct) {
    final String old = adduct;
    // Starts with M or digit
    if (adduct == null || adduct.isEmpty()
        || !(adduct.startsWith("M") || Character.isDigit(adduct.charAt(0))))
      return false;
    else
      return true;
  }

  /**
   * Sort: losses first then additions (alphabetically)
   * 
   * @param adduct
   * @return
   */
  private static String orderAdduct(String adduct) {
    final int MINDEX = adduct.indexOf("M");
    // subtract and store charge
    String charge = "";
    for (int i = adduct.length() - 1; i > MINDEX; i--) {
      char c = adduct.charAt(i);
      // charge found
      if (c == '+' || c == '-') {
        charge = adduct.substring(i);
        adduct = adduct.substring(0, i);
        break;
      }
      // no charge defined
      else if (!Character.isDigit(c))
        break;
    }

    // get all components
    List<Component> list = getCompList(adduct);
    // sort by M - - + + ... charge
    Collections.sort(list);

    return list.stream().map(Component::getFullName).collect(Collectors.joining()) + charge;
  }

  private static List<Component> getCompList(String adduct) {
    List<Component> list = new ArrayList<>();
    int lasti = 0;
    for (int i = 0; i < adduct.length(); i++) {
      if (adduct.charAt(i) == '+' || adduct.charAt(i) == '-') {
        String c = adduct.substring(lasti, i);
        list.add(new Component(c));
        lasti = i;
      }
    }
    String c = adduct.substring(lasti);
    list.add(new Component(c));
    return list;
  }

  private static class Component implements Comparable<Component> {
    String count, name, sign;

    Component(String comp) {
      if (comp.startsWith("+") || comp.startsWith("-")) {
        sign = comp.substring(0, 1);
        comp = comp.substring(1);
      } else
        sign = "";
      int i = 0;
      for (i = 0; i < comp.length() && Character.isDigit(comp.charAt(i)); i++) {
      }
      count = comp.substring(0, i);
      name = comp.substring(i);
    }

    Component(String count, String name, String sign) {
      super();
      this.count = count;
      this.name = name;
      this.sign = sign;
    }

    public String getFullName() {
      return sign + count + name;
    }

    @Override
    public int compareTo(Component o) {
      // M is always the first
      if (name.equals("M") && sign.isEmpty())
        return -1;
      if (o.name.equals("M") && o.sign.isEmpty())
        return 1;
      // minus smaller than plus
      if (!o.sign.equals(this.sign)) {
        if (this.sign.equals("-"))
          return -1;
        if (o.sign.equals("-"))
          return 1;
      }
      // then alphabetically
      return this.name.compareToIgnoreCase(o.name);
    }
  }
}
