package org.openmrs.module.morgue.api.model;

import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonName;
import java.util.Date;

public class CombinedPatientDetails {
	
	private Patient patient;
	
	private Person person;
	
	private PersonName personName;
	
	private Date encounterDatetime;
	
	// Constructor
	public CombinedPatientDetails(Patient patient, Person person, PersonName personName) {
		this(patient, person, personName, null);
	}
	
	public CombinedPatientDetails(Patient patient, Person person, PersonName personName, Date encounterDatetime) {
		this.patient = patient;
		this.person = person;
		this.personName = personName;
		this.encounterDatetime = encounterDatetime;
	}
	
	// Getters and Setters
	public Patient getPatient() {
		return patient;
	}
	
	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	public Person getPerson() {
		return person;
	}
	
	public void setPerson(Person person) {
		this.person = person;
	}
	
	public PersonName getPersonName() {
		return personName;
	}
	
	public void setPersonName(PersonName personName) {
		this.personName = personName;
	}
	
	public Date getEncounterDatetime() {
		return encounterDatetime;
	}
	
	public void setEncounterDatetime(Date encounterDatetime) {
		this.encounterDatetime = encounterDatetime;
	}
}
