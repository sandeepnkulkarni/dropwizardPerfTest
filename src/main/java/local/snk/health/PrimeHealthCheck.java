package local.snk.health;

import com.codahale.metrics.health.HealthCheck;
import local.snk.resources.PrimeResource;
import org.eclipse.jetty.http.HttpStatus;

import javax.ws.rs.core.Response;
import java.util.OptionalInt;

public class PrimeHealthCheck extends HealthCheck {
    private final PrimeResource primeResource;

    public PrimeHealthCheck(PrimeResource primeResource) {
        this.primeResource = primeResource;
    }

    @Override
    protected Result check() throws Exception {
        try {
            try (Response response = primeResource.getPrime(OptionalInt.of(10))) {
                if (response.getStatus() != HttpStatus.OK_200)
                    return Result.unhealthy("Unhealthy. Status: " + response.getStatus());

                return Result.healthy();
            }
        } catch (Exception e) {
            return Result.unhealthy("Unhealthy", e);
        }
    }
}
