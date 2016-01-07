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
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;

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
        boolean allocationOK = false;

        PowerHost ChoosenHost = null;
        for (PowerHost host : this.<PowerHost>getHostList()) {
            if (host.getACPIState().equalsIgnoreCase("G0") && host.getIndexState() == 0) {
                allocationOK = allocateHostForVm(vm, host);
                if (allocationOK) {
                    Log.printLine("CHOSEN HOST for VM " + vm.getId() + " is : Host #" + host.getId());
                    return true;
                }
            }
        }

        for (PowerHost host : this.<PowerHost>getHostList()) {
            if (host.getACPIState().equalsIgnoreCase("S2") && host.getIndexState() == PowerHost.ACPI_LEAVING) {
                if(host.isIsStateChanging()) {
                    Log.printLine("Host " + host.getId() + " is changing ACPI state to satisfy the strategy");
                    continue;
                }
                allocationOK = allocateHostForVm(vm, host);
                if (allocationOK) {
                    Log.printLine("CHOSEN HOST for VM " + vm.getId() + " is : Host #" + host.getId());
                    Log.formatLine("%.2f: Waiting %.2f seconds for the host %d to leave from (%s)", CloudSim.clock(), host.getACPILeavingTime(), host.getId(), host.getACPIState());
                    return true;
                }
            }
        }
 
        for (PowerHost host : this.<PowerHost>getHostList()) {
            if (host.getACPIState().equalsIgnoreCase("G2") && host.getIndexState() == PowerHost.ACPI_LEAVING) {
                if(host.isIsStateChanging()) {
                    Log.printLine("Host " + host.getId() + " is changing ACPI state to satisfy the strategy");
                    continue;
                }
                allocationOK = allocateHostForVm(vm, host);
                if (allocationOK) {
                    Log.printLine("CHOSEN HOST for VM " + vm.getId() + " is : Host #" + host.getId());
                    Log.formatLine("%.2f: Waiting %.2f seconds for the host %d to leave from (%s)", CloudSim.clock(), host.getACPILeavingTime(), host.getId(), host.getACPIState());
                    return true;
                }
            }
        }

        for (PowerHost host : this.<PowerHost>getHostList()) {
Log.printLine("procurando host " + host.getId());

            if (host.getACPIState().equalsIgnoreCase("S2") && host.getIndexState() == PowerHost.ACPI_STAYING) {
Log.printLine("achou host " + host.getId());

                ChoosenHost = host;
                break;
            }
        }
        if (ChoosenHost == null) {
            for (PowerHost host : this.<PowerHost>getHostList()) {
                if (host.getACPIState().equalsIgnoreCase("G2") && host.getIndexState() == PowerHost.ACPI_STAYING) {
                    ChoosenHost = host;
                    break;
                }
            }
        }
        
        if (ChoosenHost != null) {
            allocationOK = allocateHostForVm(vm, ChoosenHost);
            if (allocationOK) {               
                Log.formatLine("%.2f: Changing sleep state of host %d from %s to %s (%s)", CloudSim.clock(), ChoosenHost.getId(), ChoosenHost.getIndexState() == 1 ? "Entering" : ChoosenHost.getIndexState() == 2 ? "Leaving" : "Staying", "Leaving", ChoosenHost.getACPIState());
                ChoosenHost.setIndexState(PowerHost.ACPI_LEAVING);
                ChoosenHost.setACPILeavingTime((double) ChoosenHost.getPowerSleepModeTime());

                if (allocationOK) {
                    Log.printLine("CHOSEN HOST for VM " + vm.getId() + " is : Host #" + ChoosenHost.getId());
                    return true;
                }
            }
        }

        Log.printLine("not host available");

        return false;
    }
}
