package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.util.RawDataFileType;
import java.io.File;
import org.jetbrains.annotations.NotNull;

public record ImportFile(@NotNull File originalFile, @NotNull RawDataFileType type,
                         @NotNull File importAs) {

}
