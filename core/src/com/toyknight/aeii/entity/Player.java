package com.toyknight.aeii.entity;

import java.io.Serializable;

/**
 * Created by toyknight on 4/3/2015.
 */
public class Player implements Serializable {

    private static final long serialVersionUID = 04032015L;

    public static final int NONE = 0x0;
    public static final int LOCAL = 0x1;
    public static final int ROBOT = 0x3;
    public static final int REMOTE = 0x2;

    private int type;
    private int alliance = 0;
    private int gold = 0;
    private int population = 0;

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public int getPopulation() {
        return population;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public void changeGold(int change) {
        this.gold += change;
    }

    public void reduceGold(int reduction) {
        if (gold > reduction) {
            this.gold -= reduction;
        } else {
            this.gold = 0;
        }
    }

    public int getGold() {
        return gold;
    }

    public void setAlliance(int alliance) {
        this.alliance = alliance;
    }

    public int getAlliance() {
        return alliance;
    }

    public boolean isLocalPlayer() {
        return getType() == LOCAL;
    }

}