# Outcome Service Configuration Trace Map

> This map traces checked-in configuration sources for `outcome-service`; it does not represent effective live configuration. Secret values are omitted.

## Configuration trace map

| `application.yml` | `Default` | `application-local.yml` | `census31-fwmt-ci-terraform/k8s/outcome-service/outcome-service-deployment.yaml` | `census31-fwmt-terraform/secrets.tf` |
|---|---|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost/postgres?currentSchema=fwmtg` | — | `DB_NAME <- db-config[db-name]`; `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/$(DB_NAME)?currentSchema=fwmtg` | `db-config[db-name]=var.db_name` |
| `spring.datasource.username` | `postgres` | — | `SPRING_DATASOURCE_USERNAME <- db-credentials[username]` | `db-credentials[username]=var.db_user_name` |
| `spring.datasource.password` | `[MASKED]` | — | `SPRING_DATASOURCE_PASSWORD <- db-credentials[password]` | `db-credentials[password]=random_password.cloudsqlUser.result` |
| `spring.cloud.gcp.project-id` | `${FWMT_PUBSUB_PROJECT:fwmt-local}` | — | `SPRING_CLOUD_GCP_PROJECT_ID <- project-config[project-id]` | `project-config[project-id]=var.project_id` |
| `spring.cloud.gcp.credentials.enabled` | `false` | — | `SPRING_CLOUD_GCP_CREDENTIALS_ENABLED=true` | — |
| `spring.cloud.gcp.pubsub.emulator-host` | — | `spring.cloud.gcp.pubsub.emulator-host=localhost:8085` | — | — |
| `management.endpoint.env.enabled` | `${APP_TESTING:false}` | `management.endpoint.env.enabled=true` | `APP_TESTING=true` | — |
| `management.endpoint.env.post.enabled` | `${APP_TESTING:false}` | `management.endpoint.env.post.enabled=true` | `APP_TESTING=true` | — |
| `management.endpoint.refresh.enabled` | `${APP_TESTING:false}` | `management.endpoint.refresh.enabled=true` | `APP_TESTING=true` | — |
| `springdoc.cache.disabled` | `${APP_TESTING:false}` | — | `APP_TESTING=true` | — |
| `app.testing` | `false` | `app.testing=true` | `APP_TESTING=true` | — |
| `app.messaging.pubsub.rm-project-id` | `${RM_PUBSUB_PROJECT:}` | — | `RM_PUBSUB_PROJECT <- project-config[rm-pubsub-project-id]` | `project-config[rm-pubsub-project-id]=var.rm_pubsub_project_id` |

## Verification items

- The PGP environment-variable entries in `census31-fwmt-ci-terraform/k8s/outcome-service/outcome-service-deployment.yaml` are commented out; `outcomeservice.pgp.*` in `src/main/resources/application.yml` therefore has no active override in that manifest.

## Omitted non-property configuration

- Probes, resources, Cloud SQL proxy, and service account: `census31-fwmt-ci-terraform/k8s/outcome-service/outcome-service-deployment.yaml`.