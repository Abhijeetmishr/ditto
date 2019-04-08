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
package org.eclipse.ditto.services.gateway.health;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonObjectBuilder;
import org.eclipse.ditto.services.utils.health.cluster.ClusterStatus;
import org.eclipse.ditto.services.utils.health.status.Status;
import org.eclipse.ditto.services.utils.health.status.StatusSupplier;

import akka.actor.ActorSystem;

/**
 * Provides aggregated status information for a cluster, grouped by the cluster's roles.
 */
public class ClusterStatusSupplier implements StatusSupplier {

    private ClusterStatusAndHealthHelper clusterStatusHealthHelper;

    private ClusterStatusSupplier(final ActorSystem actorSystem, final Supplier<ClusterStatus> clusterStateSupplier) {
        this.clusterStatusHealthHelper = ClusterStatusAndHealthHelper.of(actorSystem, clusterStateSupplier);
    }

    /**
     * Returns a new {@link ClusterStatusSupplier}.
     *
     * @param actorSystem the ActorSystem to use.
     * @param clusterStateSupplier the {@link ClusterStatus} supplier to use in order to find out the reachable cluster
     * nodes.
     * @return the {@link ClusterStatusSupplier}.
     */
    public static StatusSupplier of(final ActorSystem actorSystem, final Supplier<ClusterStatus> clusterStateSupplier) {
        requireNonNull(actorSystem);
        requireNonNull(clusterStateSupplier);

        return new ClusterStatusSupplier(actorSystem, clusterStateSupplier);
    }

    @Override
    public CompletionStage<JsonObject> get() {
        final JsonObjectBuilder overallStatusBuilder = JsonFactory.newObjectBuilder();
        overallStatusBuilder.setAll(Status.provideStaticStatus());

        // append roles statuses to static status
        return clusterStatusHealthHelper.retrieveOverallRolesStatus()
                .thenApply(Status.provideStaticStatus()::setAll);
    }
}
