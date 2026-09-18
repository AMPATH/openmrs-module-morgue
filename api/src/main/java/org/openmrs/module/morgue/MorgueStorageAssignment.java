/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.morgue;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

import java.util.Date;

/**
 * The placement of a deceased patient into a {@link MorgueCompartment}, from admission to
 * discharge.
 * <p>
 * This is a plain {@code OpenmrsObject} POJO, <strong>not</strong> a Hibernate entity: its rows are
 * read and written with hand written SQL by
 * {@code org.openmrs.module.morgue.api.dao.hibernate.HibernateMorgueStorageAssignmentDao}.
 * Registering it with Hibernate adds it to the reference application's single shared
 * {@code SessionFactory}, which Spring's AOP auto-proxy creator then has to build in full while it
 * is still resolving the {@code MessageSource} bean - that pushed module install and server boot
 * from ~90 seconds to 5-40+ minutes. Do not add a mapping file for this class.
 * <p>
 * The {@code morgue_storage_assignment} table has no {@code uuid} column, so the uuid inherited
 * from {@code BaseOpenmrsObject} is generated in memory and never persisted.
 */
public class MorgueStorageAssignment extends BaseOpenmrsData {
	
	private Integer storageAssignmentId;
	
	private MorgueCompartment compartment;
	
	private Patient patient;
	
	private Date dateAdmitted;
	
	private Date dateDischarged;
	
	private String status;
	
	public MorgueStorageAssignment() {
	}
	
	@Override
	public Integer getId() {
		return storageAssignmentId;
	}
	
	@Override
	public void setId(Integer id) {
		this.storageAssignmentId = id;
	}
	
	public Integer getStorageAssignmentId() {
		return storageAssignmentId;
	}
	
	public void setStorageAssignmentId(Integer storageAssignmentId) {
		this.storageAssignmentId = storageAssignmentId;
	}
	
	public MorgueCompartment getCompartment() {
		return compartment;
	}
	
	public void setCompartment(MorgueCompartment compartment) {
		this.compartment = compartment;
	}
	
	public Patient getPatient() {
		return patient;
	}
	
	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	public Date getDateAdmitted() {
		return dateAdmitted;
	}
	
	public void setDateAdmitted(Date dateAdmitted) {
		this.dateAdmitted = dateAdmitted;
	}
	
	public Date getDateDischarged() {
		return dateDischarged;
	}
	
	public void setDateDischarged(Date dateDischarged) {
		this.dateDischarged = dateDischarged;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
}
