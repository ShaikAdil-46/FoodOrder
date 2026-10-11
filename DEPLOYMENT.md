# Deploying FoodOrder (Render + Netlify + Aiven MySQL)

This is the fastest path to a public URL without running Docker yourself:

```
Browser
  └── Netlify (React static build)        https://<your-site>.netlify.app
        └── fetch https://<your-api>.onrender.com/api/...
              └── Render (Spring Boot, Docker)   https://<your-api>.onrender.com
                    └── Aiven MySQL (managed, TLS)  <host>:<port>/defaultdb
```

- **Frontend** → Netlify (free static hosting).
- **Backend** → Render free Docker web service (750 hrs/month, sleeps after 15 min idle).
- **Database** → Aiven free MySQL (always free, no credit card, TLS required).

Render has **no managed MySQL**, which is why the database lives at Aiven.

All three providers are free and do **not** require a credit card.

> **Local config:** the backend auto-loads a `.env` file at the repo root when present, so you can keep `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` and `ADMIN_PASSWORD` there for local runs. Real environment variables take precedence, which is how Render supplies the same values in production.

---

## Step 0 — Push this code

The deployment config files (`netlify.toml`, `render.yaml`, the frontend API-base
change) must be on GitHub:

```powershell
git add .
git commit -m "Add Render + Netlify deployment configuration"
git push origin main
```

---

## Step 1 — Create the MySQL database (Aiven)

1. Sign up at <https://aiven.io> (no card needed).
2. **Create service → MySQL → plan: Free →** pick any region **→ Create**.
3. Wait until the service status is **Running**.
4. Open the service **Overview** and copy the connection details:
   - **Host** (e.g. `mysql-abc123-project.aivencloud.com`)
   - **Port** (e.g. `12345`)
   - **User**: `avnadmin`
   - **Password**: the generated password
   - **Database**: `defaultdb`

Aiven requires TLS. Flyway will create the FoodOrder tables inside `defaultdb`
on first startup, so you do not need to create a database manually.

> Keep the password somewhere safe; you will paste it into Render.

---

## Step 2 — Deploy the backend (Render)

1. Sign up at <https://render.com> with GitHub (no card needed).
2. **New → Blueprint**, connect the `FoodOrder` repository, and click **Apply**.
   Render reads `render.yaml` and creates the `foodorder-api` Docker service.
3. When prompted, fill in the environment variables (all marked `sync: false`):

   | Key | Value |
   | --- | --- |
   | `DB_URL` | `jdbc:mysql://<HOST>:<PORT>/defaultdb?sslMode=REQUIRED&serverTimezone=UTC` |
   | `DB_USER` | `avnadmin` |
   | `DB_PASSWORD` | your Aiven password |
   | `JWT_SECRET` | a random string of at least 32 bytes (see below) |
   | `FRONTEND_ORIGIN` | leave blank for now — you set this in Step 4 |
   | `ADMIN_EMAIL` | *(optional)* e.g. `admin@example.com` |
   | `ADMIN_PASSWORD` | *(optional)* at least 12 characters |

   Generate a JWT secret in PowerShell:

   ```powershell
   [Convert]::ToBase64String([byte[]](1..48 | ForEach-Object { Get-Random -Maximum 256 }))
   ```

4. Wait for the first deploy (Maven builds the jar inside Docker; a few minutes).
5. Copy the service URL, e.g. `https://foodorder-api.onrender.com`, and open:

   ```
   https://foodorder-api.onrender.com/actuator/health
   ```

   It should return `{"status":"UP"}`. The first request after idle takes ~1 minute.

### If you prefer the Dashboard instead of the Blueprint

**New → Web Service** → connect the repo → **Language: Docker** →
**Root Directory: `backend`** → **Instance Type: Free** →
**Health Check Path: `/actuator/health`** → add the same env vars
(plus `SERVER_PORT=8080`) → **Create Web Service**.

---

## Step 3 — Deploy the frontend (Netlify)

1. Sign up at <https://netlify.com> with GitHub.
2. **Add new site → Import an existing project → GitHub →** pick the repository.
   Netlify reads `netlify.toml` (base `frontend`, build `npm run build`, publish `dist`),
   so leave the build settings as detected.
3. Before deploying, expand **Environment variables** and add:

   | Key | Value |
   | --- | --- |
   | `VITE_API_BASE` | your Render URL, e.g. `https://foodorder-api.onrender.com` |

   This bakes the backend origin into the static build. Without it the site calls
   its own origin and the menu will not load.
4. Click **Deploy site**, then copy the site URL, e.g. `https://foodorder-abc.netlify.app`.

---

## Step 4 — Wire up CORS (allow Netlify to call Render)

1. Back in Render, open the `foodorder-api` service → **Environment**.
2. Set `FRONTEND_ORIGIN` to your Netlify URL (comma-separate additional origins):

   ```
   FRONTEND_ORIGIN=https://foodorder-abc.netlify.app
   ```

3. Save. Render redeploys automatically.

> If you later add a Netlify custom domain, add that origin to `FRONTEND_ORIGIN` too.

---

## Step 5 — Verify the live app

1. Open the Netlify URL.
2. Check the menu loads (proves Netlify → Render → MySQL works).
3. **Register**, **sign in**, add to cart, and **checkout**.
4. If you set `ADMIN_EMAIL`/`ADMIN_PASSWORD`, sign in as the admin and open the dashboard.

The first load after 15 idle minutes is slow (~1 minute) while Render wakes up.

---

## Keeping it awake (optional)

Render free services sleep after 15 minutes of no traffic. To avoid the cold start,
point a free uptime monitor (e.g. UptimeRobot) at:

```
https://foodorder-api.onrender.com/actuator/health
```

every 10 minutes.

---

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| Menu shows "The connected API is not returning FoodOrder menu data" | `VITE_API_BASE` not set (or wrong) in Netlify. Set it and redeploy. |
| Browser console: CORS / blocked by CORS policy | `FRONTEND_ORIGIN` on Render must exactly match the Netlify origin (scheme + host, no trailing slash). |
| `/actuator/health` returns 502 or the service crashes on boot | Bad `DB_URL`/`DB_PASSWORD`, or missing `sslMode=REQUIRED`. Check Render logs. |
| `Public Key Retrieval is not allowed` or TLS error | Ensure the JDBC URL ends with `?sslMode=REQUIRED&serverTimezone=UTC`. |
| Direct URL like `/orders` shows Netlify "Page not found" | `netlify.toml` redirect must be present (it is in this repo). |
| First request takes ~1 minute | Normal Render free-tier cold start. |
| Free MySQL powered off by Aiven | Aiven notifies first; power it back on in the Aiven console. |

---

## Security notes

- Never commit `.env` or paste secrets into `netlify.toml`/`render.yaml`. All secrets
  live in the Render dashboard.
- Rotate `JWT_SECRET`, the Aiven password, and the admin password if they ever leak.
- This is a demo deployment on free tiers — not a hardened production setup.
