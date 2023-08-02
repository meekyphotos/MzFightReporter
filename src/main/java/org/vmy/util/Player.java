package org.vmy.util;

public class Player implements Comparable<Player> {

    private String name;
    private String group;
    private String profession;
    private int kills = 0;
    private int deaths = 0;

    public Player(final String name, final String profession, final String group) {
        this.name = name;
        this.profession = profession;
        this.group = group;
    }

    public int compareTo(final Player c) {
        if (kills / (deaths == 0 ? 1 : deaths) == c.kills / (c.deaths == 0 ? 1 : c.deaths)) {
            return 0;
        } else if (kills / (deaths == 0 ? 1 : deaths) > c.kills / (c.deaths == 0 ? 1 : c.deaths)) {
            return -1;
        } else {
            return 1;
        }
    }

    public String toString() {
        return String.format(
          "%-25s",
          String.format("%.18s", name).trim() + " (" + profession.substring(0, 4) + ")"
        )
               + String.format("%,3d", kills) + String.format("%,3d", deaths);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(final String profession) {
        this.profession = profession;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(final int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(final int deaths) {
        this.deaths = deaths;
    }

}
