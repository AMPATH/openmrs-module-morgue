package org.openmrs.module.morgue;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

import java.util.Date;

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
