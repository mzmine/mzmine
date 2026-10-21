package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fingerprints raw measurements and metadata used by a native normalization calculation. */
public final class NormalizationSourceRevision {
  private NormalizationSourceRevision() { }

  public static @NotNull String capture(@NotNull final FeatureList list,
      @NotNull final MetadataTable metadata, @NotNull final BooleanSupplier canceled) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (final DataOutputStream data = new DataOutputStream(
          new DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
        final List<MetadataColumn<?>> columns = metadata.getColumns().stream()
            .sorted(Comparator.comparing((MetadataColumn<?> column) -> column.getTitle())
                .thenComparing(column -> column.getType().name()))
            .toList();
        final List<io.github.mzmine.datamodel.RawDataFile> raws = list.getRawDataFiles().stream()
            .sorted(Comparator.comparing(io.github.mzmine.datamodel.RawDataFile::getName)).toList();
        data.writeInt(raws.size());
        for (final var raw : raws) {
          checkCanceled(canceled);
          data.writeUTF(raw.getName());
          writeDate(data, MetadataTableUtils.getRunDate(metadata, raw));
        }
        data.writeInt(columns.size());
        for (final MetadataColumn<?> column : columns) {
          checkCanceled(canceled);
          data.writeUTF(column.getTitle());
          data.writeUTF(column.getType().name());
          for (final var raw : raws) {
            checkCanceled(canceled);
            writeMetadataValue(data, metadata.getValue(column, raw));
          }
        }
        data.writeInt(list.getNumberOfRows());
        for (final FeatureListRow row : list.getRows().stream()
            .sorted(Comparator.comparingInt(FeatureListRow::getID)).toList()) {
          checkCanceled(canceled);
          data.writeInt(row.getID());
          for (final var raw : raws) {
            checkCanceled(canceled);
            final var feature = row.getFeature(raw);
            data.writeBoolean(feature != null);
            if (feature == null) continue;
            data.writeUTF(feature.getFeatureStatus().name());
            writeDouble(data, feature.getMZ());
            writeFloat(data, feature.getRT());
            writeFloat(data, feature.getArea());
            writeFloat(data, feature.getHeight());
          }
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (final IOException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Cannot fingerprint normalization inputs.", exception);
    }
  }

  private static void writeDate(@NotNull final DataOutputStream data,
      final @Nullable java.time.LocalDateTime value) throws IOException {
    data.writeBoolean(value != null);
    if (value != null) {
      data.writeLong(value.toEpochSecond(java.time.ZoneOffset.UTC));
      data.writeInt(value.getNano());
    }
  }

  private static void writeMetadataValue(@NotNull final DataOutputStream data, final Object value)
      throws IOException {
    data.writeBoolean(value != null);
    if (value == null) return;
    if (value instanceof Number number) {
      data.writeByte(1);
      data.writeDouble(number.doubleValue());
    } else if (value instanceof java.time.LocalDateTime date) {
      data.writeByte(2);
      writeDate(data, date);
    } else {
      data.writeByte(3);
      data.writeUTF(value.toString());
    }
  }

  private static void writeFloat(@NotNull final DataOutputStream data, final Float value)
      throws IOException {
    data.writeBoolean(value != null);
    if (value != null) data.writeFloat(value);
  }

  private static void writeDouble(@NotNull final DataOutputStream data, final Double value)
      throws IOException {
    data.writeBoolean(value != null);
    if (value != null) data.writeDouble(value);
  }

  private static void checkCanceled(@NotNull final BooleanSupplier canceled) {
    if (canceled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
  }
}
