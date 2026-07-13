# Mental Health Backend

This module contains the Spring Boot backend foundation for the AI-Powered Mental Health Analytics Platform.

## Architecture

- Clean architecture layers: domain, application, infrastructure, interfaces
- API versioning via `/api/v1`
- Environment-based configuration via `application.yml` and profile-specific overrides
- Centralized exception handling and common API response models

## Planned layers

- `domain` for entities and repositories
- `application` for use cases and ports
- `infrastructure` for persistence, security, and external integrations
- `interfaces` for REST controllers and DTOs
- `shared` for common utilities and exception handling

## Run locally

1. Ensure MySQL is running.
2. Set environment variables or update `application-dev.yml`.
3. Run:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
