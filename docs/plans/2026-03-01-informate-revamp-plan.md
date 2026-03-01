# Informate Revamp Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Revamp Informate to use self-hosted Ollama instead of OpenAI, migrate to PostgreSQL, refresh the UI, and deploy to Kubernetes following homeops patterns.

**Architecture:** Incremental refactor of existing Java/Spark backend + React frontend. Swap OpenAI API calls for Ollama's OpenAI-compatible endpoint. Replace SQLite with PostgreSQL. Refresh all frontend components with modern Tailwind design. Create bjw-s app-template Helm releases for Kubernetes deployment.

**Tech Stack:** Java 17 + Spark Framework, React 19 + Tailwind CSS, PostgreSQL, Ollama (llama3.1:70b), Kubernetes (Flux CD + bjw-s app-template), Nginx, GitHub Actions + GHCR.

---

## Task 1: Swap OpenAI for Ollama in AI.java

**Files:**
- Modify: `Backend/informate/src/main/java/com/example/informate/AI.java:43-62`

**Step 1: Replace hardcoded OpenAI constants with configurable Ollama env vars**

Replace lines 43-62 of `AI.java` with:

```java
/**
 * The endpoint URL for the LLM API (Ollama's OpenAI-compatible endpoint).
 * Configurable via OLLAMA_URL environment variable.
 */
private static final String API_URL = EnvLoader.getEnv("OLLAMA_URL",
    "http://ollama.ai.svc.cluster.local:11434") + "/v1/chat/completions";

/**
 * The LLM model to use for text processing.
 * Configurable via OLLAMA_MODEL environment variable.
 */
private static final String MODEL = EnvLoader.getEnv("OLLAMA_MODEL", "llama3.1:70b");
```

**Step 2: Remove API key requirement from callOpenAI method**

In `AI.java`, replace the `getApiKey()` method (lines 55-62) — delete it entirely.

In `callOpenAI` method (lines 178-235), make these changes:

1. Remove the API key check block (lines 185-188)
2. Remove the Authorization header (line 196)
3. Add a connection timeout of 120 seconds after line 193:

```java
conn.setConnectTimeout(10000);  // 10s connect timeout
conn.setReadTimeout(120000);    // 120s read timeout (local LLM is slower)
```

4. Rename the method from `callOpenAI` to `callLLM` and update all references (line 109 calls it).

**Step 3: Update class javadoc**

Replace the class javadoc (lines 29-36) to reference Ollama instead of OpenAI.

**Step 4: Verify the build compiles**

Run: `cd Backend/informate && mvn compile`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add Backend/informate/src/main/java/com/example/informate/AI.java
git commit -m "feat: replace OpenAI with configurable Ollama LLM endpoint"
```

---

## Task 2: Add PostgreSQL JDBC driver to pom.xml

**Files:**
- Modify: `Backend/informate/pom.xml:16-50`

**Step 1: Add PostgreSQL dependency, remove SQLite dependency**

In `pom.xml`, replace the sqlite-jdbc dependency (lines 23-27) with:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

**Step 2: Verify dependencies resolve**

Run: `cd Backend/informate && mvn dependency:resolve`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add Backend/informate/pom.xml
git commit -m "feat: replace SQLite with PostgreSQL JDBC driver"
```

---

## Task 3: Migrate articles.java from SQLite to PostgreSQL

**Files:**
- Modify: `Backend/informate/src/main/java/com/example/informate/articles.java`

**Step 1: Replace the entire articles.java constructor and connection setup**

Replace the constructor (lines 44-67) to use PostgreSQL with env vars:

```java
public articles() {
    try {
        String dbUrl = EnvLoader.getEnv("DATABASE_URL", "jdbc:postgresql://localhost:5432/informate");
        String dbUser = EnvLoader.getEnv("POSTGRES_USER", "informate");
        String dbPassword = EnvLoader.getEnv("POSTGRES_PASSWORD", "");

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS articles (
                    id SERIAL PRIMARY KEY,
                    title TEXT UNIQUE NOT NULL,
                    summary TEXT,
                    keywords TEXT,
                    raw_text TEXT,
                    images TEXT,
                    created_at TIMESTAMP DEFAULT NOW()
                )
            """);
        }
        logger.info("Articles database initialized successfully (PostgreSQL)");
    } catch (SQLException e) {
        logger.error("FATAL: Failed to initialize articles database", e);
        System.exit(1);
    }
}
```

**Step 2: Update insertRawText to use PostgreSQL syntax**

Replace the SQL in `insertRawText` (line 81) — change `INSERT OR IGNORE` (SQLite-only) to PostgreSQL's `ON CONFLICT DO NOTHING`:

