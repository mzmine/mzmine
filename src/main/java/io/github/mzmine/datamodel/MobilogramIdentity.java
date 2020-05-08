package io.github.mzmine.datamodel;

import javax.annotation.Nonnull;
import java.util.Map;

public interface MobilogramIdentity {
    /**
     * These variables define standard properties. The PROPERTY_NAME must be present in all instances
     * of PeakIdentity. It defines the value which is returned by the toString() method.
     */
    String PROPERTY_NAME = "Name";
    String PROPERTY_FORMULA = "Molecular formula";
    String PROPERTY_METHOD = "Identification method";
    String PROPERTY_ID = "ID";
    String PROPERTY_URL = "URL";
    String PROPERTY_SPECTRUM = "SPECTRUM";

    /**
     * Returns the value of the PROPERTY_NAME property. This value must always be set. Same value is
     * returned by the toString() method.
     *
     * @return Name
     */
    @Nonnull
    String getName();

    /**
     * Returns full, multi-line description of this identity, one property per line (key: value)
     *
     * @return Description
     */
    @Nonnull
    String getDescription();

    /**
     * Returns the value for a
     *
     * @param property
     * @return Description
     */
    @Nonnull
    String getPropertyValue(String property);

    /**
     * Returns all the properties in the form of a map key --> value
     *
     * @return Description
     */
    @Nonnull
    Map<String, String> getAllProperties();

    @Nonnull
    public Object clone();
}
