package org.openmrs.module.morgue.api.dao;

import java.util.List;

import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageUnit;

public interface MorgueCompartmentDao {
	
	MorgueCompartment getCompartmentByUuid(String uuid);
	
	List<MorgueCompartment> getCompartmentsByStorageUnit(MorgueStorageUnit storageUnit);
	
	MorgueCompartment saveCompartment(MorgueCompartment compartment);
	
	void deleteCompartment(MorgueCompartment compartment);
}
