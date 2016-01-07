
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
import org.cloudbus.cloudsim.power.PowerDatacenterACPIGreenStrategy;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.cloudbus.cloudsim.power.PowerHost;

import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimpleWattPerMipsMetric;

import org.cloudbus.cloudsim.power.models.PowerModel;

import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad1;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad2;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad3;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad4;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad5;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad6;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad7;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad8;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad9;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPower_lad10;

//import org.cloudbus.cloudsim.power.models.PowerModelSleepMode;
//import org.cloudbus.cloudsim.power.sleepstates.AbstractCState;
//import org.cloudbus.cloudsim.power.sleepstates.PowerOFFCState;

import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * An example of a heterogeneous DVFS-enabled data center: the voltage and clock
 * frequency of the CPU are adjusted ideally according to current resource
 * requirements.
 */
public class Sleep_example_green {

    private static SimulationXMLParse ConfSimu;
    private static DvfsDatas ConfigDvfs;
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;

    private static int user_id;
    private static int hostsNumber = 1000;
    private static int no_cur_vm = 0;
    private static int no_cur_cloudlet = 0;
    private static int no_cur_host = 0;

    private static ArrayList<DatacenterBroker> vect_dcbroker;

    public static void main(String[] args) {

        Log.printLine("Starting Dvfs_example_simple...");
        try {
            
            hostsNumber = hostsNumber/10;
            vect_dcbroker = new ArrayList<DatacenterBroker>();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace GridSim events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            PowerDatacenterACPIGreenStrategy datacenter = createDatacenter("Datacenter_0");
            datacenter.setDisableMigrations(true);
            PowerDatacenterBroker broker = createBroker("Broker_0");

            int brokerId = broker.getId();
            user_id = brokerId;

            vmList = new ArrayList<>();
            cloudletList = new ArrayList<>();

            int time_next = 0;
            for (int i = 0; i < hostsNumber; i++) {
                
        // Host 1
                
                sendLoad(0 + time_next, 300, 100);             
                sendLoad(1800 + time_next, 2460, 100);
                sendLoad(3120 + time_next, 4560, 100);

	// Host 2
                
                sendLoad(0 + time_next, 300, 100);
                sendLoad(1920 + time_next, 2580, 100);
                sendLoad(3420 + time_next, 4680, 100);

	// Host 3
                
                sendLoad(600 + time_next, 1200, 100);
                sendLoad(3720 + time_next, 4680, 100);

	// Host 4

                sendLoad(600 + time_next, 1200, 100);
                sendLoad(3840 + time_next, 4740, 100);

	// Host 5
                
                sendLoad(820 + time_next, 1420, 100);
                sendLoad(3960 + time_next, 4800, 100);

	// Host 6

                sendLoad(880 + time_next, 1480, 100);
                sendLoad(4080 + time_next, 4860, 100);

	// Host 7

                sendLoad(840 + time_next, 1440, 100);
                sendLoad(4200 + time_next, 4920, 100);

	// Host 8

                sendLoad(900 + time_next, 1500, 100);
                sendLoad(4320 + time_next, 4980, 100);

	// Host 9
     
                sendLoad(2040 + time_next, 2700, 100);
                sendLoad(4320 + time_next, 4920, 100);

	// Host 10

                sendLoad(2160 + time_next, 2820, 100);
                sendLoad(4500 + time_next, 5700, 100);

                //time_next = time_next + 5800;
            }

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            Log.printLine(" Time to start \n\n");
            
            //Log.disable();
            double lastClock = CloudSim.startSimulation();
            //Log.enable();

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
            Log.printLine(String.format("Total Latency :  %d sec", datacenter.getTotal_latency_cloudlet()));
            Log.printLine(String.format("Total Energy States : %.8f W", (datacenter.getTotal_energy_states()/ (lastClock * 100)) * (lastClock * 100 / 3600)));
            Log.printLine(String.format("Total Energy G0 : %.8f W", (datacenter.getTotal_energy_g0() / (lastClock * 100)) * (lastClock * 100 / 3600)));
            Log.printLine();

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }

        Log.printLine("Dvfs_example_simple finished!");
    }