```java
try (PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO articles (title, raw_text) VALUES (?, ?) ON CONFLICT (title) DO NOTHING")) {
```

Also rename the column from `rawText` to `raw_text` in the parameter binding.

**Step 3: Update insertCompleteArticle to use raw_text column name**

In `insertCompleteArticle` (line 109), the SQL references `articles` table which is fine. Just verify the UPDATE query works with PostgreSQL (it does — standard SQL).

**Step 4: Update getAllArticles to use raw_text column**

In `getAllArticles` (lines 127-152), change `rs.getString("rawText")` to `rs.getString("raw_text")` (line 143).

**Step 5: Update getArticleByTitle to use raw_text column**

In `getArticleByTitle` (lines 161-185), change `rs.getString("rawText")` to `rs.getString("raw_text")` (line 174). Also add `created_at` to the returned map:

```java
article.put("createdAt", rs.getString("created_at"));
```

**Step 6: Verify compilation**

Run: `cd Backend/informate && mvn compile`
Expected: BUILD SUCCESS

**Step 7: Commit**

```bash
git add Backend/informate/src/main/java/com/example/informate/articles.java
git commit -m "feat: migrate articles storage from SQLite to PostgreSQL"
```

---

## Task 4: Migrate auth.java from SQLite to PostgreSQL

**Files:**
- Modify: `Backend/informate/src/main/java/com/example/informate/auth.java`

**Step 1: Update initialiseDB to use PostgreSQL**

Replace `initialiseDB` method (lines 45-63):

```java
public void initialiseDB(){
    try{
        String dbUrl = EnvLoader.getEnv("DATABASE_URL", "jdbc:postgresql://localhost:5432/informate");
        String dbUser = EnvLoader.getEnv("POSTGRES_USER", "informate");
        String dbPassword = EnvLoader.getEnv("POSTGRES_PASSWORD", "");

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL)");
        }
        System.out.println("Authentication database initialized successfully (PostgreSQL).");
    } catch (SQLException e){
        System.err.println("FATAL: Failed to initialize authentication database: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }
}
```

Note: Table name changed from `user` to `users` (avoids PostgreSQL reserved word issues).

**Step 2: Update all SQL references from table `user` to `users`**

Replace all occurrences of `"user"` table name in SQL strings:
- Line 80: `INSERT INTO user` → `INSERT INTO users`
- Line 130: `SELECT username FROM user` → `SELECT username FROM users`
- Line 191: `SELECT username FROM user` → `SELECT username FROM users`
- Line 239: `SELECT password FROM user` → `SELECT password FROM users`

**Step 3: Verify compilation**

Run: `cd Backend/informate && mvn compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add Backend/informate/src/main/java/com/example/informate/auth.java
git commit -m "feat: migrate auth storage from SQLite to PostgreSQL"
```

---

## Task 5: Update EnvLoader for graceful fallback (no crash without .env)

**Files:**
- Modify: `Backend/informate/src/main/java/com/example/informate/EnvLoader.java:31-66`

**Step 1: Make .env loading truly optional**

The current code (line 63-64) prints an error message suggesting a .env file is required. In Kubernetes, env vars come from the pod spec, not a .env file. Change the catch block (lines 62-65) to be silent:

```java
} catch (IOException e) {
    // .env file not found - will fall back to system environment variables
    loaded = true;
}
```

**Step 2: Commit**

```bash
git add Backend/informate/src/main/java/com/example/informate/EnvLoader.java
git commit -m "fix: make .env file optional, graceful fallback to system env vars"
```

---

## Task 6: Update .env.example and docker-compose.yml for Ollama

**Files:**
- Modify: `.env.example`
- Modify: `docker-compose.yml:11-12`

**Step 1: Update .env.example**

Replace contents of `.env.example`:

```
# Ollama Configuration
OLLAMA_URL=http://ollama.ai.svc.cluster.local:11434
OLLAMA_MODEL=llama3.1:70b

# PostgreSQL Configuration
DATABASE_URL=jdbc:postgresql://postgres.databases.svc.cluster.local:5432/informate
POSTGRES_USER=informate
POSTGRES_PASSWORD=your-postgres-password-here
```

**Step 2: Update docker-compose.yml environment**

Replace line 12 in `docker-compose.yml`:

```yaml
    environment:
      - OLLAMA_URL=${OLLAMA_URL:-http://host.docker.internal:11434}
      - OLLAMA_MODEL=${OLLAMA_MODEL:-llama3.1:70b}
      - DATABASE_URL=${DATABASE_URL:-jdbc:postgresql://host.docker.internal:5432/informate}
      - POSTGRES_USER=${POSTGRES_USER:-informate}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-}
```

