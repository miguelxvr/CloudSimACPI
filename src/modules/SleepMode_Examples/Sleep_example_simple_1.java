
/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */
/*
 *
 *@author Guérout Tom
 *Experiment to compare Dvfs behaviour and power consumption resultat between CloudSim and a Real Host.
 */
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import java.util.Map.Entry;
import java.util.Collections;

import org.cloudbus.cloudsim.xml.*;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.EventPostBroker;
import org.cloudbus.cloudsim.power.PowerDatacenterSimpleACPIEnergySavingStrategy;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.cloudbus.cloudsim.power.PowerHost;

import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimpleWattPerMipsMetric;

import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_BAZAR;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_Atl;
//import org.cloudbus.cloudsim.power.models.PowerModelSleepMode;
//import org.cloudbus.cloudsim.power.sleepstates.AbstractCState;
//import org.cloudbus.cloudsim.power.sleepstates.PowerOFFCState;

import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Sleep_example_simple {

    private static DvfsDatas ConfigDvfs;
    private static List<Cloudlet> cloudletList;

    private static List<Vm> vmList;
    private static int user_id;

    private static int hostsNumber = 3;

    private static int no_cur_vm = 0;
    private static int no_cur_cloudlet = 0;

    private static ArrayList<DatacenterBroker> vect_dcbroker;

    public static void main(String[] args) {

        Log.printLine("Starting Dvfs_example_simple...");

        try {

            vect_dcbroker = new ArrayList<DatacenterBroker>();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace GridSim events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            PowerDatacenterSimpleACPIEnergySavingStrategy datacenter = createDatacenter("Datacenter_0");
            datacenter.setDisableMigrations(true);

            PowerDatacenterBroker broker = createBroker("Broker_0");

            int brokerId = broker.getId();
            user_id = brokerId;

            vmList = createVms(user_id, 1, 0);
            cloudletList = createCloudletList(user_id, 30000, 3, 0);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            final int insertTimes[][] = {{300, 30000}};
            Log.printLine(String.format("insertTimes.length %d sec", insertTimes[0][1]));
                            
            // A thread that will create a new broker at 10 clock time
            Runnable monitor = new Runnable() {

                //int insertTimes[][] = {{600, 42000}, {660, 42000}, {720, 42000}, {1440, 18000}, {1560, 24000}, {1740, 18000}};

                @Override
                public void run() {
                    int i = 0;
                    while (i < insertTimes.length) {
                        CloudSim.pauseSimulation(insertTimes[i][0]);
                        while (true) {
                            if (CloudSim.isPaused()) {

                                Log.printLine("\n Adding VMs on the datacenter \n\n");

                                DatacenterBroker broker = createBroker("Broker_added_" + i);
                                int brokerId = broker.getId();

                                //Create VMs and Cloudlets and send them to broker
                                vmList = createVms(brokerId, 1, 100); //creating 1 vms
                                cloudletList = createCloudletList(brokerId, insertTimes[i][1], 1, 100); // creating 1 cloudlets

                                broker.submitVmList(vmList);
                                broker.submitCloudletList(cloudletList);

                                CloudSim.resumeSimulation();

                                break;
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        i++;
                    }
                }
            };

            new Thread(monitor).start();
            Thread.sleep(1000);

            Log.printLine(" Time to start \n\n");

            double lastClock = CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = vect_dcbroker.get(0).getCloudletReceivedList();
            for (int dcb = 1; dcb < vect_dcbroker.size(); dcb++) {
                newList.addAll(vect_dcbroker.get(dcb).getCloudletReceivedList());
            }

            Log.printLine("Received " + newList.size() + " cloudlets");

            CloudSim.stopSimulation();

            printCloudletList(newList);
            Log.printLine();
            Log.printLine(String.format("Total simulation time: %.2f sec", lastClock));
            Log.printLine(String.format("Power Sum :  %.8f W", datacenter.getPower()));
            Log.printLine(String.format("Power Average : %.8f W", datacenter.getPower() / (lastClock * 100)));
            Log.printLine(String.format("Energy consumption: %.8f Wh", (datacenter.getPower() / (lastClock * 100)) * (lastClock * 100 / 3600)));
            Log.printLine();

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }

        Log.printLine("Dvfs_example_simple finished!");
    }

    private static Cloudlet createCloudlet(int userId, int length) {

        int pesNumber = 2;
        long fileSize = 300;
        long outputSize = 300;
        int offset = no_cur_cloudlet + 1;

        Cloudlet cloudlet = new Cloudlet((offset), length, pesNumber, fileSize, outputSize, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        cloudlet.setUserId(userId);
        cloudlet.setVmId((offset));
        no_cur_cloudlet++;
        System.out.println("Cloudlet created // No Cloudlet =  " + no_cur_cloudlet);

        return cloudlet;
    }

    private static List<Cloudlet> createCloudletList(int userId, int length, int nb_cloudlet, int IdShift) {
        List<Cloudlet> list = new ArrayList<>();

        int pesNumber = 2;
        long fileSize = 300;
        long outputSize = 300;
        int offset = no_cur_cloudlet;
        System.out.println("IdShift = " + IdShift);

        for (int i = no_cur_cloudlet; i < (offset + nb_cloudlet); i++) {

            Cloudlet cloudlet = new Cloudlet((IdShift + i), length, pesNumber, fileSize, outputSize, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(userId);
            cloudlet.setVmId((IdShift + i));
            list.add(cloudlet);
            no_cur_cloudlet++;
            System.out.println("Cloudlet created // No Cloudlet =  " + no_cur_cloudlet + "  //  Cloudlet List Size = " + list.size());
        }
        return list;
    }

    public static void UpdateCloudletList(List<Cloudlet> list_) {
        cloudletList.addAll(list_);
    }

    private static List<Vm> createVms(int userId, int nb_vm, int IdShift) { //, int type_vm) {
        List<Vm> vms = new ArrayList<>();

        // VM description
        int mips = 100;
        int pesNumber = 2; // number of cpus
        int ram = 128; // vm memory (MB)
        long bw = 2500; // bandwidth
        long size = 2500; // image size (MB)
        String vmm = "Xen"; // VMM name

        System.out.println(no_cur_vm + "//" + nb_vm);
        int offset = no_cur_vm;
        for (int i = no_cur_vm; i < (offset + nb_vm); i++) {
            vms.add(
                    new Vm((IdShift + i), userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared())
            );
            no_cur_vm++;
            System.out.println("VM created  // No VM =  " + no_cur_vm + "  //  List Vm size = " + vms.size() + "  // ID Shift+1 = " + (IdShift + i) + "id vm = " + vms.get(0).getId());
        }

        return vms;
    }

    public static void UpdateVmList(List<Vm> list_) {
        vmList.addAll(list_);
    }

    private static PowerDatacenterSimpleACPIEnergySavingStrategy createDatacenter(String name) throws Exception {

        List<PowerHost> hostList = new ArrayList<>();

        double maxPower = 250; // 250W
        double staticPowerPercent = 0.7; // 70%
        int mips = 100;
        int nb_pe = 2;
        int ram = 10000; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 300000;
        boolean enableDVFS = true; // is the Dvfs enable on the host
        boolean enableSleepMode = true; // is the sleep state enable on the host
        ArrayList<Double> freqs = new ArrayList<>(); // frequencies available by the CPU
        freqs.add(70.7);// frequencies are defined in % , it make free to use Host MIPS like we want.
        freqs.add(76.5);  // frequencies must be in increase order !
        freqs.add(82.3);
        freqs.add(88.4);
        freqs.add(94.2);
        freqs.add(100.0);

        HashMap<Integer, String> govs = new HashMap<Integer, String>();  // Define wich governor is used by each CPU
        govs.put(0, "performance");  // CPU 1 use OnDemand Dvfs mode
        govs.put(1, "performance");  // CPU 1 use OnDemand Dvfs mode

        HashMap<String, ACPIStateDatas> acpiConfig = new HashMap<String, ACPIStateDatas>();

        // State G2
        ACPIStateDatas acpiStateDatas1 = new ACPIStateDatas();
        acpiStateDatas1.setPower_entering(150);
        acpiStateDatas1.setPower_leaving(150);
        acpiStateDatas1.setPower_staying(120);
        acpiStateDatas1.setTime_entering(9);
        acpiStateDatas1.setTime_leaving(7);
        acpiStateDatas1.setTime_staying(0);
        acpiConfig.put("S2", acpiStateDatas1);

        // State S3
        ACPIStateDatas acpiStateDatas2 = new ACPIStateDatas();
        acpiStateDatas2.setPower_entering(100);
        acpiStateDatas2.setPower_leaving(190);
        acpiStateDatas2.setPower_staying(20);
        acpiStateDatas2.setTime_entering(11);
        acpiStateDatas2.setTime_leaving(142);
        acpiStateDatas2.setTime_staying(0);
        acpiConfig.put("S3", acpiStateDatas2);

        // State G2
        ACPIStateDatas acpiStateDatas3 = new ACPIStateDatas();
        acpiStateDatas3.setPower_entering(60);
        acpiStateDatas3.setPower_leaving(160);
        acpiStateDatas3.setPower_staying(20);
        acpiStateDatas3.setTime_entering(6);
        acpiStateDatas3.setTime_leaving(122);
        acpiStateDatas3.setTime_staying(0);
        acpiConfig.put("G2", acpiStateDatas3);

        for (int i = 0; i < hostsNumber; i++) {

            //HostDatas tmp_host = vect_hosts.get(i);
            ConfigDvfs = new DvfsDatas();
            HashMap<String, Integer> tmp_HM_OnDemand = new HashMap<>();
            tmp_HM_OnDemand.put("up_threshold", 95);
            tmp_HM_OnDemand.put("sampling_down_factor", 100);
            HashMap<String, Integer> tmp_HM_Conservative = new HashMap<>();
            tmp_HM_Conservative.put("up_threshold", 80);
            tmp_HM_Conservative.put("down_threshold", 20);
            tmp_HM_Conservative.put("enablefreqstep", 0);
            tmp_HM_Conservative.put("freqstep", 6);
            HashMap<String, Integer> tmp_HM_UserSpace = new HashMap<>();
            tmp_HM_UserSpace.put("frequency", 3);
            ConfigDvfs.setHashMapOnDemand(tmp_HM_OnDemand);
            ConfigDvfs.setHashMapConservative(tmp_HM_Conservative);
            ConfigDvfs.setHashMapUserSpace(tmp_HM_UserSpace);

            List<Pe> peList = new ArrayList<Pe>();

            System.out.println("Number of CPU :  " + nb_pe);

            for (int pe = 0; pe < nb_pe; pe++) {
                peList.add(new Pe(pe, new PeProvisionerSimple(mips), freqs, govs.get(pe), ConfigDvfs));
            }

            hostList.add(
                    new PowerHost(
                            i,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw),
                            storage,
                            peList,
                            new VmSchedulerTimeShared(peList),
                            new PowerModelSpecPower_Atl(peList),
                            false,
                            enableDVFS,
                            true,
                            acpiConfig,
                            "G2")
            );
        }

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this resource
        double costPerBw = 0.0; // the cost of using bw in this resource

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        PowerDatacenterSimpleACPIEnergySavingStrategy powerDatacenterACPI = null;
        try {
            powerDatacenterACPI = new PowerDatacenterSimpleACPIEnergySavingStrategy(
                    name,
                    characteristics,
                    new PowerVmAllocationPolicySimpleWattPerMipsMetric(hostList),
                    new LinkedList<Storage>(),
                    0.01); // fix to 0.1 as the Dvfs Sampling Rate in the Linux Kernel
        } catch (Exception e) {
            e.printStackTrace();
        }

        return powerDatacenterACPI;
    }

    private static PowerDatacenterBroker createBroker(String name) {

        PowerDatacenterBroker broker = null;
        try {
            broker = new PowerDatacenterBroker(name);
            vect_dcbroker.add(broker);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static PowerDatacenterBroker createBrokerWithEvent(String name, EventPostBroker evtp) {

        System.out.println("Broker Creation " + name + " with PostEvent ");

        PowerDatacenterBroker broker = null;
        try {
            broker = new PowerDatacenterBroker(name, evtp);
            vect_dcbroker.add(broker);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "\t";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Resource ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId());

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.printLine(indent + "SUCCESS"
                        + indent + indent + cloudlet.getResourceId()
                        + indent + cloudlet.getVmId()
                        + indent + dft.format(cloudlet.getActualCPUTime())
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent + dft.format(cloudlet.getFinishTime())
                );
            }
        }
    }

}
