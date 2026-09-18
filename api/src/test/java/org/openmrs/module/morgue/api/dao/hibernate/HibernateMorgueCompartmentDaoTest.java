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
import java.util.List;

import org.junit.Test;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageUnit;

/**
 * Exercises the JDBC CRUD paths of {@link HibernateMorgueCompartmentDao}, which replaced the
 * Hibernate mapped implementation.
 */
public class HibernateMorgueCompartmentDaoTest extends BaseMorgueStorageDaoTest {
	
	private static final String FRIDGE_A_UUID = "6e9b0a5e-0d42-4a6f-8d0f-1f1d2a3b4c51";
	
	private static final String COMPARTMENT_A1_UUID = "c0ffee00-0d42-4a6f-8d0f-1f1d2a3b4c61";
	
	private static final String VOIDED_COMPARTMENT_UUID = "c0ffee00-0d42-4a6f-8d0f-1f1d2a3b4c63";
	
	@Test
	public void getCompartmentByUuid_shouldMapEveryColumnAndJoinInTheStorageUnit() {
		MorgueCompartment compartment = compartmentDao.getCompartmentByUuid(COMPARTMENT_A1_UUID);
		
		assertNotNull(compartment);
		assertEquals(Integer.valueOf(1), compartment.getCompartmentId());
		assertEquals(Integer.valueOf(1), compartment.getId());
		assertEquals(COMPARTMENT_A1_UUID, compartment.getUuid());
		assertEquals("A-1", compartment.getDisplay());
		assertFalse(compartment.getVoided());
		assertNotNull(compartment.getCreator());
		assertNotNull(compartment.getDateCreated());
		
		// The storage unit used to be a Hibernate many-to-one; it is now fetched by the same join.
		MorgueStorageUnit storageUnit = compartment.getStorageUnit();
		assertNotNull(storageUnit);
		assertEquals(Integer.valueOf(1), storageUnit.getStorageUnitId());
		assertEquals(FRIDGE_A_UUID, storageUnit.getUuid());
		assertEquals("Fridge A", storageUnit.getDisplay());
		assertNotNull(storageUnit.getLocation());
		assertEquals(Integer.valueOf(1), storageUnit.getLocation().getLocationId());
	}
	
	@Test
	public void getCompartmentByUuid_shouldReturnVoidedCompartments() {
		MorgueCompartment compartment = compartmentDao.getCompartmentByUuid(VOIDED_COMPARTMENT_UUID);
		
		assertNotNull(compartment);
		assertTrue(compartment.getVoided());
		assertEquals("Door broken", compartment.getVoidReason());
		assertNotNull(compartment.getVoidedBy());
		assertNotNull(compartment.getDateVoided());
	}
	
	@Test
	public void getCompartmentByUuid_shouldReturnNullForAnUnknownUuid() {
		assertNull(compartmentDao.getCompartmentByUuid("no-such-uuid"));
	}
	
	@Test
	public void getCompartmentByUuid_shouldReturnNullForANullUuid() {
		assertNull(compartmentDao.getCompartmentByUuid(null));
	}
	
	@Test
	public void getCompartmentsByStorageUnit_shouldReturnTheNonVoidedCompartmentsInIdOrder() {
		MorgueStorageUnit fridgeA = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		
		List<MorgueCompartment> compartments = compartmentDao.getCompartmentsByStorageUnit(fridgeA);
		
		assertEquals(2, compartments.size());
		assertEquals("A-1", compartments.get(0).getDisplay());
		assertEquals("A-2", compartments.get(1).getDisplay());
		assertEquals(fridgeA.getStorageUnitId(), compartments.get(0).getStorageUnit().getStorageUnitId());
	}
	
	@Test
	public void getCompartmentsByStorageUnit_shouldReturnAnEmptyListForAnUnsavedStorageUnit() {
		assertTrue(compartmentDao.getCompartmentsByStorageUnit(new MorgueStorageUnit()).isEmpty());
	}
	
	@Test
	public void getCompartmentsByStorageUnit_shouldReturnAnEmptyListForANullStorageUnit() {
		assertTrue(compartmentDao.getCompartmentsByStorageUnit(null).isEmpty());
	}
	
