package org.vmy;

import org.vmy.util.FightReport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FileWatcher {

    private final HashMap<String, File> fileMap = new HashMap<>();
    private final HashMap<String, File> changeMap = new HashMap<>();

    public static void main(final String[] args) throws Exception {
        final Parameters p = Parameters.getInstance();

        System.out.println("\n*** MzFightReporter ***\n");
        System.out.println("Note:\n   You should see a dot printed below every few seconds indicating ArcDps log polling.\n"
                           + "   You can change settings in the config properties file at the install location.\n");

        if (args.length > 1) {
            p.homeDir = args[1];
        }

        System.out.println("homeDir=" + p.homeDir);
        System.out.println("defaultLogFolder=" + p.defaultLogFolder);
        System.out.println("customLogFolder=" + p.customLogFolder);
        System.out.print("showDamageGraph=" + p.showDamageGraph);
        System.out.print(" showDamage=" + p.showDamage);
        System.out.print(" showCleanses=" + p.showCleanses);
        System.out.print(" showStrips=" + p.showStrips);
        System.out.print(" showSpikeDmg=" + p.showSpikeDmg);
        System.out.print(" showDefensiveBoons=" + p.showDefensiveBoons);
        System.out.print(" showCCs=" + p.showCCs);
        System.out.println(" showQuickReport=" + p.showQuickReport);
        System.out.println("discordThumbnail=" + p.discordThumbnail);
        System.out.println("discordWebhook=(" + p.discordWebhook.length() + " characters)");
        System.out.println("twitchChannelName=" + p.twitchChannelName);
        System.out.println("twitchBotToken=(" + p.twitchBotToken.length() + " characters)");
        System.out.println("jarName=" + p.jarName);
        System.out.println("maxWvwUpload=" + p.maxWvwUpload);
        System.out.println("graphPlayerLimit=" + p.graphPlayerLimit);
        System.out.println();

        if (p.discordWebhook == null || p.discordWebhook.isEmpty()) {
            System.out.println("ERROR: Discord webhook is missing.  Review README.txt for install instructions.");
            System.exit(1);
        }

        new FileWatcher().run();
    }

    public void run() throws Exception {
        final Parameters p = Parameters.getInstance();

        if (new File(p.homeDir + File.separator + p.gw2EIExe_New).exists()) {
            System.out.println("Detected GuildWars2EliteInsights Application.");
        } else {
            System.out.println("Failure to detect GuildWars2EliteInsights application at: " + p.homeDir + File.separator + p.gw2EIExe_New);
        }

        final File folder = new File(p.customLogFolder);
        final File defaultFolder = new File(p.defaultLogFolder);

        //loop to await folder detection
        System.out.println("Parent folder(s) configured to monitor ArcDps log files:");
        if (p.customLogFolder != null && p.customLogFolder.length() > 0) {
            System.out.println("   > " + folder.getAbsolutePath());
        }
        System.out.println("   > " + defaultFolder.getAbsolutePath());
        while (true) {
            if (folder.exists() || defaultFolder.exists()) {
                System.out.println("OK");
                break;
            }
            Thread.sleep(10000L);
            System.out.print(".");
        }

        List<File> listOfFiles = listLogFiles();
        listOfFiles.forEach(f -> fileMap.put(f.getAbsolutePath(), f));

        System.out.println("Monitoring ArcDps log files.");

        //continuous file monitor loop
        while (true) {

            //short pause
            Thread.sleep(2000L);

            //update map of all files
            listOfFiles = listLogFiles();

            //find any new file
            final File f = locateNewFile(listOfFiles);

            //monitor file for completion then process
            if (f != null) {
                final String fullFilePath = f.getAbsolutePath();
                fileMap.put(fullFilePath, f);
                long lastModified = f.lastModified();
                for (int i = 0; i < 200; i++) { //max retries
                    Thread.sleep(500L);
                    if (!f.exists()) {
                        System.out.println("File was removed.");
                        break; //exit loop
                    } else if (lastModified == f.lastModified()) {
                        System.out.println("Invoking GW2EI...");
                        final String confFolder = p.homeDir + p.gw2EISettings;
                        String parseConfig = confFolder + "wvwupload.conf";
                        //parse json
                        final ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "/b", "/belownormal",
                                                                     "/wait", "." + p.gw2EIExe_New, "-c", parseConfig, fullFilePath
                        );
                        pb.directory(new File(p.homeDir));
                        pb.inheritIO();
                        final Process p1 = pb.start();
                        p1.waitFor();
                        System.out.println("GW2EI Parse Status (0=success): " + p1.exitValue());

                        final File logFile = new File(fullFilePath.substring(0, fullFilePath.lastIndexOf('.')) + ".log");
                        final File jsonFile = new File(fullFilePath.substring(0, fullFilePath.lastIndexOf('.')) + "_detailed_wvw_kill.json");
                        if (jsonFile.exists()) {

                            //call parsebot
                            System.out.println("Generating FightReport...");
                            final ProcessBuilder pb2 = new ProcessBuilder("cmd", "/c", "start", "/b", "/belownormal",
                                                                          "/wait", "java", p.parseBotParameters, "-jar", p.jarName, "ParseBot", jsonFile.getAbsolutePath(),
                                                                          logFile.getAbsolutePath(), p.homeDir
                            );
                            pb2.inheritIO();
                            pb2.directory(new File(p.homeDir));
                            final Process p2 = pb2.start();
                            p2.waitFor(120, TimeUnit.SECONDS);
                            p2.destroy();
                            p2.waitFor();
                            System.out.println("FightReport Status (0=success): " + p2.exitValue());

                            if (p2.exitValue() == 0) {

                                //call graphbot
                                if (p.graphPlayerLimit > 0) {
                                    System.out.println("Generating Graph...");
                                    final ProcessBuilder pb3 = new ProcessBuilder("cmd", "/c", "start", "/b",
                                                                                  "/belownormal", "/wait", "java", "-jar", p.jarName, "GraphBot", p.homeDir
                                    );
                                    pb3.inheritIO();
                                    pb3.directory(new File(p.homeDir));
                                    final Process p3 = pb3.start();
                                    p3.waitFor(120, TimeUnit.SECONDS);
                                    p3.destroy();
                                    p3.waitFor();
                                    System.out.println("Graphing Status (0=success): " + p3.exitValue());
                                }

                                //call discordbot and twitchbot
                                final FightReport report = FightReport.readReportFile();
                                if (report == null) {
                                    System.out.println("ERROR: FightReport file not available.");
                                } else {
                                    final DiscordBot dBot = org.vmy.DiscordBot.getSingletonInstance();
                                    dBot.sendWebhookMessage(report);
                                    final TwitchBot tBot = org.vmy.TwitchBot.getSingletonInstance();
                                    tBot.sendMessage(report.getOverview());
                                }
                            }
                            try {
                                jsonFile.delete();
                                if (logFile.exists()) {
                                    logFile.delete();
                                }
                            } catch (final Exception e) {
                            }
                        }
                        break; //exit loop
                    } else { //else keep looping until file is no longer being modified
                        lastModified = f.lastModified();
                        System.out.print(">");
                    }
                }
            }

            System.out.print(".");
        }
    }

    private List<File> listLogFiles() throws IOException {
        List<File> list = new ArrayList<>();

        //custom folder
        final String customFolder = Parameters.getInstance().customLogFolder;
        if (customFolder != null && customFolder.length() > 0) {
            final File folder = new File(Parameters.getInstance().customLogFolder);
            list = !folder.exists() ? new ArrayList<>() :
                   Files.find(
                       Paths.get(folder.getAbsolutePath()),
                       Integer.MAX_VALUE,
                       (filePath, fileAttr) -> fileAttr.isRegularFile()
                     )
                     .filter(f -> f.toFile().getName().endsWith(".zevtc") || f.toFile().getName().endsWith(".evtc"))
                     .map(p -> p.toFile())
                     .collect(Collectors.toList());
        }

        //default folder
        final File defaultFolder = new File(Parameters.getInstance().defaultLogFolder);
        final List<File> list2 = !defaultFolder.exists() ? new ArrayList<>() :
                                 Files.find(
                               Paths.get(defaultFolder.getAbsolutePath()),
                               Integer.MAX_VALUE,
                               (filePath, fileAttr) -> fileAttr.isRegularFile()
                             )
                             .filter(f -> f.toFile().getName().endsWith(".zevtc") || f.toFile().getName().endsWith(".evtc"))
                             .map(p -> p.toFile())
                             .collect(Collectors.toList());
        list.addAll(list2);
        return list;
    }

    private File locateNewFile(final List<File> listOfFiles) {
        for (final File f : listOfFiles) {
            if (!fileMap.containsKey(f.getAbsolutePath())) {
                System.out.println("\nNew file detected: " + f.getName());
                return f;
            }
        }
        return null;
    }
}
