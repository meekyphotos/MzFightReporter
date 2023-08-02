package org.vmy;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vmy.util.Cleanser;
import org.vmy.util.Condier;
import org.vmy.util.DPSer;
import org.vmy.util.DefensiveBooner;
import org.vmy.util.FightReport;
import org.vmy.util.Group;
import org.vmy.util.Healer;
import org.vmy.util.Player;
import org.vmy.util.Spiker;
import org.vmy.util.Stripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseBot {

    private static final String CRLF = "\n";

    protected static FightReport processWvwJsonLog(final File jsonFile, final File logFile) throws IOException {
        final JSONObject jo = new JSONObject();
        final FightReport report = new FightReport();

        //get upload URL from log file
        String uploadURL = null;
        if (logFile.exists()) {
            final InputStream is = new FileInputStream(logFile);

            try {
                final String logTxt = IOUtils.toString(is, StandardCharsets.UTF_8);
                final int index = logTxt.indexOf("https://");
                final int end = logTxt.indexOf('\r', index);
                if (index > 0) {
                    uploadURL = logTxt.substring(index, end);
                    System.out.println("DPS Reports link=" + uploadURL);
                }
            } finally {
                is.close();
            }
        }

        final InputStream is = new FileInputStream(jsonFile);
        try {

            final String jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);
            final JSONObject jsonTop = new JSONObject(jsonTxt);

            //targets
            final JSONArray targets = jsonTop.getJSONArray("targets");
            final HashMap<String, Condier> condiers = new HashMap<String, Condier>();
            int countEnemyPlayers = 0;
            for (int i = 1; i < targets.length(); i++) {
                final JSONObject currTarget = targets.getJSONObject(i);
                final String name = currTarget.getString("name");
                countEnemyPlayers++;
                if (!currTarget.isNull("buffs")) {
                    final JSONArray bArray = currTarget.getJSONArray("buffs");
                    populateCondierBuffs(condiers, bArray);
                }
            }

            //players
            final JSONArray players = jsonTop.getJSONArray("players");
            final List<DPSer> dpsers = new ArrayList<DPSer>();
            final List<Cleanser> cleansers = new ArrayList<Cleanser>();
            final List<Stripper> strippers = new ArrayList<Stripper>();
            final List<DefensiveBooner> dbooners = new ArrayList<DefensiveBooner>();
            final List<Spiker> spikers = new ArrayList<Spiker>();
            final List<Healer> healers = new ArrayList<Healer>();
            final HashMap<String, Player> playerMap = new HashMap<String, Player>();
            final HashMap<String, Group> groups = new HashMap<String, Group>();
            int sumPlayerDmg = 0;
            int battleLength = 0;
            int countEnemyDowns = 0;
            int countEnemyDeaths = 0;
            int sumEnemyDps = 0;
            int sumEnemyDmg = 0;
            final DefensiveBooner sumBoons = new DefensiveBooner("Total", "Total", "0");
            String commander = null;
            for (int i = 0; i < players.length(); i++) {
                final JSONObject currPlayer = players.getJSONObject(i);
                final String name = currPlayer.getString("name");
                final String profession = currPlayer.getString("profession");
                final String group = String.valueOf(currPlayer.getInt("group"));
                if (currPlayer.getBoolean("hasCommanderTag")) {
                    commander = commander == null ? currPlayer.getString("name") : "n/a";
                }
                if (condiers.containsKey(name)) {
                    condiers.get(currPlayer.getString("name")).setProfession(profession);
                }

                //statsTargets
                final List<Object> playerStatsTargets = currPlayer.getJSONArray("statsTargets").toList();
                for (final Object a : playerStatsTargets) {
                    for (final Object b : (List<Object>) a) {
                        final HashMap<String, Integer> map = (HashMap<String, Integer>) b;
                        countEnemyDowns += map.get("downed");
                        countEnemyDeaths += map.get("killed");
                        if (playerMap.containsKey(name)) {
                            final Player p = playerMap.get(name);
                            p.setKills(p.getKills() + map.get("killed"));
                        } else {
                            final Player p = new Player(name, profession, group);
                            p.setKills(map.get("killed"));
                            playerMap.put(name, p);
                        }
                    }
                }

                //defenses
                final JSONObject playerDefenses = currPlayer.getJSONArray("defenses").getJSONObject(0);
                sumEnemyDmg += playerDefenses.getBigInteger("damageTaken").intValue();

                //support
                final JSONObject currPlayerSupport = currPlayer.getJSONArray("support").getJSONObject(0);
                final int cleanses = currPlayerSupport.getBigInteger("condiCleanse").intValue();
                if (cleanses > 0) {
                    cleansers.add(new Cleanser(name, profession, cleanses));
                }
                final int strips = currPlayerSupport.getBigInteger("boonStrips").intValue();
                if (strips > 0) {
                    strippers.add(new Stripper(name, profession, strips));
                }

                //targetDamage1S
                final List<Object> targetDmgList = currPlayer.getJSONArray("targetDamage1S").toList();
                List<Object> netTargetDmgList = null;
                for (final Object a : targetDmgList) {
                    final List<Object> aobj = (List<Object>) a;
                    for (final Object b : aobj) {
                        final List<Object> bobj = (List<Object>) b;
                        if (netTargetDmgList == null) {
                            netTargetDmgList = bobj; //initialize using first instance
                        } else {
                            for (int q = 0; q < netTargetDmgList.size(); q++) {
                                final Integer bdmg = (Integer) bobj.get(q);
                                final Integer fdmg = (Integer) netTargetDmgList.get(q);
                                final Integer current = bdmg + fdmg;
                                netTargetDmgList.set(q, current);
                            }
                        }
                    }
                }

                //set report dmg map
                report.getDmgMap().put(name, netTargetDmgList);

                //set player damage
                final DPSer dpser = new DPSer(name, profession, netTargetDmgList);
                dpsers.add(dpser);
                sumPlayerDmg += dpser.getDamage();
                battleLength = netTargetDmgList.size();

                //update top 10 spikes
                Spiker.computeTop10(name, profession, spikers, netTargetDmgList);

                //active buffs
                final DefensiveBooner dBooner = new DefensiveBooner(name, profession, group);
                if (!currPlayer.isNull("squadBuffs")) {
                    final JSONArray bArray = currPlayer.getJSONArray("squadBuffs");
                    populateDefensiveBoons(dBooner, bArray);
                }
                dBooner.computeRating();
                dbooners.add(dBooner);
                addBoons(sumBoons, dBooner);

                //healing
                int healing = 0;
                if (!currPlayer.isNull("extHealingStats")) {
                    final JSONObject ehObject = currPlayer.getJSONObject("extHealingStats");
                    if (!ehObject.isNull("outgoingHealing")) {
                        final JSONArray ohArray = ehObject.getJSONArray("outgoingHealing");
                        final JSONObject ohObj = (JSONObject) ohArray.get(0);
                        healing = ohObj.getInt("healing");
                    }
                }
                int barrier = 0;
                if (!currPlayer.isNull("extBarrierStats")) {
                    final JSONObject ehObject = currPlayer.getJSONObject("extBarrierStats");
                    if (!ehObject.isNull("outgoingBarrier")) {
                        final JSONArray ohArray = ehObject.getJSONArray("outgoingBarrier");
                        final JSONObject ohObj = (JSONObject) ohArray.get(0);
                        barrier = ohObj.getInt("barrier");
                    }
                }
                final Healer healer = new Healer(name, profession, healing, barrier);
                if (healer.getTotal() > 0) {
                    healers.add(healer);
                }
            }
            calculateWeightedBoons(sumBoons, dbooners);

            //basic info
            String zone = jsonTop.getString("fightName");
            zone = zone.indexOf(" - ") > 0 ? zone.substring(zone.indexOf(" - ") + 3) : zone;
            report.setZone(zone);
            report.setDuration(jsonTop.getString("duration"));
            report.setCommander("n/a".equals(commander) ? null : commander); //EI bug in json output
            final JSONArray uploadLinks = jsonTop.getJSONArray("uploadLinks");
            if (jsonTop.has("uploadLinks")) {
                report.setUrl(jsonTop.getJSONArray("uploadLinks").getString(0));
            }
            if (report.getUrl() == null || !report.getUrl().startsWith("http")) {
                report.setUrl(uploadURL);
            }
            System.out.println("URL=" + report.getUrl());
            if (jsonTop.has("timeEnd")) {
                report.setEndTime(jsonTop.getString("timeEnd"));
            }

            //mechanics
            int totalPlayersDead = 0;
            int totalPlayersDowned = 0;
            if (jsonTop.has("mechanics")) {
                final JSONArray mechanics = jsonTop.getJSONArray("mechanics");
                final List<Object> mdList = mechanics.getJSONObject(0).getJSONArray("mechanicsData").toList();
                for (final Object mdo : mdList) {
                    final HashMap<String, Object> mdMap = (HashMap<String, Object>) mdo;
                    final String actor = (String) mdMap.get("actor");
                    final Player p = playerMap.get(actor);
                    p.setDeaths(p.getDeaths() + 1);
                }
                if (mechanics.length() > 0) {
                    totalPlayersDead = mechanics.getJSONObject(0).getJSONArray("mechanicsData").length();
                }
                if (mechanics.length() > 1) {
                    totalPlayersDowned = mechanics.getJSONObject(1).getJSONArray("mechanicsData").length();
                }
            }

            //compile group data
            for (final Player plyr : playerMap.values()) {
                final String grp = plyr.getGroup();
                Group g = groups.get(grp);
                g = g == null ? new Group(grp, grp) : g;
                if ("Firebrand".equals(plyr.getProfession())) {
                    g.setName(plyr.getName());
                }
                groups.put(grp, g);
                g.setKills(g.getKills() + plyr.getKills());
                g.setDeaths(g.getDeaths() + plyr.getDeaths());
            }

            //write to buffer
            StringBuffer buffer = new StringBuffer();
            buffer.append(" Players   Damage    DPS    Downs    Deaths" + CRLF);
            buffer.append("--------- --------  -----  -------  --------" + CRLF);
            buffer.append(String.format("%6d %10s %7s %6d %8d", players.length(),
                                        DPSer.withSuffix(sumPlayerDmg, sumPlayerDmg < 1000000 ? 1 : 2), DPSer.withSuffix(sumPlayerDmg / battleLength, 1),
                                        totalPlayersDowned, totalPlayersDead
            ));
            report.setSquadSummary(buffer.toString());

            //approximate enemyDps
            sumEnemyDps = sumEnemyDmg / battleLength;

            buffer = new StringBuffer();
            buffer.append(" Enemies   Damage    DPS    Downs    Deaths" + CRLF);
            buffer.append("--------- --------  -----  -------  --------" + CRLF);
            buffer.append(String.format("%6d %10s %7s %6d %8d", countEnemyPlayers,
                                        DPSer.withSuffix(sumEnemyDmg, sumEnemyDmg < 1000000 ? 1 : 2), DPSer.withSuffix(sumEnemyDps, 1),
                                        countEnemyDowns, countEnemyDeaths
            ));
            report.setEnemySummary(buffer.toString());

            if (dpsers.size() > 0) {
                buffer = new StringBuffer();
                buffer.append(" #  Player                      Damage     DPS" + CRLF);
                buffer.append("--- -------------------------  --------   -----" + CRLF);
                dpsers.sort((d1, d2) -> d1.compareTo(d2));
                int index = 1;
                final int count = dpsers.size() > 10 ? 10 : dpsers.size();
                for (final DPSer x : dpsers.subList(0, count)) {
                    if (x.getDamage() > 0) {
                        buffer.append(String.format("%2s", (index)) + "  " + x + CRLF);
                        index++;
                    }
                }
                report.setDamage(buffer.toString());
            }

            if (spikers.size() > 0) {
                buffer = new StringBuffer();
                buffer.append(" #  Player                   2 sec  4 sec  Time" + CRLF);
                buffer.append("--- -----------------------  -----  -----  ----" + CRLF);
                spikers.sort((d1, d2) -> d1.compareTo(d2));
                int index = 1;
                final int count = spikers.size() > 10 ? 10 : spikers.size();
                for (final Spiker x : spikers.subList(0, count)) {
                    buffer.append(String.format("%2s", (index)) + "  " + x + CRLF);
                    index++;
                }
                report.setSpikers(buffer.toString());
            }

            if (cleansers.size() > 0) {
                buffer = new StringBuffer();
                buffer.append(" #  Player                     Cleanses" + CRLF);
                buffer.append("--- -------------------------  --------" + CRLF);
                cleansers.sort((d1, d2) -> d1.compareTo(d2));
                int index = 1;
                final int count = cleansers.size() > 10 ? 10 : cleansers.size();
                for (final Cleanser x : cleansers.subList(0, count)) {
                    buffer.append(String.format("%2s", (index)) + "  " + x + CRLF);
                    index++;
                }
                report.setCleanses(buffer.toString());
            }

            if (strippers.size() > 0) {
                buffer = new StringBuffer();
                buffer.append(" #  Player                      Strips" + CRLF);
                buffer.append("--- -------------------------  --------" + CRLF);
                strippers.sort((d1, d2) -> d1.compareTo(d2));
                int index = 1;
                final int count = strippers.size() > 10 ? 10 : strippers.size();
                for (final Stripper x : strippers.subList(0, count)) {
                    buffer.append(String.format("%2s", (index)) + "  " + x + CRLF);
                    index++;
                }
                report.setStrips(buffer.toString());
            }

            if (dbooners.size() > 0) {
                buffer = new StringBuffer();
                buffer.append(" #  Player                     Rating  GroupKDR" + CRLF);
                buffer.append("--- -------------------------  ------    -----" + CRLF);
                dbooners.sort((d1, d2) -> d1.compareTo(d2));
                int index = 1;
                final int count = dbooners.size() > 10 ? 10 : dbooners.size();
                for (final DefensiveBooner x : dbooners.subList(0, count)) {
                    if (x.getDefensiveRating() > 0) {
                        buffer.append(String.format("%2s", (index)) + "  " + x + "    "
                                      + String.format("%5s", groups.get(x.getGroup()).getKills() + "/" + groups.get(x.getGroup()).getDeaths()) + CRLF);
                        index++;
                    }
                }
                report.setDbooners(buffer.toString());
            }

            if (healers.size() > 0) {
                buffer = new StringBuffer();
                buffer.append(" #  Player                 Total  Heals Barrier" + CRLF);
                buffer.append("--- ---------------------- ------ ------ ------" + CRLF);
                healers.sort((d1, d2) -> d1.compareTo(d2));
                int index = 1;
                final int count = healers.size() > 10 ? 10 : healers.size();
                for (final Healer x : healers.subList(0, count)) {
                    if (x.getTotal() > 0) {
                        buffer.append(String.format("%2s", (index)) + "  " + x + CRLF);
                        index++;
                    }
                }
                report.setHealers(buffer.toString());
            }

            if (condiers.size() > 0) {
                buffer = new StringBuffer();
                buffer.append(" #  Player                          CCs" + CRLF);
                buffer.append("--- ------------------------  --------------" + CRLF);
                final List<Condier> clist = new ArrayList<Condier>(condiers.values());
                clist.sort((d1, d2) -> d1.compareTo(d2));
                int index = 1;
                final int count = clist.size() > 10 ? 10 : clist.size();
                for (final Condier x : clist.subList(0, count)) {
                    if (x.getChilledCount() > 0 || x.getCrippledCount() > 0 || x.getImmobCount() > 0 || x.getStunCount() > 0) {
                        buffer.append(String.format("%2s", (index)) + "  " + x + CRLF);
                        index++;
                    }
                }
                report.setCcs(buffer.toString());
            }

            buffer = new StringBuffer();
            buffer.append(String.format("[Report] Squad Players: %d (Deaths: %d) | Enemy Players: %d (Deaths: %d)",
                                        players.length(), totalPlayersDead,
                                        countEnemyPlayers, countEnemyDeaths
            ));
            report.setOverview(buffer.toString());
            System.out.println(buffer);

        } finally {
            is.close();
        }

        return report;
    }

    private static void populateCondierBuffs(final HashMap<String, Condier> condiers, final JSONArray bArray) {
        for (final Object obj : bArray.toList()) {
            final HashMap m = (HashMap) obj;
            final ArrayList buffData = (ArrayList) m.get("buffData");
            final HashMap bm = (HashMap) buffData.get(0);
            final HashMap generated = (HashMap) bm.get("generated");
            final int id = (int) (Integer) m.get("id");
            for (final Object e : generated.entrySet()) {
                addToCondiers(condiers, (Map.Entry<String, BigDecimal>) e, id);
            }
        }
    }

    private static void addToCondiers(final HashMap<String, Condier> condiers, final Map.Entry<String, BigDecimal> me, final int id) {
        final String name = me.getKey();
        Condier c = condiers.get(name);
        if (c == null) {
            c = new Condier(name, "    ");
        }
        switch (id) { //stun=872 chilled=722 crippled=721 immob=727 slow=26766
            case 872:
                c.setStunCount(c.getStunCount() + 1);
                c.setStunDur(c.getStunDur().add(me.getValue()));
                break;
            case 722:
                c.setChilledCount(c.getChilledCount() + 1);
                c.setChilledDur(c.getChilledDur().add(me.getValue()));
                break;
            case 721:
                c.setCrippledCount(c.getCrippledCount() + 1);
                c.setCrippledDur(c.getCrippledDur().add(me.getValue()));
                break;
            case 727:
                c.setImmobCount(c.getImmobCount() + 1);
                c.setImmobDur(c.getImmobDur().add(me.getValue()));
                break;
            case 26766:
                c.setSlowCount(c.getSlowCount() + 1);
                c.setSlowDur(c.getSlowDur().add(me.getValue()));
                break;
        }
        condiers.put(name, c);
    }

    private static void calculateWeightedBoons(final DefensiveBooner sumBoons, final List<DefensiveBooner> dbooners) {
        for (final DefensiveBooner db : dbooners) {
            final int stab = sumBoons.getStability();
            db.setStability(stab > 0 ? 100 * db.getStability() / stab : db.getStability());
            final int aegis = sumBoons.getAegis();
            db.setAegis(aegis > 0 ? 100 * db.getAegis() / aegis : db.getAegis());
            final int prot = sumBoons.getProtection();
            db.setProtection(prot > 0 ? 100 * db.getProtection() / prot : db.getProtection());
            final int resist = sumBoons.getResistance();
            db.setResistance(resist > 0 ? 100 * db.getResistance() / resist : db.getResistance());
            final int resolu = sumBoons.getResolution();
            db.setResolution(resolu > 0 ? 100 * db.getResolution() / resolu : db.getResolution());
            final int alac = sumBoons.getAlacrity();
            db.setAlacrity(alac > 0 ? 100 * db.getAlacrity() / alac : db.getAlacrity());
            db.computeRating();
        }
    }

    private static void populateDefensiveBoons(final DefensiveBooner dBooner, final JSONArray bArray) {
        for (final Object obj : bArray.toList()) {
            final HashMap m = (HashMap) obj;
            final int id = (int) (Integer) m.get("id");
            switch (id) { //1122/743/717/26980/873/30328
                case 1122:
                    dBooner.setStability(dBooner.getStability() + getBuffGeneration(m));
                    break;
                case 743:
                    dBooner.setAegis(dBooner.getAegis() + getBuffGeneration(m));
                    break;
                case 717:
                    dBooner.setProtection(dBooner.getProtection() + getBuffGeneration(m));
                    break;
                case 26980:
                    dBooner.setResistance(dBooner.getResistance() + getBuffGeneration(m));
                    break;
                case 873:
                    dBooner.setResolution(dBooner.getResolution() + getBuffGeneration(m));
                    break;
                case 30328:
                    dBooner.setAlacrity(dBooner.getAlacrity() + getBuffGeneration(m));
                    break;
            }
        }
    }

    private static int getBuffGeneration(final HashMap m) {
        if (m.containsKey("buffData")) {
            final List buffData = (List) m.get("buffData");
            if (buffData != null && buffData.size() > 0) {
                final HashMap bdMap = (HashMap) buffData.get(0);
                if (bdMap.containsKey("generation")) {
                    final BigDecimal gen = (BigDecimal) bdMap.get("generation");
                    return gen.multiply(new BigDecimal(1000)).intValue();
                } else if (bdMap.containsKey("generated")) {
                    final Map genMap = (HashMap) bdMap.get("generated");
                    BigDecimal gen = new BigDecimal("0");
                    for (final Object val : genMap.values()) {
                        gen = gen.add((BigDecimal) val);
                    }
                    return gen.multiply(new BigDecimal(1000)).intValue();
                }
            }
        }
        return 0;
    }

    private static void addBoons(final DefensiveBooner sumBoons, final DefensiveBooner player) {
        sumBoons.setStability(sumBoons.getStability() + player.getStability());
        sumBoons.setAegis(sumBoons.getAegis() + player.getAegis());
        sumBoons.setProtection(sumBoons.getProtection() + player.getProtection());
        sumBoons.setResistance(sumBoons.getResistance() + player.getResistance());
        sumBoons.setResolution(sumBoons.getResolution() + player.getResolution());
        sumBoons.setAlacrity(sumBoons.getAlacrity() + player.getAlacrity());
    }

    public static void main(final String[] args) throws Exception {
        final File jsonFile = new File(args[1]);
        final File logFile = new File(args[2]);
        final String homeDir = args[3];
        if (jsonFile.exists()) {
            final FightReport report = processWvwJsonLog(jsonFile, logFile);

            FileOutputStream frf = null;
            ObjectOutputStream o = null;
            try {
                frf = new FileOutputStream(new File(homeDir + File.separator + "fightreport.bin"));
                o = new ObjectOutputStream(frf);
                // Write objects to file
                o.writeObject(report);
            } finally {
                if (o != null) {
                    o.close();
                }
                if (frf != null) {
                    frf.close();
                }
            }
        }
    }
}
