package main

import java.util.function.{Predicate, Supplier}
import java.util.Calendar

import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.brokers.DatacenterBroker
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.distributions.{ContinuousDistribution, UniformDistr}
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple
import org.cloudbus.cloudsim.resources._
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletSchedulerSpaceShared}
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeSharedOverSubscription
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull
import org.cloudbus.cloudsim.vms.{Vm, VmCost, VmSimple}
import org.cloudsimplus.autoscaling.{HorizontalVmScaling, HorizontalVmScalingSimple, VerticalVmScalingSimple, VmScaling}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudsimplus.listeners.EventInfo
import java.util

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import src.main.java.DatacenterBrokerSimple

object aaleid3Andcbarne29_CloudProvider {

  //Here we initialize the config object so we can pull the parameters from the sim
  val config: Config = ConfigFactory.load("sim1.conf")

  //These determine how fast we'll dynamically allocate cloudlets throughout the simulation
  val schedulingInterval : Int = config.getInt("config.schedulingInterval")
  val CLOUDLETS_CREATION_INTERVAL: Int = schedulingInterval * 2

  //Number of hosts to be created and how many cores each host will have
  val HOSTS : Int = config.getInt("config.numHosts")
  val HOST_PES : Int = config.getInt("config.hostPes")

  //Number of VMs to be created and resource characteristics for each one
  val VMS : Int = config.getInt("config.numVM")
  val VM_PES : Int = config.getInt("config.VMPes")
  val mips : Int = config.getInt("config.vmMips")
  val vmRam : Int = config.getInt("config.vmRam")
  val vmBw : Int = config.getInt("config.vmBw")
  val vmSize : Int = config.getInt("config.vmSize")

  //Number of Cloudlets to be created and resource characteristics for each one
  val CLOUDLETS : Int = config.getInt("config.numCloudlets")
  val CLOUDLET_PES : Int = config.getInt("config.cloudletPes")

  //Cloudlet length is a list of unique lengths that will be randomly pulled when creating the cloudlets
  val CLOUDLET_LENGTHS : util.List[Long] = config.getLongList("config.cloudletLengths").asInstanceOf[util.List[Long]]

  //This object is configured to help us get a random value for the cloudlet length
  val cloudletLengthRand: ContinuousDistribution = new UniformDistr(0, CLOUDLET_LENGTHS.size(), Calendar.getInstance().getTime.getTime)

  //This object is configured to help us get a random vm to bind to a cloudlet
  val vmRand: ContinuousDistribution = new UniformDistr(0, VMS, Calendar.getInstance().getTime.getTime)

  //This var keeps track of how many cloudlets have been made. It is necessary to make sure that no more cloudlets than the limit are created
  //We have to use this var because the size of the brokers cloudlet lists are not guaranteed at runtime
  var cloudletsMade = CLOUDLETS

  //This object allows us to start the sim
  //The OnClockTickListener is triggered every time unit. After x time units, every time unit mod the cloudlet creation interval, a specified amount of cloudlets are created
  val simulation = new CloudSim().addOnClockTickListener(createNewCloudlets)

  //Setting up the broker using a custom broker class
  //This was done to use a different VM mapper that makes the simulation run a bit faster
  val broker0: DatacenterBroker = new DatacenterBrokerSimple(simulation.asInstanceOf[CloudSim])

  //Here we create our 2 datacenters
  val datacenter0: Datacenter = createDatacenter
  val datacenter1: Datacenter = createDatacenter

  //Here we create a list of vms to start with
  val vmList: List[Vm] = createVms(VMS, List[Vm]())

  def main(args: Array[String]): Unit = {
    run()
  }

