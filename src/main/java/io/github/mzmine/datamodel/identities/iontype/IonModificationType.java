package io.github.mzmine.datamodel.identities.iontype;

public enum IonModificationType {
  ADDUCT, UNDEFINED_ADDUCT, NEUTRAL_LOSS, CLUSTER, ISOTOPE, UNKNOWN, MIXED;
  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }

  /**
   * The common type or MIXED
   * 
   * @param adducts
   * @return
   */
  public static IonModificationType getType(IonModification[] adducts) {
    IonModificationType t = adducts[0].getType();
    for (int i = 1; i < adducts.length; i++)
      if (!t.equals(adducts[i].getType()))
        return IonModificationType.MIXED;
    return t;
  }
}
