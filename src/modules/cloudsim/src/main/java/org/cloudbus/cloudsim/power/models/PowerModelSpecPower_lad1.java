/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudbus.cloudsim.power.models;

import java.util.Iterator;
import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;

/**
 *
 * @author miguel
 */
public class PowerModelSpecPower_lad1 extends PowerModelSpecPowerDVFS {

        /**
         * Tables power, in relation with CPU Frequency !
         * tabIdle[f1][f2]....[fn] tabFull[f1][f2]....[fn]
         */

        double Tab_Power_idle[] = {90, 94, 100, 104, 107, 34};
        double Tab_Power_full[] = {170, 176, 182, 188, 193, 58};
        List<Pe> peList;

        Pe tmp_pe;
           
       
    public PowerModelSpecPower_lad1(List<Pe> PeList_) {
        peList = PeList_;
        Iterator it = peList.iterator();
        Object o = it.next();
        tmp_pe = (Pe) o;
    }

    /**
     *
     * The power model use here is the classical linear power model
     *
     * Cmin + UtilizationPe [Cmax - Cmin]
     *
     *
     * @param utilization
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public double getPower(double utilization) throws IllegalArgumentException {
        double conso;
        int index = tmp_pe.getIndexFreq();

        conso = (1 - utilization) * Tab_Power_idle[index] + utilization * Tab_Power_full[index];
        Log.printLine("Power computation : index current freq = " + index + " / associated value = " + Tab_Power_idle[index] + "/" + Tab_Power_full[index]);

        Log.printLine("(1 - " + utilization + ")*" + Tab_Power_idle[index] + " + " + utilization + " * " + Tab_Power_full[index]);

        Log.printLine("Power = " + conso);

        return conso;
    }

    public double getPMin(int frequency) {
        return Tab_Power_idle[frequency];
    }

    public double getPMax(int frequency) {
        return Tab_Power_full[frequency];
    }
}
