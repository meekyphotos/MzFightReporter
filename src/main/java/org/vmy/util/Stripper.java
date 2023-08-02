package org.vmy.util;

public class Stripper implements Comparable<Stripper> {

    private String name;
    private String profession;
    private int strips;

    public Stripper(final String name, final String profession, final int strips) {
        this.name = name;
        this.profession = profession;
        this.strips = strips;
    }

    public int compareTo(final Stripper s) {
        if (strips == s.strips) {
            return 0;
        } else if (strips > s.strips) {
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
               + String.format("%,7d", strips);
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

    public int getStrips() {
        return strips;
    }

    public void setStrips(final int strips) {
        this.strips = strips;
    }
}
