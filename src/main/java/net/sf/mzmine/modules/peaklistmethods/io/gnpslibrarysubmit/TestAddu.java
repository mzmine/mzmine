package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

public class TestAddu {

  public static void main(String[] args) {
    String[] adducts =
        new String[] {"M+", "M-", "M+H", "3M + H", "M-H2O+H", "M+Zn+2", "M+H+", "M+2H+2", "M+Cl-",
            // needs reshuffle -> first - then +
            "M+H-H2O", "M+Ca-H", "3M+Ca-H+2", "3M+ACN-H2O+Ca-H+", "2M + A-B+C-D+E+2",
            "2M + D-A+E-B+C+2"};
    String[] wrong = new String[] {"zM+H"};

    for (String a : adducts)
      isValidAdduct(a);
    System.out.println("Testing wrong adducts");
    for (String a : wrong)
      isValidAdduct(a);
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
    adduct = AdductParser.parse(adduct);

    System.out.println(old + "   ->   " + adduct);
    return !adduct.isEmpty();
  }

}
