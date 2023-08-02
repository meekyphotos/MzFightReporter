package org.vmy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Parameters {

    private static final String gw2EIExe = "GuildWars2EliteInsights.exe";
    private static Parameters instance = null;
    public String repoUrl = "https://api.github.com/repos/Swedemon/MzFightReporter/releases/latest";
    public String homeDir = "";
    public String gw2EISettingsDirectory = "Settings\\";
    public String defaultLogFolder =
      System.getenv("USERPROFILE") + "\\Documents\\Guild Wars 2\\addons\\arcdps\\arcdps.cbtlogs\\";
    public String customLogFolder = "";
    public String discordThumbnail = "https://i.imgur.com/KKddNgl.png";
    public String discordWebhook = "";
    public String twitchChannelName = "";
    public String twitchBotToken = "";
    public String jarName = "";
    public int maxWvwUpload = 10;
    public int graphPlayerLimit = 20;
    public boolean showDamageGraph = true;
    public boolean showDamage = true;
    public boolean showCleanses = true;
    public boolean showStrips = true;
    public boolean showSpikeDmg = true;
    public boolean showDefensiveBoons = true;
    public boolean showCCs = true;
    public boolean showQuickReport = true;
    public boolean showHeals = true;
    public String parseBotParameters = "-Xmx1024M";
    public String baseDirectoryGW2EI = "GW2EI-7-7-22";

    private Parameters() {
        loadResources();
    }

    public static Parameters getInstance() {
        if (instance == null) {
            instance = new Parameters();
        }
        return instance;
    }

    public File eliteInsightFile() {
        return new File (new File(homeDir, baseDirectoryGW2EI), gw2EIExe);
    }

    public File eliteInsightSettings() {
        return new File (new File(homeDir, baseDirectoryGW2EI), gw2EISettingsDirectory);
    }

    private void loadResources() {
        FileInputStream file = null;
        try {
            final String path = "config.properties";
            final Properties prop = new Properties();
            file = new FileInputStream(path);
            prop.load(file);

            //set properties
            customLogFolder = prop.getProperty("customLogFolder");
            discordThumbnail = prop.getProperty("discordThumbnail", discordThumbnail);
            discordWebhook = prop.getProperty("discordWebhook", discordWebhook);
            twitchChannelName = prop.getProperty("twitchChannelName", twitchChannelName);
            twitchBotToken = prop.getProperty("twitchBotToken", twitchBotToken);
            jarName = prop.getProperty("jarName", jarName);
            parseBotParameters = prop.getProperty("parseBotParameters", "-Xmx1024M");
            baseDirectoryGW2EI = prop.getProperty("baseDirectoryGW2EI", baseDirectoryGW2EI);
            maxWvwUpload = Integer.parseInt(prop.getProperty("maxWvwUpload", String.valueOf(maxWvwUpload)));
            graphPlayerLimit = Integer.parseInt(prop.getProperty("graphPlayerLimit", String.valueOf(graphPlayerLimit)));
            showDamageGraph = Boolean.valueOf(prop.getProperty("showDamageGraph", "true"));
            showDamage = Boolean.valueOf(prop.getProperty("showDamage", "true"));
            showCleanses = Boolean.valueOf(prop.getProperty("showCleanses", "true"));
            showStrips = Boolean.valueOf(prop.getProperty("showStrips", "true"));
            showSpikeDmg = Boolean.valueOf(prop.getProperty("showSpikeDmg", "true"));
            showDefensiveBoons = Boolean.valueOf(prop.getProperty("showDefensiveBoons", "true"));
            showCCs = Boolean.valueOf(prop.getProperty("showCCs", "true"));
            showQuickReport = Boolean.valueOf(prop.getProperty("showQuickReport", "true"));
            showHeals = Boolean.valueOf(prop.getProperty("showHeals", "true"));
        } catch (final Exception e) {
            System.out.println("Warning: Unable to read config.properties.  Using default values.");
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (final IOException e) {
                }
            }
        }
    }
}