  def run() {
    //Setting the scheduling interval and characteristics for each datacenter
    //The scheduling interval determines how often a cloudlet will be dynamically created while the simulation is running
    //This is used to simulate how workload might be dynammically scheduled when new work arrives
    datacenter0.setSchedulingInterval(schedulingInterval)
    datacenter0.getCharacteristics
        .setCostPerBw(config.getDouble("config.bwCost"))
        .setCostPerMem(config.getDouble("config.memCost"))
        .setCostPerSecond(config.getDouble("config.costPerSecond"))
        .setCostPerStorage(config.getDouble("config.storageCost"))

    datacenter1.setSchedulingInterval(schedulingInterval)
    datacenter1.getCharacteristics
      .setCostPerBw(config.getDouble("config.bwCost"))
      .setCostPerMem(config.getDouble("config.memCost"))
      .setCostPerSecond(config.getDouble("config.costPerSecond"))
      .setCostPerStorage(config.getDouble("config.storageCost"))

    /*
         * Defines the Vm Destruction Delay Function as a lambda expression
         * so that the broker will wait x seconds before destroying an idle VM.
    */
    broker0.setVmDestructionDelayFunction(vm => config.getDouble("config.vmDestructionDelay"))

    //Here we initialize a list of cloudlists
    val cloudletList: List[Cloudlet] = createCloudlets(CLOUDLETS, List[CloudletSimple]())

    //Here we submit the vms and cloudlets that the sim will start with to the broker
    broker0.submitVmList(vmList.asJava)
    broker0.submitCloudletList(cloudletList.asJava)

    //Now we start the sim
    simulation.start()

    //Here we use a handy cloudsim tool to build a table of the results
    //dynamically allocated vms are not seen on list becuase the cloudlets report the vmId as the vm they were initially committed to the broker
    val finishedCloudlets: util.List[Cloudlet] = broker0.getCloudletFinishedList[Cloudlet]
    new CloudletsTableBuilder(finishedCloudlets).build()

  //Here we have a list of vmCost objects that allow us to estimate cost of running vms
    val vmCostList: List[VmCost] = createVmCost(broker0.getVmCreatedList.asInstanceOf[util.List[Vm]])

    //Here we print some estimated costs to run the sim
    println("~~~~~~~Evaluating Costs~~~~~~~")
    vmCostList.foreach(x => println("Vm #" + x.getVm.getId +"\n Bw Cost: " + x.getBwCost + "\n Memory Cost: " + x.getMemoryCost + "\n Processing Cost: " + x.getProcessingCost + "\n Storage Cost: " + x.getStorageCost + "\n Total Cost: " + x.getTotalCost))
  }

  //Used to generate some cost statistics to analyze once the simulation is over
  def createVmCost(vms: util.List[Vm]): List[VmCost] = {
    val vmCost: List[VmCost] = List.tabulate(vms.size())(
      n =>
        new VmCost(vms.get(n))
    )

    vmCost
  }

  /**
    * Creates a Datacenter and its Hosts.
    */
  def createDatacenter: Datacenter = {

    //recursive function to create a list of hosts
    def createHostList(hostNum: Int, accumulator: List[Host]): List[Host] = {
      hostNum match {
        case 0 => accumulator
        case x => createHostList(x - 1, createHost :: accumulator)
      }
    }

    val hostList: List[Host] = createHostList(HOSTS, List[Host]())

    new DatacenterSimple(simulation, hostList.asJava).setSchedulingInterval(schedulingInterval)

  }

  //Creating all the hosts and the PEs for each one
  def createHost: Host = {
    def createPeList(pes: Int, accumulator: List[PeSimple]): List[PeSimple] = {
      pes match {
        case 0 => accumulator
        case x => createPeList(x - 1, new PeSimple(1000) :: accumulator)
      }
    }

    val PEs: List[Pe] = createPeList(HOST_PES, List[PeSimple]())
    val ram: Long = config.getLong("config.hostRam") //in Megabytes
    val bw: Long = config.getLong("config.hostBw") //in Megabits/s
    val storage: Long = config.getLong("config.hostStorage")
    /*
            Uses ResourceProvisionerSimple by default for RAM and BW provisioning
            and VmSchedulerSpaceShared for VM scheduling.
    */
    new HostSimple(ram, bw, storage, PEs.asJava)
      .setRamProvisioner(new ResourceProvisionerSimple())
      .setBwProvisioner(new ResourceProvisionerSimple())
      .setVmScheduler(new VmSchedulerTimeSharedOverSubscription())
  }

  /**
    * Creates a list of VMs.
    */
  @tailrec
  def createVms(vms: Int, accumulator: List[Vm]): List[Vm] = {
    vms match {
      case 0 => accumulator
      case x => createVms(x - 1, addVmScaling(createNewVm(x)) :: accumulator)
    }
  }

  //This is used when a new VM is being created dynamically in order to adapt to the workload
  def createNewVmSupplier: Supplier[Vm] = {
    new Supplier[Vm] {
      override def get(): Vm = {
        val id = broker0.getVmCreatedList.size()
        val newVm: Vm = new VmSimple(id + 1, mips, VM_PES)
          .setRam(vmRam)
          .setBw(vmBw)
          .setSize(vmSize)
          .setCloudletScheduler(new CloudletSchedulerSpaceShared())

        //Here we make sure to add the vertical and horizontal scaling to the new vm
        addVmScaling(newVm)

        //we submit the new vm to the broker for use
        broker0.submitVm(newVm)

        newVm
      }
    }
  }

  //helper method for the recursive function
  def createNewVm(x: Int): Vm = {
    new VmSimple(x, mips, VM_PES)
      .setRam(vmRam)
      .setBw(vmBw)
      .setSize(vmSize)
      .setCloudletScheduler(new CloudletSchedulerSpaceShared())
  }

