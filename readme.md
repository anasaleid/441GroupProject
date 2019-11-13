#CS441 Project
##Anas Aleid and Cole Barnes
##aaleid3 and cbarne29

#Instructions:


    Using Intellij:
        1- Clone the bitbucket repo
        2- Import the project to intellij using sbt as the import method
        3- run "sbt clean compile run"
        4- The simulation should start
    Using Docker Hub:
        1- Run the command "docker pull cbarne29/aaleid3andcbarne29cloudprovider"
        2- In the same directory, run the command "docker run cbarne29/aaleid3andcbarne29cloudprovider"
        3- Simulation should start

    If you want to run the simluation with different parameters, open the project using intellij and change the
    config file that is being read from. The ConfigFactor.load method should be the first line in the "aaleid3andcbarne29cloudprovider"
    object. There should be multiple config files already made that we used when doing our analysis. Either select a different one or change the values
    however you see fit.

##Design:
    Key concepts used in the simulation:
        1- Horizontal VM Scaling: Depending on the workload, more VM instances can be created to balance the load created by the cloudlets
        2- Vertical VM Scaling: Vertical Scaling is added to each individual VM so the sim can scale up or scale down the resources it is using depending
        on the workload given to it.
        3- DatacenterBroker's default mapper is replaced with a mapper that uses a Best Fit policy to map cloudlets to vms rather than round-robin
        4- Dynamic Cloudlet Creation: Cloudlets of random length will be created dynamically while the simulation is running. This is used
        to test dynamic load balancing.
        5- Cost Estimation of Vms: costs of running the vms is estimated and displayed at the end of the simulation
        6- Automatic Vm Destruction: Vms are destroyed automatically if they are idle for too long. How long is determined by a static value
        7- To ensure that we spread the cloudlets around at the start as much as possible, the cloudlets are initially bound to a random vm. This ensures that the cloudlets
            won't all be bound to 1 vm on 1 host.

##Analysis:
####How does the number of hosts affect costs in a data center? 
        sim1 and sim2 are the two config files that were compared to test this out. They have the exact same specifications except for the fact that sim2
    has double the hosts at its disposal. With low workloads, sim1 tends to always be faster and more cost effeciant. Once you start scaling the workload up equally
    between the two sim configurations, sim 2 ends up closing the gap pretty quickly and out performs sim 1 in both cost and speed. So for low workloads, less hosts 
    might be better since there is less time being spent on allocating and deallocating VM's. This might be because the time it takes to create a new VM is longer than
    just waiting for a cloudlet in progress to finish. 

####How will costs and speed scale with workload? 
        sim 1, 3, and 4 were used in this test. They have the same specifications except the number of cloudlets (workload) initially assigned to each rises. Although
    the worload increases in a consistant manner between each sim, this might not be the case when actually running the sim. This is because we implemented dynamic
    cloudlet creation to mimic more coming in and it being dealt with dynamically. So each sim will be handling a lot more workload than that which is predefined in the
    config files. In terms of speed, the execution time for each cloudlet stays consistant between every configuration. The costs rank from sim2 being the highest and sim 1 and 3 being tied for cost.
    The total time it takes to run sim 1, 3, and 4 is 1401, 1542, and 2029 seconds respectively. So even though the workload grows by a factor of 5 for each simulation,
    the total time grows by a factor of 1.10 between sim 1 and 3 and a factor of 1.32 between sim 3 and 4.

####How does Vertical Scaling effect the efficiency of a VM?
        Vertical Scaling is the process of increasing or decreasing the amount of resources allocated to a Vm relative to the current workload. Our cloud provider uses this property to manage
    the virtual cores, bandwidth, and ram of every vm it creates. On paper, this seems like a perfect idea for all scenarios. In practice, we must keep one important player in mind, the vm's host.
    Consider sims 1-5. These sims all configure vms with relatively low system requirements. The host isnt exactly burdened for resources at the beginning, and will continue in that state until the vms
    usage increases when the Vm is given more resources. Now consider sim 6. This sim has much more demanding vms, but the same hosts. Remember that the scaling implemented here does not effect the resources
    on the host, only its Vm. So when usage goes up, if the scaling factor is set high enough, the Vm might now require more resources than the host can provide. The sim will attempt to assign it to a host that
    can handle it, but it won't be able to find one. This renders the Vm utterly useless until it has sat idle long enough to either be destoyed or scaled down based on usage. The moral of the story is that 
    Vertical scaling is very useful, but still dependent on its hosts.
        
    sim6.conf ex logs
    INFO  0.0: VmAllocationPolicySimple: Vm 1/Broker 1 has been allocated to Host 0/DC 2
    INFO  0.0: VmAllocationPolicySimple: Vm 2/Broker 1 has been allocated to Host 1/DC 2
    WARN  0.0: VmAllocationPolicySimple: No suitable host found for Vm 3/Broker 1 in Datacenter 2
    WARN  0.0: VmAllocationPolicySimple: No suitable host found for Vm 4/Broker 1 in Datacenter 2
    WARN  0.0: VmAllocationPolicySimple: No suitable host found for Vm 5/Broker 1 in Datacenter 2
    INFO  0.1: DatacenterBrokerSimple1: Trying to create Vm 3 in DatacenterSimple3 (due to lack of a suitable Host in previous one)
    INFO  0.1: DatacenterBrokerSimple1: Trying to create Vm 4 in DatacenterSimple3 (due to lack of a suitable Host in previous one)
    INFO  0.1: DatacenterBrokerSimple1: Trying to create Vm 5 in DatacenterSimple3 (due to lack of a suitable Host in previous one)
    INFO  0.1: VmAllocationPolicySimple: Vm 3/Broker 1 has been allocated to Host 0/DC 3
    INFO  0.1: VmAllocationPolicySimple: Vm 4/Broker 1 has been allocated to Host 1/DC 3
    WARN  0.1: VmAllocationPolicySimple: No suitable host found for Vm 5/Broker 1 in Datacenter 3
            
