package local.snk.resources;

import com.google.common.primitives.Ints;
import local.snk.model.PrimeResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("/v1/prime")
public class PrimeResource {
    private static final Random RANDOM_GENERATOR = new Random();
    private static final int MIN_NUMBER = 10000;
    private static final int MAX_NUMBER = 1000000;

    private final ExecutorService executorService;

    public PrimeResource(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPrime(
            @QueryParam("upto") OptionalInt upto
    ) {
        PrimeResponse primeResponse = getPrimeNumbersTill(upto);
        return Response.ok(primeResponse).build();
    }

    /**
     * From:
     * https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/async.html
     * https://blog.allegro.tech/2014/10/async-rest.html
     * https://github.com/mplanchant/dropwizard-async/blob/master/src/main/java/com/logiccache/resources/BookResource.java
     * <p>
     * Other references:
     * https://medium.com/quartictech/async-therefore-i-am-7458bb336b9e
     * https://groups.google.com/g/dropwizard-user/c/yO4wNwHiO4Q
     * https://docs.oracle.com/javaee/7/api/javax/ws/rs/container/AsyncResponse.html
     * https://github.com/dropwizard/dropwizard/blob/release/2.1.x/dropwizard-jersey/src/test/java/io/dropwizard/jersey/dummy/DummyResource.java
     * https://github.com/ixtendio/reactive-jersey/blob/master/src/main/java/com/example/reactive/jersey/infrastructure/resource/StockExchangeEventsResource.java
     * <p>
     * Interesting:
     * https://github.com/knowm/XDropWizard
     */

    @GET
    @Path("/async")
    @Produces(MediaType.APPLICATION_JSON)
    public void asyncGetPrime(
            @QueryParam("upto") OptionalInt upto,
            @Suspended final AsyncResponse asyncResponse
    ) {
        executorService.execute(() -> {
            PrimeResponse primeResponse = getPrimeNumbersTill(upto);
            asyncResponse.resume(primeResponse);
        });
    }

    @GET
    @Path("/managedAsync")
    @ManagedAsync
    @Produces(MediaType.APPLICATION_JSON)
    public void managedAsyncGetPrime(
            @QueryParam("upto") OptionalInt upto,
            @Suspended final AsyncResponse asyncResponse
    ) {
        PrimeResponse primeResponse = getPrimeNumbersTill(upto);
        asyncResponse.resume(primeResponse);
    }

    @GET
    @Path("/asyncFuture")
    @Produces(MediaType.APPLICATION_JSON)
    public void asyncFutureGetPrime(
            @QueryParam("upto") OptionalInt upto,
            @Suspended final AsyncResponse asyncResponse
    ) {
        CompletableFuture
                .supplyAsync(() -> getPrimeNumbersTill(upto), executorService)
                .thenApply(asyncResponse::resume);
    }

    private PrimeResponse getPrimeNumbersTill(OptionalInt upto) {
        final int maxNumber = Ints.constrainToRange(upto.orElse(MAX_NUMBER), MIN_NUMBER, MAX_NUMBER);
        final int primeNumbersTill = RANDOM_GENERATOR.nextInt(maxNumber - MIN_NUMBER + 1) + MIN_NUMBER;

        sortStrings();
        return new PrimeResponse(primeNumbersTill(primeNumbersTill));
    }

    private List<Integer> primeNumbersTill(int n) {
        return IntStream.rangeClosed(2, n)
                .filter(this::isPrime).boxed()
                .collect(Collectors.toList());
    }

    private boolean isPrime(int number) {
        return IntStream.rangeClosed(2, (int) (Math.sqrt(number)))
                .allMatch(i -> number % i != 0);
    }

    private void sortStrings() {
        final List<String> strings = new ArrayList<>();
        IntStream.rangeClosed(1, 1000)
                .forEach(i -> strings.add(RandomStringUtils.randomAlphabetic(25)));
        Collections.sort(strings);
        Collections.shuffle(strings);
        Collections.sort(strings);
    }
}
