/*
 * Copyright text:
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.ditto.policies.model.signals.commands.exceptions;

import java.net.URI;
import java.text.MessageFormat;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.base.model.exceptions.DittoRuntimeException;
import org.eclipse.ditto.base.model.exceptions.DittoRuntimeExceptionBuilder;
import org.eclipse.ditto.base.model.headers.DittoHeaders;
import org.eclipse.ditto.base.model.json.JsonParsableException;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.policies.model.PolicyException;
import org.eclipse.ditto.policies.model.PolicyId;

/**
 * Thrown if the {@link org.eclipse.ditto.policies.model.PolicyImports} of a {@link org.eclipse.ditto.policies.model.Policy} could not be modified because
 * the number of imported policies is too large.
 */
@Immutable
@JsonParsableException(errorCode = PolicyImportsTooLargeException.ERROR_CODE)
public final class PolicyImportsTooLargeException extends DittoRuntimeException implements PolicyException {

    /**
     * Error code of this exception.
     */
    public static final String ERROR_CODE = ERROR_CODE_PREFIX + "imports.toolarge";

    private static final String DEFAULT_MESSAGE_TEMPLATE =
            "The Imports of the Policy with ID ''{0}'' could not be modified as they exceed the allowed number of ''{1}''.";

    private static final String MESSAGE_TEMPLATE_POLICY_IMPORTS =
            "The number of Imports ''{0}'' for the Policy was exceeded the allowed maximum number of ''{1}''.";
    private static final String DEFAULT_DESCRIPTION =
            "Please reduce the number of imports you're trying to add to ";

    private static final long serialVersionUID = 42995375595891879L;

    private PolicyImportsTooLargeException(final DittoHeaders dittoHeaders,
            @Nullable final String message,
            @Nullable final String description,
            @Nullable final Throwable cause,
            @Nullable final URI href) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, dittoHeaders, message, description, cause, href);
    }

    /**
     * A mutable builder for a {@code PolicyImportsTooLargeException}.
     *
     * @param policyId the identifier of the Policy.
     * @return the builder.
     */
    public static PolicyImportsTooLargeException.Builder newBuilder(final PolicyId policyId) {
        return new PolicyImportsTooLargeException.Builder(policyId);
    }

    /**
     * A mutable builder for a {@code PolicyImportsTooLargeException}.
     *
     * @param importsSize the number of imports to be added.
     * @return the builder.
     */
    public static PolicyImportsTooLargeException.Builder newBuilder(final int importsSize) {
        return new PolicyImportsTooLargeException.Builder(importsSize);
    }

    /**
     * Constructs a new {@code PolicyImportsTooLargeException} object with given message.
     *
     * @param message detail message. This message can be later retrieved by the {@link #getMessage()} method.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new PolicyImportsTooLargeException.
     * @throws NullPointerException if {@code dittoHeaders} is {@code null}.
     */
    public static PolicyImportsTooLargeException fromMessage(@Nullable final String message,
            final DittoHeaders dittoHeaders) {
        return DittoRuntimeException.fromMessage(message, dittoHeaders, new PolicyImportsTooLargeException.Builder());
    }

    /**
     * Constructs a new {@code PolicyImportsTooLargeException} object with the exception message extracted from the
     * given JSON object.
     *
     * @param jsonObject the JSON to read the {@link org.eclipse.ditto.base.model.exceptions.DittoRuntimeException.JsonFields#MESSAGE} field from.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new PolicyImportsTooLargeException.
     * @throws NullPointerException if any argument is {@code null}.
     * @throws org.eclipse.ditto.json.JsonMissingFieldException if this JsonObject did not contain an error message.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected
     * format.
     */
    public static PolicyImportsTooLargeException fromJson(final JsonObject jsonObject,
            final DittoHeaders dittoHeaders) {
        return DittoRuntimeException.fromJson(jsonObject, dittoHeaders, new PolicyImportsTooLargeException.Builder());
    }

    @Override
    public DittoRuntimeException setDittoHeaders(final DittoHeaders dittoHeaders) {
        return new PolicyImportsTooLargeException.Builder()
                .message(getMessage())
                .description(getDescription().orElse(null))
                .cause(getCause())
                .href(getHref().orElse(null))
                .dittoHeaders(dittoHeaders)
                .build();
    }

    /**
     * A mutable builder with a fluent API for a {@link org.eclipse.ditto.policies.model.signals.commands.exceptions.PolicyImportsTooLargeException}.
     */
    @NotThreadSafe
    public static final class Builder extends DittoRuntimeExceptionBuilder<PolicyImportsTooLargeException> {

        private Builder() {
            description(DEFAULT_DESCRIPTION + System.getProperty("ditto.limits.policy.imports-limit"));
        }

        private Builder(final PolicyId policyId) {
            this();
            message(MessageFormat.format(DEFAULT_MESSAGE_TEMPLATE, String.valueOf(policyId),
                    System.getProperty("ditto.limits.policy.imports-limit")));
        }

        private Builder(final int importsSize) {
            this();
            message(MessageFormat.format(MESSAGE_TEMPLATE_POLICY_IMPORTS, String.valueOf(importsSize),
                    System.getProperty("ditto.limits.policy.imports-limit")));
        }

        @Override
        protected PolicyImportsTooLargeException doBuild(final DittoHeaders dittoHeaders,
                @Nullable final String message,
                @Nullable final String description,
                @Nullable final Throwable cause,
                @Nullable final URI href) {
            return new PolicyImportsTooLargeException(dittoHeaders, message, description, cause, href);
        }
    }
}
