package org.vmy.util;

public class DefensiveBooner {

    private String name;
    private String profession;
    private String group;
    private int stability = 0;
    private int aegis = 0;
    private int protection = 0;
    private int resistance = 0;
    private int resolution = 0;
    private int alacrity = 0;
    private int defensiveRating = 0;

    public DefensiveBooner(final String name, final String profession, final String group) {
        this.name = name;
        this.profession = profession;
        this.group = group;
    }

    public DefensiveBooner(final String name, final String profession, final String group, final int stability, final int aegis, final int protection, final int resistance, final int resolution, final int alacrity) {
        this.name = name;
        this.profession = profession;
        this.group = group;
        this.stability = stability;
        this.aegis = aegis;
        this.protection = protection;
        this.resistance = resistance;
        this.resolution = resolution;
        this.alacrity = alacrity;
        computeRating();
    }

    public void computeRating() {
        defensiveRating = 3 * stability + 2 * aegis + protection + (int) (0.5 * resistance) + resolution + (int) (0.5 * alacrity);
    }

    public int compareTo(final DefensiveBooner d) {
        if (defensiveRating == d.defensiveRating) {
            return 0;
        } else if (defensiveRating > d.defensiveRating) {
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
               + String.format("%7s", defensiveRating);
        //+ " =>" + 3*stability + "+" +2*aegis + "+" +protection + "+" +(int)(0.5*resistance) + "+" +resolution + "+" +(int)(0.5*alacrity);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(final String profession) {
        this.profession = profession;
    }

    public int getStability() {
        return stability;
    }

    public void setStability(final int stability) {
        this.stability = stability;
    }

    public int getAegis() {
        return aegis;
    }

    public void setAegis(final int aegis) {
        this.aegis = aegis;
    }

    public int getProtection() {
        return protection;
    }

    public void setProtection(final int protection) {
        this.protection = protection;
    }

    public int getResistance() {
        return resistance;
    }

    public void setResistance(final int resistance) {
        this.resistance = resistance;
    }

    public int getResolution() {
        return resolution;
    }

    public void setResolution(final int resolution) {
        this.resolution = resolution;
    }

    public int getDefensiveRating() {
        return defensiveRating;
    }

    public void setDefensiveRating(final int defensiveRating) {
        this.defensiveRating = defensiveRating;
    }

    public int getAlacrity() {
        return alacrity;
    }

    public void setAlacrity(final int alacrity) {
        this.alacrity = alacrity;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

}