	@Test
	public void saveCompartment_shouldInsertANewRowAndStampTheAuditInfo() throws SQLException {
		MorgueStorageUnit fridgeA = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		MorgueCompartment compartment = new MorgueCompartment();
		compartment.setDisplay("A-4");
		compartment.setStorageUnit(fridgeA);
		
		MorgueCompartment saved = compartmentDao.saveCompartment(compartment);
		
		assertNotNull(saved.getCompartmentId());
		assertNotNull(saved.getCreator());
		assertNotNull(saved.getDateCreated());
		assertFalse(saved.getVoided());
		assertEquals(1, countRows("morgue_compartment", "compartment_id = " + saved.getCompartmentId()));
		
		MorgueCompartment reloaded = compartmentDao.getCompartmentByUuid(compartment.getUuid());
		assertNotNull(reloaded);
		assertEquals("A-4", reloaded.getDisplay());
		assertEquals(Integer.valueOf(1), reloaded.getStorageUnit().getStorageUnitId());
		assertEquals(3, compartmentDao.getCompartmentsByStorageUnit(fridgeA).size());
	}
	
	@Test
	public void saveCompartment_shouldUpdateAnExistingRowRatherThanInsertAnother() throws SQLException {
		int before = countRows("morgue_compartment", null);
		MorgueStorageUnit fridgeB = storageUnitDao.getStorageUnitByUuid("6e9b0a5e-0d42-4a6f-8d0f-1f1d2a3b4c52");
		MorgueCompartment compartment = compartmentDao.getCompartmentByUuid(COMPARTMENT_A1_UUID);
		compartment.setDisplay("B-2");
		compartment.setStorageUnit(fridgeB);
		
		compartmentDao.saveCompartment(compartment);
		
		assertEquals(before, countRows("morgue_compartment", null));
		
		MorgueCompartment reloaded = compartmentDao.getCompartmentByUuid(COMPARTMENT_A1_UUID);
		assertEquals("B-2", reloaded.getDisplay());
		assertEquals(Integer.valueOf(2), reloaded.getStorageUnit().getStorageUnitId());
		assertNotNull(reloaded.getChangedBy());
		assertNotNull(reloaded.getDateChanged());
	}
	
	@Test
	public void saveCompartment_shouldPersistTheVoidInfoTheRestResourceSets() {
		MorgueStorageUnit fridgeA = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		MorgueCompartment compartment = compartmentDao.getCompartmentByUuid(COMPARTMENT_A1_UUID);
		compartment.setVoided(true);
		compartment.setVoidReason("Retired via REST");
		
		compartmentDao.saveCompartment(compartment);
		
		MorgueCompartment reloaded = compartmentDao.getCompartmentByUuid(COMPARTMENT_A1_UUID);
		assertTrue(reloaded.getVoided());
		assertEquals("Retired via REST", reloaded.getVoidReason());
		assertNotNull(reloaded.getVoidedBy());
		assertNotNull(reloaded.getDateVoided());
		assertEquals(1, compartmentDao.getCompartmentsByStorageUnit(fridgeA).size());
	}
	
	@Test(expected = DAOException.class)
	public void saveCompartment_shouldRejectANullCompartment() {
		compartmentDao.saveCompartment(null);
	}
	
	@Test
	public void deleteCompartment_shouldRemoveTheRow() throws SQLException {
		MorgueCompartment compartment = compartmentDao.getCompartmentByUuid(VOIDED_COMPARTMENT_UUID);
		assertNotNull(compartment);
		
		compartmentDao.deleteCompartment(compartment);
		
		assertEquals(0, countRows("morgue_compartment", "compartment_id = 3"));
		assertNull(compartmentDao.getCompartmentByUuid(VOIDED_COMPARTMENT_UUID));
	}
	
	@Test(expected = DAOException.class)
	public void deleteCompartment_shouldRejectAnUnsavedCompartment() {
		compartmentDao.deleteCompartment(new MorgueCompartment());
	}
}
