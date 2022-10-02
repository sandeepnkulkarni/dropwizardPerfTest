package local.snk;

import io.dropwizard.Application;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import local.snk.health.PrimeHealthCheck;
import local.snk.metrics.CpuUsageGauge;
import local.snk.resources.PrimeResource;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.servlets.QoSFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.FilterRegistration;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriBuilder;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class DropwizardPerfTestApplication extends Application<DropwizardPerfTestConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(DropwizardPerfTestApplication.class);
    private static final boolean EXECUTE_FILTERS_IN_ORDER_ADDED = true;

    private static String processName;

    private DropwizardPerfTestConfiguration configuration;
    private Environment environment;

    public static void main(final String[] args) throws Exception {
        // Set process name in Mapped Diagnostic Context (MDC) so logging includes the process name
        MDC.put("processName", getProcessName());

        new DropwizardPerfTestApplication().run(args);
    }

    @Override
    public String getName() {
        return getServiceName();
    }

    @Override
    public void initialize(final Bootstrap<DropwizardPerfTestConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DropwizardPerfTestConfiguration configuration,
                    final Environment environment) {
        this.configuration = configuration;
        this.environment = environment;

        setupLifecycleHooks();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService primeAsyncExecutor = environment.lifecycle().executorService(name(PrimeResource.class, "prime-%d"))
                .maxThreads(availableProcessors)
                .minThreads(1)
                .build();

        PrimeResource primeResource = new PrimeResource(primeAsyncExecutor);

        environment.healthChecks().register("Prime health check", new PrimeHealthCheck(primeResource));
        environment.jersey().register(primeResource);

        environment.metrics().register(name(CpuUsageGauge.class, "cpu"), new CpuUsageGauge(3, TimeUnit.SECONDS));

        // Throttling is done at executor level now
        // setupThrottling();
    }

    static String getProcessName() {
        if (StringUtils.isBlank(processName)) {
            String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
            processName = getServiceName() + ":" + pid;
        }
        return processName;
    }

    static String getServiceName() {
        return "dropwizardPerfTest";
    }

    /**
     * @return The actual port the connector is listening on
     * (this is the 1st NetworkConnector), or 0 if not open.
     */
    private int getPort() {
        Connector[] connectors = environment.getApplicationContext().getServer().getConnectors();
        if (connectors != null) {
            for (Connector connector : connectors) {
                if (connector instanceof NetworkConnector) {
                    return ((NetworkConnector) connector).getLocalPort();
                }
            }
        }
        return 0;
    }

    private DefaultServerFactory getDefaultServerFactory() {
        return (DefaultServerFactory) configuration.getServerFactory();
    }

    private String getApiRootPath() {
        return getDefaultServerFactory().getJerseyRootPath().orElseThrow(NotFoundException::new);
    }

    private String getMappingPath(Class<?> resourceClass) {
        return UriBuilder.fromPath(getApiRootPath())
                .path(resourceClass)
                .path("*")
                .toString();
    }

    /**
     * Make use of QoSFilter to throttle number of concurrent requests being processed.
     * Also set request wait time.
     */
    private void setupThrottling() {
        int maxConcurrentRequests = Runtime.getRuntime().availableProcessors();
        Duration maxRequestWaitTime = Duration.minutes(5);

        LOG.info("Request throttling: Max concurrent requests [{}], Max request wait time [{}]",
                maxConcurrentRequests, maxRequestWaitTime);

        FilterRegistration.Dynamic filter = environment.servlets().addFilter("QOSFilterPrime", QoSFilter.class);

        filter.addMappingForUrlPatterns(null, EXECUTE_FILTERS_IN_ORDER_ADDED, getMappingPath(PrimeResource.class));
        filter.setInitParameter("maxRequests", Integer.toString(maxConcurrentRequests));
        filter.setInitParameter("suspendMs", Long.toString(maxRequestWaitTime.toMilliseconds()));
    }

    private void setupLifecycleHooks() {
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
                int port = getPort();
                LOG.info("Started listening on port {}", port);
                System.out.printf("%nPORT:%d%n", port);
            }

            @Override
            public void stop() throws Exception {
                LOG.info("Stopped");
            }
        });
    }
}
