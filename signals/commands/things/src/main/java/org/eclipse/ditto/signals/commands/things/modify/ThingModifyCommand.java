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
package org.eclipse.ditto.signals.commands.things.modify;

import javax.annotation.Nullable;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.signals.base.WithOptionalEntity;
import org.eclipse.ditto.signals.commands.things.ThingCommand;
import org.eclipse.ditto.signals.commands.things.exceptions.PoliciesConflictingException;
import org.eclipse.ditto.signals.commands.things.exceptions.PolicyIdNotAllowedException;

/**
 * Aggregates all {@link ThingCommand}s which modify the state of a {@link org.eclipse.ditto.model.things.Thing}.
 *
 * @param <T> the type of the implementing class.
 */
public interface ThingModifyCommand<T extends ThingModifyCommand> extends ThingCommand<T>, WithOptionalEntity {

    @Override
    T setDittoHeaders(DittoHeaders dittoHeaders);

    /**
     * Checks whether this command may change authorization of the Thing.
     *
     * @return {@code true} if authorization would change, {@code false} otherwise.
     */
    boolean changesAuthorization();

    /**
     * Validates that policyIdOrPlaceholder must not be defined when either initialPolicy or the policy id of the given
     * thing is not null.
     *
     * @param thingId the thing id.
     * @param initialPolicy the initial policy of the thing.
     * @param policyIdOrPlaceholder the policy id to copy.
     * @param dittoHeaders the ditto headers.
     * @throws PolicyIdNotAllowedException if the validation fails.
     */
    static void ensurePolicyCopyFromDoesNotConflictWithInlinePolicy(final String thingId,
            @Nullable final JsonObject initialPolicy, @Nullable final String policyIdOrPlaceholder,
            final DittoHeaders dittoHeaders) {

        if (policyIdOrPlaceholder != null && initialPolicy != null) {
            throw PoliciesConflictingException.newBuilder(thingId).dittoHeaders(dittoHeaders).build();
        }
    }

}
