package edu.utas.util;

import edu.utas.vo.InstanceInfo;

import java.util.Comparator;

public class InstanceInfoComparator implements Comparator<InstanceInfo> {

    @Override
    public int compare(InstanceInfo t0, InstanceInfo t1) {
        double var0 = 100 - t0.getIdlePercentage();
        double var2 = 100 - t1.getIdlePercentage();
        return var0 < var2 ? -1 : (var0 == var2 ? 0 : 1);
    }
}
