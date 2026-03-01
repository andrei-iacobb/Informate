# Informate Revamp Design

**Date**: 2026-03-01
**Status**: Approved

## Goal

Revamp Informate to use self-hosted Ollama instead of OpenAI API keys, migrate to PostgreSQL, refresh the UI, and deploy to the existing Kubernetes cluster following homeops patterns.

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Deployment target | Kubernetes (same cluster as Ollama) | Internal DNS access to Ollama, consistent with existing infra |
| LLM model | llama3.1:70b via Ollama | High quality summaries, server has 252GB RAM |
| Approach | Incremental refactor (keep Java+React) | Least risk, builds on working code |
| K8s pattern | bjw-s app-template + Flux CD + SOPS | Match homeops repo conventions |
| Access | External via *.iacob.co.uk (Cloudflare Tunnel) | Read news on the go |
| Database | Migrate to cluster PostgreSQL | Better for K8s, concurrent access, backups |
| Auth sessions | Keep in-memory (accept re-login on restart) | Simple, acceptable for personal app |
| Container registry | GitHub Container Registry (ghcr.io) | Consistent with homeops apps |

## 1. Ollama Integration

**Current**: `AI.java` calls `https://api.openai.com/v1/chat/completions` with `gpt-3.5-turbo` and bearer token auth.

**Change**: Point at Ollama's OpenAI-compatible endpoint:
- URL: `http://ollama.ai.svc.cluster.local:11434/v1/chat/completions`
- Model: `llama3.1:70b`
- Auth: None (Ollama doesn't require API keys)
- Timeout: 120s (up from 30s — local inference is slower)

**Configuration**: Environment variables `OLLAMA_URL` and `OLLAMA_MODEL` so the model/endpoint can be changed without rebuilding. Remove `OPENAI_API_KEY` dependency.

**Prompt**: Keep existing summarization prompt (works model-agnostically).

## 2. PostgreSQL Migration

**Current**: Two SQLite databases (`data.db` for users, `articles.db` for articles).

**Change**: Single PostgreSQL database with two tables.

### Schema

```sql
CREATE TABLE users (
  username TEXT PRIMARY KEY,
  password TEXT NOT NULL
);

CREATE TABLE articles (
  id SERIAL PRIMARY KEY,
  title TEXT UNIQUE NOT NULL,
  summary TEXT,
  keywords TEXT,
  raw_text TEXT,
  images TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
```

**Connection**: `DATABASE_URL` env var (e.g., `jdbc:postgresql://postgres.databases.svc.cluster.local:5432/informate`).

**Credentials**: `POSTGRES_USER` and `POSTGRES_PASSWORD` via SOPS-encrypted secret.

**Dependencies**: Add `org.postgresql:postgresql` JDBC driver to `pom.xml`.

**Code changes**: Update `articles.java` and `auth.java` to use PostgreSQL connection strings and syntax.

## 3. UI Refresh

Keep React 19 + Tailwind CSS. No framework change.

### Changes

1. **Dashboard**: Redesigned article cards with visual hierarchy — prominent image, title, summary excerpt, keyword tags, timestamp. Grid/masonry layout.
2. **Article View**: Cleaner reading experience — larger typography, proper content width, better image display, keyword chips.
3. **Add Article**: Real-time processing stages (scraping -> summarizing -> done) instead of generic spinner.
4. **Navigation**: Proper navbar with app branding, dark mode toggle, user menu.
5. **Splash/Auth screens**: Modern, minimal design.
6. **Responsive**: Mobile-friendly (externally accessible, reading on the go).

## 4. Kubernetes Deployment

### File Structure (in homeops repo)

```
kubernetes/apps/default/informate/
├── ks.yaml                    # Flux Kustomization
└── app/
    ├── helmrelease.yaml       # bjw-s app-template HelmRelease
    ├── httproute.yaml         # informate.iacob.co.uk -> envoy-external
    ├── secret.sops.yaml       # DB credentials (SOPS encrypted)
    ├── ocirepository.yaml     # bjw-s chart source
    └── kustomization.yaml     # Kustomize resources list
```

### HelmRelease Configuration

- **Backend container**: Java app (GHCR image), port 8080
- **Frontend container**: Nginx serving React build, proxying /api/ to backend
- **Environment**: `OLLAMA_URL`, `OLLAMA_MODEL`, `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- **Storage**: PVC for `/app/SiteImages` (scraped article images)
- **Health check**: GET `/api/articles` every 30s
- **Resources**: 500m-2000m CPU, 512Mi-2Gi RAM
- **HTTPRoute**: `informate.iacob.co.uk` via envoy-external gateway

### Container Images

- Built via GitHub Actions CI on push to main
- Published to `ghcr.io/<user>/informate-backend` and `ghcr.io/<user>/informate-frontend`

## 5. Bug Fixes

| Issue | Fix |
|-------|-----|
| Hardcoded OpenAI endpoint | Configurable via env vars |
| SQLite DB files in repo | .gitignore, use PostgreSQL |
| CORS for localhost only | Update for production domain |
| Missing .env crashes app | Graceful fallback to system env vars |
| Image path inconsistency | Consistent `/app/SiteImages/` with PVC mount |
| Uncommitted index.html change | Review and commit |

## Architecture Diagram

```
                    Internet
                       |
              Cloudflare Tunnel
                       |
               envoy-external
          (informate.iacob.co.uk)
                       |
            ┌──────────┴──────────┐
            │  Informate Frontend  │
            │  (Nginx + React)     │
            │  Port 80             │
            └──────────┬──────────┘
                /api/  │  proxy
            ┌──────────┴──────────┐
            │  Informate Backend   │
            │  (Java/Spark)        │
            │  Port 8080           │
            └──┬──────────────┬───┘
               │              │
    ┌──────────┴───┐   ┌─────┴──────────┐
    │  Ollama       │   │  PostgreSQL     │
    │  (ai ns)      │   │  (databases ns) │
    │  :11434       │   │  :5432          │
    │  llama3.1:70b │   │  informate db   │
    └──────────────┘   └────────────────┘
```
