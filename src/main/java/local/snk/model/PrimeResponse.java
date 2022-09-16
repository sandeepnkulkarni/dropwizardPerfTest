package local.snk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public class PrimeResponse {
    List<Integer> primes;
    long timestamp;

    public PrimeResponse() {

    }

    public PrimeResponse(List<Integer> primes) {
        this.primes = primes;
        this.timestamp = Instant.now().toEpochMilli();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<Integer> getPrimes() {
        return primes;
    }

    public void setPrimes(List<Integer> primes) {
        this.primes = primes;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
