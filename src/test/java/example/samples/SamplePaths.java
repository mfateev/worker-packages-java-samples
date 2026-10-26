package example.samples;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves the sample build directory supplied by Gradle to tests and the restart demo. */
final class SamplePaths {
  private SamplePaths() {}

  static Path root() {
    return Paths.get(System.getProperty("samples.rootDir", ".")).toAbsolutePath().normalize();
  }
}
