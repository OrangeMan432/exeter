package me.larp.client.config;

import java.io.File;
import me.larp.api.interfaces.Labeled;
import me.larp.client.core.Larp;

/** An object that is saved to a file, and an implementation of Labeled */
public abstract class Config implements Labeled {
  private final String label;
  private final File file;
  private final File directory;

  /**
   * Instantiates label, file, and directory. registers current Config instance with ConfigManager
   *
   * @param label the name of the file that will be created and saved to
   */
  public Config(String label) {
    this.label = label;
    this.directory = Larp.getInstance().getDirectory();
    this.file = new File(this.directory, label);
    Larp.getInstance().getConfigManager().register(this);
  }

  /**
   * Instantiates label, file, and directory. registers current Config instance with ConfigManager
   *
   * @param label the name of the file that will be created and saved to
   * @param directory the directory that will be saved to
   */
  public Config(String label, File directory) {
    this.label = label;
    this.directory = directory;
    this.file = new File(directory, label);
    Larp.getInstance().getConfigManager().register(this);
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  public File getDirectory() {
    return this.directory;
  }

  public File getFile() {
    return this.file;
  }

  public abstract void load(Object... var1);

  public abstract void save(Object... var1);
}
