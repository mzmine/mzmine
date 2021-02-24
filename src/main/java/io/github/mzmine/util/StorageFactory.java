package io.github.mzmine.util;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javax.annotation.Nonnull;

public class StorageFactory {

  private static final ObservableMap<MemoryMapStorage, List<MappedByteBuffer>> storagesAndBufers =
      FXCollections.synchronizedObservableMap(FXCollections.observableMap(new HashMap<>()));

  public static MemoryMapStorage getNewMemoryMapStorage() {
    MemoryMapStorage storage = new MemoryMapStorage();
    registerNewMemoryMapStorage(storage);
    return storage;
  }

  /*public static MemoryMapStorage getStorageForNewFeatureList(ModularFeatureList newflist) {
    if()
    newflist.getRawDataFiles()
  }*/

  /**
   * Registers a new {@link MemoryMapStorage} to this class.
   *
   * @param instance
   */
  static void registerNewMemoryMapStorage(@Nonnull MemoryMapStorage instance) {
    storagesAndBufers.put(instance, new ArrayList<>());
  }

  static void registerNewByteBuffer(@Nonnull MemoryMapStorage storage, @Nonnull MappedByteBuffer instance) {
    List<MappedByteBuffer> bufferList = storagesAndBufers.computeIfAbsent(storage, key -> new ArrayList<>());
    bufferList.add(instance);
  }

}

