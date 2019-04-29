/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.utils.health.mongo;

import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

import org.junit.Test;
import org.mutabilitydetector.unittesting.MutabilityAssert;

/**
 * Unit test for {@link RetrieveMongoStatus}.
 */
public class RetrieveMongoStatusTest {


    @Test
    public void assertImmutability() {
        MutabilityAssert.assertInstancesOf(RetrieveMongoStatus.class, areImmutable());
    }
}
