# Updating gradle

https://mzmine.github.io/mzmine_documentation/coding/upgrade_jdk_version.html#2-update-gradle-version-in-the-wrapper

https://docs.gradle.org/current/userguide/compatibility.html

This command will replace the gradle wrapper:

```bash
./gradlew wrapper --gradle-version 9.1.0
```

# Updating jdk

- Change jdk version in libs.versions.toml
- Change jdk version in [build.gradle.kts](convention-plugins/java-convention/build.gradle.kts)
- In IntelliJ make sure Project structure settings are pointing the correct jdk and that all modules
  use the project SDK