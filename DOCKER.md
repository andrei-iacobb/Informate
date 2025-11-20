# Docker Setup for Informate

This guide explains how to run Informate using Docker and Docker Compose.

## Prerequisites

- Docker (version 20.10 or higher)
- Docker Compose (version 2.0 or higher)
- External `proxy` network (see setup below)

## Quick Start

### 1. Create the proxy network

The application uses an external Docker network for reverse proxy integration:

```bash
docker network create proxy
```

### 2. Set up environment variables

Create a `.env` file in the root directory:

```bash
cp .env.example .env
```

Edit `.env` and add your OpenAI API key:

```
OPENAI_API_KEY=sk-your-actual-api-key-here
```

### 3. Build and run with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes (deletes all data)
docker-compose down -v
```

### 4. Access the application

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080

**Note**: The frontend is also accessible via your reverse proxy on the `proxy` network.

## Architecture

The Docker setup consists of two services:

### Backend Service
- **Image**: Java 17 with Maven
- **Port**: 8080
- **Volumes**:
  - `backend-data`: Persists SQLite databases (data.db, articles.db)
  - `backend-images`: Persists scraped article images
- **Environment**: Requires `OPENAI_API_KEY`

### Frontend Service
- **Image**: Nginx Alpine
- **Port**: 3001:80 (host:container)
- **Networks**:
  - `informate-network` (internal, for backend communication)
  - `proxy` (external, for reverse proxy integration)
- **Features**:
  - Serves React build optimized for production
  - Proxies API requests to backend
  - Handles React Router with URL rewriting
  - Gzip compression enabled
  - Security headers configured

## Individual Service Commands

### Build individual services

```bash
# Backend only
docker-compose build backend

# Frontend only
docker-compose build frontend
```

### Run individual services

```bash
# Backend only
docker-compose up -d backend

# Frontend only
docker-compose up -d frontend
```

## Development

### Rebuild after code changes

```bash
# Rebuild and restart
docker-compose up -d --build

# Rebuild specific service
docker-compose up -d --build backend
```

### View logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Access container shell

```bash
# Backend
docker-compose exec backend sh

# Frontend
docker-compose exec frontend sh
```

## Data Persistence

Data is persisted in Docker volumes:

- **backend-data**: Contains SQLite databases
- **backend-images**: Contains scraped article images

To backup data:

```bash
# Backup databases
docker run --rm -v informate_backend-data:/data -v $(pwd):/backup alpine tar czf /backup/backend-data-backup.tar.gz -C /data .

# Backup images
docker run --rm -v informate_backend-images:/data -v $(pwd):/backup alpine tar czf /backup/backend-images-backup.tar.gz -C /data .
```

To restore data:

```bash
# Restore databases
docker run --rm -v informate_backend-data:/data -v $(pwd):/backup alpine tar xzf /backup/backend-data-backup.tar.gz -C /data

# Restore images
docker run --rm -v informate_backend-images:/data -v $(pwd):/backup alpine tar xzf /backup/backend-images-backup.tar.gz -C /data
```

## Troubleshooting

### Port already in use

If port 80 or 8080 is already in use, edit `docker-compose.yml`:

```yaml
services:
  backend:
    ports:
      - "8081:8080"  # Change 8080 to 8081

  frontend:
    ports:
      - "8000:80"    # Change 80 to 8000
```

### Check service health

```bash
docker-compose ps
```

### Reset everything

```bash
# Stop and remove all containers, networks, and volumes
docker-compose down -v

# Remove images
docker-compose down --rmi all

# Start fresh
docker-compose up -d --build
```

### Backend not connecting to OpenAI

1. Check your `.env` file has the correct API key
2. Restart services: `docker-compose restart backend`
3. Check logs: `docker-compose logs backend`

### Frontend not connecting to backend

1. Check backend is running: `docker-compose ps`
2. Check backend health: `curl http://localhost:8080/api/articles`
3. Check nginx logs: `docker-compose logs frontend`

### Proxy network not found

If you get an error about the `proxy` network not existing:

```bash
# Create the network
docker network create proxy

# Restart services
docker-compose up -d
```

## Reverse Proxy Integration

The frontend container is connected to an external `proxy` network, making it easy to integrate with reverse proxies like Traefik, Nginx Proxy Manager, or Caddy.

### Example with Traefik

Add labels to the frontend service in `docker-compose.yml`:

```yaml
frontend:
  labels:
    - "traefik.enable=true"
    - "traefik.http.routers.informate.rule=Host(`informate.yourdomain.com`)"
    - "traefik.http.routers.informate.entrypoints=websecure"
    - "traefik.http.routers.informate.tls.certresolver=letsencrypt"
    - "traefik.http.services.informate.loadbalancer.server.port=80"
    - "traefik.docker.network=proxy"
```

### Example with Nginx Proxy Manager

1. Create a new proxy host in NPM
2. Set the destination to `informate-frontend:80`
3. Enable SSL and configure your domain

### Direct Access

Even with a reverse proxy configured, you can still access the frontend directly:
- Direct access: http://localhost:3001
- Via reverse proxy: https://your-domain.com

## Production Deployment

For production deployment:

1. **Use environment-specific configuration**:
   - Create `.env.production` with production API keys
   - Update `docker-compose.yml` to use production settings

2. **Enable HTTPS**:
   - Use a reverse proxy like Traefik or nginx-proxy
   - Configure SSL certificates (Let's Encrypt recommended)

3. **Resource limits**:
   - Add resource constraints to prevent container overconsumption:

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

4. **Security**:
   - Never commit `.env` file to version control
   - Use Docker secrets for sensitive data
   - Keep images updated regularly
   - Run security scans: `docker scan informate-backend`

## Performance Optimization

### Multi-stage builds

Both Dockerfiles use multi-stage builds to minimize image size:
- Backend: ~250MB (vs ~800MB without multi-stage)
- Frontend: ~25MB (vs ~400MB without multi-stage)

### Caching

Docker layer caching is optimized:
- Dependencies are cached separately from source code
- Changes to source code don't invalidate dependency cache

### Network

All services communicate via a dedicated Docker network (`informate-network`) for better isolation and performance.

## Monitoring

### Resource usage

```bash
# Check resource usage
docker stats

# Check specific service
docker stats informate-backend informate-frontend
```

### Disk usage

```bash
# Check Docker disk usage
docker system df

# Clean up unused resources
docker system prune -a
```

## Support

For issues specific to Docker setup, check:
- Docker logs: `docker-compose logs`
- Container status: `docker-compose ps`
- Network connectivity: `docker network inspect informate_informate-network`

For application issues, refer to the main README.md.