  //Adds the vertical and horizontal scaling for each VM
  def addVmScaling(newVm: Vm): Vm = {
    //The scaling factor is used to calculate to a specific magnitude how much of the resource to allocate
    val scalingFactor = config.getDouble("config.scalingFactor")

    //Here we setup the vertical scaling objects for cores, bandwidth and ram
    val vPEscaling: VerticalVmScalingSimple = new VerticalVmScalingSimple(classOf[Processor], scalingFactor)
    val vBWscaling: VerticalVmScalingSimple = new VerticalVmScalingSimple(classOf[Bandwidth], scalingFactor)
    val vRAMscaling: VerticalVmScalingSimple = new VerticalVmScalingSimple(classOf[Ram], scalingFactor)

    //Here we set a lambda function for each type of vertical scaling
    vPEscaling.setResourceScaling(vs => 2 * vs.getScalingFactor() * vs.getAllocatedResource())
    vBWscaling.setResourceScaling(vs => 2 * vs.getScalingFactor() * vs.getAllocatedResource())
    vRAMscaling.setResourceScaling(vs => 2 * vs.getScalingFactor() * vs.getAllocatedResource())

    //There is a functions for lower and upper thresholds for each resource. They simply return the value of the threshold
    vPEscaling.setLowerThresholdFunction(lowerPE_ThresholdFunction)
    vPEscaling.setUpperThresholdFunction(upperPE_ThresholdFunction)

    vBWscaling.setLowerThresholdFunction(lowerBW_ThresholdFunction)
    vBWscaling.setUpperThresholdFunction(upperBW_ThresholdFunction)

    vRAMscaling.setLowerThresholdFunction(lowerRAM_ThresholdFunction)
    vRAMscaling.setUpperThresholdFunction(upperRAM_ThresholdFunction)

    //Here we set up the horizontal scaling object
    //we give it a lambda function that will create the newVm and another to call to determine if a vm is overloaded
    val hScaling: VmScaling = new HorizontalVmScalingSimple()
      .setVmSupplier(createNewVmSupplier)
      .setOverloadPredicate(isVmOverloaded(newVm))

    //finally we add all of the created scalings to the vm
    newVm.setPeVerticalScaling(vPEscaling)
    newVm.setBwVerticalScaling(vBWscaling)
    newVm.setRamVerticalScaling(vRAMscaling)
    newVm.setHorizontalScaling(hScaling.asInstanceOf[HorizontalVmScaling])
    newVm
  }

  //Create a list of cloudlets
  //uses utilizationmodelfull which makes a cloudlet always use 100% of its allocated resources all the time
  @tailrec
  def createCloudlets(clds: Int, accumulator: List[CloudletSimple]): List[CloudletSimple] = {
    clds match {
      case 0 => accumulator
      case x => createCloudlets(x - 1, new CloudletSimple(CLOUDLET_LENGTHS.get(cloudletLengthRand.sample().asInstanceOf[Int]),
        CLOUDLET_PES,
        new UtilizationModelFull())
        .setVm(vmList(vmRand.sample().asInstanceOf[Int])).asInstanceOf[CloudletSimple] :: accumulator)
    }
  }

  //This method is called by the sims OnClockTickListener
  //After some amount of time units, if the current time % creation interval ==0, then n new cloudlets are made
  //a limit is set on the dynamic creation of cloudlets to make sure the sim can finish
  //In cases where the coming workload is very high, the sim can't keep up with the workload and the dynamic cloudlets
  def createNewCloudlets(eventInfo: EventInfo): Unit = {
    val time: Long = eventInfo.getTime.asInstanceOf[Long]

    //add to config how many new cloudlets made
    if (time % CLOUDLETS_CREATION_INTERVAL == 0 && time <= config.getInt("config.cloudletCreationTimeLimit") && cloudletsMade < config.getLong("config.CloudletLimit")) {
      val newCloudlets: List[Cloudlet] = createCloudlets(config.getInt("config.cloudletCreationAmount"), List[CloudletSimple]())

      //add to keep track of all the cloudlets
      cloudletsMade += config.getInt("config.cloudletCreationAmount")

      broker0.submitCloudletList(newCloudlets.asJava)
    }
  }

  //These predicates give the thresholds for the vm scaling

  def isVmOverloaded(vm: Vm): Predicate[Vm] = {
    new Predicate[Vm] {
      override def test(t: Vm): Boolean = vm.getCpuPercentUsage > config.getDouble("config.cpuUsageThreshold")
    }
  }

  def lowerPE_ThresholdFunction(vm: Vm): Double = config.getDouble("config.lowerPeThreshold")

  def upperPE_ThresholdFunction(vm: Vm): Double = config.getDouble("config.upperPeThreshold")

  def lowerBW_ThresholdFunction(vm: Vm): Double = config.getDouble("config.lowerBwThreshold")

  def upperBW_ThresholdFunction(vm: Vm): Double = config.getDouble("config.upperBwThreshold")

  def lowerRAM_ThresholdFunction(vm: Vm): Double = config.getDouble("config.lowerRAMThreshold")

  def upperRAM_ThresholdFunction(vm: Vm): Double = config.getDouble("config.upperRAMThreshold")

}
