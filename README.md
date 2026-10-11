# FoodOrder

FoodOrder is a full-stack online food ordering application built from the supplied **“FoodOrder: Online Food Ordering System”** abstract. Customers can discover dishes, search and filter a menu, manage a persistent cart, place orders, and check order status. Administrators have a separate store dashboard to maintain food availability, review orders, and move them through the fulfilment workflow.

The application follows the supplied orange-and-cream restaurant reference: responsive food photography, category tiles, dish cards, clean account forms, a compact checkout, order history, and an administrator dashboard.

## Project goals

- Put the customer food-order journey in one responsive web application.
- Maintain customer accounts, menu data, carts, and order records.
- Make menu search, categories, and availability easy to use.
- Let customers review their own orders and follow fulfilment status.
- Give administrators protected tools for menu and order management.

## Pages and roles

| Page | Customer | Administrator |
| --- | --- | --- |
| Home, categories, menu search, food details | Browse and search available dishes | Browse and search dishes |
| Register, sign in, profile | Create and update own account | Update own account |
| Cart and checkout | Manage own cart and place an order | Manage own cart and place an order |
| Order history and confirmation | See own orders and their current status | See own orders |
| Store dashboard | Not available | Manage dish availability, create dishes, view all orders, and advance order status |

Public registration always assigns the `CUSTOMER` role. An account cannot grant itself administrator access. Backend authorization protects administrator APIs independently of the UI.

## Technology

- **Frontend:** React 19, TypeScript, Vite, React Router, Lucide icons, responsive CSS.
- **Backend:** Java 17+, Spring Boot 3.4, Spring Web, Spring Security, Spring Data JPA, Hibernate and Bean Validation.
- **Database:** MySQL 8.4; Flyway migrations and development menu seed data.
- **Authentication:** BCrypt (cost 12), short-lived signed JWT bearer tokens, and server-side role checks.
- **Tests:** JUnit 5, Spring Boot Test, MockMvc and an isolated H2 database.
- **Local stack:** Maven 3.9+, Node.js 22+, npm, optional Docker Compose.
- **API reference:** OpenAPI JSON at `/v3/api-docs` and Swagger UI at `/swagger-ui/index.html`.

## Architecture

The React app makes typed HTTP requests to `/api`. During local development, Vite forwards `/api` and `/actuator` to FoodOrder's Spring Boot API at port 8090 (`frontend/.env.development`). This avoids collisions with unrelated applications on the common Spring Boot port 8080. In the container stack, Nginx serves the built single-page app, proxies API and health requests, and supports direct navigation to client-side routes.

The backend separates authentication, users, food, cart, orders, security, and API error handling into small packages. Controllers accept validated DTOs, services enforce business rules and transaction boundaries, repositories handle persistence, and JPA entities map to relational tables. Flyway owns the production schema; Hibernate checks it rather than silently creating or recreating tables.

## Data model

| Entity/table | Purpose | Important relationships |
| --- | --- | --- |
| `users` | Customer and administrator accounts | Unique email and mobile |
| `food_items` | Named, categorized, priced, available menu dishes | Indexed by category/availability and name |
| `cart_items` | Persistent per-customer item quantities | Unique user + food pair |
| `orders` | Customer checkout, delivery address, totals, fulfilment status | Belongs to one user |
| `order_items` | Historical dish names, unit prices, and quantities | Belongs to one order; preserves checkout prices |

Order progression is `PLACED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED`. Eligible earlier orders can be cancelled. Customers can only see their own cart, profile, and order history.

## Prerequisites

- Java 17 or newer and Maven 3.9 or newer.
- Node.js 22 or newer and npm.
- MySQL 8.0 or newer for the normal application runtime.
- Docker Desktop with Docker Compose v2 is optional if running the complete container stack.

The verified development machine has Java 17.0.20 and Maven 3.9.16. It has Node.js 24.18 and npm 11.16. MySQL 8.0 is installed and running. Docker is **not installed** on the verified machine; container builds and Compose startup have not been tested there.

## Run locally on Windows

Open PowerShell in the repository root.

> The backend automatically loads a `.env` file from the repository root (or its parent) when one is present, so you can keep local `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` and other settings in `.env` instead of exporting them. Real environment variables always take precedence, which is how a production host supplies secrets. `JWT_SECRET` is required: the API fails fast at startup if it is missing or shorter than 32 bytes.