**Step 3: Commit**

```bash
git add .env.example docker-compose.yml
git commit -m "feat: update env config for Ollama and PostgreSQL"
```

---

## Task 7: Update nginx.conf proxy timeout for Ollama

**Files:**
- Modify: `Frontend/informate-frontend/nginx.conf:30-40`

**Step 1: Add proxy timeout for slow LLM responses**

In `nginx.conf`, add timeout settings to the `/api/` location block (after line 39):

```nginx
proxy_read_timeout 300s;
proxy_connect_timeout 10s;
proxy_send_timeout 300s;
```

**Step 2: Commit**

```bash
git add Frontend/informate-frontend/nginx.conf
git commit -m "fix: increase nginx proxy timeout for LLM inference"
```

---

## Task 8: Fix AddArticle processing status display

**Files:**
- Modify: `Frontend/informate-frontend/src/components/AddArticle.js:49-81`

**Step 1: Update polling logic to handle backend status strings**

The backend sends status as a plain string (e.g., "Scraping website...", "Processing with AI...", "Complete", "Error: ..."). But the frontend (lines 58-70) expects `data.completed`, `data.success`, `data.progress` which don't exist.

Replace the polling effect (lines 49-81) with:

```javascript
useEffect(() => {
    if (!processingId) return;

    const pollStatus = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/articles/status/${processingId}`);
        const statusText = response.data.status || 'Processing...';

        setStatus(statusText);

        // Map status text to progress
        const progressMap = {
          'Scraping website...': 15,
          'Extracting content...': 30,
          'Processing images...': 45,
          'Saving to database...': 60,
          'Processing with AI...': 75,
          'Complete': 100
        };
        setProgress(progressMap[statusText] || progress);

        if (statusText === 'Complete') {
          setSuccess(true);
        } else if (statusText.startsWith('Error:')) {
          setFailed(true);
        }
      } catch (err) {
        console.error('Error polling status:', err);
      }
    };

    const interval = setInterval(pollStatus, 2000);
    return () => clearInterval(interval);
  }, [processingId, API_BASE_URL, progress]);
```

**Step 2: Commit**

```bash
git add Frontend/informate-frontend/src/components/AddArticle.js
git commit -m "fix: align processing status polling with backend status strings"
```

---

## Task 9: Refresh Dashboard UI

**Files:**
- Modify: `Frontend/informate-frontend/src/components/Dashboard.js`

**Step 1: Add created_at display and improve article card layout**

The Dashboard already has a solid layout with article cards, images, keywords, and a grid. The main improvement is adding the `created_at` timestamp from the new PostgreSQL schema.

Add a timestamp display in the article card (after the keywords div around line 222):

```jsx
{/* Timestamp */}
{article.createdAt && (
  <div className="mt-3 pt-3 border-t border-slate-100 dark:border-slate-700">
    <span className="text-xs text-slate-500 dark:text-slate-500">
      {new Date(article.createdAt).toLocaleDateString('en-GB', {
        day: 'numeric', month: 'short', year: 'numeric'
      })}
    </span>
  </div>
)}
```

**Step 2: Commit**

```bash
git add Frontend/informate-frontend/src/components/Dashboard.js
git commit -m "feat: add article timestamps to dashboard cards"
```

---

## Task 10: Refresh ArticleView with better typography

**Files:**
- Modify: `Frontend/informate-frontend/src/components/ArticleView.js`

**Step 1: Add created_at display and improve reading experience**

Add timestamp below the title (after line 107):

```jsx
{/* Metadata */}
{article.createdAt && (
  <p className="text-sm text-slate-500 dark:text-slate-400 mb-6">
    Added {new Date(article.createdAt).toLocaleDateString('en-GB', {
      day: 'numeric', month: 'long', year: 'numeric'
    })}
  </p>
)}
```

**Step 2: Commit**

```bash
git add Frontend/informate-frontend/src/components/ArticleView.js
git commit -m "feat: add timestamp to article view"
```

---

## Task 11: Update SplashScreen copy for self-hosted AI

**Files:**
- Modify: `Frontend/informate-frontend/src/components/SplashScreen.js:94-96,117`

**Step 1: Update marketing copy**

The splash screen currently says "AI-Powered" generically. Update to reflect self-hosted AI:

- Line 92: Change `AI-Powered` to `AI-Powered` (keep same, it's generic enough)
- Line 117: Change `"Process articles in seconds with our advanced AI technology"` to `"Process articles locally with self-hosted AI — no API keys needed"`

**Step 2: Commit**

```bash
git add Frontend/informate-frontend/src/components/SplashScreen.js
git commit -m "feat: update splash screen copy for self-hosted AI"
```

---

## Task 12: Create GitHub Actions CI for container images

**Files:**
- Create: `.github/workflows/build-backend.yml`
- Create: `.github/workflows/build-frontend.yml`

**Step 1: Create backend build workflow**

Create `.github/workflows/build-backend.yml`:

```yaml
name: Build Backend

