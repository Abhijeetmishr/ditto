/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.base.model.common;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public final class ILikeHelperTest {

    @Test
    public void testWildcards() {
        assertExpression("", "*", true);
        assertExpression("foo", "*", true);
        assertExpression("foo.bar", "FOO.BAR", true);
        assertExpression("foo..bar", "foo.bar", false);
        assertExpression("foo..bar", "FOO*", true);
        assertExpression("foo..bar", "*Bar", true);
        assertExpression("foo.bar.baz", "bar", false);
        assertExpression("foo.bar.baz", "*bAr*", true);
    }

    private static void assertExpression(final String value, final String expression, final boolean matches) {
        Pattern p = Pattern.compile(LikeHelper.convertToRegexSyntax(expression), Pattern.CASE_INSENSITIVE);
        Assert.assertEquals(matches, p.matcher(value).matches());
    }
}