    private static List<Cloudlet> createCloudletList(int userId, int nb_cloudlet, long length, int IdShift) {
        List<Cloudlet> list = new ArrayList<>();

        int pesNumber = 1;
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

    private static Cloudlet createCloudlet(int userId, long length, int performance_metric) {

        int pesNumber = 1;
        long fileSize = 300;
        long outputSize = 300;

        length = ((length * 100) / (100/performance_metric));
                
        Cloudlet cloudlet = new Cloudlet(no_cur_cloudlet, length, pesNumber, fileSize, outputSize, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        cloudlet.setUserId(userId);
        cloudlet.setVmId(no_cur_cloudlet);
        no_cur_cloudlet++;
        System.out.println("Cloudlet created // No Cloudlet =  " + no_cur_cloudlet);

        return cloudlet;
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

    private static Vm createVm(int userId, double startTime, int mips) { //, int type_vm) {

        int pesNumber = 1; // number of cpus
        int ram = 128; // vm memory (MB)
        long bw = 2500; // bandwidth
        long size = 2500; // image size (MB)
        String vmm = "Xen"; // VMM name

        Vm vm = new Vm(no_cur_vm, userId, mips, pesNumber, ram, bw, size, vmm, startTime, new CloudletSchedulerSpaceShared());
        no_cur_vm++;

        System.out.println("VM created  // No VM =  " + no_cur_vm);

        return vm;
    }

    private static void sendLoad(double startTime, double finishTime, int performance_metric) {
        
        int mod = performance_metric % 100;
        int nVM = (performance_metric - mod)/100;          
            
        for (int i = 0; i < nVM; i++) { 
            vmList.add(createVm(user_id, startTime, 100));
            cloudletList.add(createCloudlet(user_id, (long) (finishTime - startTime), 100));
        }
        
        if(mod > 0) {
            vmList.add(createVm(user_id, startTime, mod));
            cloudletList.add(createCloudlet(user_id, (long) (finishTime - startTime), mod));           
        }
    }

    public static void UpdateVmList(List<Vm> list_) {
        vmList.addAll(list_);
    }
    
    private static PowerHost createPowerHost(HashMap<String, ACPIStateDatas> acpiConfig, List<Pe> peList, PowerModel powerModel) throws Exception  {
        
        PowerHost powerHost;
        int mips = 100;
        int ram = 10000; 
        long storage = 1000000; 
        int bw = 300000;
        boolean enableDVFS = true; 
       
        powerHost = new PowerHost(
                        no_cur_host,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList),
                        powerModel,
                        false,
                        enableDVFS,
                        true,
                        acpiConfig,
                        "S2");

        no_cur_host++;

        return powerHost;
    }

    private static PowerDatacenterACPIGreenStrategy createDatacenter(String name) throws Exception {

        List<PowerHost> hostList = new ArrayList<>();

        int mips = 100;
        ArrayList<Double> freqs = new ArrayList<>(); // frequencies available by the CPU
        freqs.add(70.7);// frequencies are defined in % , it make free to use Host MIPS like we want.
        freqs.add(76.5);  // frequencies must be in increase order !
        freqs.add(82.3);
        freqs.add(88.4);
        freqs.add(94.2);
        freqs.add(100.0);
       
        List<Pe> peList = new ArrayList<Pe>();

        int nb_pe = 1;
        System.out.println("Number of CPU :  " + nb_pe);

        for (int pe = 0; pe < nb_pe; pe++)
            peList.add(new Pe(pe, new PeProvisionerSimple(mips), freqs, "performance", ConfigDvfs));
        
        HashMap<String, ACPIStateDatas> acpiConfig;
        ACPIStateDatas acpiStateDatas;
        
        for (int i = 0; i < hostsNumber; i++) {

            // HOST 1
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(27, 36, 6.2);
            acpiStateDatas.setTime(3, 8, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(36, 42, 0);
            acpiStateDatas.setTime(7, 28, 5);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(30, 40, 5);
            acpiStateDatas.setTime(4, 44, 0);
            acpiConfig.put("G2", acpiStateDatas);

            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad1(peList)));

            // HOST 2
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(28, 35, 11.6);
            acpiStateDatas.setTime(2, 9, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(50, 27, 10.5);
            acpiStateDatas.setTime(9, 21, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(31, 46, 10);
            acpiStateDatas.setTime(3, 48, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad2(peList)));
    //        
            // HOST 3
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(28, 37, 6);
            acpiStateDatas.setTime(3, 7, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(39, 42, 5.4);
            acpiStateDatas.setTime(8, 31, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(30, 40, 5);
            acpiStateDatas.setTime(16, 60, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad3(peList)));

            // HOST 4
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(71, 74, 9.9);
            acpiStateDatas.setTime(3, 14, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(92, 101, 7.6);
            acpiStateDatas.setTime(9, 21, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(74, 87, 8);
            acpiStateDatas.setTime(4, 57, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad4(peList)));

            // HOST 5
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(35, 35, 7.2);
            acpiStateDatas.setTime(4, 8, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(37, 57, 6.3);
            acpiStateDatas.setTime(9, 20, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(31, 42, 5.5);
            acpiStateDatas.setTime(5, 56, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad5(peList)));

            //HOST 6
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(30, 40, 9.7);
            acpiStateDatas.setTime(3, 7, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(38, 63, 9);
            acpiStateDatas.setTime(6, 42, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(29, 55, 9.3);
            acpiStateDatas.setTime(4, 65, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad6(peList)));

            // HOST 7
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(29, 37, 10);
            acpiStateDatas.setTime(3, 4, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(35, 60, 9.2);
            acpiStateDatas.setTime(8, 39, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(29, 53, 9.9);
            acpiStateDatas.setTime(5, 50, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad7(peList)));

            // HOST 8
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(25, 46, 10.5);
            acpiStateDatas.setTime(4, 8, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(38, 62, 9.6);
            acpiStateDatas.setTime(3, 39, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(35, 61, 10.4);
            acpiStateDatas.setTime(6, 44, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad8(peList)));

            // HOST 9
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(28, 36, 7.7);
            acpiStateDatas.setTime(4, 7, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(34, 43, 7.3);
            acpiStateDatas.setTime(8, 27, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(35, 42, 8.8);
            acpiStateDatas.setTime(3, 49, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad9(peList)));

            // hOST 10
            acpiConfig = new HashMap<String, ACPIStateDatas>();
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(41, 57, 10.5);
            acpiStateDatas.setTime(2, 9, 0);
            acpiConfig.put("S2", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(65, 65, 10);
            acpiStateDatas.setTime(8, 27, 0);
            acpiConfig.put("S3", acpiStateDatas);
            acpiStateDatas = new ACPIStateDatas();
            acpiStateDatas.setPower(49, 62, 9.6);
            acpiStateDatas.setTime(3, 58, 0);
            acpiConfig.put("G2", acpiStateDatas);
            hostList.add(createPowerHost(acpiConfig, peList, new PowerModelSpecPower_lad10(peList)));
        
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

        PowerDatacenterACPIGreenStrategy powerDatacenterACPI = null;
        try {
            powerDatacenterACPI = new PowerDatacenterACPIGreenStrategy(
                    name,
                    characteristics,
                    new PowerVmAllocationPolicySimpleWattPerMipsMetric(hostList),
                    new LinkedList<Storage>(),
                    1.00); // fix to 0.1 as the Dvfs Sampling Rate in the Linux Kernel
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
                + "Resource ID" + indent + "Host ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId());

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.printLine(indent + "SUCCESS"
                        + indent + indent + cloudlet.getResourceId()
                        + indent + cloudlet.getHostId()
                        + indent + cloudlet.getVmId()
                        + indent + dft.format(cloudlet.getActualCPUTime())
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent + dft.format(cloudlet.getFinishTime())
                );
            }
        }
    }

}
