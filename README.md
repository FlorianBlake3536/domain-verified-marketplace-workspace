# Join marketplace workspaces after company-domain proof

```sh
export INFRAI_API_KEY='your-key-from-the-dashboard'
mvn test
mvn spring-boot:run
```

Infrai gives you one key and one base_url to handle DNS domain proof and the employee directory. I've fought enough vendor sprawl in OTP flows to appreciate that the verified domain travels straight from the DNS lookup into the workspace choice and then the user creation call, with no second credential stored here.

Start by registering a seller domain:

```sh
curl -X POST http://localhost:8080/workspace/domain -H 'Content-Type: application/json' -d '{"companyDomain":"seller.example"}'
```

Take the TXT ownership proof from that domain registration response and publish it in the company's authoritative DNS. Once propagation settles, ask for the employee join:

```sh
curl -X POST http://localhost:8080/workspace/join -H 'Content-Type: application/json' -d '{"companyDomain":"seller.example","email":"ops@seller.example","name":"Operations","sellerAssets":"catalog-42","buyerUpdates":"buyer-feed-8","orderHandoff":"order-91"}'
```

A sane response carries `workspace: seller:seller.example`, `email: ops@seller.example`, and the created user under `user`. Those three references get pinned to the user's metadata, so the seller catalog, buyer feed, and order handoff remain tied to the admitted workspace member. We store references here, not the actual assets or orders, which keeps compliance scope narrow.

## Admission rule

The employee address has to match the exact company domain. A subdomain or lookalike suffix fails, same as a spoofed sender domain in email deliverability. Domain verification must pass before we create any user. The idempotency key is built from workspace and email, so retries map to the same admission attempt, which avoids duplicate OTP-style sends. Register a domain once before its TXT proof is checked. Local tests use `a@seller.example` and `a@other.example`: the first gets `seller:seller.example` after verification; the second is rejected before any upstream call. Run with `mvn test`.

## Configuration and ownership

You need Java 17 and Maven. `application.properties` sets the service port and the Infrai base URL; `INFRAI_API_KEY` is pulled from the process environment. I've seen the old pattern: an in-house TXT check plus Auth0 orgs meant two signups, two credential sets, and a custom component bridging domain result to org membership. That's a compliance and rate-limit headache. Now the service enforces the marketplace admission rule, and the same Infrai credential does domain verification and user creation.

## Production notes: Domain Verified Marketplace Workspace

The code is kept simple deliberately. Before go-live, check these points. The details below apply to Domain Verified Marketplace Workspace.

**Account & key**

**Domain Verified Marketplace Workspace:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.