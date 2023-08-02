package org.vmy.util;

public class Healer implements Comparable<Healer> {

    private String name;
    private String profession;
    private int healing;
    private int barrier;
    private int total;

    public Healer(final String name, final String profession, final int healing, final int barrier) {
        this.name = name;
        this.profession = profession;
        this.healing = healing;
        this.barrier = barrier;
        this.total = healing + barrier;
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

    public int compareTo(final Healer c) {
        if (total == c.total) {
            return 0;
        } else if (total > c.total) {
            return -1;
        } else {
            return 1;
        }
    }

    public String toString() {
        return String.format(
          "%-22s",
          String.format("%.15s", name).trim() + " (" + profession.substring(0, 4) + ")"
        )
               + String.format("%7s", withSuffix(total, 1))
               + String.format("%7s", withSuffix(healing, 1))
               + String.format("%7s", withSuffix(barrier, 1));
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

    public int getHealing() {
        return healing;
    }

    public void setHealing(final int healing) {
        this.healing = healing;
        this.total = healing + barrier;
    }

    public int getBarrier() {
        return barrier;
    }

    public void setBarrier(final int barrier) {
        this.barrier = barrier;
        this.total = healing + barrier;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(final int total) {
        this.total = total;
    }
}
