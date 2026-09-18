/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.morgue.api.dao.hibernate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageAssignment;

/**
 * Exercises the JDBC CRUD paths of {@link HibernateMorgueStorageAssignmentDao}, which replaced the
 * Hibernate mapped implementation.
 */
public class HibernateMorgueStorageAssignmentDaoTest extends BaseMorgueStorageDaoTest {
	
	private static final String COMPARTMENT_A1_UUID = "c0ffee00-0d42-4a6f-8d0f-1f1d2a3b4c61";
	
	private static final String COMPARTMENT_A2_UUID = "c0ffee00-0d42-4a6f-8d0f-1f1d2a3b4c62";
	
	private static final String VOIDED_COMPARTMENT_UUID = "c0ffee00-0d42-4a6f-8d0f-1f1d2a3b4c63";
	
	/** Patient 2 is currently occupying compartment A-1. */
	private static final int ADMITTED_PATIENT_ID = 2;
	
	/** Patient 6 was admitted to A-2 and has since been discharged. */
	private static final int DISCHARGED_PATIENT_ID = 6;
	
	/** Patient 7's assignment to B-1 was voided. */
	private static final int VOIDED_PATIENT_ID = 7;
	
	@Test
	public void getActiveAssignmentForPatient_shouldMapEveryColumnAndJoinInTheCompartmentAndPatient() {
		MorgueStorageAssignment assignment = storageAssignmentDao
		        .getActiveAssignmentForPatient(patient(ADMITTED_PATIENT_ID));
		
		assertNotNull(assignment);
		assertEquals(Integer.valueOf(1), assignment.getStorageAssignmentId());
		assertEquals(Integer.valueOf(1), assignment.getId());
		assertEquals("OCCUPIED", assignment.getStatus());
		assertNotNull(assignment.getDateAdmitted());
		assertNull(assignment.getDateDischarged());
		assertFalse(assignment.getVoided());
		assertNotNull(assignment.getCreator());
		assertNotNull(assignment.getDateCreated());
		
		assertNotNull(assignment.getPatient());
		assertEquals(Integer.valueOf(ADMITTED_PATIENT_ID), assignment.getPatient().getPatientId());
		
		// The compartment and its storage unit used to be Hibernate many-to-ones; they now come
		// from the same joined statement.
		MorgueCompartment compartment = assignment.getCompartment();
		assertNotNull(compartment);
		assertEquals(COMPARTMENT_A1_UUID, compartment.getUuid());
		assertEquals("A-1", compartment.getDisplay());
		assertNotNull(compartment.getStorageUnit());
		assertEquals("Fridge A", compartment.getStorageUnit().getDisplay());
		assertEquals(Integer.valueOf(1), compartment.getStorageUnit().getLocation().getLocationId());
	}
	
	@Test
	public void getActiveAssignmentForPatient_shouldIgnoreDischargedAssignments() {
		assertNull(storageAssignmentDao.getActiveAssignmentForPatient(patient(DISCHARGED_PATIENT_ID)));
	}
	
	@Test
	public void getActiveAssignmentForPatient_shouldIgnoreVoidedAssignments() {
		assertNull(storageAssignmentDao.getActiveAssignmentForPatient(patient(VOIDED_PATIENT_ID)));
	}
	
	@Test
	public void getActiveAssignmentForPatient_shouldReturnNullForANullPatient() {
		assertNull(storageAssignmentDao.getActiveAssignmentForPatient(null));
	}
	
	@Test
	public void getActiveAssignmentForPatient_shouldReturnNullForAnUnsavedPatient() {
		assertNull(storageAssignmentDao.getActiveAssignmentForPatient(new Patient()));
	}
	
	@Test
	public void getAssignmentsForPatient_shouldReturnDischargedButNotVoidedAssignments() {
		List<MorgueStorageAssignment> assignments = storageAssignmentDao
		        .getAssignmentsForPatient(patient(DISCHARGED_PATIENT_ID));
		
		assertEquals(1, assignments.size());
		assertEquals("DISCHARGED", assignments.get(0).getStatus());
		assertNotNull(assignments.get(0).getDateDischarged());
		
		assertTrue(storageAssignmentDao.getAssignmentsForPatient(patient(VOIDED_PATIENT_ID)).isEmpty());
	}
	
	@Test
	public void getAssignmentsForPatient_shouldReturnAnEmptyListForANullPatient() {
		assertTrue(storageAssignmentDao.getAssignmentsForPatient(null).isEmpty());
	}
	