on:
  push:
    branches: [main]
    paths:
      - 'Backend/**'
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository_owner }}/informate-backend

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - uses: docker/metadata-action@v5
        id: meta
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha
            type=raw,value=latest,enable={{is_default_branch}}

      - uses: docker/build-push-action@v5
        with:
          context: ./Backend/informate
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

**Step 2: Create frontend build workflow**

Create `.github/workflows/build-frontend.yml`:

```yaml
name: Build Frontend

on:
  push:
    branches: [main]
    paths:
      - 'Frontend/**'
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository_owner }}/informate-frontend

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - uses: docker/metadata-action@v5
        id: meta
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha
            type=raw,value=latest,enable={{is_default_branch}}

      - uses: docker/build-push-action@v5
        with:
          context: ./Frontend/informate-frontend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

**Step 3: Commit**

```bash
git add .github/workflows/build-backend.yml .github/workflows/build-frontend.yml
git commit -m "ci: add GitHub Actions workflows for GHCR image builds"
```

---

## Task 13: Create Kubernetes manifests in homeops repo

**Files:**
- Create: `~/homeops/kubernetes/apps/default/informate/ks.yaml`
- Create: `~/homeops/kubernetes/apps/default/informate/app/kustomization.yaml`
- Create: `~/homeops/kubernetes/apps/default/informate/app/ocirepository.yaml`
- Create: `~/homeops/kubernetes/apps/default/informate/app/helmrelease.yaml`
- Create: `~/homeops/kubernetes/apps/default/informate/app/httproute.yaml`

**Step 1: Create Flux Kustomization**

Create `~/homeops/kubernetes/apps/default/informate/ks.yaml`:

```yaml
---
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: informate
spec:
  interval: 1h
  path: ./kubernetes/apps/default/informate/app
  postBuild:
    substituteFrom:
      - name: cluster-secrets
        kind: Secret
  prune: true
  sourceRef:
    kind: GitRepository
    name: flux-system
    namespace: flux-system
  targetNamespace: default
  wait: false
```

**Step 2: Create Kustomize resources list**

Create `~/homeops/kubernetes/apps/default/informate/app/kustomization.yaml`:

```yaml
---
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ./helmrelease.yaml
  - ./ocirepository.yaml
  - ./httproute.yaml
```

**Step 3: Create OCI Repository**

Create `~/homeops/kubernetes/apps/default/informate/app/ocirepository.yaml`:

```yaml
---
apiVersion: source.toolkit.fluxcd.io/v1
kind: OCIRepository
metadata:
  name: informate
spec:
  interval: 15m
  layerSelector:
    mediaType: application/vnd.cncf.helm.chart.content.v1.tar+gzip
    operation: copy
  ref:
    tag: 4.6.2
  url: oci://ghcr.io/bjw-s-labs/helm/app-template
```

**Step 4: Create HelmRelease**

Create `~/homeops/kubernetes/apps/default/informate/app/helmrelease.yaml`:

```yaml
---
apiVersion: helm.toolkit.fluxcd.io/v2
kind: HelmRelease
metadata:
  name: informate
