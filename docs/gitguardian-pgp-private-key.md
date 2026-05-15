# GitGuardian: PGP private key alert

## Alert

GitGuardian reported **PGP Private Key Exposed** on this repository. The scanner matched
`-----BEGIN PGP PRIVATE KEY BLOCK-----` in committed files:

- `src/main/resources/testPrivateKey.private`
- `src/main/resources/testSecondaryPrivateKey.private`

GitGuardian flags any valid OpenPGP secret key in the repository, including keys intended
only for local or acceptance testing.

## What the keys were used for

This service **encrypts** refusal contact details (title, forename, surname) when processing
certain outcomes (e.g. hard refusal), using **public** keys only at runtime:

- **`GpgConfig`** loads `outcomeservice.pgp.fwmtPublicKey` and `outcomeservice.pgp.midlPublicKey`.
- **`EncryptNames`** / **`HardRefusalReceivedProcessor`** encrypt plaintext with those public keys.

The removed **private** key files were **not referenced** by `application.yml` or Java code.
They were almost certainly copied as part of a shared FWMT test keypair (so developers or
acceptance tests could decrypt round-trip data). Runtime configuration only uses:

- `testPublicKey.public` (FWMT)
- `testSecondaryPublicKey.public` (MIDL)

Those public files remain in `src/main/resources` for local/default encryption behaviour.

## Action taken

| Item | Change |
|------|--------|
| `src/main/resources/testPrivateKey.private` | **Removed** (unused) |
| `src/main/resources/testSecondaryPrivateKey.private` | **Removed** (unused) |
| `testPublicKey.public` | **Kept** |
| `testSecondaryPublicKey.public` | **Kept** |
| `application.yml` | Unchanged (already public-key only) |

## How to fix / restore local behaviour

### Run the service locally

Default config uses classpath public keys, which is sufficient for encryption:

```yaml
outcomeservice:
  pgp:
    fwmtPublicKey: "classpath:/testPublicKey.public"
    midlPublicKey: "classpath:/testSecondaryPublicKey.public"
```

For environments that mirror production, override with URIs to real public keys (see
`GpgConfig` / `StorageUtils`).

### Acceptance tests or decrypt round-trips

If you need to decrypt ciphertext produced by this service in tests:

1. Keep private keys **outside git** (e.g. `~/acceptance/data/` as suggested by
   `outcomeservice.pgp.directory` in `application.yml`).
2. Generate a **new** test keypair; do not restore the keys that were removed from the repo.
3. Align public keys in config with the keypair used to generate fixtures.

### Clear the GitGuardian alert on GitHub

1. Merge the commit that removes the private key files.
2. **Resolve** the finding in GitGuardian.
3. If required by policy, rewrite git history to purge old commits containing the keys.

## Related services

- **census31-fwmt-job-service** decrypts the same refusal contact fields; see its
  `docs/gitguardian-pgp-private-key.md` for decryption configuration after the private keys
  were removed from that repo.
