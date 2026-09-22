# CustomerService

CustomerService is the standalone Spring Boot microservice responsible for all customer management in the guesthouse system.

It is intentionally maintained as its own project and GitHub repository. BookingService remains a separate application and database.

## Responsibilities

CustomerService is responsible for:

- Registering customers.
- Retrieving customers.
- Updating customer information.
- Deleting customers.
- Authenticating customers and issuing JWTs.
- Storing all customer data in its own MySQL database.
- Checking with BookingService before deleting a customer.
- Returning appropriate REST status codes and clear errors.
- Forwarding the JWT when CustomerService calls BookingService.

CustomerService does not access the BookingService database directly.

## Technology

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- MySQL
- Maven
- BCrypt password hashing
- JWT authentication
- Docker
- GitHub Actions

## Project structure

```text
CustomerService/
├── .github/
│   └── workflows/
│       └── ci.yml
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── se/erik/customerservice/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── model/
│   │   │       ├── repository/
│   │   │       ├── service/
│   │   │       ├── security/
│   │   │       ├── config/
│   │   │       └── error/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   │           └── index.html
│   └── test/
├── Dockerfile
├── .dockerignore
├── .env.example
├── .gitignore
├── mvnw
├── pom.xml
└── README.md
```

## REST API

### Customers

```text
GET    /customers
GET    /customers/{id}

POST   /customers

PUT    /customers/{id}

DELETE /customers/{id}
```

Creating a customer returns:

```text
201 Created
```

Deleting a customer is allowed only when there are no active bookings.

If the customer has active bookings, CustomerService returns a conflict response rather than deleting the customer.

### Authentication

```text
POST /auth/login
POST /auth/register
```

Login returns a JWT:

```json
{
  "token": "<JWT>"
}
```

The token is then sent to authenticated endpoints as:

```text
Authorization: Bearer <JWT>
```

## Service-to-service communication

Before deleting a customer, CustomerService calls BookingService:

```text
GET /bookings/customer/{customerId}/active
```

The Authorization header is forwarded with this request.

The result determines whether deletion is allowed.

Conceptually:

```text
DELETE /customers/5
        |
        v
CustomerService
        |
        | REST + forwarded JWT
        v
BookingService
        |
        v
Active booking?
     /       \
   yes       no
    |         |
  409       delete
```

CustomerService never reads the BookingService database directly.

## Security

Passwords are stored as password hashes rather than plain text.

JWTs are generated after successful login.

Do not put JWT secrets, database passwords or other private values in source code.

Authenticated requests use:

```text
Authorization: Bearer <JWT>
```

## Configuration

Create a local `.env` from `.env.example`.

Typical local configuration is:

```text
PORT=8081
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/customerdb
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME
JWT_EXPIRATION=3600000
BOOKING_SERVICE_BASE_URL=http://localhost:8080
FRONTEND_ORIGIN=http://localhost:8080
```

The real `.env` file must never be committed.

Production values are supplied through Railway environment variables.

For Railway, CustomerService should call BookingService through Railway private networking, for example:

```text
BOOKING_SERVICE_BASE_URL=http://booking-service.railway.internal:8080
```

The exact hostname depends on the Railway service name.

## Local development

### Run from IntelliJ

Use Java 17 and a local MySQL database.

The default application port is:

```text
http://localhost:8081
```

### Run tests

Windows:

```powershell
.\mvnw test
```

Linux/macOS:

```bash
./mvnw test
```

The project contains service-layer unit tests and controller-level tests covering customer creation, duplicates, retrieval, updates, deletion rules, and service-unavailable handling.

## Docker

Build the service image locally:

```bash
docker build -t customerservice .
```

Run it with the required environment variables and a reachable MySQL database.

The overall guesthouse Docker Compose setup can run CustomerService together with BookingService and the two databases.

## GitHub Actions

The CI workflow runs on pushes and pull requests targeting `main`.

The push-to-main flow is:

```text
Push to main
    |
    v
Checkout source
    |
    v
Set up Java 17
    |
    v
Run Maven tests
    |
    v
If tests pass:
    |
    +--> Log in to Docker Hub
    |
    +--> Build Docker image
    |
    +--> Push:
          username/customerservice:latest
          username/customerservice:<commit-sha>
```

Docker Hub credentials are stored in GitHub repository secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

The Docker Hub token must be a Personal Access Token.

## Docker image

The CI pipeline publishes:

```text
<DOCKERHUB_USERNAME>/customerservice:latest
<DOCKERHUB_USERNAME>/customerservice:<commit-sha>
```

The image is built only after the test step succeeds.

## Railway deployment

This repository is intended to be connected directly to Railway as a GitHub repository.

Railway detects the root `Dockerfile` and builds the service from the repository.

The intended deployment flow is:

```text
git push main
    |
    v
GitHub Actions
    |
    +--> tests
    |
    +--> Docker Hub image
    |
    v
Railway GitHub deployment
    |
    v
New CustomerService deployment
```

Railway's `Wait for CI` feature can be enabled so Railway waits for the GitHub Actions check to finish before deploying a commit.

CustomerService should have a public Railway domain if the browser-based frontend needs to call CustomerService directly.

Its private Railway hostname should be used for server-to-server communication from CustomerService to BookingService only.

## Environment and secret handling

Never commit:

```text
.env
.env.*
target/
.idea/
```

`.env.example` is safe to commit because it contains placeholders only.

Store these values outside the source code:

- MySQL password.
- JWT secret.
- Docker Hub token.
- Railway production environment variables.

GitHub Actions secrets are used for CI/CD credentials.

Railway Variables are used for deployed application configuration.

## Testing

The service-layer tests cover meaningful application logic, including:

- Customer creation.
- Duplicate email handling.
- Customer retrieval.
- Missing customer handling.
- Customer updates.
- Deletion when active bookings exist.
- Successful deletion when no active bookings exist.
- BookingService unavailable during deletion.

Controller tests also verify REST behavior.

## Assignment role

CustomerService satisfies the Customer Service part of the guesthouse microservice assignment:

- customer management is isolated here;
- customer data has its own database;
- login issues JWTs;
- deletion checks BookingService through REST;
- the service has its own Dockerfile;
- GitHub Actions runs the tests;
- the CI workflow builds and publishes a Docker image after successful tests;
- Railway can deploy the service from its GitHub repository.

The optional ReviewService and Kubernetes requirements are separate assignment items and are not implemented by this repository.
