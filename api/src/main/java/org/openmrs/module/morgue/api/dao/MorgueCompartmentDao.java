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

import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageUnit;

/**
 * Database access for {@link MorgueCompartment}. Backed by hand written SQL rather than a Hibernate
 * mapping - see {@code org.openmrs.module.morgue.api.dao.hibernate.MorgueJdbcSupport}.
 */
public interface MorgueCompartmentDao {
	
	/**
	 * Looks up a single compartment by uuid, voided or not, with its owning storage unit populated.
	 * 
	 * @param uuid the uuid to look for
	 * @return the matching compartment, or {@code null} if there is none
	 */
	MorgueCompartment getCompartmentByUuid(String uuid);
	
	/**
	 * Lists the non voided compartments of a storage unit, ordered by id.
	 * 
	 * @param storageUnit the owning storage unit
	 * @return the matching compartments, never {@code null}
	 */
	List<MorgueCompartment> getCompartmentsByStorageUnit(MorgueStorageUnit storageUnit);
	
	/**
	 * Inserts the compartment if it is new, otherwise updates the existing row.
	 * 
	 * @param compartment the compartment to save
	 * @return the saved compartment, with its id populated
	 */
	MorgueCompartment saveCompartment(MorgueCompartment compartment);
	
	/**
	 * Permanently removes the compartment's row. Callers that only want to retire it should void it
	 * and save instead.
	 * 
	 * @param compartment the compartment to delete
	 */
	void deleteCompartment(MorgueCompartment compartment);
}
