# Fingaurd AI — Deployment Guide

This guide details the complete deployment setup for Fingaurd AI across **Vercel** (Frontend), **Render** (Spring Boot Backend), and **Managed MySQL** (Aiven or TiDB Cloud Serverless).

---

## 1. Environment Variables Reference

| Component | Variable Name | Recommended Value / Example | Purpose |
|---|---|---|---|
| **Backend** | `PORT` | `8080` (or `10000` on Render) | Web server binding port |
| **Backend** | `SPRING_DATASOURCE_URL` | `jdbc:mysql://<host>:3306/<db>?sslMode=REQUIRED` | MySQL 8 connection URI |
| **Backend** | `SPRING_DATASOURCE_USERNAME` | `<db_username>` | Database user |
| **Backend** | `SPRING_DATASOURCE_PASSWORD` | `<db_password>` | Database password |
| **Backend** | `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | 256-bit HMAC secret |
| **Backend** | `FINGAURD_ALLOWED_ORIGINS` | `https://<your-frontend>.vercel.app,http://localhost:5173` | CORS allowed origins |
| **Frontend**| `VITE_API_BASE_URL` | `https://<your-backend>.onrender.com` | Base backend API URL |

---

## 2. Step 1: Database (Free Cloud MySQL on Aiven or TiDB)

1. Sign up for a free managed MySQL service on [Aiven.io](https://aiven.io/) or [TiDB Cloud Serverless](https://tidbcloud.com/).
2. Create a new MySQL service named `fingaurddb`.
3. Note your connection details:
   - Host, Port (e.g. `3306` or `4000`)
   - User, Password
   - Database name: `fingaurddb`
4. Schema migrations are performed automatically on startup via Hibernate DDL update, and the demo user (`demo@fingaurd.ai`) is automatically seeded.

---

## 3. Step 2: Backend Deployment on Render

### Option A: Using Render Web Console (Click-by-Click)
1. Push this repository to your GitHub account.
2. In [Render Dashboard](https://dashboard.render.com/), click **New +** $\rightarrow$ **Web Service**.
3. Connect your GitHub repository.
4. Set the following parameters:
   - **Name:** `fingaurd-backend`
   - **Language:** `Docker`
   - **Dockerfile Path:** `deploy/Dockerfile.backend`
   - **Docker Context:** `backend`
   - **Region:** Singapore or Oregon
   - **Instance Type:** Free
   - **Health Check Path:** `/actuator/health`
5. Under **Environment Variables**, add:
   - `PORT`: `10000`
   - `SPRING_DATASOURCE_URL`: your cloud MySQL connection string
   - `SPRING_DATASOURCE_USERNAME`: your MySQL user
   - `SPRING_DATASOURCE_PASSWORD`: your MySQL password
   - `JWT_SECRET`: a 64-character hex key
   - `FINGAURD_ALLOWED_ORIGINS`: `https://<your-app>.vercel.app`
6. Click **Create Web Service**. Wait for the build and deployment to complete. Note the backend URL (e.g., `https://fingaurd-backend.onrender.com`).

### Option B: Using Render Blueprint (`render.yaml`)
1. Click **New +** $\rightarrow$ **Blueprint**.
2. Select repository, and Render will parse `deploy/render.yaml` automatically.

---

## 4. Step 3: Frontend Deployment on Vercel

1. Go to [Vercel Dashboard](https://vercel.com/) and click **Add New...** $\rightarrow$ **Project**.
2. Import your GitHub repository.
3. Configure project settings:
   - **Framework Preset:** Vite
   - **Root Directory:** `frontend`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
4. In **Environment Variables**:
   - `VITE_API_BASE_URL`: `https://fingaurd-backend.onrender.com`
5. Note that `frontend/vercel.json` already contains SPA client-side routing and `/api/*` proxies to avoid cross-origin issues:
   ```json
   {
     "framework": "vite",
     "rewrites": [
       { "source": "/api/(.*)", "destination": "https://fingaurd-backend.onrender.com/api/$1" },
       { "source": "/(.*)", "destination": "/index.html" }
     ]
   }
   ```
6. Click **Deploy**. Within 60 seconds, your site is live with an SSL certificate.

---

## 5. Step 4: Verification & Smoke Test (5 Minutes)

1. Open your live frontend URL (e.g. `https://fingaurd-ai.vercel.app`).
2. Click **Try Interactive Demo** — verify auto-login as `demo@fingaurd.ai`.
3. Check Dashboard: verify Total Exposure, Risk Index gauge sweep, area chart, and donut chart.
4. Navigate to **Pre-Commitment Check**:
   - Propose ₹4,00,000 to "Apex Wealth Advisory".
   - Click "Paste Sample Pressure Text", check consent, and click **Run Pre-Commitment Risk Analysis**.
5. Confirm generated report displays **HIGH (73)** without pressure text or **CRITICAL** with pressure text.
6. Click **Pause Screen** and confirm the Proceed button is disabled until the checklist acknowledgement checkbox is ticked.
