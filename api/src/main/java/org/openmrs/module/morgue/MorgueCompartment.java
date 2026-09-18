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

/**
 * A single compartment within a {@link MorgueStorageUnit}, into which one body is placed.
 * <p>
 * This is a plain {@code OpenmrsObject} POJO, <strong>not</strong> a Hibernate entity: its rows are
 * read and written with hand written SQL by
 * {@code org.openmrs.module.morgue.api.dao.hibernate.HibernateMorgueCompartmentDao}. Registering it
 * with Hibernate adds it to the reference application's single shared {@code SessionFactory}, which
 * Spring's AOP auto-proxy creator then has to build in full while it is still resolving the
 * {@code MessageSource} bean - that pushed module install and server boot from ~90 seconds to 5-40+
 * minutes. Do not add a mapping file for this class.
 */
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
