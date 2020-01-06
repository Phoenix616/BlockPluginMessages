package de.themoep.blockpluginmessages;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class YamlConfig {
    protected Configuration cfg;
    protected final static ConfigurationProvider ymlCfg = ConfigurationProvider.getProvider( YamlConfiguration.class );

    protected File configFile;

    private Plugin plugin;
    
    /**
     * read configuration into memory
     * @param plugin The plugin this config belongs to
     * @param path The path to the config file
     * @throws IOException
     */
    public YamlConfig(Plugin plugin, String path) throws IOException {
        this(plugin, new File(path));
    }

    /**
     * read configuration into memory
     * @param plugin The plugin this config belongs to
     * @param file The config file
     * @throws IOException
     */
    public YamlConfig(Plugin plugin, File file) throws IOException {
        this.plugin = plugin;

        configFile = file;

        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            configFile.createNewFile();
            cfg = ymlCfg.load(configFile);

            createDefaultConfig();
        } else {
            cfg = ymlCfg.load(configFile);
        }
    }

    /**
     * save configuration to disk
     */
    public void save() {
        try {
            ymlCfg.save(cfg, configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to save configuration at " + configFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    public void createDefaultConfig() {
        cfg = ymlCfg.load(new InputStreamReader(plugin.getResourceAsStream(configFile.getName())));

        save();
    }    
    
    /**
     * deletes configuration file
     */
    public void removeConfig() {
        configFile.delete();
    }

    public String getString(String path) {
        return cfg.getString(path);
    }

    public String getString(String path, String def) {
        return cfg.getString(path, def);
    }

    public List<String> getStringList(String path) {
        return cfg.getStringList(path);
    }

    public Configuration getSection(String path) {
        return cfg.getSection(path);
    }
}