spec:
  chartRef:
    kind: OCIRepository
    name: informate
  interval: 1h
  values:
    controllers:
      backend:
        strategy: RollingUpdate
        containers:
          app:
            image:
              repository: ghcr.io/andrei-iacobb/informate-backend
              tag: latest
            env:
              OLLAMA_URL: http://ollama.ai.svc.cluster.local:11434
              OLLAMA_MODEL: llama3.1:70b
              DATABASE_URL: jdbc:postgresql://postgres.databases.svc.cluster.local:5432/informate
              POSTGRES_USER:
                valueFrom:
                  secretKeyRef:
                    name: informate-secret
                    key: POSTGRES_USER
              POSTGRES_PASSWORD:
                valueFrom:
                  secretKeyRef:
                    name: informate-secret
                    key: POSTGRES_PASSWORD
            probes:
              liveness: &probes
                enabled: true
                custom: true
                spec:
                  httpGet:
                    path: /api/auth/validate
                    port: 8080
                  initialDelaySeconds: 60
                  periodSeconds: 10
                  timeoutSeconds: 5
                  failureThreshold: 3
              readiness: *probes
            securityContext:
              allowPrivilegeEscalation: false
              readOnlyRootFilesystem: false
              capabilities:
                drop: ["ALL"]
            resources:
              requests:
                cpu: 500m
                memory: 512Mi
              limits:
                cpu: 2000m
                memory: 2Gi
      frontend:
        strategy: RollingUpdate
        containers:
          app:
            image:
              repository: ghcr.io/andrei-iacobb/informate-frontend
              tag: latest
            probes:
              liveness: &frontend-probes
                enabled: true
                custom: true
                spec:
                  httpGet:
                    path: /
                    port: 80
                  initialDelaySeconds: 10
                  periodSeconds: 10
                  timeoutSeconds: 5
                  failureThreshold: 3
              readiness: *frontend-probes
            securityContext:
              allowPrivilegeEscalation: false
              readOnlyRootFilesystem: false
              capabilities:
                drop: ["ALL"]
            resources:
              requests:
                cpu: 50m
                memory: 64Mi
              limits:
                cpu: 500m
                memory: 256Mi
    defaultPodOptions:
      securityContext:
        runAsNonRoot: false
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
    service:
      backend:
        controller: backend
        ports:
          http:
            port: 8080
      frontend:
        controller: frontend
        ports:
          http:
            port: 80
    persistence:
      images:
        type: persistentVolumeClaim
        storageClass: openebs-hostpath
        size: 10Gi
        accessMode: ReadWriteOnce
        advancedMounts:
          backend:
            app:
              - path: /app/SiteImages
```

**Step 5: Create HTTPRoute**

Create `~/homeops/kubernetes/apps/default/informate/app/httproute.yaml`:

```yaml
---
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: informate
spec:
  parentRefs:
    - name: envoy-external
      namespace: network
      sectionName: https
  hostnames:
    - informate.iacob.co.uk
  rules:
    - backendRefs:
        - name: informate-frontend
          port: 80
```

**Step 6: Commit in homeops repo**

```bash
cd ~/homeops
git add kubernetes/apps/default/informate/
git commit -m "feat: add Informate app to Kubernetes cluster"
```

---

## Task 14: Update nginx.conf for Kubernetes service names

**Files:**
- Modify: `Frontend/informate-frontend/nginx.conf:31,44`

**Step 1: Update backend proxy target**

In K8s, the backend and frontend are separate services. The nginx proxy_pass needs to reference the K8s service name. Change line 31:

```nginx
proxy_pass http://informate-backend:8080/api/;
```

And line 44:

```nginx
proxy_pass http://informate-backend:8080/SiteImages/;
```

Wait — in the bjw-s app-template, the service name for the backend controller is `informate-backend`. The nginx needs to resolve this. But since backend and frontend are in the same HelmRelease, the backend service will be named `informate-backend` in the same namespace.

**Step 2: Commit**

```bash
git add Frontend/informate-frontend/nginx.conf
git commit -m "fix: update nginx proxy to use K8s service name"
```

---

## Task 15: Review uncommitted index.html change

**Files:**
- Review: `Frontend/informate-frontend/public/index.html`

**Step 1: Check what's changed**

Run: `git diff Frontend/informate-frontend/public/index.html`

Review the diff. If it's a legitimate change (e.g., title update, meta tags), commit it. If accidental, revert it.

**Step 2: Commit or revert**

```bash
# If legitimate:
git add Frontend/informate-frontend/public/index.html
git commit -m "fix: update index.html metadata"

# If accidental:
git checkout -- Frontend/informate-frontend/public/index.html
```

---

## Task 16: Final verification — build both Docker images locally

**Step 1: Build backend image**

Run: `cd /Users/andreiiacob/informate/Informate && docker build -t informate-backend:test -f Backend/informate/Dockerfile Backend/informate`
Expected: Successfully built

**Step 2: Build frontend image**

Run: `docker build -t informate-frontend:test -f Frontend/informate-frontend/Dockerfile Frontend/informate-frontend`
Expected: Successfully built

**Step 3: Verify backend starts (will fail on DB connect, that's OK)**

Run: `docker run --rm -e OLLAMA_URL=http://localhost:11434 -e OLLAMA_MODEL=llama3.1:8b -e DATABASE_URL=jdbc:postgresql://localhost:5432/informate -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test informate-backend:test`
Expected: Starts, prints "Starting Informate REST API Server", then may fail connecting to PostgreSQL (expected — no DB running). The important thing is the JAR starts and reads env vars correctly.

**Step 4: Commit any remaining changes**

```bash
git add -A
git commit -m "chore: final cleanup for Informate revamp"
```
