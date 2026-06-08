package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.util.RawDataFileType;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * During import, some raw files may be renamed/remapped due to conversion by MSConvert, or, e.g., a
 * .tdf selected instead of the .d folder. This is a record to keep track of what is happening
 * during the data import and to check for files that are already imported into the project.
 */
public record ImportFile(@NotNull File originalFile, @Nullable RawDataFileType type,
                         @NotNull File importedFile) {

}
