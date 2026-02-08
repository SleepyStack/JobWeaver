# 🚀 JobWeaver

A distributed job processing platform built using a microservices architecture.

## 📋 Table of Contents
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Development Setup](#-development-setup)
- [Future Roadmap](#-future-roadmap)

## 🛠️ Tech Stack

### Backend
- **Java 21** - Modern Java features and performance
- **Spring Boot** - Application framework
- **PostgreSQL** - Primary database
- **Apache Kafka** - Message broker for distributed processing
- **Flyway** - Database migrations

### Frontend
- **React** - UI framework with Vite build tool

### Infrastructure
- **Docker & Docker Compose** - Containerization and orchestration

## 📁 Project Structure

```
├── jobweaver-common/     # Shared DTOs and messaging models
├── jobweaver-api/        # REST API + database migrations
├── jobweaver-worker/     # Kafka consumer and job executor
├── jobweaver-scheduler/  # Retry logic and monitoring service
└── jobweaver-dashboard/  # Frontend UI (React)
```

## 📋 Prerequisites

Make sure you have the following installed:

- ☕ **JDK 21**
- 📦 **Maven**
- 🐋 **Docker Desktop**
- 🟢 **Node.js** *(only required for dashboard development)*

## 🚀 Quick Start

### 1. Start Infrastructure Services

From the project root directory:

```bash
docker compose up -d
```

This will start:
- **PostgreSQL** → `localhost:5432`
- **Apache Kafka** → `localhost:9092`

Verify all containers are healthy:
```bash
docker ps
```

### 2. Run Backend Services

> **⚠️ Important:** Add this JVM option to each service run configuration:
> `-Duser.timezone=Asia/Kolkata`

Start services in the following order:

1. **jobweaver-api** *(runs Flyway migrations automatically)*
2. **jobweaver-worker**
3. **jobweaver-scheduler**

### 3. Start the Dashboard

```bash
cd jobweaver-dashboard
npm install
npm run dev
```

The dashboard will be available via the Vite development server.

## 💻 Development Setup

Currently, backend services are run directly from the IDE for easier debugging and development. Each service can be started independently from your preferred Java IDE.

## 🗺️ Future Roadmap

- **Containerization**: Backend services will be containerized and integrated into Docker Compose
- **Enhanced Monitoring**: Additional observability and metrics
- **Advanced Scheduling**: More sophisticated job scheduling capabilities

---

> This repository is designed for seamless team onboarding and reproducible local development environments. Happy coding! 🎉
