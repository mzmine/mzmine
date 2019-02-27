package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

public class TestAddu {

  public static void main(String[] args) {
    String[] adducts =
        new String[] {"M+", "M-", "M+H", "3M + H", "M-H2O+H", "M+Zn+2", "M+H+", "M+2H+2", "M+Cl-",
            // needs reshuffle -> first - then +
            "M+H-H2O", "M+Ca-H"};
    String[] wrong = new String[] {"zM+H"};

    for (String a : adducts)
      isValidAdduct(a);
    System.out.println("Testing wrong adducts");
    for (String a : wrong)
      isValidAdduct(a);
  }


<<<<<<< HEAD
=======
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
    if (adduct == null || adduct.isEmpty())
      return false;

    // delete all spaces
    adduct = adduct.replaceAll(" ", "");

    // Starts with M or digit
    if (!(adduct.startsWith("M") || Character.isDigit(adduct.charAt(0))))
      return false;

    // ends with single charge? remove
    if (adduct.endsWith("+"))
      adduct = adduct.substring(0, adduct.length() - 1);

    // first -FRAGMENTS then +ADDUCT
    orderAdduct(adduct);


    System.out.println(old + " -> " + adduct);
    return false;
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

    // first -FRAGMENTS then +ADDUCT
    for (int i = adduct.length() - 1; i > 0; i--) {
      char c = adduct.charAt(i);
      // loss found
      if (c == '-') {

      }
    }
  }
>>>>>>> branch 'tomasmaster_submit_library_entry_gnps' of https://github.com/robinschmid/mzmine2.git
}
