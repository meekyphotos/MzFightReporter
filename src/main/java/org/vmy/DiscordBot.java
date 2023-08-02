package org.vmy;

import org.vmy.util.FightReport;

import java.awt.Color;
import java.io.File;
import java.io.PrintStream;
import java.time.Instant;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

public class DiscordBot {

    private static DiscordBot singleton;
    private WebhookClient client = null;

    //hide needless errors via class block
    {
        final PrintStream filterOut = new PrintStream(System.err) {
            public void println(final String l) {
                if (!l.startsWith("SLF4J")) {
                    super.println(l);
                }
            }
        };
        System.setErr(filterOut);
    }

    private DiscordBot() {
        try {
            openSession();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static DiscordBot getSingletonInstance() {
        if (singleton == null) {
            singleton = new DiscordBot();
        }
        return singleton;
    }

    public static void main(final String[] args) throws Exception {
        final DiscordBot bot = new DiscordBot().openSession();
        bot.sendWebhookMessage(new FightReport());
        bot.client.close();
    }

    private DiscordBot openSession() throws Exception {
        if (client == null) {
            client = WebhookClient.withUrl(Parameters.getInstance().discordWebhook);
        }
        return this;
    }

    protected void sendWebhookMessage(final FightReport report) throws InterruptedException {
        final Parameters p = Parameters.getInstance();
        final File graphImage = new File(p.homeDir + File.separator + "fightreport.png");

        // Send and log (using embed)
        final WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder();
        embedBuilder.setColor(Color.CYAN.getAlpha());
        embedBuilder.setThumbnailUrl(p.discordThumbnail);
        embedBuilder.setDescription("> " + report.getZone() + "\n\n" + (report.getCommander() != null ? "**Commander**: " + report.getCommander() + "\n" : "") + "**Duration**: " + report.getDuration() + "\n");
        final String squadSummary = "```" + report.getSquadSummary() + "```";
        embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Squad Summary", squadSummary));
        embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Enemy Summary", "```" + report.getEnemySummary() + "```"));
        if (p.showDamage && report.getDamage() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Damage", "```" + report.getDamage() + "```"));
        }
        if (p.showSpikeDmg && report.getSpikers() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Spike Damage", "```" + report.getSpikers() + "```"));
        }
        if (p.showCleanses && report.getCleanses() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Cleanses", "```" + report.getCleanses() + "```"));
        }
        if (p.showStrips && report.getStrips() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Strips", "```" + report.getStrips() + "```"));
        }
        if (p.showDefensiveBoons && report.getDbooners() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Defensive Boons", "```" + report.getDbooners() + "```"));
        }
        if (p.showHeals && report.getHealers() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Healing", "```" + report.getHealers() + "```"));
        }
        if (p.showCCs && report.getCcs() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Outgoing CC's  (stuns immobs chills cripples)", "```" + report.getCcs() + "```"));
        }
        if (p.showQuickReport && report.getOverview() != null) {
            embedBuilder.addField(new WebhookEmbed.EmbedField(false, "Quick Report", "```" + report.getOverview() + "```"));
        }
        embedBuilder.addField(new WebhookEmbed.EmbedField(true, "\u200b", report.getUrl() == null ? "[DPSReports using EI: Upload process failed]" : "[Full Report](" + report.getUrl() + ")"));
        //embedBuilder.setImageUrl("attachment://fightreport.png");
        embedBuilder.setTimestamp(Instant.now());
        final WebhookEmbed embed = embedBuilder.build();
        client.send(embed);
        if (graphImage.exists() && p.graphPlayerLimit > 0 && p.showDamageGraph) {
            client.send(graphImage);
        }
        System.out.println("Discord msg sent via webhook.");
    }

    protected void finalize() {
        if (client != null) {
            client.close();
        }
    }
}
