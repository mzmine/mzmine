package io.github.mzmine.modules.io.projectload.version_3_0;

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.features.types.DataType;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Contains a mapping of all {@link DataType} id and an instance of the respective data type.
 */
public class DataTypes {

  private static final Logger logger = Logger.getLogger(DataTypes.class.getName());

  private static final HashMap<String, DataType<?>> map = new HashMap<>();

  static {
    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.datamodel.features.types")
          .forEach(classInfo -> {
            try {
              Object o = classInfo.load().getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                map.put(dt.getUniqueID(), dt);
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//               can go silent
//              logger.log(Level.INFO, e.getMessage(), e);
            }
          });
    } catch (IOException e) {
      logger.severe("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }

  private DataTypes() {
  }

  @Nullable
  public static DataType<?> getTypeForId(String uniqueId) {
    return map.get(uniqueId);
  }
}
