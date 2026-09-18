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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the transaction demarcation that the morgue storage DAOs depend on.
 * <p>
 * OpenMRS wires {@code transactionAttributeSource} to an
 * {@code AnnotationTransactionAttributeSource}, so the {@code TransactionProxyFactoryBean} around
 * {@code morgue.MorgueService} only opens a transaction for annotated methods. Because the storage
 * DAOs write through {@code Session#doWork} / {@code doReturningWork} rather than through mapped
 * entities, there is no Hibernate flush for the session lifecycle to commit on their behalf: drop
 * the annotation and every insert and update is silently discarded when the pooled connection is
 * returned, while REST still reports success.
 * <p>
 * The DAO tests cannot catch that, because {@code BaseContextSensitiveTest} is itself
 * {@code @Transactional} and so supplies the transaction that production would be missing.
 */
public class MorgueServiceImplTransactionalTest {
	
	@Test
	public void morgueServiceImpl_shouldBeAnnotatedTransactional() {
		assertNotNull("MorgueServiceImpl must stay @Transactional - without it the JDBC writes in the "
		        + "morgue storage DAOs are never committed", MorgueServiceImpl.class.getAnnotation(Transactional.class));
	}
}
