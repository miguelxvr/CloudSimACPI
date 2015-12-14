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

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;

/**
 * PowerDatacenter is a class that enables simulation of power-aware data
 * centers.
 *
 * If you are using any algorithms, policies or workload included in the power
 * package please cite the following paper:
 *
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic
 * Algorithms and Adaptive Heuristics for Energy and Performance Efficient
 * Dynamic Consolidation of Virtual Machines in Cloud Data Centers", Concurrency
 * and Computation: Practice and Experience, ISSN: 1532-0626, Wiley Press, New
 * York, USA, 2011, DOI: 10.1002/cpe.1867
 *
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class PowerDatacenterACPITimeout extends Datacenter {

    /**
     * The power.
     */
    private double power;

    /**
     * The disable migrations.
     */
    private boolean disableMigrations;

    /**
     * The cloudlet submited.
     */
    private double cloudletSubmitted;

    /**
     * The migration count.
     */
    private int migrationCount;
    
    private boolean finishsimulation;
    
    private int total_latency_cloudlet;
    private double total_energy_states;
    private double total_energy_g0;

    /**
     * Instantiates a new datacenter.
     *
     * @param name the name
     * @param characteristics the res config
     * @param schedulingInterval the scheduling interval
     * @param utilizationBound the utilization bound
     * @param vmAllocationPolicy the vm provisioner
     * @param storageList the storage list
     * @throws Exception the exception
     */
    public PowerDatacenterACPITimeout(
            String name,
            DatacenterCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        setPower(0.0);
        setDisableMigrations(false);
        setCloudletSubmitted(-1);
        setMigrationCount(0);
        finishsimulation = false;
        
    }

    /**
     * Updates processing of each cloudlet running in this PowerDatacenter. It
     * is necessary because Hosts and VirtualMachines are simple objects, not
     * entities. So, they don't receive events and updating cloudlets inside
     * them must be called from the outside.
     *
     * @pre $none
     * @post $none
     */
    @Override
    protected void updateCloudletProcessing() {
//        if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
//            CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
//            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
//            return;
//        }
        double currentTime = CloudSim.clock();
        
        if(finishsimulation == false)
            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);

        // if some time passed since last processing
        if (currentTime > getLastProcessTime()) {
            System.out.print(currentTime + " ");

             double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();

            if (!isDisableMigrations()) {
                List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
                        getVmList());

                if (migrationMap != null) {
                    for (Map<String, Object> migrate : migrationMap) {
                        Vm vm = (Vm) migrate.get("vm");
                        PowerHost targetHost = (PowerHost) migrate.get("host");
                        PowerHost oldHost = (PowerHost) vm.getHost();

                        if (oldHost == null) {
                            Log.formatLine(
                                    "%.2f: Migration of VM #%d to Host #%d is started",
                                    currentTime,
                                    vm.getId(),
                                    targetHost.getId());
                        } else {
                            Log.formatLine(
                                    "%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
                                    currentTime,
                                    vm.getId(),
                                    oldHost.getId(),
                                    targetHost.getId());
                        }

                        targetHost.addMigratingInVm(vm);
                        incrementMigrationCount();

                        /**
                         * VM migration delay = RAM / bandwidth *
                         */
                        // we use BW / 2 to model BW available for migration purposes, the other
                        // half of BW is for VM communication
                        // around 16 seconds for 1024 MB using 1 Gbit/s network
                        send(
                                getId(),
                                vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
                                CloudSimTags.VM_MIGRATE,
                                migrate);
                    }
                }
            }

            // schedules an event to the next time
            //Log.printLine("mintime = " + minTime + " //  double.MAXvalue = " + Double.MAX_VALUE);
            if (minTime != Double.MAX_VALUE) {
                //      Log.printLine("SCHEDULE EVENT TO THE NEXT' TIME !");
                CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
                send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            }

            setLastProcessTime(currentTime);
        }
    }

    /**
     * Update cloudet processing without scheduling future events.
     *
     * @return the double
     */
    protected double updateCloudetProcessingWithoutSchedulingFutureEvents() {
        if (CloudSim.clock() > getLastProcessTime()) {
            return updateCloudetProcessingWithoutSchedulingFutureEventsForce();
        }
        return 0;
    }

    /**
     * Update cloudet processing without scheduling future events.
     *
     * @return the double
     */
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;
        double timeFrameHostEnergy = 0.0;
        
        int nG0 = 0;
        int nS2 = 0;
        int nS3 = 0;
        int nG2 = 0;

        Log.printLine("\n\n--------------------------------------------------------------\n\n");
        Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

        for (PowerHost host : this.<PowerHost>getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            Log.formatLine("%.2f: [Host #%d] utilization is %.2f%%", currentTime, host.getId(), host.getUtilizationOfCpu() * 100);
        }

        if (timeDiff > 0) {
            Log.formatLine("\nEnergy consumption for the last time frame from %.2f to %.2f:", getLastProcessTime(), currentTime);

            for (PowerHost host : this.<PowerHost>getHostList()) {
                
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                
                if (host.isACPIEnergySavingEnable()) {
                    if (host.getACPIState().equalsIgnoreCase("G0"))
                        nG0++;
                    else if (host.getACPIState().equalsIgnoreCase("S3")) {
                        nS3++;
                    } else if (host.getACPIState().equalsIgnoreCase("S2")) {
                        nS2++;
                    } else if (host.getACPIState().equalsIgnoreCase("G2")) {
                        nG2++;
                    }
                }
                    
                /* If we are in a sleep state */
                if (host.isACPIEnergySavingEnable() && ! host.getACPIState().equalsIgnoreCase("G0")) {
                    if(host.getIndexState() == PowerHost.ACPI_STAYING) {
                        host.setACPIStayingTime(host.getACPIStayingTime() + timeDiff);
                    
                        if(host.getACPIStayingTime() >= 75) {
                            if (!host.getACPIState().equalsIgnoreCase("G2")) {
                                int[] data = new int[2];
                                
                                /* Leave from current state */
                                data[0] = host.getId();
                                data[1] = PowerHost.ACPI_LEAVING;
                                sendNow(getId(), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data);
                                
                                /* Enter into G0 */
                                int[] data2 = new int[2];
                                data2[0] = host.getId();
                                data2[1] = 0;
                                send(getId(), host.getPowerSleepModeTime(PowerHost.ACPI_LEAVING), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data2);

                                /* Change to the deeper state */
                                int[] data3 = new int[2];
                                data3[0] = host.getId();
                                if(host.getACPIState().equalsIgnoreCase("S2"))
                                    data3[1] = PowerHost.ACPI_S3;
                                else if (host.getACPIState().equalsIgnoreCase("S3"))
                                    data3[1] = PowerHost.ACPI_G2;
                                
                                int timewait = 40;
                                if(host.getId() == 7 || host.getId() == 9)
                                    timewait = 60;
                                send(getId(), host.getPowerSleepModeTime(PowerHost.ACPI_LEAVING) + timewait, CloudSimTags.HOST_CHANGE_ACPI_STATE, data3);                
                            }
                        }
                    }
                            
                    timeFrameHostEnergy = host.getPowerSleepState(timeDiff);
                    Log.formatLine("%.2f: [Host #%d] ACPI state %s in index %s", currentTime, host.getId(), host.getACPIState(), host.getIndexState() == 1 ? "Entering" : host.getIndexState() == 2 ? "Leaving" : "Staying");
                    Log.formatLine("%.2f: [Host #%d] staying time %.2f", currentTime, host.getId(), host.getACPIStayingTime());                   

                    total_energy_states += timeFrameHostEnergy;
                    
                } else {
                    timeFrameHostEnergy = host.getEnergyLinearInterpolation(previousUtilizationOfCpu, utilizationOfCpu, timeDiff);
                    total_energy_g0 += timeFrameHostEnergy;
                }

                timeFrameDatacenterEnergy += timeFrameHostEnergy;

                //Log.printLine();
                Log.formatLine("%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%", currentTime, host.getId(), getLastProcessTime(), previousUtilizationOfCpu * 100, utilizationOfCpu * 100);
                Log.formatLine("%.2f: [Host #%d] energy is %f W*sec", currentTime, host.getId(), timeFrameHostEnergy);

                host.isDvfsActivatedOnHost();
                
//                if (!host.getVmList().isEmpty()) {
//                    if (host.isEnableSleepMode() && host.getIndexSleepMode() != 0) {
//                        host.setIndexSleepMode(0);
//                        System.out.println("SLEEP STATE STATUS OF HOST " + host.getId() + " HAS CHANGED TO " + host.getIndexSleepMode());
//                    }
//                }
            }

            Log.formatLine("\n%.2f: Data center's energy is %.2f W*sec", currentTime, timeFrameDatacenterEnergy);
            Log.formatLine("\n%.2f: Data center's sleep states G0:%d,S2:%d,S3:%d,G2:%d", currentTime, nG0, nS2, nS3, nG2);

        }

        setPower(getPower() + timeFrameDatacenterEnergy);
        Log.formatLine("%.2f: Data center's energy sum is %.2f W*sec\n", currentTime, getPower());
        Log.formatLine("\n%.2f: Data center's energy is %.2f W*sec\n", currentTime, timeFrameDatacenterEnergy);

        checkCloudletCompletion();

        /**
         * Remove completed VMs *
         */
        for (PowerHost host : this.<PowerHost>getHostList()) {
            for (Vm vm : host.getCompletedVms()) {
                getVmAllocationPolicy().deallocateHostForVm(vm);
                getVmList().remove(vm);
                Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
            }
        }

        Log.printLine();

        setLastProcessTime(currentTime);
        return minTime;
    }

    /*
     * (non-Javadoc)
     * @see org.cloudbus.cloudsim.Datacenter#processVmMigrate(org.cloudbus.cloudsim.core.SimEvent,
     * boolean)
     */
    @Override
    protected void processVmMigrate(SimEvent ev, boolean ack) {
        updateCloudetProcessingWithoutSchedulingFutureEvents();
        super.processVmMigrate(ev, ack);
        SimEvent event = CloudSim.findFirstDeferred(getId(), new PredicateType(CloudSimTags.VM_MIGRATE));
        if (event == null || event.eventTime() > CloudSim.clock()) {
            updateCloudetProcessingWithoutSchedulingFutureEventsForce();
        }
    }
    
    @Override
    protected void processVmCreate(SimEvent ev, boolean ack) {
        Vm vm = (Vm) ev.getData();
        double currentTime = CloudSim.clock();

        boolean result = getVmAllocationPolicy().allocateHostForVm(vm);
        double delay = 0.0;
        
        PowerHost host = (PowerHost) vm.getHost();
        if (result && host.isACPIEnergySavingEnable()) {

            /* If the host is powered off */
            //if (host.getIndexState() == PowerHost.ACPI_STAYING) {
                /* Wake up the host */
            //    int[] data = new int[2];
            //    data[0] = host.getId();
            //    data[1] = PowerHost.ACPI_LEAVING;

            //    send(getId(), 0.01, CloudSimTags.HOST_CHANGE_SLEEP_STATE, data);              
            //}
            Log.formatLine("%.2f: Now the state of the host %d is %d", currentTime, host.getId(), host.getIndexState());
            updateCloudletProcessing();

        }
        
        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = vm.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }

            /* The host is taking from powered off */
            if (result && ((PowerHost) vm.getHost()).isACPIEnergySavingEnable() && 
                    ((PowerHost) vm.getHost()).getIndexState() == PowerHost.ACPI_LEAVING) {
                PowerHost tmp_host = (PowerHost) vm.getHost();
                delay = tmp_host.getPowerSleepModeTime(PowerHost.ACPI_LEAVING);
                total_latency_cloudlet += delay;
                Log.formatLine("%.2f: VM %d will take %.2f seconds to start up", currentTime, vm.getId(), delay);
            }

            send(vm.getUserId(), delay, CloudSimTags.VM_CREATE_ACK, data);
            
        }

        if (result) {
            double amount = 0.0;
            if (getDebts().containsKey(vm.getUserId())) {
                amount = getDebts().get(vm.getUserId());
            }
            amount += getCharacteristics().getCostPerMem() * vm.getRam();
            amount += getCharacteristics().getCostPerStorage() * vm.getSize();

            getDebts().put(vm.getUserId(), amount);

            getVmList().add(vm);

            if (vm.isBeingInstantiated()) {
                vm.setBeingInstantiated(false);
            }

            vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler()
                    .getAllocatedMipsForVm(vm));
        }

    }

    /*
     * (non-Javadoc)
     * @see cloudsim.Datacenter#processCloudletSubmit(cloudsim.core.SimEvent, boolean)
     */
    @Override
    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        Cloudlet cl = (Cloudlet) ev.getData();
        int userId = cl.getUserId();
        int vmId = cl.getVmId();
                  
        super.processCloudletSubmit(ev, ack);
        setCloudletSubmitted(CloudSim.clock());
               
        PowerHost host = (PowerHost) getVmAllocationPolicy().getHost(vmId, userId);
        if (host.isACPIEnergySavingEnable() && host.getIndexState() != 0) {
            int[] data = new int[2];
            data[0] = host.getId();
            data[1] = 0; /* 0 means powered on */
            
            send(getId(), 0.02, CloudSimTags.HOST_CHANGE_SLEEP_STATE, data);
        }
    }
    
    @Override
    protected void processVmDestroy(SimEvent ev, boolean ack) {
        Vm vm = (Vm) ev.getData();
        PowerHost h = (PowerHost) vm.getHost();
        double currentTime = CloudSim.clock();
        
        /* all VMs already done */
        if(h.getVmList().isEmpty())
            return;
       
       if (h.isACPIEnergySavingEnable() && (h.getVmList().size() - 1) == 0) {
            int[] data = new int[2];
            data[0] = h.getId();
           
            Log.formatLine("%.2f: Putting host %d into entering mode to save power", currentTime, h.getId());
            h.setACPIState(h.getACPIEnergySavingStrategy());
            Log.formatLine("%.2f: Changing sleep state of host %d from %s to %s (%s)", CloudSim.clock(), h.getId(), h.getIndexState() == 1 ? "Entering" : h.getIndexState() == 2 ? "Leaving" : "Staying", "Entering", h.getACPIState());
            h.setIndexState(PowerHost.ACPI_ENTERING);
            Log.formatLine("%.2f: Now the ACPI state is %s ", currentTime, h.getACPIState());
            
            /* Schedule to effectively power the host off at the current sleep mode time  */
            data[1] = PowerHost.ACPI_STAYING; /* 3 means powered off */
            send(getId(), h.getPowerSleepModeTime(), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data);
        }
        super.processVmDestroy(ev, ack);
    }

    /**
     * Gets the power.
     *
     * @return the power
     */
    public double getPower() {
        return power;
    }

    /**
     * Sets the power.
     *
     * @param power the new power
     */
    protected void setPower(double power) {
        this.power = power;
        //  Log.printLine("power sum = " + getPower());
    }

    /**
     * Checks if PowerDatacenter is in migration.
     *
     * @return true, if PowerDatacenter is in migration
     */
    protected boolean isInMigration() {
        boolean result = false;
        for (Vm vm : getVmList()) {
            if (vm.isInMigration()) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Checks if is disable migrations.
     *
     * @return true, if is disable migrations
     */
    public boolean isDisableMigrations() {
        return disableMigrations;
    }

    /**
     * Sets the disable migrations.
     *
     * @param disableMigrations the new disable migrations
     */
    public void setDisableMigrations(boolean disableMigrations) {
        this.disableMigrations = disableMigrations;
    }

    /**
     * Checks if is cloudlet submited.
     *
     * @return true, if is cloudlet submited
     */
    protected double getCloudletSubmitted() {
        return cloudletSubmitted;
    }

    /**
     * Sets the cloudlet submited.
     *
     * @param cloudletSubmitted the new cloudlet submited
     */
    protected void setCloudletSubmitted(double cloudletSubmitted) {
        this.cloudletSubmitted = cloudletSubmitted;
    }

    /**
     * Gets the migration count.
     *
     * @return the migration count
     */
    public int getMigrationCount() {
        return migrationCount;
    }

    /**
     * Sets the migration count.
     *
     * @param migrationCount the new migration count
     */
    protected void setMigrationCount(int migrationCount) {
        this.migrationCount = migrationCount;
    }

    /**
     * Increment migration count.
     */
    protected void incrementMigrationCount() {
        setMigrationCount(getMigrationCount() + 1);
    }
    
    @Override
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName() + ".processOtherEvent(): Error - an event is null.");
        }

        switch (ev.getTag()) {
            case CloudSimTags.END_OF_SIMULATION:
                finishsimulation = true;
                break;
            case CloudSimTags.HOST_CHANGE_SLEEP_STATE:
                Log.formatLine("%.2f: Event HOST_CHANGE_SLEEP_STATE received ", CloudSim.clock());
                updateCloudletProcessing();
                processChangeHostSleepState(ev, false);
                updateCloudletProcessing();
                //checkCloudletCompletion();
                break;
            case CloudSimTags.HOST_CHANGE_ACPI_STATE:
                Log.formatLine("%.2f: Event HOST_CHANGE_ACPI_STATE received ", CloudSim.clock());
                updateCloudletProcessing();
                processChangeHostACPIState(ev, false);
                updateCloudletProcessing();
                //checkCloudletCompletion();
                break;
        }
    }

    protected void processChangeHostSleepState(SimEvent ev, boolean ack) {
            int receivedData[] = (int[]) ev.getData();
            int hostId = receivedData[0];
            int sleepStateIndex = receivedData[1];        
            PowerHost h = (PowerHost) getHostList().get(hostId);
            
            //Log.formatLine("%.2f: Current transaction %d, New transaction %d", CloudSim.clock(), h.getIndexState(), sleepStateIndex);

            if (h.getIndexState() != sleepStateIndex) {
                
                /* The host is in the Global state 0. 
                 That is the host is now waked up */
                if(sleepStateIndex == 0)
                    h.setACPIState("G0");
         
                Log.formatLine("%.2f: Changing sleep state of host %d from %s to %s (%s)", CloudSim.clock(), hostId, h.getIndexState() == 1 ? "Entering" : h.getIndexState() == 2 ? "Leaving" : "Staying", sleepStateIndex == 1 ? "Entering" : sleepStateIndex == 2 ? "Leaving" : "Staying", h.getACPIState());

                h.setIndexState(sleepStateIndex);
                if (sleepStateIndex == PowerHost.ACPI_STAYING)
                    h.setACPIStayingTime(0);
            }
    }
    protected void processChangeHostACPIState(SimEvent ev, boolean ack) {
        int receivedData[] = (int[]) ev.getData();
        int hostId = receivedData[0];
        int ACPIState = receivedData[1];        
        PowerHost h = (PowerHost) getHostList().get(hostId);
        
        String old_ACPI_state = h.getACPIState();
        if (ACPIState == PowerHost.ACPI_G2) {
            h.setACPIState("G2");             
        } else if (ACPIState == PowerHost.ACPI_S3) {
            h.setACPIState("S3");  
        } else if (ACPIState == PowerHost.ACPI_S2) {
            h.setACPIState("S2");  
        }
        
        int[] data = new int[2];
        data[0] = hostId;
        data[1] = PowerHost.ACPI_ENTERING;
        sendNow(getId(), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data);
 
        int[] data2 = new int[2];
        data2[0] = hostId;
        data2[1] = PowerHost.ACPI_STAYING;
        send(getId(), h.getPowerSleepModeTime(PowerHost.ACPI_ENTERING), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data2);
 
        Log.formatLine("%.2f: Changing ACPI state of host %d from %s to %s", CloudSim.clock(), hostId, old_ACPI_state, h.getACPIState());
    }
    
    public int getTotal_latency_cloudlet() {
        return total_latency_cloudlet;
    }

    public void setTotal_latency_cloudlet(int total_latency_cloudlet) {
        this.total_latency_cloudlet = total_latency_cloudlet;
    }

    public double getTotal_energy_states() {
        return total_energy_states;
    }

    public void setTotal_energy_states(double total_energy_states) {
        this.total_energy_states = total_energy_states;
    }

    public double getTotal_energy_g0() {
        return total_energy_g0;
    }

    public void setTotal_energy_g0(double total_energy_g0) {
        this.total_energy_g0 = total_energy_g0;
    }
}
