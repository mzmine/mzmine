package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The catalog (index.json) of the community lipid library.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LipidLibraryCatalog(int formatVersion, @Nullable String generated,
                                  @NotNull List<LipidLibraryEntry> classes) {

}
