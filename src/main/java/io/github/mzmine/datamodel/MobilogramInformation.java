package io.github.mzmine.datamodel;

import javax.annotation.Nonnull;
import java.util.Map;

public interface MobilogramInformation {
    /**
     * Returns the value of a property
     *
     * @param property name
     * @return
     */

    @Nonnull
    String getPropertyValue(String property);

    @Nonnull
    String getPropertyValue(String property, String defaultValue);

    /**
     * Returns all the properties in the form of a map <key, value>
     *
     * @return
     */

    @Nonnull
    Map<String, String> getAllProperties();

    /**
     * Returns a copy of PeakInformation object
     *
     * @return
     */

    @Nonnull
    public Object clone();
}
