package org.openmrs.module.morgue.api.dao;

import java.util.Date;
import java.util.List;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageAssignment;

public interface MorgueStorageAssignmentDao {
	
	MorgueStorageAssignment getActiveAssignmentForPatient(Patient patient);
	
	List<MorgueStorageAssignment> getAssignmentsForPatient(Patient patient);
	
	List<MorgueStorageAssignment> getAssignmentsForLocation(Location location, Boolean includeVoided, Date createdOnOrAfter,
	        Date admittedOnOrAfter, Date admittedOnOrBefore);
	
	List<MorgueStorageAssignment> getAssignmentsForCompartment(MorgueCompartment compartment);
	
	MorgueStorageAssignment saveAssignment(MorgueStorageAssignment assignment);
	
	void deleteAssignment(MorgueStorageAssignment assignment);
}
