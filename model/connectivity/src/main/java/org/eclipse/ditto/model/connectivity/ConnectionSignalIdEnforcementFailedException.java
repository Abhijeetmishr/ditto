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
package org.eclipse.ditto.model.connectivity;

import java.net.URI;
import java.text.MessageFormat;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeExceptionBuilder;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.JsonParsableException;

/**
 * Thrown if the enforcement of the Signal ID (e.g. a Thing ID) failed.
 */
@Immutable
@JsonParsableException(errorCode = ConnectionSignalIdEnforcementFailedException.ERROR_CODE)
public final class ConnectionSignalIdEnforcementFailedException extends DittoRuntimeException
        implements ConnectivityException {

    /**
     * Error code of this exception.
     */
    public static final String ERROR_CODE = ERROR_CODE_PREFIX + "connection.id.enforcement.failed";

    private static final String MESSAGE_TEMPLATE =
            "The configured filters could not be matched against the given target with ID ''{0}''.";

    private static final String DEFAULT_DESCRIPTION =
            "Either modify the configured filter or ensure that the message is sent via the correct ID.";

    private static final long serialVersionUID = 2672495302389903822L;

    private ConnectionSignalIdEnforcementFailedException(final DittoHeaders dittoHeaders,
            @Nullable final String message,
            @Nullable final String description,
            @Nullable final Throwable cause,
            @Nullable final URI href) {
        super(ERROR_CODE, HttpStatusCode.BAD_REQUEST, dittoHeaders, message, description, cause, href);
    }

    /**
     * A mutable builder for a {@link ConnectionSignalIdEnforcementFailedException}.
     *
     * @param target the enforcement target.
     * @return the builder.
     */
    public static Builder newBuilder(final String target) {
        return new Builder(target);
    }

    /**
     * Constructs a new {@code ConnectionSignalIdEnforcementFailedException} object with given message.
     *
     * @param message detail message. This message can be later retrieved by the {@link #getMessage()} method.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new ConnectionSignalIdEnforcementFailedException.
     */
    public static ConnectionSignalIdEnforcementFailedException fromMessage(final String message,
            final DittoHeaders dittoHeaders) {
        return new Builder()
                .dittoHeaders(dittoHeaders)
                .message(message)
                .build();
    }

    /**
     * Constructs a new {@code ConnectionSignalIdEnforcementFailedException} object with the exception message extracted from
     * the given JSON object.
     *
     * @param jsonObject the JSON to read the {@link JsonFields#MESSAGE} field from.
     * @param dittoHeaders the headers of the command which resulted in this exception.
     * @return the new ConnectionSignalIdEnforcementFailedException.
     * @throws org.eclipse.ditto.json.JsonMissingFieldException if the {@code jsonObject} does not have the {@link
     * JsonFields#MESSAGE} field.
     */
    public static ConnectionSignalIdEnforcementFailedException fromJson(final JsonObject jsonObject,
            final DittoHeaders dittoHeaders) {
        return new Builder()
                .dittoHeaders(dittoHeaders)
                .message(readMessage(jsonObject))
                .description(readDescription(jsonObject).orElse(DEFAULT_DESCRIPTION))
                .href(readHRef(jsonObject).orElse(null))
                .build();
    }

    /**
     * A mutable builder with a fluent API for a {@link ConnectionSignalIdEnforcementFailedException}.
     */
    @NotThreadSafe
    public static final class Builder
            extends DittoRuntimeExceptionBuilder<ConnectionSignalIdEnforcementFailedException> {

        private Builder() {
            description(DEFAULT_DESCRIPTION);
        }

        private Builder(final String target) {
            this();
            message(MessageFormat.format(MESSAGE_TEMPLATE, target));
        }

        @Override
        protected ConnectionSignalIdEnforcementFailedException doBuild(final DittoHeaders dittoHeaders,
                @Nullable final String message,
                @Nullable final String description,
                @Nullable final Throwable cause,
                @Nullable final URI href) {
            return new ConnectionSignalIdEnforcementFailedException(dittoHeaders, message, description, cause, href);
        }

    }

}
