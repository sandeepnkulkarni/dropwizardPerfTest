package local.snk.resources;

import com.google.common.primitives.Ints;
import local.snk.model.PrimeResponse;
import org.apache.commons.lang3.RandomStringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("/v1/prime")
public class PrimeResource {
    private static final Random RANDOM_GENERATOR = new Random();
    private static final int MIN_NUMBER = 10000;
    private static final int MAX_NUMBER = 1000000;

    @GET
    public Response getPrime(
            @QueryParam("upto") OptionalInt upto
    ) {
        final int maxNumber = Ints.constrainToRange(upto.orElse(MAX_NUMBER), MIN_NUMBER, MAX_NUMBER);
        final int primeNumbersTill = RANDOM_GENERATOR.nextInt(maxNumber - MIN_NUMBER + 1) + MIN_NUMBER;

        sortStrings();

        return Response.ok(new PrimeResponse(primeNumbersTill(primeNumbersTill)), MediaType.APPLICATION_JSON).build();
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
