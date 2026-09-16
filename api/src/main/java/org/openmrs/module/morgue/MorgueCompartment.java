package org.openmrs.module.morgue;

import org.openmrs.BaseOpenmrsData;

public class MorgueCompartment extends BaseOpenmrsData {
	
	private Integer compartmentId;
	
	private String display;
	
	private MorgueStorageUnit storageUnit;
	
	public MorgueCompartment() {
	}
	
	@Override
	public Integer getId() {
		return compartmentId;
	}
	
	@Override
	public void setId(Integer id) {
		this.compartmentId = id;
	}
	
	public Integer getCompartmentId() {
		return compartmentId;
	}
	
	public void setCompartmentId(Integer compartmentId) {
		this.compartmentId = compartmentId;
	}
	
	public String getDisplay() {
		return display;
	}
	
	public void setDisplay(String display) {
		this.display = display;
	}
	
	public MorgueStorageUnit getStorageUnit() {
		return storageUnit;
	}
	
	public void setStorageUnit(MorgueStorageUnit storageUnit) {
		this.storageUnit = storageUnit;
	}
}
