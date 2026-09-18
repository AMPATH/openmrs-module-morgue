/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.morgue.api.impl;

import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.impl.BaseOpenmrsService;
import java.util.Date;
import java.util.List;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageAssignment;
import org.openmrs.module.morgue.MorgueStorageUnit;
import org.openmrs.module.morgue.api.MorgueService;
import org.openmrs.module.morgue.api.dao.MorgueCompartmentDao;
import org.openmrs.module.morgue.api.dao.MorgueDao;
import org.openmrs.module.morgue.api.dao.MorgueStorageAssignmentDao;
import org.openmrs.module.morgue.api.dao.MorgueStorageUnitDao;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link MorgueService} implementation.
 * <p>
 * The class level {@code @Transactional} is load bearing, not decoration. OpenMRS wires
 * {@code transactionAttributeSource} to an {@code AnnotationTransactionAttributeSource}, so the
 * {@code TransactionProxyFactoryBean} around this service only opens a transaction for methods that
 * carry the annotation. The morgue storage DAOs write through {@code Session#doWork}/
 * {@code doReturningWork} rather than through mapped entities, so there is no Hibernate flush for
 * the session lifecycle to commit on their behalf: without a transaction the insert or update runs
 * on the pooled connection and is then discarded when it is returned to the pool (c3p0 is
 * configured with {@code autoCommitOnClose=false}), which shows up as a REST call that reports
 * success while the row never lands.
 */
@Transactional
public class MorgueServiceImpl extends BaseOpenmrsService implements MorgueService {
	
	MorgueDao dao;
	
	UserService userService;
	
	private MorgueStorageUnitDao storageUnitDao;
	
	private MorgueCompartmentDao compartmentDao;
	
	private MorgueStorageAssignmentDao storageAssignmentDao;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(MorgueDao dao) {
		this.dao = dao;
	}
	
	public void setStorageUnitDao(MorgueStorageUnitDao storageUnitDao) {
		this.storageUnitDao = storageUnitDao;
	}
	
	public void setCompartmentDao(MorgueCompartmentDao compartmentDao) {
		this.compartmentDao = compartmentDao;
	}
	
	public void setStorageAssignmentDao(MorgueStorageAssignmentDao storageAssignmentDao) {
		this.storageAssignmentDao = storageAssignmentDao;
	}
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
	
	@Override
	public List<Object[]> getPatients(String dead, String name, String uuid, Date createdOnOrAfterDate,
	        Date createdOnOrBeforeDate, String locationUuid) {
		return dao.getPatients(dead, name, uuid, createdOnOrAfterDate, createdOnOrBeforeDate, locationUuid);
	}
	
	// ----- Storage Unit -----
	
	@Override
	public MorgueStorageUnit getStorageUnitByUuid(String uuid) {
		return storageUnitDao.getStorageUnitByUuid(uuid);
	}
	
	@Override
	public List<MorgueStorageUnit> getAllStorageUnits(Boolean includeVoided, Location location) {
		return storageUnitDao.getAllStorageUnits(includeVoided, location);
	}
	
	@Override
	public MorgueStorageUnit saveStorageUnit(MorgueStorageUnit storageUnit) {
		return storageUnitDao.saveStorageUnit(storageUnit);
	}
	
	@Override
	public void deleteStorageUnit(MorgueStorageUnit storageUnit) {
		storageUnitDao.deleteStorageUnit(storageUnit);
	}
	
	// ----- Compartment -----
	
	@Override
	public MorgueCompartment getCompartmentByUuid(String uuid) {
		return compartmentDao.getCompartmentByUuid(uuid);
	}
	
	@Override
	public List<MorgueCompartment> getCompartmentsByStorageUnit(MorgueStorageUnit storageUnit) {
		return compartmentDao.getCompartmentsByStorageUnit(storageUnit);
	}
	
	@Override
	public MorgueCompartment saveCompartment(MorgueCompartment compartment) {
		return compartmentDao.saveCompartment(compartment);
	}
	
	@Override
	public void deleteCompartment(MorgueCompartment compartment) {
		compartmentDao.deleteCompartment(compartment);
	}
	
	// ----- Storage Assignment -----
	
	@Override
	public MorgueStorageAssignment getActiveAssignmentForPatient(Patient patient) {
		return storageAssignmentDao.getActiveAssignmentForPatient(patient);
	}
	
	@Override
	public List<MorgueStorageAssignment> getAssignmentsForPatient(Patient patient) {
		return storageAssignmentDao.getAssignmentsForPatient(patient);
	}
	
	@Override
	public List<MorgueStorageAssignment> getAssignmentsForLocation(Location location, Boolean includeVoided,
	        Date createdOnOrAfter, Date admittedOnOrAfter, Date admittedOnOrBefore) {
		return storageAssignmentDao.getAssignmentsForLocation(location, includeVoided, createdOnOrAfter, admittedOnOrAfter,
		    admittedOnOrBefore);
	}
	
	@Override
	public MorgueStorageAssignment assignPatientToCompartment(Patient patient, MorgueCompartment compartment) {

		if (patient == null || compartment == null) {
			throw new APIException("Patient and compartment are required");
		}

		// Business rule: a patient can't have two active assignments at once
		MorgueStorageAssignment existing = storageAssignmentDao.getActiveAssignmentForPatient(patient);
		if (existing != null) {
			throw new APIException("Patient already has an active morgue storage assignment");
		}

		List<MorgueStorageAssignment> compartmentAssignments = storageAssignmentDao
				.getAssignmentsForCompartment(compartment);
		boolean compartmentOccupied = compartmentAssignments.stream()
				.anyMatch(a -> a.getDateDischarged() == null);
		if (compartmentOccupied) {
			throw new APIException("Compartment is already occupied");
		}

		MorgueStorageAssignment assignment = new MorgueStorageAssignment();
		assignment.setPatient(patient);
		assignment.setCompartment(compartment);
		assignment.setDateAdmitted(new Date());
		assignment.setStatus("OCCUPIED");

		return storageAssignmentDao.saveAssignment(assignment);
	}
	
	@Override
	public MorgueStorageAssignment dischargeAssignment(MorgueStorageAssignment assignment) {
		
		if (assignment == null) {
			throw new APIException("Assignment is required");
		}
		if (assignment.getDateDischarged() != null) {
			throw new APIException("Assignment has already been discharged");
		}
		
		assignment.setDateDischarged(new Date());
		assignment.setStatus("DISCHARGED");
		
		return storageAssignmentDao.saveAssignment(assignment);
	}
	
	@Override
	public void deleteAssignment(MorgueStorageAssignment assignment) {
		storageAssignmentDao.deleteAssignment(assignment);
	}
}
