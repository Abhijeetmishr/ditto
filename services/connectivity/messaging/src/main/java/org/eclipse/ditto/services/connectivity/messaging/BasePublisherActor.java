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
package org.eclipse.ditto.services.connectivity.messaging;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;
import static org.eclipse.ditto.model.base.headers.DittoHeaderDefinition.CORRELATION_ID;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.headers.DittoHeaderDefinition;
import org.eclipse.ditto.model.connectivity.Connection;
import org.eclipse.ditto.model.connectivity.ConnectionId;
import org.eclipse.ditto.model.connectivity.ConnectivityModelFactory;
import org.eclipse.ditto.model.connectivity.ConnectivityStatus;
import org.eclipse.ditto.model.connectivity.HeaderMapping;
import org.eclipse.ditto.model.connectivity.ReplyTarget;
import org.eclipse.ditto.model.connectivity.ResourceStatus;
import org.eclipse.ditto.model.connectivity.Source;
import org.eclipse.ditto.model.connectivity.Target;
import org.eclipse.ditto.model.placeholders.ExpressionResolver;
import org.eclipse.ditto.model.placeholders.HeadersPlaceholder;
import org.eclipse.ditto.model.placeholders.PlaceholderFactory;
import org.eclipse.ditto.model.placeholders.PlaceholderFilter;
import org.eclipse.ditto.model.placeholders.ThingPlaceholder;
import org.eclipse.ditto.model.placeholders.TopicPathPlaceholder;
import org.eclipse.ditto.services.connectivity.messaging.config.DittoConnectivityConfig;
import org.eclipse.ditto.services.connectivity.messaging.config.MonitoringConfig;
import org.eclipse.ditto.services.connectivity.messaging.internal.RetrieveAddressStatus;
import org.eclipse.ditto.services.connectivity.messaging.monitoring.ConnectionMonitor;
import org.eclipse.ditto.services.connectivity.messaging.monitoring.ConnectionMonitorRegistry;
import org.eclipse.ditto.services.connectivity.messaging.monitoring.DefaultConnectionMonitorRegistry;
import org.eclipse.ditto.services.connectivity.messaging.monitoring.logs.ConnectionLogger;
import org.eclipse.ditto.services.connectivity.messaging.monitoring.logs.ConnectionLoggerRegistry;
import org.eclipse.ditto.services.connectivity.util.ConnectionLogUtil;
import org.eclipse.ditto.services.models.connectivity.ExternalMessage;
import org.eclipse.ditto.services.models.connectivity.ExternalMessageBuilder;
import org.eclipse.ditto.services.models.connectivity.ExternalMessageFactory;
import org.eclipse.ditto.services.models.connectivity.OutboundSignal;
import org.eclipse.ditto.services.utils.akka.LogUtil;
import org.eclipse.ditto.services.utils.config.DefaultScopedConfig;
import org.eclipse.ditto.services.utils.config.InstanceIdentifierSupplier;
import org.eclipse.ditto.signals.base.Signal;

import akka.actor.AbstractActor;
import akka.event.DiagnosticLoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

/**
 * Base class for publisher actors. Holds the map of configured targets.
 *
 * @param <T> the type of targets for this actor
 */
public abstract class BasePublisherActor<T extends PublishTarget> extends AbstractActor {

    private static final HeadersPlaceholder HEADERS_PLACEHOLDER = PlaceholderFactory.newHeadersPlaceholder();
    private static final ThingPlaceholder THING_PLACEHOLDER = PlaceholderFactory.newThingPlaceholder();
    private static final TopicPathPlaceholder TOPIC_PLACEHOLDER = PlaceholderFactory.newTopicPathPlaceholder();

    protected final ConnectionId connectionId;
    protected final List<Target> targets;
    protected final Map<Target, ResourceStatus> resourceStatusMap;

    protected final ConnectionLogger connectionLogger;
    protected final ConnectionMonitor responsePublishedMonitor;
    protected final ConnectionMonitor responseDroppedMonitor;
    private final ConnectionMonitorRegistry<ConnectionMonitor> connectionMonitorRegistry;
    private final List<Optional<ReplyTarget>> replyTargets;

