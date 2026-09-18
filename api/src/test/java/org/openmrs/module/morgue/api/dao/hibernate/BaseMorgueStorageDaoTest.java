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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Before;
import org.openmrs.module.morgue.api.dao.MorgueCompartmentDao;
import org.openmrs.module.morgue.api.dao.MorgueStorageAssignmentDao;
import org.openmrs.module.morgue.api.dao.MorgueStorageUnitDao;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared setup for the morgue storage DAO tests.
 * <p>
 * The three morgue storage tables are deliberately not Hibernate entities, so
 * {@code hibernate.hbm2ddl.auto=create-drop} does not create them in the in-memory test database
 * the way it does for mapped classes. This class creates them itself before each test. The DDL
 * mirrors the changesets in {@code api/src/main/resources/liquibase.xml}, which remains the source
 * of truth for the real schema.
 * <p>
 * The statements are {@code if not exists} so they are idempotent across test classes sharing one
 * in-memory database, and they run before any test writes its own rows, so the implicit commit that
 * H2 performs on DDL never commits test data.
 */
public abstract class BaseMorgueStorageDaoTest extends BaseModuleContextSensitiveTest {
	
	protected static final String MORGUE_STORAGE_DATASET = "org/openmrs/module/morgue/include/morgueStorageTestDataset.xml";
	
	private static final String[] TABLE_DDL = {
	        "create table if not exists morgue_storage_unit ("
	                + "storage_unit_id int not null auto_increment primary key, "
	                + "uuid char(38) not null unique, "
	                + "display varchar(255), "
	                + "location_id int not null, "
	                + "creator int, "
	                + "date_created datetime not null, "
	                + "changed_by int, "
	                + "date_changed datetime, "
	                + "voided boolean default false, "
	                + "voided_by int, "
	                + "date_voided datetime, "
	                + "voided_reason varchar(255), "
	                + "constraint fk_morgue_storage_unit_location foreign key (location_id) references location (location_id))",
	        "create table if not exists morgue_compartment (" + "compartment_id int not null auto_increment primary key, "
	                + "uuid char(38) not null unique, " + "display varchar(255), " + "storage_unit_id int not null, "
	                + "creator int, " + "date_created datetime not null, " + "changed_by int, " + "date_changed datetime, "
	                + "voided boolean default false, " + "voided_by int, " + "date_voided datetime, "
	                + "voided_reason varchar(255), "
	                + "constraint fk_morgue_compartment_morgue_storage_unit foreign key (storage_unit_id) "
	                + "references morgue_storage_unit (storage_unit_id))",
	        "create table if not exists morgue_storage_assignment ("
	                + "storage_assignment_id int not null auto_increment primary key, " + "compartment_id int not null, "
	                + "patient_id int not null, " + "date_admitted datetime not null, " + "date_discharged datetime, "
	                + "status varchar(50) not null, " + "creator int, " + "date_created datetime not null, "
	                + "changed_by int, " + "date_changed datetime, " + "voided boolean default false, " + "voided_by int, "
	                + "date_voided datetime, " + "voided_reason varchar(255), "
	                + "constraint fk_morgue_storage_assignment_morgue_compartment foreign key (compartment_id) "
	                + "references morgue_compartment (compartment_id), "
	                + "constraint fk_morgue_storage_assignment_patient foreign key (patient_id) "
	                + "references patient (patient_id))" };
	
	@Autowired
	protected MorgueStorageUnitDao storageUnitDao;
	
	@Autowired
	protected MorgueCompartmentDao compartmentDao;
	
	@Autowired
	protected MorgueStorageAssignmentDao storageAssignmentDao;
	
	/**
	 * Creates the morgue storage tables, then loads the test rows for them.
	 * 
	 * @throws SQLException if the schema cannot be created
	 */
	@Before
	public void setUpMorgueStorageTables() throws SQLException {
		try (Statement statement = getConnection().createStatement()) {
			for (String ddl : TABLE_DDL) {
				statement.execute(ddl);
			}
		}
		
		executeDataSet(MORGUE_STORAGE_DATASET);
	}
	
	/**
	 * Counts the rows of a table straight over JDBC, to check what the DAOs actually wrote.
	 * 
	 * @param table the table to count
	 * @param where a where clause without the {@code where} keyword, or {@code null} for all rows
	 * @return the number of matching rows
	 * @throws SQLException if the count cannot be run
	 */
	protected int countRows(String table, String where) throws SQLException {
		String sql = "select count(*) from " + table + (where == null ? "" : " where " + where);
		try (Statement statement = getConnection().createStatement(); ResultSet rs = statement.executeQuery(sql)) {
			rs.next();
			return rs.getInt(1);
		}
	}
}
