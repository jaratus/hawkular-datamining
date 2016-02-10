/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.datamining.inventory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.inventory.api.model.AbstractElement;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Tenant;

/**
 * @author Pavol Loffay
 */
public class InventoryUtil {

    public static Long parseForecastingHorizon(Map<String, Object> properties) {
        String forecastingHorizonObject = (String) properties.get(InventoryConfiguration.PREDICTION_INTERVAL_PROP);

        return forecastingHorizonObject == null ? null : Long.parseLong(forecastingHorizonObject);
    }

    public static Set<org.hawkular.datamining.api.model.Metric> convertMetrics(Set<Metric> metrics,
                                                                               Relationship relationship) {
        return convertMetrics(metrics, new HashSet<>(Arrays.asList(relationship)));
    }

    public static Set<org.hawkular.datamining.api.model.Metric> convertMetrics(Set<Metric> metrics,
                                             Set<Relationship> relationships) {

        Set<org.hawkular.datamining.api.model.Metric> result = new HashSet<>(metrics.size());

        for (Metric invMetric: metrics) {
            org.hawkular.datamining.api.model.Metric metric = convertMetric(invMetric, relationships);
            result.add(metric);
        }

        return result;
    }

    public static org.hawkular.datamining.api.model.Metric convertMetric(Metric invMetric,
                                                                         Relationship relationship) {
        return convertMetric(invMetric, new HashSet<>(Arrays.asList(relationship)));
    }

    public static org.hawkular.datamining.api.model.Metric convertMetric(Metric invMetric,
                                                                         Set<Relationship> relationships) {
        Long metricForecastingHorizon = forecastingHorizon(relationships, invMetric.getPath());
        Long typeForecastingHorizon = forecastingHorizon(relationships, invMetric.getType().getPath());

        org.hawkular.datamining.api.model.MetricType type = convertMetricType(invMetric.getType(),
                typeForecastingHorizon);

        String tenant = invMetric.getPath().ids().getTenantId();
        String feed = invMetric.getPath().ids().getFeedId();
        org.hawkular.datamining.api.model.Metric metric = new org.hawkular.datamining.api.model.Metric(tenant,
                feed, invMetric.getId(), invMetric.getCollectionInterval(), type, metricForecastingHorizon);

        return metric;
    }

    public static org.hawkular.datamining.api.model.Metric convertMetric(Metric invMetric,
                                                                         Long typeForecastingHorizon,
                                                                         Long metricForecastingHorizon) {

        org.hawkular.datamining.api.model.MetricType type = convertMetricType(invMetric.getType(),
                typeForecastingHorizon);

        String tenant = invMetric.getPath().ids().getTenantId();
        String feed = invMetric.getPath().ids().getFeedId();
        org.hawkular.datamining.api.model.Metric metric = new org.hawkular.datamining.api.model.Metric(tenant,
                feed, invMetric.getId(), invMetric.getCollectionInterval(), type, metricForecastingHorizon);

        return metric;
    }

    private static org.hawkular.datamining.api.model.MetricType convertMetricType(MetricType metricType,
                                                                                  Long forecastingHorizon) {
        return new org.hawkular.datamining.api.model.MetricType(metricType.getPath().toString(),
                metricType.getCollectionInterval(), forecastingHorizon);
    }

    public static Long forecastingHorizon(Set<Relationship> relationships, CanonicalPath targetEntityPath) {

        Long forecastingHorizon = null;
        for (Relationship relationship: relationships) {

            if (relationship.getTarget().equals(targetEntityPath)) {
                forecastingHorizon = parseForecastingHorizon(relationship.getProperties());
            }
        }

        return forecastingHorizon;
    }

    public static Set<CanonicalPath> extractCanonicalPaths(Collection<? extends AbstractElement<?, ?>> elements) {
        Set<CanonicalPath> canonicalPaths = new HashSet<>();

        for (AbstractElement<?, ?> abstractElement: elements) {
            canonicalPaths.add(abstractElement.getPath());
        }

        return canonicalPaths;
    }

    public static Set<SubscriptionManager.ModelOwner> predictionRelationshipsToOwners(Set<Relationship>
                                                                                               relationships) {
        Set<SubscriptionManager.ModelOwner> modelOwners = new HashSet<>();

        for (Relationship relationship: relationships) {
            Class<?> targetEntity = relationship.getTarget().getSegment().getElementType();

            if (targetEntity.equals(Metric.class)) {
                modelOwners.add(SubscriptionManager.ModelOwner.Metric);
            } else if (targetEntity.equals(MetricType.class)) {
                modelOwners.add(SubscriptionManager.ModelOwner.MetricType);
            } else if (targetEntity.equals(Tenant.class)) {
                modelOwners.add(SubscriptionManager.ModelOwner.Tenant);
            }
        }

        return modelOwners;
    }

    public static Set<SubscriptionManager.ModelOwner> predictionRelationshipsToOwners(
            Relationship relationship) {

        return predictionRelationshipsToOwners(new HashSet<>(Arrays.asList(relationship)));
    }
}
