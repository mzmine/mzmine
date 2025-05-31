/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.javafx.metadatatreeview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MetadataWrapper<T, G> extends TreeItem<T> {

  private final StringProperty titleProperty;
  @NotNull
  private final Function<@Nullable G, @Nullable String> groupingSupplier;
  private final boolean isGroup;

  public static MetadataWrapper<RawDataFile, MetadataColumn<?>> of(
      @NotNull final RawDataFile file) {
    return new MetadataWrapper<>(file, new SimpleStringProperty(file.getName()),
        col -> Objects.requireNonNullElse(ProjectService.getMetadata().getValue(col, file), "")
            .toString());
  }

  public static MetadataWrapper<FeatureList, List<RawDataFile>> of(
      @NotNull final FeatureList flist) {
    return new MetadataWrapper<>(flist, new SimpleStringProperty(flist.getName()),
        l -> l.size() > 1 ? "Aligned feature lists" : l.getFirst().getName());
  }

  public MetadataWrapper(@Nullable T value, @NotNull final StringProperty title,
      @NotNull Function<@Nullable G, @Nullable String> groupingSupplier) {
    this(value, title, groupingSupplier, value == null);
  }

  public MetadataWrapper(@Nullable T value, @NotNull final StringProperty titleProperty,
      @NotNull Function<@Nullable G, @Nullable String> groupingSupplier, final boolean isGroup) {
    super(value);
    this.titleProperty = titleProperty;
    this.groupingSupplier = groupingSupplier;
    this.isGroup = isGroup;
  }

  @Nullable
  public String getGrouping(@Nullable G groupBy) {
    return groupingSupplier.apply(groupBy);
  }

  public boolean isGroup() {
    return isGroup;
  }

  public String getTitle() {
    return titleProperty.get();
  }
}
