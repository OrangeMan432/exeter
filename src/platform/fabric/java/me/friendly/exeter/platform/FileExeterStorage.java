package me.friendly.exeter.platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * {@link ExeterStorage} backed by real files in a directory, for the Java Edition (Fabric) build.
 *
 * <p>The directory is created on construction so a fresh install does not fail on first read.
 */
public final class FileExeterStorage implements ExeterStorage {

  private final File directory;

  public FileExeterStorage(File directory) {
    this.directory = directory;
    if (!directory.exists() && !directory.mkdirs()) {
      System.err.println("[Exeter] Could not create config directory: " + directory);
    }
  }

  @Override
  public String read(String name) {
    File file = new File(directory, name);
    if (!file.isFile()) {
      return null;
    }
    try {
      return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      System.err.println("[Exeter] Failed to read " + name + ": " + e.getMessage());
      return null;
    }
  }

  @Override
  public void write(String name, String contents) {
    try {
      File file = new File(directory, name);
      Files.writeString(file.toPath(), contents == null ? "" : contents, StandardCharsets.UTF_8);
    } catch (IOException e) {
      System.err.println("[Exeter] Failed to write " + name + ": " + e.getMessage());
    }
  }

  @Override
  public void delete(String name) {
    File file = new File(directory, name);
    if (file.isFile() && !file.delete()) {
      System.err.println("[Exeter] Failed to delete " + name);
    }
  }
}
