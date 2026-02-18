# CatchIt - Public Transportation Management App

A full-stack mobile application for managing public transportation systems, built with TypeScript, React, Node.js, and PostgreSQL, as a project for the Applications Engineering profile.

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Development](#development)
- [Environment Variables](#environment-variables)
- [Available Scripts](#available-scripts)


##  Overview

CatchIt is a monorepo application designed to manage public transportation routes, vehicles, stops, and user interactions. The application provides a mobile-friendly interface for passengers and administrative tools for transportation management.

##  Tech Stack

### Frontend
- **React** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework
- **Axios** - HTTP client
- **React Router** - Navigation

### Backend
- **Node.js** - Runtime environment
- **Express** - Web framework
- **TypeScript** - Type safety
- **Sequelize** - ORM for PostgreSQL
- **PostgreSQL** - Database
- **dotenv** - Environment variable management

### Monorepo
- **npm workspaces** - Monorepo management
- **Concurrently** - Run multiple commands simultaneously

## ✅ Prerequisites

Before you begin, ensure you have the following installed on your system:

### Required Software

1. **Node.js** (v18 or higher)
   - Download: https://nodejs.org/
   - Verify installation: `node -v`

2. **npm** (v9 or higher, comes with Node.js)
   - Verify installation: `npm -v`

3. **PostgreSQL** (v14 or higher)
   - Download: https://www.postgresql.org/download/
   - Verify installation: `psql --version`

4. **Git**
   - Download: https://git-scm.com/
   - Verify installation: `git --version`


## Project Structure

```
catchit/
├── packages/
│   ├── frontend/                 # React TypeScript application
│   │   ├── public/              # Static assets
│   │   ├── src/
│   │   │   ├── assets/          # Images, fonts, etc.
│   │   │   ├── components/      # React components
│   │   │   ├── pages/           # Page components
│   │   │   ├── services/        # API services
│   │   │   ├── utils/           # Utility functions
│   │   │   ├── App.tsx          # Main App component
│   │   │   ├── main.tsx         # Entry point
│   │   │   └── index.css        # Global styles with Tailwind
│   │   ├── index.html
│   │   ├── package.json
│   │   ├── tailwind.config.js   # Tailwind configuration
│   │   ├── postcss.config.js    # PostCSS configuration
│   │   ├── tsconfig.json        # TypeScript configuration
│   │   └── vite.config.ts       # Vite configuration
│   │
│   └── backend/                 # Node.js Express API
│       ├── database/
│       │   └── schema.sql       # Database schema definition
│       ├── src/
│       │   ├── config/
│       │   │   └── database.ts  # Database connection config
│       │   ├── models/          # Sequelize models
│       │   ├── routes/          # API routes
│       │   ├── controllers/     # Route controllers
│       │   ├── middleware/      # Custom middleware
│       │   ├── scripts/         # Utility scripts
│       │   └── index.ts         # Entry point
│       ├── .env                 # Environment variables (not in git)
│       ├── .env.example         # Environment variables template
│       ├── package.json
│       └── tsconfig.json        # TypeScript configuration
│
├── .gitignore                   # Git ignore rules
├── package.json                 # Root package.json (workspace config)
├── package-lock.json            # Lock file
└── README.md                    # This file
```

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd projeto-ea/catchit
```

### 2. Install dependencies

```bash
# Install all dependencies for root and workspaces
npm install
```

This will install dependencies for:
- Root workspace
- Frontend package
- Backend package

### 3. Set up PostgreSQL

#### Start PostgreSQL service

**On Windows:**
```bash
# Open Services (Win + R, type "services.msc")
# Find "postgresql-x64-[version]" and start it

# Or via command line (as Administrator):
net start postgresql-x64-14
```

**On macOS:**
```bash
brew services start postgresql
```

**On Linux:**
```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql  # Start on boot
```

#### Create the database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE catchit_db;

# Verify
\l

# Exit
\q
```

### 4. Configure environment variables

```bash
# Navigate to backend
cd packages/backend

# Copy the example env file
cp .env.example .env

# Edit .env with your actual credentials
```

Update `packages/backend/.env`:

```env
PORT=3000
DB_HOST=localhost
DB_PORT=5432
DB_NAME=catchit_db
DB_USER=postgres
DB_PASSWORD=your_postgres_password
```

**Important:** Replace `your_postgres_password` with your actual PostgreSQL password!

### 5. Run database migrations

```bash
# Load the schema into the database
psql -U postgres -d catchit_db -f packages/backend/database/schema.sql
```

Or from within psql:
```sql
\c catchit_db
\i 'path/to/catchit/packages/backend/database/schema.sql'
```

### 6. Start development servers

```bash
# From root directory
npm run dev
```

This will start:
- **Frontend:** http://localhost:5173
- **Backend:** http://localhost:3000

## Development

### Running individual services

```bash
# Run only frontend
npm run dev:frontend

# Run only backend
npm run dev:backend
```

### Building for production

```bash
# Build all packages
npm run build

# Build individual packages
npm run build:frontend
npm run build:backend
```

### Installing new dependencies

```bash
# Install for frontend
npm install <package-name> --workspace=frontend

# Install for backend
npm install <package-name> --workspace=backend

# Install dev dependency for frontend
npm install -D <package-name> --workspace=frontend
```


## 🗄 Database Setup

### Schema Overview

The database includes the following main tables:

- **users** - Application users (passengers, drivers, admins)
- **routes** - Transportation routes
- **vehicles** - Fleet vehicles
- **stops** - Bus/train stops with coordinates




##  Environment Variables

### Backend (.env)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `PORT` | Backend server port | 3000 | No |
| `DB_HOST` | PostgreSQL host | localhost | No |
| `DB_PORT` | PostgreSQL port | 5432 | No |
| `DB_NAME` | Database name | catchit_db | Yes |
| `DB_USER` | Database user | postgres | Yes |
| `DB_PASSWORD` | Database password | - | Yes |

### Frontend

Environment variables for frontend should be prefixed with `VITE_` and placed in `packages/frontend/.env`:

```env
VITE_API_URL=http://localhost:3000
```

##  Available Scripts

### Root Level

| Script | Description |
|--------|-------------|
| `npm run dev` | Start both frontend and backend |
| `npm run dev:frontend` | Start only frontend |
| `npm run dev:backend` | Start only backend |
| `npm run build` | Build both packages |
| `npm run build:frontend` | Build only frontend |
| `npm run build:backend` | Build only backend |

### Frontend (packages/frontend)

| Script | Description |
|--------|-------------|
| `npm run dev` | Start Vite dev server |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |
| `npm run lint` | Run ESLint |

### Backend (packages/backend)

| Script | Description |
|--------|-------------|
| `npm run dev` | Start with nodemon (auto-reload) |
| `npm run build` | Compile TypeScript to JavaScript |
| `npm start` | Start production server |



### Commit Message Guidelines

- `Feat:` for new features
- `Fix:` for bug fixes
- `Update:` for changes to existing features
- `Docs:` for documentation changes
- `Refactor:` for code refactoring
- `Style:` for formatting changes
- `Test:` for adding tests

Example: `Feat: user authentication endpoint`

### Code Style

- Use TypeScript for all new code
- Follow ESLint rules
- Use meaningful variable and function names
- Write self-documenting code
- Add JSDoc comments for complex functions



##  Team

- [Carolina Ribeiro](https://github.com/carolina242408)
- [Diana Dinis](https://github.com/xaloftal)
- [Gonçalo Caixeiro](https://github.com/GCaixeiro)
- [Rui Rodrigues](https://github.com/RuiRodrigues17)




## 📚 Additional Resources

- [React Documentation](https://react.dev/)
- [TypeScript Documentation](https://www.typescriptlang.org/docs/)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [Express Documentation](https://expressjs.com/)
- [Sequelize Documentation](https://sequelize.org/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---



*Last updated: 2026-02-18*