####How does Horizontal Scaling effect the overall efficieny of the provider? 
        Horizontal Scaling is the process of allocating more Vms for the sim to which new cloudlets will be sent, in order to balance the load being sent to already existing Vms. Because our cloud provider
    is also configured to dynamically create and send new cloudlets to our Vms, horizontal scaling is very useful. Consider sim 7, which starts off with a fair amount of cloudlets and leaves room to add more
    at runtime. It also has just a measely 3 Vms to handle all that load. With horizontal scaling, we create a few more vms at runtime to receive some of the new cloudlets coming in. This allows the sim to finish
    much faster, thereby shorting the load in our case because the sim doesn't have the time to make any more cloudlets. This is obviously where we diverge from real datacenters, but it still serves to communicate 
    the importance of Horizontal Scaling
        
####Why bother limiting the amount of Cloudlets being in a sim? 
        This is a problem not unique to our own sim but unique to simulations in general. We chose to design the dynamic cloudlet creation mechanism this way so that we can better analyze the results of the sims themselves.
    In a real datacenter, the workload isn't just halted when you receive a certain number of jobs. The cloud infrastructure continues to process jobs as they come in for the forseeable future. In our case however, we need to
    be able to judge the results. With high initial workloads, the sim continues to create potentially large amounts of new cloudlets while it is still working on the jobs it was given. The sim will continue to run and execute jobs,
    but it will never finish. For the purposes of this sim and this assignment, we had to implement a limit on the number of cloudlets. Although, via the config file you can specify what that limit is, how many cloudlets to every time
    new cloudlets are made, and when to start making those cloudlets. This way we preserve functionality.
        
####Why use a custom mapper? 
        The DatacenterBroker contains a default mapper for mapping cloudlets to vms. This mapper is very basic and uses the "round-robin" technique to map cloudlets to vms. This means that when mapping cloudlets to vms, it choses the vm
    at the index after the vm it chose last. It does not make any intelligent choices based on resources, workload, or anything really. To speed up this process and overally make better mapping decisions, a new mapper was created. The class 
    DatacenterBrokerCustom now exists to fulfill this purpose. It's default mapper follows a "best-fit" policy tp map cloudlets to vms. First it filters the list of available vms to those that have at least the same number of virtual cores as
    the target cloudlet does. Then, we take the vm that has the minimum amount of cores from that filtered set and bind it to the cloudlet. This noticeably speeds up the sim. Consider sim8 for this comparison
    DISCLAIMER: These measurements were taken but changing the broker0 object to a type DatacenterBrokerSimple. Also dynamic cloudlet creation was turned off to get an accurate reading.
        
    Here we see the execution time for sim8 with the custom mapper
        ================== Simulation finished at time 4707.59 ==================

            
    Now the execution time without the custom mapper
        ================== Simulation finished at time 4751.94 ==================
            
    This may not seem like much but in a real world datacenter that it makes all the difference.

##Example Output (sim1.conf):

                                                 SIMULATION RESULTS
        
        Cloudlet|Status |DC|Host|Host PEs |VM|VM PEs   |CloudletLen|CloudletPEs|StartTime|FinishTime|ExecTime
              ID|       |ID|  ID|CPU cores|ID|CPU cores|         MI|  CPU cores|  Seconds|   Seconds| Seconds
        -----------------------------------------------------------------------------------------------------
               0|SUCCESS| 2|   0|       32| 1|        6|       4000|          2|        0|         8|       8
               3|SUCCESS| 2|   0|       32| 1|        6|       4000|          2|       30|        40|      10
               1|SUCCESS| 2|   0|       32| 1|        6|      16000|          2|        8|        40|      33
               7|SUCCESS| 2|   0|       32| 1|        6|       2000|          2|       40|        44|       5
               4|SUCCESS| 2|   0|       32| 1|        6|       4000|          2|       40|        48|       9
               5|SUCCESS| 2|   0|       32| 1|        6|       4000|          2|       40|        48|       9
               6|SUCCESS| 2|   0|       32| 1|        6|       4000|          2|       40|        48|       9
               2|SUCCESS| 2|   0|       32| 1|        6|      16000|          2|       30|        62|      33
               8|SUCCESS| 2|   0|       32| 1|        6|      16000|          2|       40|        72|      33
               9|SUCCESS| 2|   0|       32| 1|        6|      30000|          2|       40|       100|      61
        -----------------------------------------------------------------------------------------------------
        ~~~~~~~Evaluating Costs~~~~~~~
        Vm #1
         Bw Cost: 40500.0
         Memory Cost: 2304.0
         Processing Cost: 3.0
         Storage Cost: 1000.0
         Total Cost: 43807.0
        Vm #2
         Bw Cost: 500.0
         Memory Cost: 256.0
         Processing Cost: 1.0
         Storage Cost: 1000.0
         Total Cost: 1757.0
        Vm #2
         Bw Cost: 500.0
         Memory Cost: 256.0
         Processing Cost: 1.0
         Storage Cost: 1000.0
         Total Cost: 1757.0