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

import java.util.Date;
import java.util.List;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageAssignment;

/**
 * Database access for {@link MorgueStorageAssignment}. Backed by hand written SQL rather than a
 * Hibernate mapping - see {@code org.openmrs.module.morgue.api.dao.hibernate.MorgueJdbcSupport}.
 */
public interface MorgueStorageAssignmentDao {
	
	/**
	 * Finds the patient's one undischarged, non voided assignment, if they have one.
	 * 
	 * @param patient the patient to look up
	 * @return the active assignment, or {@code null} if the patient has none
	 */
	MorgueStorageAssignment getActiveAssignmentForPatient(Patient patient);
	
	/**
	 * Lists all of a patient's non voided assignments, ordered by id.
	 * 
	 * @param patient the patient to look up
	 * @return the matching assignments, never {@code null}
	 */
	List<MorgueStorageAssignment> getAssignmentsForPatient(Patient patient);
	
	/**
	 * Lists assignments across a location's storage units, ordered by id.
	 * 
	 * @param location only return assignments whose storage unit is at this location, or
	 *            {@code null} for any location
	 * @param includeVoided whether voided assignments should be included; {@code null} is treated
	 *            as {@code false}
	 * @param createdOnOrAfter only return assignments created on or after this instant, or
	 *            {@code null} for no lower bound
	 * @param admittedOnOrAfter only return assignments admitted on or after this instant, or
	 *            {@code null} for no lower bound
	 * @param admittedOnOrBefore only return assignments admitted on or before this instant, or
	 *            {@code null} for no upper bound
	 * @return the matching assignments, never {@code null}
	 */
	List<MorgueStorageAssignment> getAssignmentsForLocation(Location location, Boolean includeVoided, Date createdOnOrAfter,
	        Date admittedOnOrAfter, Date admittedOnOrBefore);
	
	/**
	 * Lists the non voided assignments of a compartment, ordered by id. Used to decide whether a
	 * compartment is already occupied.
	 * 
	 * @param compartment the compartment to look up
	 * @return the matching assignments, never {@code null}
	 */
	List<MorgueStorageAssignment> getAssignmentsForCompartment(MorgueCompartment compartment);
	
	/**
	 * Inserts the assignment if it is new, otherwise updates the existing row.
	 * 
	 * @param assignment the assignment to save
	 * @return the saved assignment, with its id populated
	 */
	MorgueStorageAssignment saveAssignment(MorgueStorageAssignment assignment);
	
	/**
	 * Permanently removes the assignment's row. Callers that only want to retire it should void it
	 * and save instead.
	 * 
	 * @param assignment the assignment to delete
	 */
	void deleteAssignment(MorgueStorageAssignment assignment);
}