1. **Configure MySQL.** Start the MySQL service if needed. Create a database/user or use a MySQL account you already control. Never put production credentials in source control.

   ```powershell
   # Optional local service start (requires appropriate Windows permissions)
   Start-Service MySQL80
   ```

2. **Set backend environment variables** using your local database username, password, and an unpredictable secret of at least 32 bytes:

   ```powershell
   $env:DB_URL = 'jdbc:mysql://localhost:3306/foodorder?createDatabaseIfNotExist=true&serverTimezone=UTC'
   $env:DB_USER = 'your_local_mysql_user'
   $env:DB_PASSWORD = 'your_local_mysql_password'
   $env:JWT_SECRET = 'replace-with-at-least-32-random-secret-bytes'
   $env:FRONTEND_ORIGIN = 'http://localhost:5173,http://127.0.0.1:5173'
   ```

3. **Start the API** in this PowerShell window. Flyway creates the schema and inserts sample dishes the first time:

   ```powershell
   $env:SERVER_PORT = '8090'
   mvn -f backend\pom.xml spring-boot:run
   ```

   Alternatively, run with the isolated in-memory H2 database (no MySQL account required):

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'local-h2'
   mvn -Plocal-h2 -f backend\pom.xml spring-boot:run
   ```

4. **Install frontend libraries and start Vite** in another PowerShell window. `frontend/.env.development` configures its API proxy to `http://127.0.0.1:8090`; do not point it at another application's port:

   ```powershell
   Set-Location frontend
   npm install
   npm run dev
   ```

5. Open <http://localhost:5173>. The API is at <http://localhost:8090>, health at <http://localhost:8090/actuator/health>, and Swagger UI at <http://localhost:8090/swagger-ui/index.html>.

The backend needs a reachable MySQL server; setting the environment variables alone does not start one. For the complete isolated local database and application, use Compose below.

## Optional administrator account

Set `ADMIN_EMAIL` and a unique `ADMIN_PASSWORD` containing at least 12 characters **before the first application start**. On startup, FoodOrder creates this administrator if the email is not already registered. Subsequent starts do not overwrite or reset that account. The initial account gets a stable, unique placeholder mobile number; update it from the profile page.

```powershell
$env:ADMIN_EMAIL = 'admin@example.com'
$env:ADMIN_PASSWORD = 'replace-with-your-own-long-unique-password'
```

No administrator password or signing key is hardcoded into the source tree. For a real deployment, provision administrators through a controlled secrets-management process.

## Docker Compose

Copy `.env.example` to `.env`, replace **all** sample password and key values with strong unique values, and then run these commands from the repository root:

```powershell
Copy-Item .env.example .env
# Edit .env using a local editor; do not commit it.
docker compose up --build
```

Open <http://localhost>. Compose starts MySQL, waits for the MySQL/backend health checks, builds a non-root Java runtime image and an Nginx-served frontend, and runs Flyway on API startup. Persistent development database files live in the named `mysql_data` volume. The frontend proxies API calls to the backend, so browser CORS is not needed for the composed UI.

Stop the development stack with `docker compose down`. **This leaves the named database volume intact.** Removing the volume destroys its data; only do that when you explicitly want to reset your local development database.

## REST API

Responses use JSON. Request validation errors include a `fieldErrors` object; other failures return a safe message and HTTP status. Invalid credentials return the same message regardless of whether the email exists.

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Register with first name, last name, mobile, email, and password |
| `POST` | `/api/auth/login` | Public | Sign in using email and password |
| `GET` | `/api/auth/me` | Signed-in user | Read the current user's safe profile |
| `PUT` | `/api/profile` | Signed-in user | Update the current user's first name, last name, or mobile |
| `GET` | `/api/foods` | Public | Paginated name/description search; optional `search`, `category`, and `available` filters |
| `GET` | `/api/foods/{id}` | Public | Read one food item |
| `POST`, `PUT`, `DELETE` | `/api/foods[/{id}]` | Administrator | Create, edit, or remove menu items |
| `GET` | `/api/cart` | Signed-in user | Read the current user's cart |
| `POST` | `/api/cart/items` | Signed-in user | Add `{ "foodId": 1, "quantity": 1 }` |
| `PUT` | `/api/cart/items/{foodId}?quantity=2` | Signed-in user | Change an item quantity; use zero to remove |
| `DELETE` | `/api/cart/items/{foodId}` | Signed-in user | Remove one item |
| `POST` | `/api/orders` | Signed-in user | Checkout with `{ "deliveryAddress": "..." }` |
| `GET` | `/api/orders` | Signed-in user | Read only the current user's order history |
| `GET` | `/api/orders/admin` | Administrator | Read all customer orders |
| `PATCH` | `/api/orders/{id}/status` | Administrator | Advance or cancel an eligible order |

