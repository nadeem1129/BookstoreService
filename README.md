# Bookstore Service

A Spring Boot REST API for a bookstore application with JWT-based authentication, book catalog browsing, cart management, and order checkout.

## Tech stack

- Java 17
- Spring Boot 3.2.4
- Spring Web / Spring Security / Spring Data JPA
- H2 in-memory database
- Maven wrapper included

## Prerequisites

Before running the project, make sure you have:

- Java 17 or later installed
- Maven 3.9+ installed, or use the included Maven wrapper (`mvnw`)
- Git installed (optional, for cloning)

To verify Java is installed:

```bash
java -version
```

## Run the project

From the project root (`BookstoreService`), run either of the following commands:

Using the Maven wrapper:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell or Command Prompt:

```powershell
mvnw.cmd spring-boot:run
```

Or build and then run the packaged JAR:

```bash
./mvnw clean package
java -jar target/bookstore-api-0.0.1-SNAPSHOT.jar
```

The application starts on:

```text
http://localhost:8080
```

## Configuration

The app uses an embedded H2 database and is configured in `src/main/resources/application.yml`.

- Server port: `8080`
- Database URL: `jdbc:h2:mem:bookstoredb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Username: `sa`
- Password: empty string
- H2 console: `http://localhost:8080/h2-console`

### JWT signing key

The application requires the `JWT_SIGNING_SECRET` environment variable to start. It must be a Base64-encoded key containing at least 32 random bytes (256 bits), used to sign and verify JWTs. Generate a fresh key for local development; do not commit it or reuse a sample key in production.

Generate a key with OpenSSL:

```bash
openssl rand -base64 32
```

Set the generated value in the same terminal session before starting the app.

Linux/macOS:

```bash
export JWT_SIGNING_SECRET="<generated-key>"
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
$env:JWT_SIGNING_SECRET = "<generated-key>"
.\mvnw.cmd spring-boot:run
```

Windows Command Prompt:

```bat
set JWT_SIGNING_SECRET=<generated-key>
mvnw.cmd spring-boot:run
```

You can also set this environment variable in your IDE run configuration. The variable must be set for packaged JAR runs as well.

To connect to the H2 console, use:

- JDBC URL: `jdbc:h2:mem:bookstoredb`
- Username: `sa`
- Password: empty

## Seed data

The app loads sample books at startup from `src/main/resources/data.sql`.

## API endpoints

### Authentication

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Log in and receive a JWT token

Example login request body:

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

After login, include the token in the request header for protected endpoints:

```http
Authorization: Bearer <your-jwt-token>
```

### Books

- `GET /api/books` - Get all books
- `GET /api/books/{id}` - Get a book by ID

### Cart

- `GET /api/cart` - Get current cart
- `POST /api/cart/items` - Add item to cart
- `PUT /api/cart/items/{bookId}` - Update quantity
- `DELETE /api/cart/items/{bookId}` - Remove item from cart

### Orders

- `POST /api/orders/checkout` - Create an order from the current cart (initially `CREATED`)
- `POST /api/orders/{id}/payment` - Explicitly attempt payment and mark the order `PAID` after approval
- `GET /api/orders` - Get all orders for the current user
- `GET /api/orders/{id}` - Get order by ID

Payments use a development stub outside the `prod` profile. It approves payment attempts without collecting money; production requires a real `PaymentProcessor` implementation.

## Run tests

```bash
./mvnw test
```

## Troubleshooting

- If Maven wrapper permission issues occur on Linux/macOS, run:

```bash
chmod +x mvnw
```

- If a port conflict occurs, update the `server.port` value in `src/main/resources/application.yml`.
- If dependencies fail to resolve, run:

```bash
./mvnw clean install
```

## Notes

This project is built for local development and uses an in-memory database, so data resets each time the application restarts.
