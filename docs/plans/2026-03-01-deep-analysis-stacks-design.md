# Deep Analysis Stacks - Design Document

**Date**: 2026-03-01
**Status**: Approved

## Goal

Add a "Stacks" feature to Informate that groups related news articles, searches for additional sources via SearXNG, and uses RAG (Qdrant + Ollama) to generate structured intelligence-briefing-style analysis reports with market impacts and future scenario predictions.

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| News search | SearXNG (self-hosted) | Fully self-hosted, no API keys, searches multiple news sources |
| Analysis pipeline | RAG with Qdrant | Scales to many articles, precise retrieval, Qdrant already in cluster |
| Stack creation | Hybrid: AI suggests, user confirms | Best UX — automated but user keeps control |
| Search depth | Configurable per Stack (default 10, max 30) | Different topics need different depth |
| Analysis format | Structured report (intelligence briefing) | Sections: Executive Summary, Key Facts, Perspectives, Market Impact, Future Scenarios |
| Focus system | Optional focus field per Stack | User can steer analysis toward specific aspects (stocks, AI, geopolitics) or leave blank for comprehensive |
| SearXNG deployment | User deploys separately | Informate just needs SEARXNG_URL env var |

## 1. Stack Data Model

```sql
CREATE TABLE stacks (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    keywords TEXT,           -- comma-separated
    focus TEXT,              -- optional focus area (e.g. "stock market impact")
    search_depth INTEGER DEFAULT 10,
    status TEXT DEFAULT 'pending',  -- pending | searching | analyzing | ready | error
    analysis TEXT,           -- JSON blob: structured report
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE stack_articles (
    id SERIAL PRIMARY KEY,
    stack_id INTEGER REFERENCES stacks(id) ON DELETE CASCADE,
    article_id INTEGER REFERENCES articles(id),
    source TEXT DEFAULT 'manual',  -- manual | searxng
    added_at TIMESTAMP DEFAULT NOW()
);
```

A Stack contains:
- **name**: Topic label (e.g., "Israel-Iran Nuclear Tensions")
- **keywords**: Extracted from articles, used for SearXNG search
- **focus**: Optional — steers RAG retrieval and analysis prompt
- **search_depth**: How many articles AI should find (default 10, max 30)
- **status**: Pipeline progress indicator
- **analysis**: JSON blob containing the structured report sections

## 2. RAG Pipeline Architecture

### Flow

```
User adds article
       |
       v
Ollama extracts keywords → suggests Stack (existing or new)
       |
       v
User confirms Stack assignment
       |
       v
[Async Pipeline Starts]
       |
       ├── 1. Search: keywords → SearXNG → list of URLs
       |
       ├── 2. Scrape: URLs → JSoup → article texts
       |
       ├── 3. Embed: article chunks → Ollama (nomic-embed-text) → Qdrant
       |
       ├── 4. Retrieve: Stack keywords + focus → Qdrant → top ~50 chunks
       |
       └── 5. Synthesize: chunks + prompt → Ollama (llama3.1:70b) → structured report
              |
              v
         Analysis stored in stacks.analysis (JSON)
```

### Components

**Embedding** (new):
- Model: `nomic-embed-text` in Ollama
- Endpoint: `POST http://ollama:11434/api/embeddings`
- Chunk size: ~500 tokens per chunk
- Storage: Qdrant collection `informate_stacks`
- Metadata per vector: stack_id, article_title, source, chunk_index

**Search** (new):
- SearXNG: `GET {SEARXNG_URL}/search?q={keywords}&categories=news&format=json`
- Parse results: extract URLs, titles, snippets
- Scrape each URL with existing JSoup scraper

**Retrieval** (new):
- Query Qdrant with combined vector: keywords + focus area
- Return top 50 most relevant chunks
- Filter by stack_id

**Synthesis** (new):
- Feed retrieved chunks to Ollama llama3.1:70b
- Structured prompt (see Section 5)
- Parse response into JSON sections

### New Backend Services

| Service | Responsibility |
|---------|---------------|
| `StackService` | Stack CRUD, pipeline orchestration |
| `EmbeddingService` | Ollama embeddings, Qdrant storage/retrieval |
| `SearchService` | SearXNG queries, result parsing |
| `AnalysisService` | RAG retrieval + Ollama synthesis |

### New API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/stacks` | List all Stacks |
| POST | `/api/stacks` | Create new Stack |
| GET | `/api/stacks/:id` | Get Stack detail + analysis |
| PUT | `/api/stacks/:id` | Update Stack (focus, search_depth) |
| DELETE | `/api/stacks/:id` | Delete Stack |
| POST | `/api/stacks/:id/articles` | Add article to Stack |
| DELETE | `/api/stacks/:id/articles/:articleId` | Remove article from Stack |
| POST | `/api/stacks/suggest` | Suggest Stack for an article |
| GET | `/api/stacks/:id/status` | Get pipeline status |

