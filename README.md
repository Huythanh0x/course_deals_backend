# Spring Boot: Course Deal Server (Kotlin)

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)

The Course Deal Server is a modern, asynchronous application designed to crawl 100% off coupons from various websites and validate them. It provides a robust REST API for fetching, searching, and filtering free coupons, featuring password-less authentication and automated background processing.

## Features

- **Automated Crawling**: Discovers course deals from multiple aggregator sources.
- **Asynchronous Scraping**: Non-blocking background validation engine powered by **JobRunr**.
- **Modern Authentication**: Secure, biometric-ready auth supporting **Google**, **Apple**, and **Passkeys (WebAuthn)**.
- **Targeted Notifications**: Personalized **FCM push notifications** based on user-defined keywords and categories.
- **SSRF Hardened**: Strict URL validation to protect infrastructure.
- **Search & Filter**: Powerful querying capabilities with rich metadata (language, rating, reviews, etc.) for local client-side sorting.

## Architecture
Each module owns one responsibility; the two deployables (`coupon-api-service`, `coupon-crawler-service`) compose the rest. For the full topology, sequence diagrams, and per-module breakdown, see **[docs/architecture.md](docs/architecture.md)**.
- **`modules/coupon-common`**: Cross-cutting utilities used by nearly every other module (logging helper, shared exceptions).
- **`modules/coupon-domain`**: Pure data layer containing JPA entities, repositories, and DTOs.
- **`modules/coupon-infrastructure`**: Redis caching/config only.
- **`modules/notification-service`**: Firebase/FCM push notifications.
- **`modules/course-scraper`**: Coupon scraping/validation pipeline shared by both deployables (JobRunr-backed).
- **`modules/course-external-api`**: Client for Udemy's public course API (used only by `coupon-api-service`).
- **`modules/identity-service`**: Auth (Social/Passkeys) and user preferences.
- **`modules/coupon-api-service`**: Business REST API (port 8080).
- **`modules/coupon-crawler-service`**: Background discovery and JobRunr worker (port 8081).

## Quick Start
Needs [Java 17](https://jdk.java.net/17/) and [Docker](https://www.docker.com/).

```shell
git clone https://github.com/huythanh0x/course-deal-server
cd course-deal-server
docker compose -f docker-compose.prod.yml up
```

This pulls the API/crawler images built by GitHub Actions and starts MySQL/Redis alongside them.

## Documentation
- **[docs/architecture.md](docs/architecture.md)** — comprehensive system architecture, 9-module deep dive, and end-to-end data flow sequence diagram.
- **[docs/getting-started.md](docs/getting-started.md)** — running services from source, local dev profiles, database migrations, configuration, API docs, and troubleshooting.
- **[docs/business-logic.md](docs/business-logic.md)** — how the discovery/crawling/notification pipeline actually flows.
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — code style, linting, and how to submit a PR.
- **[docs/project-management.md](docs/project-management.md)** — branching, commit, PR, versioning, and CI conventions.

## Contributing
We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for more details.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE.md) file for details.

## Contact
For any inquiries or issues, please open an issue on GitHub or contact us at <a href="mailto:huythanh0x@gmail.com">huythanh0x@gmail.com</a>