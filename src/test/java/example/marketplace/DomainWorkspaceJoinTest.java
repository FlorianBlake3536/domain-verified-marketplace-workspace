package example.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainWorkspaceJoinTest {
    @Test
    void joinsOnlyMatchingEmployeeAfterDomainProof() {
        class Gateway implements InfraiGateway {
            int verified;
            Map<String, Object> created;
            public JsonNode addDomain(String domain) { return new ObjectMapper().createObjectNode(); }
            public JsonNode verifyDomain(String domain) {
                verified++;
                assertEquals("seller.example", domain);
                return new ObjectMapper().createObjectNode();
            }
            public JsonNode createUser(Map<String, Object> body) {
                created = body;
                return new ObjectMapper().createObjectNode().put("id", "employee-1");
            }
        }
        Gateway gateway = new Gateway();
        DomainWorkspaceJoin service = new DomainWorkspaceJoin(gateway);
        var wrong = new DomainWorkspaceJoin.JoinRequest("seller.example", "a@other.example", "A",
                "catalog-42", "buyer-feed-8", "order-91");
        assertThrows(DomainWorkspaceJoin.JoinRejected.class, () -> service.join(wrong));
        assertEquals(0, gateway.verified);
        var right = new DomainWorkspaceJoin.JoinRequest("seller.example", "a@seller.example", "A",
                "catalog-42", "buyer-feed-8", "order-91");
        var result = service.join(right);
        assertEquals("seller:seller.example", result.workspace());
        assertEquals(1, gateway.verified);
        assertEquals("catalog-42", ((Map<?, ?>) gateway.created.get("metadata")).get("seller_assets"));
        assertEquals("order-91", ((Map<?, ?>) gateway.created.get("metadata")).get("order_handoff"));
        assertEquals("employee-1", result.user().path("id").asText());
        assertNotNull(gateway.created.get("idempotency_key"));
    }
}
