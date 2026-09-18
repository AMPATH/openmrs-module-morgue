/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.morgue.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueConfig;
import org.openmrs.module.morgue.MorgueStorageAssignment;
import org.openmrs.module.morgue.MorgueStorageUnit;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;

import org.openmrs.Location;
import org.openmrs.Patient;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface MorgueService extends OpenmrsService {
	
	List<Object[]> getPatients(String dead, String name, String uuid, Date createdOnOrAfterDate, Date createdOnOrBeforeDate,
	        String locationUuid);
	
	// Storage Unit
	MorgueStorageUnit getStorageUnitByUuid(String uuid);
	
	List<MorgueStorageUnit> getAllStorageUnits(Boolean includeVoided, Location location);
	
	MorgueStorageUnit saveStorageUnit(MorgueStorageUnit storageUnit);
	
	void deleteStorageUnit(MorgueStorageUnit storageUnit);
	
	// Compartment
	MorgueCompartment getCompartmentByUuid(String uuid);
	
	List<MorgueCompartment> getCompartmentsByStorageUnit(MorgueStorageUnit storageUnit);
	
	MorgueCompartment saveCompartment(MorgueCompartment compartment);
	
	void deleteCompartment(MorgueCompartment compartment);
	
	// Storage Assignment
	MorgueStorageAssignment getActiveAssignmentForPatient(Patient patient);
	
	List<MorgueStorageAssignment> getAssignmentsForPatient(Patient patient);
	
	List<MorgueStorageAssignment> getAssignmentsForCompartment(MorgueCompartment compartment);
	
	List<MorgueStorageAssignment> getAssignmentsForLocation(Location location, Boolean includeVoided, Date createdOnOrAfter,
	        Date admittedOnOrAfter, Date admittedOnOrBefore);
	
	List<MorgueStorageAssignment> getAssignmentsForLocation(Location location, Boolean includeVoided, String status,
	        Date createdOnOrAfter, Date admittedOnOrAfter, Date admittedOnOrBefore);
	
	MorgueStorageAssignment assignPatientToCompartment(Patient patient, MorgueCompartment compartment);
	
	MorgueStorageAssignment dischargeAssignment(MorgueStorageAssignment assignment);
	
	void deleteAssignment(MorgueStorageAssignment assignment);
}