    protected BasePublisherActor(final Connection connection) {
        checkNotNull(connection, "connection");
        this.connectionId = connection.getId();
        this.targets = connection.getTargets();
        resourceStatusMap = new HashMap<>();
        final Instant now = Instant.now();
        targets.forEach(target ->
                resourceStatusMap.put(target,
                        ConnectivityModelFactory.newTargetStatus(getInstanceIdentifier(), ConnectivityStatus.OPEN,
                                target.getAddress(), "Started at " + now)));

        final MonitoringConfig monitoringConfig = DittoConnectivityConfig.of(
                DefaultScopedConfig.dittoScoped(getContext().getSystem().settings().config())
        ).getMonitoringConfig();
        connectionMonitorRegistry = DefaultConnectionMonitorRegistry.fromConfig(monitoringConfig);
        responseDroppedMonitor = connectionMonitorRegistry.forResponseDropped(this.connectionId);
        responsePublishedMonitor = connectionMonitorRegistry.forResponsePublished(this.connectionId);
        connectionLogger =
                ConnectionLoggerRegistry.fromConfig(monitoringConfig.logger()).forConnection(this.connectionId);
        replyTargets = connection.getSources().stream().map(Source::getReplyTarget).collect(Collectors.toList());
    }

    private static String getInstanceIdentifier() {
        return InstanceIdentifierSupplier.getInstance().get();
    }

    @Override
    public Receive createReceive() {
        final ReceiveBuilder receiveBuilder = receiveBuilder();
        preEnhancement(receiveBuilder);

        receiveBuilder.match(OutboundSignal.WithExternalMessage.class, BasePublisherActor::isResponseOrError,
                outbound -> {
                    final ExternalMessage response = outbound.getExternalMessage();
                    final String correlationId = response.getHeaders().get(CORRELATION_ID.getKey());
                    ConnectionLogUtil.enhanceLogWithCorrelationIdAndConnectionId(log(), correlationId, connectionId);

                    final Optional<ReplyTarget> replyTargetOptional = response.getInternalHeaders()
                            .getReplyTarget()
                            .flatMap(this::getReplyTargetByIndex);
                    final String replyToFromHeader = response.getHeaders().get(ExternalMessage.REPLY_TO_HEADER);
                    if (replyTargetOptional.isPresent()) {
                        final ReplyTarget replyTarget = replyTargetOptional.get();
                        final ExpressionResolver expressionResolver =
                                getExpressionResolver(outbound.getExternalMessage(), outbound.getSource());
                        final T replyTargetAddress =
                                toPublishTarget(applyExpressionResolver(expressionResolver, replyTarget.getAddress()));
                        final ExternalMessage responseWithMappedHeaders =
                                applyHeaderMapping(expressionResolver, outbound,
                                        replyTarget.getHeaderMapping().orElse(null), log(), this::withMappedHeaders);
                        publishResponseOrError(replyTargetAddress, outbound, responseWithMappedHeaders);
                    } else if (replyToFromHeader != null) {
                        publishResponseOrError(toReplyTarget(replyToFromHeader), outbound, response);
                    } else {
                        log().info("Response dropped, missing replyTo address: {}", response);
                        responseDroppedMonitor.failure(outbound.getSource(),
                                "Response dropped since it was missing a replyTo address.");
                    }
                })
                .match(OutboundSignal.WithExternalMessage.class, outbound -> {
                    final ExternalMessage message = outbound.getExternalMessage();
                    final String correlationId = message.getHeaders().get(CORRELATION_ID.getKey());
                    ConnectionLogUtil.enhanceLogWithCorrelationIdAndConnectionId(log(), correlationId, connectionId);

                    final Signal<?> outboundSource = outbound.getSource();
                    log().debug("Publishing mapped message of type <{}> to targets <{}>: {}",
                            outboundSource.getType(), outbound.getTargets(), message);
                    outbound.getTargets().forEach(target -> {
                        log().info("Publishing mapped message of type <{}> to target address <{}>",
                                outboundSource.getType(), target.getAddress());

                        final ConnectionMonitor publishedMonitor =
                                connectionMonitorRegistry.forOutboundPublished(connectionId,
                                        target.getOriginalAddress());
                        try {
                            final T publishTarget = toPublishTarget(target.getAddress());
                            final ExternalMessage messageWithMappedHeaders =
                                    applyHeaderMapping(outbound, target.getHeaderMapping().orElse(null), log(),
                                            this::withMappedHeaders);
                            publishMessage(target, publishTarget, messageWithMappedHeaders, publishedMonitor);
                        } catch (final DittoRuntimeException e) {
                            // TODO: might there be private information in the exception message so we shouldn't be allowed to see them?
                            publishedMonitor.failure(outboundSource,
                                    "Ran into a failure when applying header mapping: {0}",
                                    e.getMessage());
                            log().warning("Got unexpected DittoRuntimeException when applying header mapping - " +
                                            "thus NOT publishing the message: {} {}",
                                    e.getClass().getSimpleName(), e.getMessage());
                        }
                    });
                })
                .match(RetrieveAddressStatus.class, ram -> getCurrentTargetStatus().forEach(rs ->
                        getSender().tell(rs, getSelf())))
                .matchAny(m -> {
                    log().warning("Unknown message: {}", m);
                    unhandled(m);
                });

        postEnhancement(receiveBuilder);
        return receiveBuilder.build();
    }

