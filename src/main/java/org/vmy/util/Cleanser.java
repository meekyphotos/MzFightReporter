package org.vmy.util;

public class Cleanser implements Comparable<Cleanser> {
    private String name;
    private final String profession;
    private final int cleanses;

    public Cleanser(final String name, final String profession, final int cleanses) {
        this.name = name;
        this.profession = profession;
        this.cleanses = cleanses;
    }

    public int compareTo(final Cleanser c) {
        return Integer.compare(c.cleanses, cleanses);
    }

    public String toString() {
        return String.format(
          "%-25s",
          String.format("%.18s", name).trim() + " (" + profession.substring(0, 4) + ")"
        )
               + String.format("%,7d", cleanses);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
