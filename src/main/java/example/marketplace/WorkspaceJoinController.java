package example.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace")
public class WorkspaceJoinController {
    private final DomainWorkspaceJoin join;

    public WorkspaceJoinController(DomainWorkspaceJoin join) { this.join = join; }

    @PostMapping("/domain")
    public JsonNode register(@RequestBody Map<String, String> request) {
        return join.registerDomain(request.get("companyDomain"));
    }

    @PostMapping("/join")
    public DomainWorkspaceJoin.JoinResult join(@RequestBody DomainWorkspaceJoin.JoinRequest request) {
        return join.join(request);
    }

    @ExceptionHandler(DomainWorkspaceJoin.JoinRejected.class)
    public ResponseEntity<Map<String, String>> invalid(DomainWorkspaceJoin.JoinRejected error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(InfraiRestGateway.ApiRejected.class)
    public ResponseEntity<Map<String, String>> rejected(InfraiRestGateway.ApiRejected error) {
        HttpStatus status = error.status() >= 400 && error.status() < 500
                ? HttpStatus.valueOf(error.status()) : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("code", error.code(), "error", error.getMessage()));
    }
}
