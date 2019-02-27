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


}
