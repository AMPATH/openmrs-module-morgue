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
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.morgue.MorgueStorageUnit;

/**
 * Exercises the JDBC CRUD paths of {@link HibernateMorgueStorageUnitDao}, which replaced the
 * Hibernate mapped implementation.
 */
public class HibernateMorgueStorageUnitDaoTest extends BaseMorgueStorageDaoTest {
	
	private static final String FRIDGE_A_UUID = "6e9b0a5e-0d42-4a6f-8d0f-1f1d2a3b4c51";
	
	private static final String VOIDED_FRIDGE_UUID = "6e9b0a5e-0d42-4a6f-8d0f-1f1d2a3b4c53";
	
	@Test
	public void getStorageUnitByUuid_shouldMapEveryColumnIncludingTheLocationAndAuditInfo() {
		MorgueStorageUnit unit = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		
		assertNotNull(unit);
		assertEquals(Integer.valueOf(1), unit.getStorageUnitId());
		assertEquals(Integer.valueOf(1), unit.getId());
		assertEquals(FRIDGE_A_UUID, unit.getUuid());
		assertEquals("Fridge A", unit.getDisplay());
		assertNotNull(unit.getLocation());
		assertEquals(Integer.valueOf(1), unit.getLocation().getLocationId());
		assertNotNull(unit.getCreator());
		assertEquals(Integer.valueOf(1), unit.getCreator().getUserId());
		assertNotNull(unit.getDateCreated());
		assertNull(unit.getChangedBy());
		assertNull(unit.getDateChanged());
		assertFalse(unit.getVoided());
		assertNull(unit.getVoidedBy());
		assertNull(unit.getDateVoided());
		assertNull(unit.getVoidReason());
	}
	
	@Test
	public void getStorageUnitByUuid_shouldReturnVoidedStorageUnits() {
		MorgueStorageUnit unit = storageUnitDao.getStorageUnitByUuid(VOIDED_FRIDGE_UUID);
		
		assertNotNull(unit);
		assertTrue(unit.getVoided());
		assertNotNull(unit.getVoidedBy());
		assertNotNull(unit.getDateVoided());
		assertEquals("Decommissioned", unit.getVoidReason());
	}
	
	@Test
	public void getStorageUnitByUuid_shouldReturnNullForAnUnknownUuid() {
		assertNull(storageUnitDao.getStorageUnitByUuid("no-such-uuid"));
	}
	
	@Test
	public void getStorageUnitByUuid_shouldReturnNullForANullUuid() {
		assertNull(storageUnitDao.getStorageUnitByUuid(null));
	}
	
	@Test
	public void getAllStorageUnits_shouldExcludeVoidedStorageUnitsByDefault() {
		List<MorgueStorageUnit> units = storageUnitDao.getAllStorageUnits(false, null);
		
		assertEquals(2, units.size());
		assertEquals(Integer.valueOf(1), units.get(0).getStorageUnitId());
		assertEquals(Integer.valueOf(2), units.get(1).getStorageUnitId());
	}
	
	@Test
	public void getAllStorageUnits_shouldTreatANullIncludeVoidedAsFalse() {
		assertEquals(2, storageUnitDao.getAllStorageUnits(null, null).size());
	}
	
	@Test
	public void getAllStorageUnits_shouldIncludeVoidedStorageUnitsWhenAsked() {
		assertEquals(3, storageUnitDao.getAllStorageUnits(true, null).size());
	}
	
	@Test
	public void getAllStorageUnits_shouldFilterByLocation() {
		Location xanadu = Context.getLocationService().getLocation(2);
		
		List<MorgueStorageUnit> units = storageUnitDao.getAllStorageUnits(false, xanadu);
		
		assertEquals(1, units.size());
		assertEquals("Fridge B", units.get(0).getDisplay());
	}
	
