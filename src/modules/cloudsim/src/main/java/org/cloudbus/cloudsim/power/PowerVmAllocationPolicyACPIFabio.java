/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.power;

import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;


public class PowerVmAllocationPolicyACPIFabio extends PowerVmAllocationPolicyAbstract {

    public PowerVmAllocationPolicyACPIFabio(List<? extends Host> list) {
        super(list);
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        double minMetric = -1;
        PowerHost ChoosenHost = null;
        for (PowerHost host : this.<PowerHost>getHostList()) {
            if (host.getACPIState().equalsIgnoreCase("S2") && host.getIndexState() == PowerHost.ACPI_STAYING) {
                ChoosenHost = host;
                break;
            }
        }

        if(ChoosenHost != null) {
            Log.printLine("CHOSEN HOST for VM " + vm.getId() + " is : Host #" + ChoosenHost.getId());
        } else {
            Log.printLine("Host not available");
            return false;  
        }
        boolean allocationOK = false;
        allocationOK = allocateHostForVm(vm, ChoosenHost);

        if (allocationOK) {
            Log.formatLine("%.2f: Changing sleep state of host %d from %s to %s (%s)", CloudSim.clock(), ChoosenHost.getId(), ChoosenHost.getIndexState() == 1 ? "Entering" : ChoosenHost.getIndexState() == 2 ? "Leaving" : "Staying", "Leaving", ChoosenHost.getACPIState());

            ChoosenHost.setIndexState(PowerHost.ACPI_LEAVING);
            return true;
        }
        
        return false;
    }
}
