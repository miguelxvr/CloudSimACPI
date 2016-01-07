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
public class PowerDatacenterACPIFabio extends Datacenter {

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

    private double performance_metric_last;
    private double performance_metric_current;

    private double lambda;
    private double alpha;
    private double check_interval;
    private double current_check_time;
    private int nG0_lasttime;
    
    private int nG0 = 0;
    private int nS2 = 0;
    private int nS3 = 0;
    private int nG2 = 0;

    private int nG0_tmp = 0;
    private int nG2_tmp = 0;
    private int nS2_tmp = 0;

    private int req_S2 = 0;
    private int req_G2 = 0;
    private boolean hasStateChanges = false;
    private double timeFrameDatacenterEnergy = 0.0;
    private double timeFrameHostEnergy = 0.0;
    private double currentTime = CloudSim.clock();
    private double minTime = Double.MAX_VALUE;
    private double timeDiff = currentTime - getLastProcessTime();
    
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
    public PowerDatacenterACPIFabio(
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

        if (finishsimulation == false) {
            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
        }

        // if some time passed since last processing
        if (currentTime > getLastProcessTime()) {

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
        currentTime = CloudSim.clock();
        minTime = Double.MAX_VALUE;
        timeDiff = currentTime - getLastProcessTime();
        timeFrameDatacenterEnergy = 0.0;
        timeFrameHostEnergy = 0.0;

        nG0 = 0;
        nS2 = 0;
        nS3 = 0;
        nG2 = 0;

        nG0_tmp = 0;
        nG2_tmp = 0;
        nS2_tmp = 0;
        
        req_S2 = 0;
        req_G2 = 0;
        hasStateChanges = false;

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

            if (current_check_time >= check_interval) {
                for (PowerHost host : this.<PowerHost>getHostList()) {
                    if (host.isACPIEnergySavingEnable()) {
                        if (host.getACPIState().equalsIgnoreCase("G0") && host.getIndexState() != PowerHost.ACPI_LEAVING) {
                            nG0_tmp++;
                        }
                        if (host.getACPIState().equalsIgnoreCase("S2") && host.getIndexState() != PowerHost.ACPI_LEAVING) {
                            nS2_tmp++;
                        }
                        if (host.getACPIState().equalsIgnoreCase("G2") && host.getIndexState() != PowerHost.ACPI_LEAVING) {
                            nG2_tmp++;
                        }
                    }
                }
                
                if(nG0_lasttime != 0) {
                    lambda = (double) ((double) nG0_tmp / (double) nG0_lasttime);// * 0.5;
                    //alpha = 1 - ((getSla_threashhold() - getSla_current()) / getSla_threashhold()) - lambda;
                    if (performance_metric_current > 0) {
                        alpha = Math.abs(((performance_metric_current - performance_metric_last)/ performance_metric_current) + lambda);
                        req_S2 = (int) (nG0_tmp * alpha);
                    } else {
                        alpha = getHostList().size() * 0.5;
                        req_S2 = (int) alpha;
                    }
                    
                    if (req_S2 > (getHostList().size() - nG0_tmp))
                        req_S2 = getHostList().size() - nG0_tmp;

                    req_G2 = (getHostList().size() - req_S2 - nG0_tmp);
                    
                    Log.formatLine("lambda -> %d / %d = %.2f", nG0_tmp, nG0_lasttime, lambda);
                    //Log.formatLine("alpha -> 1 - ((%.2f - %.2f) / %.2f) - %.2f) = %.2f", getSla_threashhold(), getSla_current(), getSla_threashhold(), lambda, alpha);
                    Log.formatLine("alpha -> ((%.2f - %.2f) / %.2f) + %.2f) = %.2f", performance_metric_current, performance_metric_last, performance_metric_current, lambda, alpha);

                    Log.formatLine("G0 -> %d", nG0_tmp);
                    Log.formatLine("S2 -> %d", nS2_tmp);
                    Log.formatLine("G2 -> %d", nG2_tmp);
                    Log.formatLine("req_S2 -> %d * %.2f = %d", nG0_tmp, alpha, req_S2);
                    Log.formatLine("req_G2 -> %d - %d - %d = %d", getHostList().size(), req_S2, nG0_tmp, req_G2);

                    //Log.formatLine("lambda %.2f", lambda);
                    //Log.formatLine("alpha %.2f", alpha);
                    Log.formatLine("requested S2 %d", req_S2);
                    Log.formatLine("requested G2 %d", req_G2);
                    
                    hasStateChanges = true;
                    Log.formatLine("\nProcessing Fabio's strategy");
                }

                current_check_time = 0;
                nG0_lasttime = nG0_tmp;
                performance_metric_last = performance_metric_current;
            }

            current_check_time += timeDiff;

            for (PowerHost host : this.<PowerHost>getHostList()) {

                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();

                if (host.isACPIEnergySavingEnable()) {
                    if (host.getACPIState().equalsIgnoreCase("G0")) {
                        nG0++;
                    } else if (host.getACPIState().equalsIgnoreCase("S3")) {
                        nS3++;
                    } else if (host.getACPIState().equalsIgnoreCase("S2")) {
                        nS2++;
                    } else if (host.getACPIState().equalsIgnoreCase("G2")) {
                        nG2++;
                    }
                }

                /* If we are in a sleep state */
                if (host.isACPIEnergySavingEnable() && !host.getACPIState().equalsIgnoreCase("G0")) {
                    if(hasStateChanges && host.getIndexState() == PowerHost.ACPI_STAYING) {
                        host.setACPIStayingTime(host.getACPIStayingTime() + timeDiff);
                        
                        if(nS2_tmp < req_S2 && host.getACPIState().equalsIgnoreCase("G2")) {
                                int[] data = new int[2];
                                
                                /* Leave from current state */
                                data[0] = host.getId();
                                data[1] = PowerHost.ACPI_LEAVING;
                                sendNow(getId(), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data);
                                host.setIsStateChanging(true);
                                
                                /* Enter into G0 */
                                int[] data2 = new int[2];
                                data2[0] = host.getId();
                                data2[1] = 0;
                                send(getId(), host.getPowerSleepModeTime(PowerHost.ACPI_LEAVING), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data2);

                                /* Change to the deeper state */
                                int[] data3 = new int[2];
                                data3[0] = host.getId();
                                data3[1] = PowerHost.ACPI_S2;

                                send(getId(), host.getPowerSleepModeTime(PowerHost.ACPI_LEAVING), CloudSimTags.HOST_CHANGE_ACPI_STATE, data3);                
                                nS2_tmp++;
                                Log.formatLine("\nSending host %d to S2 to satisfy %d/%d", host.getId(), nS2_tmp, req_S2);
                        } else if(nG2_tmp < req_G2 && nS2_tmp > req_S2 && host.getACPIState().equalsIgnoreCase("S2")) {
                                int[] data = new int[2];
                                
                                /* Leave from current state */
                                data[0] = host.getId();
                                data[1] = PowerHost.ACPI_LEAVING;
                                sendNow(getId(), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data);
                                host.setIsStateChanging(true);
                                
                                /* Enter into G0 */
                                int[] data2 = new int[2];
                                data2[0] = host.getId();
                                data2[1] = 0;
                                send(getId(), host.getPowerSleepModeTime(PowerHost.ACPI_LEAVING), CloudSimTags.HOST_CHANGE_SLEEP_STATE, data2);

                                /* Change to the deeper state */
                                int[] data3 = new int[2];
                                data3[0] = host.getId();
                                data3[1] = PowerHost.ACPI_G2;

                                send(getId(), host.getPowerSleepModeTime(PowerHost.ACPI_LEAVING), CloudSimTags.HOST_CHANGE_ACPI_STATE, data3);                
                                nG2_tmp++;
                                Log.formatLine("\nSending host %d to G2 to satisfy %d/%d", host.getId(), nG2_tmp, req_G2);
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
                if (host.getIndexState() == PowerHost.ACPI_LEAVING && host.getACPILeavingTime() > 0) {
                    host.setACPILeavingTime(host.getACPILeavingTime() - timeDiff);
                    Log.formatLine("%.2f: [Host #%d] time to leave from state %s now is %.2f%%", currentTime, host.getId(), host.getACPIState(), host.getACPILeavingTime());
                }
                        
                Log.formatLine("%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%", currentTime, host.getId(), getLastProcessTime(), previousUtilizationOfCpu * 100, utilizationOfCpu * 100);
                Log.formatLine("%.2f: [Host #%d] energy is %f W*sec\n", currentTime, host.getId(), timeFrameHostEnergy);
            }

            hasStateChanges = false;
            Log.formatLine("%.2f: Data center's sleep states G0:%d,S2:%d,S3:%d,G2:%d", currentTime, nG0, nS2, nS3, nG2);

        }

        setPower(getPower() + timeFrameDatacenterEnergy);
        Log.formatLine("%.2f: Data center's energy sum is %.2f W*sec", currentTime, getPower());
        Log.formatLine("%.2f: Data center's energy is %.2f W*sec", currentTime, timeFrameDatacenterEnergy);

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
            if (result && ((PowerHost) vm.getHost()).isACPIEnergySavingEnable()
                    && ((PowerHost) vm.getHost()).getIndexState() == PowerHost.ACPI_LEAVING) {
                PowerHost tmp_host = (PowerHost) vm.getHost();
                delay = tmp_host.getACPILeavingTime();
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
        
        cl.setHostId(host.getId());
        setPerformance_metric_current(getPerformance_metric_current() + cl.getPerformance_metric());
    }

    @Override
    protected void processVmDestroy(SimEvent ev, boolean ack) {
        Vm vm = (Vm) ev.getData();
        PowerHost h = (PowerHost) vm.getHost();
        double currentTime = CloudSim.clock();

        /* all VMs already done */
        if (h.getVmList().isEmpty()) {
            return;
        }

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
        ev = null;
        int hostId = receivedData[0];
        int sleepStateIndex = receivedData[1];
        PowerHost h = (PowerHost) getHostList().get(hostId);

        //Log.formatLine("%.2f: Current transaction %d, New transaction %d", CloudSim.clock(), h.getIndexState(), sleepStateIndex);
        if (h.getIndexState() != sleepStateIndex) {

            /* The host is in the Global state 0. 
             That is the host is now waked up */
            if (sleepStateIndex == 0) {
                h.setACPIState("G0");
            }

            Log.formatLine("%.2f: Changing sleep state of host %d from %s to %s (%s)", CloudSim.clock(), hostId, h.getIndexState() == 1 ? "Entering" : h.getIndexState() == 2 ? "Leaving" : "Staying", sleepStateIndex == 1 ? "Entering" : sleepStateIndex == 2 ? "Leaving" : "Staying", h.getACPIState());

            h.setIndexState(sleepStateIndex);
            if (sleepStateIndex == PowerHost.ACPI_STAYING) {
                h.setACPIStayingTime(0);
                h.setIsStateChanging(false);
            }
        }
    }

    protected void processChangeHostACPIState(SimEvent ev, boolean ack) {
        int receivedData[] = (int[]) ev.getData();
        ev = null;
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

    @Override
    protected void checkCloudletCompletion() {
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {
                        setPerformance_metric_current(getPerformance_metric_current() - cl.getPerformance_metric());
                        Log.formatLine("Ajusting current SLA to %.2f", getPerformance_metric_current());
        		sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                        Log.printLine("End of cloudlet " + cl.getCloudletId() + " send event CLOUDLET_RETURN");
                    }
                }
            }
        }
        //super.checkCloudletCompletion();
    }

    public double getCheck_interval() {
        return check_interval;
    }

    public void setCheck_interval(double check_interval) {
        this.check_interval = check_interval;
    }
    
    public double getPerformance_metric_last() {
        return performance_metric_last;
    }

    public void setPerformance_metric_last(double performance_metric_last) {
        this.performance_metric_last = performance_metric_last;
    }
    
    public double getPerformance_metric_current() {
        return performance_metric_current;
    }

    public void setPerformance_metric_current(double performance_metric_current) {
        this.performance_metric_current = performance_metric_current;
    }
}
