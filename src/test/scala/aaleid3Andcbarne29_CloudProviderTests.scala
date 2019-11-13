import com.typesafe.config.{Config, ConfigFactory}
import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test

import scala.collection.JavaConverters._
import main.aaleid3Andcbarne29_CloudProvider
import org.cloudbus.cloudsim.vms.{Vm, VmSimple}
import org.cloudbus.cloudsim.cloudlets.CloudletSimple

//DISCLAIMER:
//These tests use an instanciation of the sim to run. This sim was configured with the sim1 file

class aaleid3Andcbarne29_CloudProviderTests {

  //need to test sims with different config files
  val sim = aaleid3Andcbarne29_CloudProvider

  //This test makes sure that the horizontal load balancing for vms is working
  //We submit 2 vms to the sim, run it, and check if the sim made more vms while running
  @Test
  def horizontalVmScalingTest(): Unit = {
    val vm: List[Vm] = sim.createVms(2, List[Vm]())
    sim.broker0.submitVmList(vm.asJava)
    assertEquals(sim.broker0.getVmWaitingList.size, 2)
    sim.run
    assertTrue(sim.broker0.getVmWaitingList.size > 2)
  }

  //This test makes sure that the vertical scaling for vm resources is working
  //we set the vm resources to an initial value, then we check if that value increased during the sim
  @Test
  def verticalVmScalingIncreaseTest() = {
    val vm: List[Vm] = sim.createVms(2, List[Vm]())
    vm.foreach(vm => vm.setRam(128).setBw(250))
    sim.broker0.submitVmList(vm.asJava)

    assertEquals(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getBw.getAvailableResource, 250)
    assertEquals(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getRam.getAvailableResource, 128)

    sim.run()

    assertTrue(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getBw.getAvailableResource > 250)
    assertTrue(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getRam.getAvailableResource > 128)
  }

  //This test makes sure that the vertical scaling for vm resources is working
  //we set the vm resources to an initial value, then we check if that value decreased during the sim
  @Test
  def verticalVmScalingDecreaseTest() = {
    val vm: List[Vm] = sim.createVms(2, List[Vm]())
    vm.foreach(vm => vm.setRam(1000000).setBw(1000000))
    sim.broker0.submitVmList(vm.asJava)

    assertEquals(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getBw.getAvailableResource, 1000000)
    assertEquals(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getRam.getAvailableResource, 1000000)

    sim.run()

    assertEquals(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getBw.getAvailableResource, 1000000)
    assertEquals(sim.broker0.getVmWaitingList.get(0).asInstanceOf[VmSimple].getRam.getAvailableResource, 1000000)
  }

  //This test makes sure that the dynamic cloudlet creation portion of the sim is working
  //We create a list of 20 cloudlets, submit them to the broker and run the sim.
  //Finally we check if any more cloudlets were submitted during the sim
  @Test
  def dynamicCloudletCreationTest(): Unit = {
    val cloudlets = sim.createCloudlets(20, List[CloudletSimple]())
    sim.broker0.submitCloudletList(cloudlets.asJava)
    assertEquals(sim.broker0.getCloudletWaitingList.size, 20)

    sim.run

    assertTrue(sim.broker0.getCloudletSubmittedList.size > 20)
  }

  //This test makes sure that the cloudlet creation limit is functioning correctly
  //In this test, the sim is to load config file 1, which creates 200 cloudlets
  //The limit of creation in the sim is 15, so no more cloudlets should be made
  //We check after the sim finishes to see if any more cloudlets were submitted
  @Test
  def testDynamicCloudletCreationLimit() = {
    sim.run

    //change the value 200 to test with other configurations
    assertEquals(sim.broker0.getCloudletFinishedList.size(), 200)
  }

  //This test makes sure that the vm destruction delay is being used to destroy idle vms
  @Test
  def testVmDestructionDelay() = {
    val vms: List[Vm] = sim.createVms(30, List[Vm]())
    sim.broker0.submitVmList(vms.asJava)

    sim.run()

    assertTrue(sim.broker0.getVmWaitingList.size() < 30)
  }
}
