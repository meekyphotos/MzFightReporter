package org.vmy.util;

import java.util.List;

public class DPSer implements Comparable<DPSer> {

    private String name;
    private String profession;
    private int damage;
    private int dps;

    public DPSer(final String name, final String profession, final List<Object> dmgList) {
        this.name = name;
        this.profession = profession;
        final int size = dmgList.size();
        damage = (int) dmgList.get(size - 1);
        int firstDmg = 0;
        int lastDmg = 0;
        int prevDmg = 0;
        for (int i = 0; i < size; i++) {
            final int currDmg = (int) dmgList.get(i);
            if (currDmg > 0) {
                if (currDmg > prevDmg) {
                    lastDmg = i;
                }
                if (firstDmg == 0) {
                    firstDmg = i;
                }
            }
            prevDmg = currDmg;
        }
        int dmgLength = lastDmg - firstDmg + 1;
        dmgLength = dmgLength == 0 ? 1 : dmgLength;
        dps = damage / dmgLength;
    }

    public static String withSuffix(final long count, final int decimals) {
        if (count < 1000) {
            return String.valueOf(count);
        }
        final int exp = (int) (Math.log(count) / Math.log(1000));
        return String.format(
          "%." + decimals + "f%c",
          count / Math.pow(1000, exp),
          "kmbtqQ".charAt(exp - 1)
        );
    }

    public int compareTo(final DPSer d) {
        if (damage == d.damage) {
            return 0;
        } else if (damage > d.damage) {
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
               + String.format("%9s", withSuffix(damage, damage < 1000000 ? 1 : 2)) + " "
               + String.format("%8s", withSuffix(dps, 1));
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

    public int getDamage() {
        return damage;
    }

    public void setDamage(final int damage) {
        this.damage = damage;
    }

    public int getDps() {
        return dps;
    }

    public void setDps(final int dps) {
        this.dps = dps;
    }
}
