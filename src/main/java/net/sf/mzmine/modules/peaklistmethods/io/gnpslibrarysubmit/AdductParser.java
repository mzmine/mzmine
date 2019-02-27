package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

public class AdductParser {

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
    if (adduct.endsWith("+"))
      adduct = adduct.substring(0, adduct.length() - 1);

    // first -FRAGMENTS then +ADDUCT
    adduct = orderAdduct(adduct);

    System.out.println(old + " -> " + adduct);
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

    // only test if - and + are present
    if (!(adduct.contains("+") && adduct.contains("-")))
      return adduct + charge;

    // get all components


    // first -FRAGMENTS then +ADDUCT
    for (int i = adduct.length() - 1; i > MINDEX + 1; i--) {
      char c = adduct.charAt(i);
      // loss found
      if (c == '-') {

      }
    }
  }

  private class Component {
    String count, name, sign;

    public Component(String count, String name, String sign) {
      super();
      this.count = count;
      this.name = name;
      this.sign = sign;
    }

  }
}
