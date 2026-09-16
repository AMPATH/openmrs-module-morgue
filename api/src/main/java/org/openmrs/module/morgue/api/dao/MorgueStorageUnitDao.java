package org.openmrs.module.morgue.api.dao;

import java.util.List;

import org.openmrs.Location;
import org.openmrs.module.morgue.MorgueStorageUnit;

public interface MorgueStorageUnitDao {
	
	MorgueStorageUnit getStorageUnitByUuid(String uuid);
	
	List<MorgueStorageUnit> getAllStorageUnits(Boolean includeVoided, Location location);
	
	MorgueStorageUnit saveStorageUnit(MorgueStorageUnit storageUnit);
	
	void deleteStorageUnit(MorgueStorageUnit storageUnit);
}
