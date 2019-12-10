/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.MapUtils;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.IDType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;

/**
 * Map of all feature related data.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
@SuppressWarnings("rawtypes")
public class ModularFeatureListRow implements ModularDataModel {

  private final @Nonnull ModularFeatureList flist;
  private final ObservableMap<DataType, Object> map = FXCollections.observableMap(new HashMap<>());

  /**
   * this final map is used in the FeaturesType - only ModularFeatureListRow is supposed to change
   * this map see {@link #addPeak(RawDataFile, ModularFeature)}
   */
  private final Map<RawDataFile, ModularFeature> features;

  // buffert col charts and nodes
  private Map<String, Node> buffertColCharts = new HashMap<>();

  public ModularFeatureListRow(@Nonnull ModularFeatureList flist) {
    this.flist = flist;
    List<RawDataFile> raws = flist.getRawDataFiles();
    if (!raws.isEmpty()) {
      // init FeaturesType map (is final)
      HashMap<RawDataFile, ModularFeature> fmap = new HashMap<>(raws.size());
      for (RawDataFile r : raws)
        fmap.put(r, null);
      // wrap in fixed size not thread safe! TODO discuss
      // only allows change of existing elements
      features = MapUtils.fixedSizeMap(fmap);
    } else
      features = Collections.emptyMap();
  }

  /**
   * Constructor for row with only one raw data file.
   * 
   * @param flist
   * @param id
   * @param raw
   * @param p
   */
  public ModularFeatureListRow(@Nonnull ModularFeatureList flist, int id, RawDataFile raw,
      ModularFeature p) {
    this(flist);
    set(IDType.class, (id));
    addPeak(raw, p);
  }

  public ModularFeatureList getFeatureList() {
    return flist;
  }

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getTypes() {
    return flist.getRowTypes();
  }

  @Override
  public ObservableMap<DataType, Object> getMap() {
    return map;
  }

  public Stream<ModularFeature> streamFeatures() {
    return this.getFeatures().values().stream().filter(Objects::nonNull);
  }

  // Helper methods
  // most common data types
  public FeatureStatus getDetectionType() {
    return get(DetectionType.class).orElse(FeatureStatus.UNKNOWN);
  }

  public Double getMZ() {
    return get(MZType.class).orElse(-1d);
  }

  public Float getRT() {
    return get(RTType.class).orElse(-1f);
  }

  public double getHeight() {
    return get(HeightType.class).orElse(0f);
  }

  public double getArea() {
    return get(AreaType.class).orElse(0f);
  }

  public Map<RawDataFile, ModularFeature> getFeatures() {
    return get(FeaturesType.class).orElse(Collections.emptyMap());
  }

  /**
   * 
   * @param raw
   * @param f
   */
  public void addPeak(RawDataFile raw, ModularFeature f) {
    features.put(raw, f);
  }

  /**
   * Row ID or -1 if not present
   * 
   * @return
   */
  public int getID() {
    return get(IDType.class).orElse(-1);
  }

  public List<RawDataFile> getRawDataFiles() {
    return flist.getRawDataFiles();
  }

  public boolean hasFeature(ModularFeature feature) {
    return getFeatures().values().contains(feature);
  }

  public Node getBufferedColChart(String colname) {
    return buffertColCharts.get(colname);
  }

  public void addBufferedColChart(String colname, Node node) {
    buffertColCharts.put(colname, node);
  }

}
