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
package org.eclipse.ditto.model.things;

import static org.mutabilitydetector.unittesting.MutabilityAssert.assertInstancesOf;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.areImmutable;

import org.junit.Test;

/**
 * Unit test for {@link ThingsModelFactory}.
 */
public final class AccessControlListModelFactoryTest {


    @Test
    public void assertImmutability() {
        assertInstancesOf(AccessControlListModelFactory.class, areImmutable());
    }


    @Test(expected = UnsupportedOperationException.class)
    public void unmodifiablePermissionsAreUnmodifiable() {
        final Permissions unmodifiablePermissions =
                AccessControlListModelFactory.newUnmodifiablePermissions(Permission.READ, Permission.WRITE);
        unmodifiablePermissions.add(Permission.ADMINISTRATE);
    }

}
