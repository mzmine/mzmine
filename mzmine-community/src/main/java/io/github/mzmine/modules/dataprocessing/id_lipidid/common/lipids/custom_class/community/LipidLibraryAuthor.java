package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Author of a lipid class in the community lipid library.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LipidLibraryAuthor(@Nullable String name, @Nullable String orcid,
                                 @Nullable String github) {

}
