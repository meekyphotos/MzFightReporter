package org.vmy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class FightReport implements Serializable {

    private String zone;
    private String duration;
    private String commander;
    private String squadSummary;
    private String enemySummary;
    private String damage;
    private String cleanses;
    private String strips;
    private String ccs;
    private String overview;
    private String dbooners;
    private String spikers;
    private String healers;
    private String url;
    private String endTime;
    private HashMap<String, List<Object>> dmgMap = new HashMap<>();

    public static FightReport readReportFile() throws Exception {
        FightReport myReport = null;
        final File reportFile = new File(org.vmy.Parameters.getInstance().homeDir + File.separator + "fightreport.bin");

        if (!reportFile.exists()) {
            throw new Exception("Fight Report object file not found: " + reportFile.getAbsolutePath());
        }

        FileInputStream frf = null;
        ObjectInputStream o = null;
        try {
            frf = new FileInputStream(reportFile);
            o = new ObjectInputStream(frf);
            // Write objects to file
            myReport = (FightReport) o.readObject();
        } finally {
            if (o != null) {
                o.close();
            }
            if (frf != null) {
                frf.close();
            }
        }
        return myReport;
    }

    public HashMap<String, List<Object>> getDmgMap() {
        return dmgMap;
    }

    public void setDmgMap(final HashMap<String, List<Object>> dmgMap) {
        this.dmgMap = dmgMap;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(final String zone) {
        this.zone = zone;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public String getCommander() {
        return commander;
    }

    public void setCommander(final String commander) {
        this.commander = commander;
    }

    public String getSquadSummary() {
        return squadSummary;
    }

    public void setSquadSummary(final String squadSummary) {
        this.squadSummary = squadSummary;
    }

    public String getEnemySummary() {
        return enemySummary;
    }

    public void setEnemySummary(final String enemySummary) {
        this.enemySummary = enemySummary;
    }

    public String getDamage() {
        return damage;
    }

    public void setDamage(final String damage) {
        this.damage = damage;
    }

    public String getCleanses() {
        return cleanses;
    }

    public void setCleanses(final String cleanses) {
        this.cleanses = cleanses;
    }

    public String getStrips() {
        return strips;
    }

    public void setStrips(final String strips) {
        this.strips = strips;
    }

    public String getDbooners() {
        return dbooners;
    }

    public void setDbooners(final String dbooners) {
        this.dbooners = dbooners;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }

    public String getCcs() {
        return ccs;
    }

    public void setCcs(final String ccs) {
        this.ccs = ccs;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(final String overview) {
        this.overview = overview;
    }

    public String getSpikers() {
        return spikers;
    }

    public void setSpikers(final String spikers) {
        this.spikers = spikers;
    }

    public String getHealers() {
        return healers;
    }

    public void setHealers(final String healers) {
        this.healers = healers;
    }
}
