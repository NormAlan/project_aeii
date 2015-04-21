package com.toyknight.aeii.animator;

import com.toyknight.aeii.entity.Point;
import com.toyknight.aeii.entity.Unit;

import java.util.ArrayList;

/**
 * Created by toyknight on 4/21/2015.
 */
public class UnitMoveAnimator extends UnitAnimator {

    public UnitMoveAnimator(Unit unit, ArrayList<Point> path, int ts) {
        super(unit, unit.getX(), unit.getY());
    }

}
