/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package src.main.java;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerAbstract;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;

/*
This class was based more on the most recent (literally 5/10) commits on the cloudsimplus repo.
This DatacenterBroker implementation uses a best fit mapper as its default mapper
 */

public class DatacenterBrokerSimple extends DatacenterBrokerAbstract {
    /**
     * Creates a new DatacenterBroker.
     *
     * @param simulation name to be associated with this entity
     */
    public DatacenterBrokerSimple(final CloudSim simulation) {
        this(simulation, "");
    }

    /**
     * Creates a DatacenterBroker giving a specific name.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name the DatacenterBroker name
     */
    public DatacenterBrokerSimple(final CloudSim simulation, final String name) {
        super(simulation, name);
        setDatacenterSupplier(this::selectDatacenterForWaitingVms);
        setFallbackDatacenterSupplier(this::selectFallbackDatacenterForWaitingVms);
        setVmMapper(this::defaultVmMapper);
    }

    /**
     * Defines the policy to select a Datacenter to Host a VM.
     * It always selects the first Datacenter from the Datacenter list.
     *
     * @return the Datacenter selected to request the creating
     * of waiting VMs or {@link Datacenter#NULL} if no suitable Datacenter was found
     */
    protected Datacenter selectDatacenterForWaitingVms() {
        return getDatacenterList().isEmpty() ? Datacenter.NULL : getDatacenterList().get(0);
    }

    /**
     * Defines the policy to select a fallback Datacenter to Host a VM
     * when a previous selected Datacenter failed to create the requested VMs.
     *
     * <p>It gets the first Datacenter that has not been tried yet.</p>
     *
     * @return the Datacenter selected to try creating
     * the remaining VMs or {@link Datacenter#NULL} if no suitable Datacenter was found
     */
    protected Datacenter selectFallbackDatacenterForWaitingVms() {
        return getDatacenterList()
                .stream()
                .filter(dc -> !getDatacenterRequestedList().contains(dc))
                .findFirst()
                .orElse(Datacenter.NULL);
    }

    @Override
    public Vm defaultVmMapper(final Cloudlet cloudlet) {
        if (cloudlet.isBindToVm()) {
            return cloudlet.getVm();
        }

        final Vm mappedVm = getVmCreatedList()
                .stream()
                .filter(vm -> vm.getNumberOfPes() >= cloudlet.getNumberOfPes())
                .min(Comparator.comparingLong(vm -> vm.getNumberOfPes()))
                .orElse(Vm.NULL);

        if(mappedVm != Vm.NULL){
            LOGGER.debug("{}: {}: {} (PEs: {}) mapped to {} (available PEs: {}, tot PEs: {})",
                    getSimulation().clock(), getName(), cloudlet, cloudlet.getNumberOfPes(), mappedVm,
                    mappedVm.getNumberOfPes(), mappedVm.getNumberOfPes());
        }
        else
        {
            LOGGER.warn(": {}: {}: {} (PEs: {}) couldn't be mapped to any VM",
                    getSimulation().clock(), getName(), cloudlet, cloudlet.getNumberOfPes());
        }
        return mappedVm;
    }

}