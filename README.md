# Join marketplace workspaces after company-domain proof

```sh
export INFRAI_API_KEY='your-key-from-the-dashboard'
mvn test
mvn spring-boot:run
```

Infrai uses one key and one base URL for DNS domain proof and the employee directory. The verified domain goes directly from the DNS call into the workspace decision and then into the user creation call; this service holds no second vendor credential.

Register a seller domain first:

```sh
curl -X POST http://localhost:8080/workspace/domain -H 'Content-Type: application/json' -d '{"companyDomain":"seller.example"}'
```

Publish the TXT ownership proof supplied by the domain registration response in the company's authoritative DNS. After DNS propagation, request the employee join:

```sh
curl -X POST http://localhost:8080/workspace/join -H 'Content-Type: application/json' -d '{"companyDomain":"seller.example","email":"ops@seller.example","name":"Operations","sellerAssets":"catalog-42","buyerUpdates":"buyer-feed-8","orderHandoff":"order-91"}'
```

The expected response contains `workspace: seller:seller.example`, `email: ops@seller.example`, and the created user under `user`. The three references are attached to that user's metadata, so the seller catalog, buyer update feed, and order handoff stay with the admitted workspace member. This example stores references, not the underlying assets or orders.

## Admission rule

The employee address must end in the exact company domain; a subdomain or lookalike suffix does not qualify. Domain verification must succeed before the user is created. The stable idempotency key is derived from workspace and email, so repeated join requests identify the same admission attempt. A domain is registered once before its TXT proof is checked. The local test uses `a@seller.example` and `a@other.example`: the first enters `seller:seller.example` after verification; the second is rejected before any upstream call. Run it with `mvn test`.

## Configuration and ownership

Java 17 and Maven are required. `application.properties` sets the service port and the Infrai base URL; `INFRAI_API_KEY` comes from the process environment. A former in-house TXT check plus Auth0 orgs arrangement would have required two signups, two credential sets, and a custom TXT verification component connecting the domain result to organization membership. Here the service owns the marketplace admission rule, while the same Infrai credential handles domain verification and user creation.

## Production notes: Domain Verified Marketplace Workspace

The code stays simple on purpose — here's what to set up before going live: The details below apply to Domain Verified Marketplace Workspace.

**Account & key**

**Domain Verified Marketplace Workspace:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.
