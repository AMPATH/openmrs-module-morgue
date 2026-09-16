package org.openmrs.module.morgue;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Location;

public class MorgueStorageUnit extends BaseOpenmrsData {
	
	private Integer storageUnitId;
	
	private String display;
	
	private Location location;
	
	public MorgueStorageUnit() {
		
	}
	
	@Override
	public Integer getId() {
		return storageUnitId;
	}
	
	@Override
	public void setId(Integer id) {
		this.storageUnitId = id;
	}
	
	public Integer getStorageUnitId() {
		return storageUnitId;
	}
	
	public void setStorageUnitId(Integer storageUnitId) {
		this.storageUnitId = storageUnitId;
	}
	
	public String getDisplay() {
		return display;
	}
	
	public void setDisplay(String display) {
		this.display = display;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
}
