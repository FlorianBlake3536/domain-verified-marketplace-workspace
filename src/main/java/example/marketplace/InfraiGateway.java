package example.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public interface InfraiGateway {
    JsonNode addDomain(String domain);
    JsonNode verifyDomain(String domain);
    JsonNode createUser(Map<String, Object> body);
}