    private void publishResponseOrError(final T address, final OutboundSignal outbound,
            final ExternalMessage response) {

        log().info("Publishing mapped response/error message of type <{}> to reply target <{}>",
                outbound.getSource().getType(), address);
        log().debug("Publishing mapped response/error message of type <{}> to reply target <{}>: {}",
                outbound.getSource().getType(), address, response);
        publishMessage(null, address, response, responsePublishedMonitor);
    }

    private Optional<ReplyTarget> getReplyTargetByIndex(final int replyTargetIndex) {
        return 0 <= replyTargetIndex && replyTargetIndex < replyTargets.size()
                ? replyTargets.get(replyTargetIndex)
                : Optional.empty();
    }

    private Collection<ResourceStatus> getCurrentTargetStatus() {
        return resourceStatusMap.values();
    }

    /**
     * Provides the possibility to add custom matchers before applying the default matchers of the BasePublisherActor.
     *
     * @param receiveBuilder the ReceiveBuilder to add other matchers to.
     */
    protected abstract void preEnhancement(final ReceiveBuilder receiveBuilder);

    /**
     * Provides the possibility to add custom matchers after applying the default matchers of the BasePublisherActor.
     *
     * @param receiveBuilder the ReceiveBuilder to add other matchers to.
     */
    protected abstract void postEnhancement(final ReceiveBuilder receiveBuilder);

    /**
     * Converts the passed {@code address} to a {@link PublishTarget} of type {@code <T>}.
     *
     * @param address the address to convert to a {@link PublishTarget} of type {@code <T>}.
     * @return the instance of type {@code <T>}
     */
    protected abstract T toPublishTarget(final String address);

    /**
     * Converts the passed {@code replyToAddress} to a {@link PublishTarget} of type {@code <T>}.
     *
     * @param replyToAddress the replyTo address to convert to a {@link PublishTarget} of type {@code <T>}.
     * @return the instance of type {@code <T>}
     */
    protected abstract T toReplyTarget(final String replyToAddress);

    /**
     * Publishes the passed {@code message} to the passed {@code publishTarget}.
     *
     * @param target the nullable Target for getting even more information about the configured Target to publish to.
     * @param publishTarget the {@link PublishTarget} to publish to.
     * @param message the {@link org.eclipse.ditto.services.models.connectivity.ExternalMessage} to publish.
     * @param publishedMonitor the monitor that can be used for monitoring purposes.
     */
    protected abstract void publishMessage(@Nullable final Target target, final T publishTarget,
            final ExternalMessage message, final ConnectionMonitor publishedMonitor);

    /**
     * @return the logger to use.
     */
    protected abstract DiagnosticLoggingAdapter log();

    /**
     * Control how mapped headers are added to external messages. Append the mapped headers by default.
     * Override in subclasses to change the behavior.
     *
     * @param builder the builder of the external message to send.
     * @param mappedHeaders result of header mapping.
     * @return builder incorporating the mapped headers.
     */
    protected ExternalMessageBuilder withMappedHeaders(final ExternalMessageBuilder builder,
            final Map<String, String> mappedHeaders) {

        return builder.withAdditionalHeaders(mappedHeaders);
    }

