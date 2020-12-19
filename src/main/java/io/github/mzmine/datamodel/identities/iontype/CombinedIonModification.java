package io.github.mzmine.datamodel.identities.iontype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombinedIonModification extends IonModification {

  private IonModification[] adducts;


  /**
   * fast creation of combined adducts
   * 
   * @param adduct
   */
  public CombinedIonModification(IonModification... adduct) {
    super();

    // all adducts
    List<IonModification> ad = new ArrayList<IonModification>();
    for (int i = 0; i < adduct.length; i++) {
      for (IonModification n : adduct[i].getAdducts()) {
        ad.add(n);
      }
    }
    adducts = ad.toArray(new IonModification[ad.size()]);
    Arrays.sort(adducts);
    type = IonModificationType.getType(adducts);

    double md = 0;
    int z = 0;
    for (int i = 0; i < adducts.length; i++) {
      IonModification a = adducts[i];
      md += a.getMass();
      z += a.getCharge();
    }
    charge = z;
    mass = md;
    this.parsedName = parseName();
  }

  @Override
  public IonModification[] getAdducts() {
    return adducts;
  }

  @Override
  public int getNumberOfAdducts() {
    return adducts == null ? 0 : adducts.length;
  }

  @Override
  public IonModification createOpposite() {
    IonModification[] mod =
        Arrays.stream(adducts).map(IonModification::createOpposite).toArray(IonModification[]::new);
    return new CombinedIonModification(mod);
  }

  /**
   * 
   * @return array of names
   */
  @Override
  public String[] getRawNames() {
    if (adducts == null)
      return new String[0];
    String[] names = new String[adducts.length];
    for (int i = 0; i < names.length; i++)
      names[i] = adducts[i].getName();
    return names;
  }

  @Override
  public String parseName() {
    String nname = "";
    if (adducts != null) {
      String s = null;
      int counter = 0;
      for (int i = 0; i < adducts.length; i++) {
        String cs = adducts[i].getName();
        if (s == null) {
          s = cs;
          counter = 1;
        } else if (s.equals(cs))
          counter++;
        else {
          String sign = (adducts[i - 1].getMass() < 0 ? "-" : "+");
          String counterS = counter > 1 ? String.valueOf(counter) : "";
          nname += sign + counterS + s;
          s = cs;
          counter = 1;
        }
      }
      String sign = (adducts[adducts.length - 1].getMass() < 0 ? "-" : "+");
      String counterS = counter > 1 ? String.valueOf(counter) : "";
      nname += sign + counterS + s;
    }
    return nname;
  }

  @Override
  public IonModification remove(IonModification type) {
    List<IonModification> newList = new ArrayList<>();
    for (IonModification m : this.getAdducts())
      newList.add(m);

    for (IonModification m : type.getAdducts())
      newList.remove(m);

    if (newList.isEmpty())
      return null;
    else if (newList.size() == 1)
      return new IonModification(newList.get(0));
    else
      return new CombinedIonModification(newList.toArray(new IonModification[newList.size()]));
  }


  /**
   * this or any sub modification (for combined) equals to mod?
   * 
   * @param mod
   * @return
   */
  @Override
  public boolean contains(IonModification mod) {
    return Arrays.stream(getAdducts()).anyMatch(m -> m.equals(mod));
  }


  /**
   * Specifies whether this object limits further modification
   * 
   * @return
   */
  @Override
  public boolean hasModificationLimit() {
    return Arrays.stream(adducts).anyMatch(IonModification::hasModificationLimit);
  }

  @Override
  public int getModificationLimit() {
    return Arrays.stream(adducts).mapToInt(IonModification::getModificationLimit)
        .filter(limit -> limit == -1).min().orElse(-1);
  }

  /**
   * Number of sub IonModifications
   * 
   * @return
   */
  @Override
  public int getAdductsCount() {
    return adducts.length;
  }
}
