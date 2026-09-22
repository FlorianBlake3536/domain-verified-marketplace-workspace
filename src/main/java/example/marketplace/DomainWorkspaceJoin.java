package example.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DomainWorkspaceJoin {
    private final InfraiGateway gateway;

    public DomainWorkspaceJoin(InfraiGateway gateway) {
        this.gateway = gateway;
    }

    public JsonNode registerDomain(String domain) {
        return gateway.addDomain(normalize(domain));
    }

    public JoinResult join(JoinRequest request) {
        String domain = normalize(request.companyDomain());
        if (request.email() == null || request.name() == null || request.name().isBlank()) {
            throw new JoinRejected("Employee email and name are required");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!email.endsWith("@" + domain) || email.indexOf('@') != email.lastIndexOf('@')) {
            throw new JoinRejected("Employee email must match the company domain");
        }
        if (request.sellerAssets() == null || request.buyerUpdates() == null || request.orderHandoff() == null) {
            throw new JoinRejected("Handoff fields are required");
        }
        gateway.verifyDomain(domain);
        String workspace = "seller:" + domain;
        String requestId = UUID.nameUUIDFromBytes((workspace + ":" + email)
                .getBytes(StandardCharsets.UTF_8)).toString();
        JsonNode user = gateway.createUser(Map.of(
                "email", email,
                "name", request.name(),
                "idempotency_key", requestId,
                "metadata", Map.of(
                        "workspace", workspace,
                        "seller_assets", request.sellerAssets(),
                        "buyer_updates", request.buyerUpdates(),
                        "order_handoff", request.orderHandoff())));
        return new JoinResult(workspace, email, user);
    }

    private static String normalize(String domain) {
        if (domain == null || !domain.trim().toLowerCase(Locale.ROOT)
                .matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\\.[a-z]{2,}")) {
            throw new JoinRejected("A valid company domain is required");
        }
        return domain.trim().toLowerCase(Locale.ROOT);
    }

    public record JoinRequest(String companyDomain, String email, String name,
                              String sellerAssets, String buyerUpdates, String orderHandoff) {}
    public record JoinResult(String workspace, String email, JsonNode user) {}
    public static class JoinRejected extends RuntimeException {
        public JoinRejected(String message) { super(message); }
    }
}