    /**
     * Checks whether the passed in {@code outboundSignal} is a response or an error.
     *
     * @param outboundSignal the OutboundSignal to check.
     * @return {@code true} if the OutboundSignal is a response or an error, {@code false} otherwise
     */
    private static boolean isResponseOrError(final OutboundSignal.WithExternalMessage outboundSignal) {
        return outboundSignal.getExternalMessage().isResponse() || outboundSignal.getExternalMessage().isError();
    }

    /**
     * Applies the optional "header mapping" potentially configured on the passed {@code target} on the passed {@code
     * outboundSignal}.
     *
     * @param outboundSignal the OutboundSignal containing the {@link ExternalMessage} with headers potentially
     * containing placeholders.
     * @param mapping headerMappings to apply.
     * @param log the logger to use for logging.
     * @return the ExternalMessage with replaced headers
     */
    static ExternalMessage applyHeaderMapping(final OutboundSignal.WithExternalMessage outboundSignal,
            final @Nullable HeaderMapping mapping, final DiagnosticLoggingAdapter log,
            final BiFunction<ExternalMessageBuilder, Map<String, String>, ExternalMessageBuilder> withMappedHeaders) {

        return applyHeaderMapping(
                getExpressionResolver(outboundSignal.getExternalMessage(), outboundSignal.getSource()),
                outboundSignal, mapping, log, withMappedHeaders);
    }

    private static ExternalMessage applyHeaderMapping(final ExpressionResolver expressionResolver,
            final OutboundSignal.WithExternalMessage outboundSignal,
            final @Nullable HeaderMapping mapping, final DiagnosticLoggingAdapter log,
            final BiFunction<ExternalMessageBuilder, Map<String, String>, ExternalMessageBuilder> withMappedHeaders) {

        final ExternalMessage originalMessage = outboundSignal.getExternalMessage();
        final Map<String, String> originalHeaders = new HashMap<>(originalMessage.getHeaders());

        // clear all existing headers in the builder which is used for building the ExternalMessage to be returned:
        final ExternalMessageBuilder messageBuilder = ExternalMessageFactory.newExternalMessageBuilder(originalMessage)
                .clearHeaders();

        // keep correlation-id, content-type and reply-to:
        Optional.ofNullable(originalHeaders.get(DittoHeaderDefinition.CORRELATION_ID.getKey()))
                .ifPresent(c ->
                        messageBuilder.withAdditionalHeaders(DittoHeaderDefinition.CORRELATION_ID.getKey(), c));
        Optional.ofNullable(originalHeaders.get(ExternalMessage.CONTENT_TYPE_HEADER))
                .ifPresent(c ->
                        messageBuilder.withAdditionalHeaders(ExternalMessage.CONTENT_TYPE_HEADER, c));
        Optional.ofNullable(originalHeaders.get(ExternalMessage.REPLY_TO_HEADER))
                .ifPresent(r ->
                        messageBuilder.withAdditionalHeaders(ExternalMessage.REPLY_TO_HEADER, r));

        if (mapping != null) {
            final Signal<?> sourceSignal = outboundSignal.getSource();

            final Map<String, String> mappedHeaders = mapping.getMapping().entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                            applyExpressionResolver(expressionResolver, e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            LogUtil.enhanceLogWithCorrelationId(log, sourceSignal);
            log.debug("Result of header mapping <{}> are these headers to be published: {}", mapping, mappedHeaders);

            // combine with external headers
            return withMappedHeaders.apply(messageBuilder, mappedHeaders).build();
        } else {
            return messageBuilder.build();
        }
    }

    private static String applyExpressionResolver(final ExpressionResolver expressionResolver, final String value) {
        return PlaceholderFilter.apply(value, expressionResolver, true);
    }

    private static ExpressionResolver getExpressionResolver(final ExternalMessage originalMessage,
            final Signal sourceSignal) {
        return PlaceholderFactory.newExpressionResolver(
                PlaceholderFactory.newPlaceholderResolver(HEADERS_PLACEHOLDER, originalMessage.getHeaders()),
                PlaceholderFactory.newPlaceholderResolver(THING_PLACEHOLDER, sourceSignal.getEntityId()),
                PlaceholderFactory.newPlaceholderResolver(TOPIC_PLACEHOLDER,
                        originalMessage.getTopicPath().orElse(null))
        );
    }
}