	@Test
	public void getAssignmentsForCompartment_shouldReturnTheNonVoidedAssignmentsOfThatCompartment() {
		MorgueCompartment a1 = compartmentDao.getCompartmentByUuid(COMPARTMENT_A1_UUID);
		
		List<MorgueStorageAssignment> assignments = storageAssignmentDao.getAssignmentsForCompartment(a1);
		
		assertEquals(1, assignments.size());
		assertEquals(Integer.valueOf(1), assignments.get(0).getStorageAssignmentId());
		
		MorgueCompartment a3 = compartmentDao.getCompartmentByUuid(VOIDED_COMPARTMENT_UUID);
		assertTrue(storageAssignmentDao.getAssignmentsForCompartment(a3).isEmpty());
	}
	
	@Test
	public void getAssignmentsForCompartment_shouldReturnAnEmptyListForANullCompartment() {
		assertTrue(storageAssignmentDao.getAssignmentsForCompartment(null).isEmpty());
	}
	
	@Test
	public void getAssignmentsForLocation_shouldReturnEveryNonVoidedAssignmentWhenNoFilterIsGiven() {
		List<MorgueStorageAssignment> assignments = storageAssignmentDao.getAssignmentsForLocation(null, null, null, null,
		    null);
		
		assertEquals(2, assignments.size());
		assertEquals(Integer.valueOf(1), assignments.get(0).getStorageAssignmentId());
		assertEquals(Integer.valueOf(2), assignments.get(1).getStorageAssignmentId());
	}
	
	@Test
	public void getAssignmentsForLocation_shouldIncludeVoidedAssignmentsWhenAsked() {
		assertEquals(3, storageAssignmentDao.getAssignmentsForLocation(null, true, null, null, null).size());
	}
	
	@Test
	public void getAssignmentsForLocation_shouldFilterByTheStorageUnitsLocation() {
		// Fridge A is at location 1 and holds assignments 1 and 2; Fridge B is at location 2 and
		// holds only the voided assignment 3.
		assertEquals(2, storageAssignmentDao.getAssignmentsForLocation(location(1), false, null, null, null).size());
		assertEquals(0, storageAssignmentDao.getAssignmentsForLocation(location(2), false, null, null, null).size());
		assertEquals(1, storageAssignmentDao.getAssignmentsForLocation(location(2), true, null, null, null).size());
	}
	
	@Test
	public void getAssignmentsForLocation_shouldFilterByAdmissionDate() {
		Date cutoff = date(2026, Calendar.SEPTEMBER, 15);
		
		List<MorgueStorageAssignment> admittedAfter = storageAssignmentDao.getAssignmentsForLocation(null, false, null,
		    cutoff, null);
		assertEquals(1, admittedAfter.size());
		assertEquals(Integer.valueOf(1), admittedAfter.get(0).getStorageAssignmentId());
		
		List<MorgueStorageAssignment> admittedBefore = storageAssignmentDao.getAssignmentsForLocation(null, false, null,
		    null, cutoff);
		assertEquals(1, admittedBefore.size());
		assertEquals(Integer.valueOf(2), admittedBefore.get(0).getStorageAssignmentId());
	}
	
	@Test
	public void getAssignmentsForLocation_shouldFilterByCreationDate() {
		assertEquals(1,
		    storageAssignmentDao.getAssignmentsForLocation(null, false, date(2026, Calendar.SEPTEMBER, 15), null, null)
		            .size());
		assertEquals(0,
		    storageAssignmentDao.getAssignmentsForLocation(null, false, date(2026, Calendar.SEPTEMBER, 20), null, null)
		            .size());
	}
	
	@Test
	public void getAssignmentsForLocation_shouldCombineEveryFilter() {
		List<MorgueStorageAssignment> assignments = storageAssignmentDao.getAssignmentsForLocation(location(1), false,
		    date(2026, Calendar.SEPTEMBER, 1), date(2026, Calendar.SEPTEMBER, 1), date(2026, Calendar.SEPTEMBER, 30));
		
		assertEquals(2, assignments.size());
	}
	
	@Test
	public void saveAssignment_shouldInsertANewRowAndStampTheAuditInfo() throws SQLException {
		MorgueCompartment a3 = compartmentDao.getCompartmentByUuid(VOIDED_COMPARTMENT_UUID);
		MorgueStorageAssignment assignment = new MorgueStorageAssignment();
		assignment.setCompartment(a3);
		assignment.setPatient(patient(8));
		assignment.setDateAdmitted(new Date());
		assignment.setStatus("OCCUPIED");
		
		MorgueStorageAssignment saved = storageAssignmentDao.saveAssignment(assignment);
		
		assertNotNull(saved.getStorageAssignmentId());
		assertNotNull(saved.getCreator());
		assertNotNull(saved.getDateCreated());
		assertFalse(saved.getVoided());
		assertEquals(1, countRows("morgue_storage_assignment", "storage_assignment_id = " + saved.getStorageAssignmentId()));
		
		MorgueStorageAssignment reloaded = storageAssignmentDao.getActiveAssignmentForPatient(patient(8));
		assertNotNull(reloaded);
		assertEquals(saved.getStorageAssignmentId(), reloaded.getStorageAssignmentId());
		assertEquals("OCCUPIED", reloaded.getStatus());
		assertEquals(a3.getCompartmentId(), reloaded.getCompartment().getCompartmentId());
		assertEquals(Integer.valueOf(8), reloaded.getPatient().getPatientId());
	}
	