## 3. SearXNG Requirements

**For user to deploy separately.** Informate needs:

- **Endpoint**: `SEARXNG_URL` env var (e.g., `http://searxng.default.svc.cluster.local:8080`)
- **API format**: JSON responses (`format=json` parameter)
- **Categories**: `news` category enabled
- **Engines to enable**: Google News, Bing News, DuckDuckGo News (at minimum)
- **Usage**: `GET {SEARXNG_URL}/search?q={query}&categories=news&format=json`
- **Response format used**:
  ```json
  {
    "results": [
      {
        "url": "https://...",
        "title": "Article title",
        "content": "Snippet...",
        "engine": "google news",
        "publishedDate": "2026-03-01T..."
      }
    ]
  }
  ```
- **Rate limiting**: Backend will add 1-2s delay between searches to be respectful
- **Access**: Internal only (K8s service DNS)

## 4. UI Design

### Dashboard Update
- Article cards show small Stack badge (Stack name)
- New "Stacks" navigation item in navbar

### Stack List (`/stacks`)
- Grid of Stack cards showing: name, article count, focus, status badge, last updated
- "Create Stack" button

### Stack Detail (`/stack/:id`)
- **Header**: Stack name, focus input field, search depth slider, status badge
- **Analysis Panel** (main area):
  - Executive Summary (2-3 paragraphs)
  - Key Facts (bullet points, noting source agreement/disagreement)
  - Multiple Perspectives (3+ viewpoints)
  - Market & Economic Impact (tickers, sectors, currencies)
  - Future Scenarios (probability bars: Low/Medium/High, indicators, timeline)
  - Sources (linked list)
- **Articles Sidebar** (right):
  - List of articles tagged "manual" or "AI-found"
  - Remove button per article
  - "Add Article" button
- **Responsive**: Sidebar collapses to bottom on mobile

### Stack Suggestion Modal
- Triggered when adding any article
- Shows: "This article matches Stack 'X'. Add to it?"
- Options: Confirm, Choose different Stack, Create new Stack

## 5. Analysis Prompt

```
You are an expert intelligence analyst. Using the following source material
from {article_count} news articles, produce a structured briefing report.

{focus_instruction}

## Source Material
{rag_retrieved_chunks}

## Report Format (respond in valid JSON)

{
  "executive_summary": "2-3 paragraphs covering the core situation...",
  "key_facts": ["fact 1", "fact 2", ...],
  "perspectives": [
    {"viewpoint": "...", "description": "..."},
    ...
  ],
  "market_impact": {
    "summary": "...",
    "sectors": ["...", "..."],
    "tickers": ["...", "..."],
    "outlook": "..."
  },
  "future_scenarios": [
    {
      "scenario": "...",
      "probability": "Low|Medium|High",
      "reasoning": "...",
      "indicators": ["what to watch for"],
      "timeline": "..."
    }
  ],
  "sources": [
    {"title": "...", "url": "..."}
  ]
}
```

**Focus instruction** when focus is set:
> "Pay particular attention to: {focus}. Prioritize analysis of this aspect while still covering other dimensions."

When blank:
> "Provide a comprehensive analysis covering all aspects including geopolitical, economic, social, and technological dimensions."

## 6. New Dependencies

### Backend (pom.xml)
- Qdrant Java client: `io.qdrant:client:1.9.1`
- HTTP client for SearXNG: (use existing `java.net.HttpURLConnection`)

### Ollama Models
- `nomic-embed-text` (embedding model, ~270MB) — needs `ollama pull nomic-embed-text`

### Infrastructure
- SearXNG: user deploys separately
- Qdrant: already running at `qdrant.iacob.uk:6333`

## Architecture Diagram

```
                    User adds article
                           |
                    ┌──────┴──────┐
                    │  Informate   │
                    │  Backend     │
                    └──┬───┬───┬──┘
                       │   │   │
          ┌────────────┘   │   └────────────┐
          │                │                │
    ┌─────┴─────┐   ┌─────┴─────┐   ┌─────┴─────┐
    │  SearXNG   │   │  Qdrant    │   │  Ollama    │
    │  (search)  │   │  (vectors) │   │  (LLM +    │
    │  :8080     │   │  :6333     │   │  embed)    │
    └───────────┘   └───────────┘   │  :11434    │
                                     └───────────┘

Pipeline: Search → Scrape → Embed → Retrieve → Synthesize
```
