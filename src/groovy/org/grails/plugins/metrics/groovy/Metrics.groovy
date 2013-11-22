package org.grails.plugins.metrics.groovy

import com.codahale.metrics.*
import com.codahale.metrics.servlets.MetricsServlet
import grails.util.Holders
import org.codehaus.groovy.reflection.ReflectionUtils

import java.util.concurrent.TimeUnit

class Metrics {

    private static final MetricRegistry builtInRegistry = new MetricRegistry()

    // ignore org.springsource.loaded.ri.ReflectiveInterceptor when not running as a war, and ignore this class for convenience
    static final List<String> extraIgnoredPackages = ["org.springsource.loaded.ri", "org.grails.plugins.metrics.groovy"]

    private static String buildMetricName(String metricName) {
        if (Holders?.config?.metrics?.core?.prependClassName == false) {
            return metricName
        }

        Class callingClass = ReflectionUtils.getCallingClass(0, extraIgnoredPackages)
        return MetricRegistry.name(callingClass, metricName)
    }

    static Metric getOrAdd(String name, Metric metricToAdd) {
        String metricName = buildMetricName(name)
        Metric metric = registry.getMetrics().get(metricName)
        if (!metric) {
            metric = registry.register(metricName, metricToAdd)
        }
        return metric
    }

    static Gauge newGauge(String name, Gauge gauge) {
        return getOrAdd(name, gauge) as Gauge
    }

    static Counter newCounter(String name) {
        String metricName = buildMetricName(name)
        return registry.counter(metricName)
    }

    static Histogram newHistogram(String name) {
        String metricName = buildMetricName(name)
        return registry.histogram(metricName)
    }

    static Histogram newHistogram(String name, Reservoir reservoir) {
        Histogram histogram = new Histogram(reservoir)
        return getOrAdd(name, histogram) as Histogram
    }

    static Meter newMeter(String name) {
        String metricName = buildMetricName(name)
        return registry.meter(metricName)
    }

    static Timer newTimer(String name) {
        String metricName = buildMetricName(name)
        return registry.timer(metricName)
    }

    static MetricRegistry getRegistry() {

        // Use the registry from the servletContext if one has been configured in doWithApplicationContext (i.e., the
        // normal runtime case for apps using the plugin) or fall back to the built in one otherwise (e.g., unit tests
        // which happen to touch instrumented classes)

        MetricRegistry metricRegistry = Holders?.servletContext?.getAttribute(MetricsServlet.METRICS_REGISTRY) as MetricRegistry
        if (!metricRegistry) {
            metricRegistry = builtInRegistry
        }
        return metricRegistry
    }

    static JmxReporter startJmxReporter(TimeUnit rateUnit = TimeUnit.SECONDS, TimeUnit durationUnit = TimeUnit.MILLISECONDS) {
        final JmxReporter reporter = JmxReporter
                .forRegistry(registry)
                .convertRatesTo(rateUnit)
                .convertDurationsTo(durationUnit)
                .build();
        reporter.start();
        return reporter
    }

    static removeAll() {
        registry.removeMatching(MetricFilter.ALL)
    }

    private Metrics() {}
}