Send authenticated requests with `Authorization: Bearer <token>`. Registration/login responses contain a signed token and a public user profile; password hashes are never returned. Logout is client-side token deletion because the API is stateless; issued tokens otherwise remain usable until their configured expiration.

## Install, build, and test

```powershell
# Frontend dependency installation
Set-Location frontend
npm install

# Type-check + production bundle; ESLint
npm run build
npm run lint

# Backend dependency resolution, compilation and integration tests (from repository root)
Set-Location ..
mvn -f backend\pom.xml test

# Build an executable jar after tests pass
mvn -f backend\pom.xml package
```

The backend test profile uses an in-memory H2 database and does not connect to or modify MySQL. The integration suite covers the full register → browse → cart → checkout → order history journey, prevents self-assigned administrator roles, checks password hashing/duplicate registration, rejects unauthenticated access, and enforces customer/administrator permissions.

## Security and operational notes

- BCrypt password hashes, validated DTOs, unique email/mobile constraints, safe error messages, expiring signed JWTs, role-guarded administrator endpoints, and customer-scoped data access are implemented.
- CORS defaults to the local Vite origin and is configurable with `FRONTEND_ORIGIN`. The Compose frontend proxies same-origin API calls.
- Local tokens are held in browser local storage. This project does not yet provide refresh tokens, token revocation/denylisting, email verification, password reset, or login rate limiting.
- Checkout records an order only; it does **not** process a real payment or collect card details. Delivery timing is illustrative and no external restaurant/delivery service is connected.
- Actuator exposes only basic health and info endpoints, with health details hidden. Database credentials and JWT signing keys must be supplied out of band. Use HTTPS at a production reverse proxy and a production secrets manager.
- The development Flyway migration inserts illustrative sample dishes using publicly hosted food photographs. They are starting menu content, not a live restaurant feed.
- `npm install` reported zero known vulnerabilities during setup. A dedicated Maven CVE scanner, production MySQL migration run, production deployment, image scan, and Docker runtime test have **not** been performed in this environment.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| API cannot connect to MySQL | Confirm the service is running, `DB_URL`/`DB_USER`/`DB_PASSWORD` match your local database, and the selected database is reachable. |
| Flyway validation fails | Preserve production data; inspect the migration and schema before applying any change. Do not enable Hibernate `create` or `create-drop` for production. |
| Vite cannot reach the API | Check that FoodOrder listens on port 8090 and that `frontend/.env.development` targets `http://127.0.0.1:8090`. |
| Login returns 401 | Check the email/password pair. The application intentionally does not reveal which value is incorrect. |
| A local administrator is missing | Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` before API startup; check that the configured email is not already a customer account. |
| Port 8090, 5173, 3306, or 80 is in use | Stop only the service you own or configure an unused local port. Port 8080 may belong to another application. |

## Project layout

```text
backend/
  src/main/java/com/foodorder/{auth,cart,common,food,order,security,user}/
  src/main/resources/db/migration/
  src/test/
frontend/
  src/App.tsx
  src/App.css
  src/api.ts
  src/index.css
docker-compose.yml
.env.example
```

## Current limitations

The verified implementation covers the abstract's account, food catalog, category/search filters, cart, order placement/history/status, and administrator menu/order operations. Online payments, address book, favorites, email notifications, delivery-partner tracking, advanced analytics, and administrator user management are not implemented. Full interactive screen coverage from the supplied 30-screen concept is not claimed; the implemented pages follow its principal storefront, account, cart, history, and admin designs.
