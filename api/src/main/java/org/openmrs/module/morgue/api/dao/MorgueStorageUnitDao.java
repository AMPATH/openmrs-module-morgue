/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.morgue.api.dao;

import java.util.List;

import org.openmrs.Location;
import org.openmrs.module.morgue.MorgueStorageUnit;

/**
 * Database access for {@link MorgueStorageUnit}. Backed by hand written SQL rather than a Hibernate
 * mapping - see {@code org.openmrs.module.morgue.api.dao.hibernate.MorgueJdbcSupport}.
 */
public interface MorgueStorageUnitDao {
	
	/**
	 * Looks up a single storage unit by uuid, voided or not.
	 * 
	 * @param uuid the uuid to look for
	 * @return the matching storage unit, or {@code null} if there is none
	 */
	MorgueStorageUnit getStorageUnitByUuid(String uuid);
	
	/**
	 * Lists storage units, ordered by id.
	 * 
	 * @param includeVoided whether voided storage units should be included; {@code null} is treated
	 *            as {@code false}
	 * @param location only return storage units at this location, or {@code null} for any location
	 * @return the matching storage units, never {@code null}
	 */
	List<MorgueStorageUnit> getAllStorageUnits(Boolean includeVoided, Location location);
	
	/**
	 * Inserts the storage unit if it is new, otherwise updates the existing row.
	 * 
	 * @param storageUnit the storage unit to save
	 * @return the saved storage unit, with its id populated
	 */
	MorgueStorageUnit saveStorageUnit(MorgueStorageUnit storageUnit);
	
	/**
	 * Permanently removes the storage unit's row. Callers that only want to retire it should void
	 * it and save instead.
	 * 
	 * @param storageUnit the storage unit to delete
	 */
	void deleteStorageUnit(MorgueStorageUnit storageUnit);
}