	@Test
	public void saveAssignment_shouldUpdateAnExistingRowWhenTheAssignmentIsDischarged() throws SQLException {
		int before = countRows("morgue_storage_assignment", null);
		MorgueStorageAssignment assignment = storageAssignmentDao
		        .getActiveAssignmentForPatient(patient(ADMITTED_PATIENT_ID));
		assignment.setDateDischarged(new Date());
		assignment.setStatus("DISCHARGED");
		
		storageAssignmentDao.saveAssignment(assignment);
		
		assertEquals(before, countRows("morgue_storage_assignment", null));
		assertNull(storageAssignmentDao.getActiveAssignmentForPatient(patient(ADMITTED_PATIENT_ID)));
		
		List<MorgueStorageAssignment> assignments = storageAssignmentDao
		        .getAssignmentsForPatient(patient(ADMITTED_PATIENT_ID));
		assertEquals(1, assignments.size());
		assertEquals("DISCHARGED", assignments.get(0).getStatus());
		assertNotNull(assignments.get(0).getDateDischarged());
		assertNotNull(assignments.get(0).getChangedBy());
		assertNotNull(assignments.get(0).getDateChanged());
	}
	
	@Test
	public void saveAssignment_shouldPersistTheVoidInfoTheRestResourceSets() {
		MorgueStorageAssignment assignment = storageAssignmentDao
		        .getActiveAssignmentForPatient(patient(ADMITTED_PATIENT_ID));
		assignment.setVoided(true);
		assignment.setVoidReason("Retired via REST");
		
		storageAssignmentDao.saveAssignment(assignment);
		
		assertTrue(storageAssignmentDao.getAssignmentsForPatient(patient(ADMITTED_PATIENT_ID)).isEmpty());
		
		List<MorgueStorageAssignment> withVoided = storageAssignmentDao.getAssignmentsForLocation(null, true, null, null,
		    null);
		MorgueStorageAssignment reloaded = withVoided.stream()
		        .filter(a -> Integer.valueOf(1).equals(a.getStorageAssignmentId())).findFirst().orElse(null);
		assertNotNull(reloaded);
		assertTrue(reloaded.getVoided());
		assertEquals("Retired via REST", reloaded.getVoidReason());
		assertNotNull(reloaded.getVoidedBy());
		assertNotNull(reloaded.getDateVoided());
	}
	
	@Test
	public void saveAssignment_shouldMoveAnAssignmentToAnotherCompartment() {
		MorgueCompartment a2 = compartmentDao.getCompartmentByUuid(COMPARTMENT_A2_UUID);
		MorgueStorageAssignment assignment = storageAssignmentDao
		        .getActiveAssignmentForPatient(patient(ADMITTED_PATIENT_ID));
		assignment.setCompartment(a2);
		
		storageAssignmentDao.saveAssignment(assignment);
		
		MorgueStorageAssignment reloaded = storageAssignmentDao.getActiveAssignmentForPatient(patient(ADMITTED_PATIENT_ID));
		assertEquals(a2.getCompartmentId(), reloaded.getCompartment().getCompartmentId());
	}
	
	@Test(expected = DAOException.class)
	public void saveAssignment_shouldRejectANullAssignment() {
		storageAssignmentDao.saveAssignment(null);
	}
	
	@Test
	public void deleteAssignment_shouldRemoveTheRow() throws SQLException {
		List<MorgueStorageAssignment> assignments = storageAssignmentDao.getAssignmentsForLocation(null, true, null, null,
		    null);
		MorgueStorageAssignment voided = assignments.stream()
		        .filter(a -> Integer.valueOf(3).equals(a.getStorageAssignmentId())).findFirst().orElse(null);
		assertNotNull(voided);
		
		storageAssignmentDao.deleteAssignment(voided);
		
		assertEquals(0, countRows("morgue_storage_assignment", "storage_assignment_id = 3"));
		assertEquals(2, storageAssignmentDao.getAssignmentsForLocation(null, true, null, null, null).size());
	}
	
	@Test(expected = DAOException.class)
	public void deleteAssignment_shouldRejectAnUnsavedAssignment() {
		storageAssignmentDao.deleteAssignment(new MorgueStorageAssignment());
	}
	
	private static Patient patient(int patientId) {
		return Context.getPatientService().getPatient(patientId);
	}
	
	private static Location location(int locationId) {
		return Context.getLocationService().getLocation(locationId);
	}
	
	private static Date date(int year, int month, int dayOfMonth) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(year, month, dayOfMonth);
		return calendar.getTime();
	}
}
