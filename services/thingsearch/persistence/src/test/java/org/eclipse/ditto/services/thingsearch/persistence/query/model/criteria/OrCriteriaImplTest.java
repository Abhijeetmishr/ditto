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
package org.eclipse.ditto.services.thingsearch.persistence.query.model.criteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.conversions.Bson;
import org.eclipse.ditto.model.query.criteria.Criteria;
import org.eclipse.ditto.model.query.criteria.OrCriteriaImpl;
import org.eclipse.ditto.services.thingsearch.persistence.read.criteria.visitors.CreateBsonVisitor;
import org.junit.Test;

import com.mongodb.client.model.Filters;

/**
 * Unit test for {@link OrCriteriaImpl}.
 */
public final class OrCriteriaImplTest extends AbstractCriteriaTestBase {


    @Test(expected = NullPointerException.class)
    public void orWithNullSubCriteria() {
        new OrCriteriaImpl(null);
    }


    @Test
    public void orWithEmptySubCriteria() {
        or(Collections.emptyList());
    }

    private static void or(final List<Criteria> orCriteria) {
        final Iterable<Bson> bsonObjects =
                orCriteria.stream().map(CreateBsonVisitor::sudoApply).collect(Collectors.toList());
        final Bson expectedBson = Filters.or(bsonObjects);

        final Criteria actualCriteria = new OrCriteriaImpl(orCriteria);

        assertSudoCriteria(expectedBson, actualCriteria);
    }


    @Test
    public void orWithOneSubCriteria() {
        or(Collections.singletonList(KNOWN_CRITERIA_1));
    }


    @Test
    public void orWithMoreThanOneSubCriteria() {
        or(Arrays.asList(KNOWN_CRITERIA_1, KNOWN_CRITERIA_2));
    }

}
