package example.samples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/** UTF-8 file helpers compatible with Java 8. */
final class TestSupport {
  private TestSupport() {}

  static String readString(Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  static Path writeString(Path path, CharSequence text, OpenOption... options) throws IOException {
    return Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8), options);
  }
}
