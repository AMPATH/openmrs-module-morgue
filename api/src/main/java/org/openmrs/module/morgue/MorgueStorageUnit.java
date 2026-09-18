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
import org.openmrs.Location;

/**
 * A physical morgue storage unit - a fridge or bank of compartments - sited at a {@link Location}.
 * <p>
 * This is a plain {@code OpenmrsObject} POJO, <strong>not</strong> a Hibernate entity: its rows are
 * read and written with hand written SQL by
 * {@code org.openmrs.module.morgue.api.dao.hibernate.HibernateMorgueStorageUnitDao}. Registering it
 * with Hibernate adds it to the reference application's single shared {@code SessionFactory}, which
 * Spring's AOP auto-proxy creator then has to build in full while it is still resolving the
 * {@code MessageSource} bean - that pushed module install and server boot from ~90 seconds to 5-40+
 * minutes. Do not add a mapping file for this class.
 */
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
