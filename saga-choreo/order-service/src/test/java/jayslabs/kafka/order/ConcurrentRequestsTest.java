package jayslabs.kafka.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import jayslabs.kafka.order.common.dto.OrderCreateRequest;
import reactor.core.publisher.Flux;

public class ConcurrentRequestsTest {
    public static void main(String[] args) {

        Logger log = LoggerFactory.getLogger(ConcurrentRequestsTest.class);

        var client = WebClient.builder().baseUrl("http://localhost:8080/order").build();

        int reqCount = 1000;
        int totalRequests = reqCount * 3;
        
        log.info("Starting concurrent request test - Total requests: {}", totalRequests);
        long startTime = System.currentTimeMillis();
        
        Flux.merge(
            createNthFlux(1,1, reqCount),
            createNthFlux(2,2, reqCount),
            createNthFlux(3,3, reqCount))
            .flatMap( req -> client.post().bodyValue(req).retrieve().bodyToMono(Object.class).then())
            .blockLast();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        log.info("Completed concurrent request test - Total requests: {}, Execution time: {} ms ({} seconds), Throughput: {} requests/second", 
            totalRequests, executionTime, executionTime / 1000.0, (totalRequests * 1000.0) / executionTime);
    }

    private static Flux<OrderCreateRequest> createNthFlux(int customerId, int productId, int reqCount) {
        var req = createRequest(customerId, productId);
        return Flux.range(1, reqCount)
            .map(i -> req);
    }

    private static OrderCreateRequest createRequest(int customerId, int productId) {
        return OrderCreateRequest.builder()
            .unitPrice(1)
            .quantity(1)
            .customerId(customerId)
            .productId(productId)
            .build();
    }    
}