	@Test
	public void getAllStorageUnits_shouldCombineTheLocationAndVoidedFilters() {
		Location unknown = Context.getLocationService().getLocation(1);
		
		assertEquals(1, storageUnitDao.getAllStorageUnits(false, unknown).size());
		assertEquals(2, storageUnitDao.getAllStorageUnits(true, unknown).size());
	}
	
	@Test
	public void saveStorageUnit_shouldInsertANewRowAndStampTheAuditInfo() throws SQLException {
		MorgueStorageUnit unit = new MorgueStorageUnit();
		unit.setDisplay("Fridge D");
		unit.setLocation(Context.getLocationService().getLocation(1));
		
		MorgueStorageUnit saved = storageUnitDao.saveStorageUnit(unit);
		
		assertNotNull(saved.getStorageUnitId());
		assertNotNull(saved.getCreator());
		assertEquals(Context.getAuthenticatedUser(), saved.getCreator());
		assertNotNull(saved.getDateCreated());
		assertFalse(saved.getVoided());
		assertEquals(1, countRows("morgue_storage_unit", "storage_unit_id = " + saved.getStorageUnitId()));
		
		MorgueStorageUnit reloaded = storageUnitDao.getStorageUnitByUuid(unit.getUuid());
		assertNotNull(reloaded);
		assertEquals("Fridge D", reloaded.getDisplay());
		assertEquals(Integer.valueOf(1), reloaded.getLocation().getLocationId());
		assertEquals(saved.getStorageUnitId(), reloaded.getStorageUnitId());
	}
	
	@Test
	public void saveStorageUnit_shouldUpdateAnExistingRowRatherThanInsertAnother() throws SQLException {
		int before = countRows("morgue_storage_unit", null);
		MorgueStorageUnit unit = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		unit.setDisplay("Fridge A (relabelled)");
		unit.setLocation(Context.getLocationService().getLocation(2));
		
		storageUnitDao.saveStorageUnit(unit);
		
		assertEquals(before, countRows("morgue_storage_unit", null));
		
		MorgueStorageUnit reloaded = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		assertEquals("Fridge A (relabelled)", reloaded.getDisplay());
		assertEquals(Integer.valueOf(2), reloaded.getLocation().getLocationId());
		assertNotNull(reloaded.getChangedBy());
		assertNotNull(reloaded.getDateChanged());
	}
	
	@Test
	public void saveStorageUnit_shouldPersistTheVoidInfoTheRestResourceSets() {
		MorgueStorageUnit unit = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		unit.setVoided(true);
		unit.setVoidReason("Retired via REST");
		
		storageUnitDao.saveStorageUnit(unit);
		
		MorgueStorageUnit reloaded = storageUnitDao.getStorageUnitByUuid(FRIDGE_A_UUID);
		assertTrue(reloaded.getVoided());
		assertEquals("Retired via REST", reloaded.getVoidReason());
		assertNotNull(reloaded.getVoidedBy());
		assertNotNull(reloaded.getDateVoided());
		assertEquals(1, storageUnitDao.getAllStorageUnits(false, null).size());
	}
	
	@Test(expected = DAOException.class)
	public void saveStorageUnit_shouldRejectANullStorageUnit() {
		storageUnitDao.saveStorageUnit(null);
	}
	
	@Test
	public void deleteStorageUnit_shouldRemoveTheRow() throws SQLException {
		MorgueStorageUnit unit = storageUnitDao.getStorageUnitByUuid(VOIDED_FRIDGE_UUID);
		assertNotNull(unit);
		
		storageUnitDao.deleteStorageUnit(unit);
		
		assertEquals(0, countRows("morgue_storage_unit", "storage_unit_id = 3"));
		assertNull(storageUnitDao.getStorageUnitByUuid(VOIDED_FRIDGE_UUID));
	}
	
	@Test(expected = DAOException.class)
	public void deleteStorageUnit_shouldRejectAnUnsavedStorageUnit() {
		storageUnitDao.deleteStorageUnit(new MorgueStorageUnit());
	}
}
