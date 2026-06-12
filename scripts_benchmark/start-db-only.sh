#!/bin/bash

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}         Starting Database Only Service                        ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

# Verificar se Docker está instalado
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed!${NC}"
    echo -e "${YELLOW}Please install Docker first:${NC}"
    echo "  curl -fsSL https://get.docker.com -o get-docker.sh"
    echo "  sudo sh get-docker.sh"
    exit 1
fi

# Verificar se Docker está a correr
if ! docker ps &> /dev/null; then
    echo -e "${RED}❌ Docker is not running!${NC}"
    echo -e "${YELLOW}Starting Docker service...${NC}"
    sudo service docker start 2>/dev/null || sudo systemctl start docker 2>/dev/null
    sleep 2
fi

# Configurações
DB_CONTAINER="catchit-db"
DB_PORT="5433"
DB_NAME="catchitdb"
DB_USER="postgres"
DB_PASSWORD="1234"

# Função para verificar se o container existe
container_exists() {
    docker ps -a --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"
}

# Função para verificar se o container está a correr
container_running() {
    docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"
}

# Função para verificar se a BD está saudável
check_database_healthy() {
    docker exec $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME > /dev/null 2>&1
}

echo -e "\n${BLUE}[1/4] Checking existing database container...${NC}"

# Verificar se o container já existe
if container_exists; then
    if container_running; then
        echo -e "${GREEN}✅ Database container is already running${NC}"
        
        # Perguntar se quer reiniciar
        echo -e "${YELLOW}Do you want to restart the database for a clean state? (y/n)${NC}"
        read -p "Choice: " restart_db
        if [[ $restart_db == "y" || $restart_db == "Y" ]]; then
            echo -e "${YELLOW}🛑 Stopping and removing existing container...${NC}"
            docker stop $DB_CONTAINER > /dev/null 2>&1
            docker rm $DB_CONTAINER > /dev/null 2>&1
            echo -e "${GREEN}✅ Container removed${NC}"
            container_exists=false
        else
            echo -e "${GREEN}✅ Using existing database${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  Container exists but is stopped. Starting it...${NC}"
        docker start $DB_CONTAINER > /dev/null 2>&1
        sleep 3
    fi
fi

# Se o container não existe, criar novo
if ! container_exists; then
    echo -e "\n${BLUE}[2/4] Creating new database container...${NC}"
    echo -e "${GREEN}🐘 Starting PostgreSQL container...${NC}"
    
    docker run -d \
        --name $DB_CONTAINER \
        -e POSTGRES_DB=$DB_NAME \
        -e POSTGRES_USER=$DB_USER \
        -e POSTGRES_PASSWORD=$DB_PASSWORD \
        -p ${DB_PORT}:5432 \
        -v catchit_postgres_data:/var/lib/postgresql/data \
        postgres:16-alpine
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Failed to start container${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Container created successfully${NC}"
fi

echo -e "\n${BLUE}[3/4] Waiting for database to be ready...${NC}"

# Aguardar a BD ficar saudável
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if check_database_healthy; then
        echo -e "\n${GREEN}✅ Database is healthy!${NC}"
        break
    fi
    echo -n "."
    sleep 2
    attempt=$((attempt + 1))
done

if ! check_database_healthy; then
    echo -e "\n${RED}❌ Database failed to start properly${NC}"
    echo -e "${YELLOW}Container logs:${NC}"
    docker logs $DB_CONTAINER --tail=20
    exit 1
fi

echo -e "\n${BLUE}[4/4] Database Information:${NC}"
echo -e "  ${GREEN}Container:${NC} $DB_CONTAINER"
echo -e "  ${GREEN}Host:${NC} localhost:$DB_PORT"
echo -e "  ${GREEN}Database:${NC} $DB_NAME"
echo -e "  ${GREEN}Username:${NC} $DB_USER"
echo -e "  ${GREEN}Password:${NC} $DB_PASSWORD"

# Criar schema catchit se não existir
echo -e "\n${BLUE}Creating schema 'catchit'...${NC}"
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "CREATE SCHEMA IF NOT EXISTS catchit;" > /dev/null 2>&1
echo -e "${GREEN}✅ Schema 'catchit' ready${NC}"

# Testar conexão
echo -e "\n${BLUE}Testing connection...${NC}"
if docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Connection successful${NC}"
else
    echo -e "${RED}❌ Connection failed${NC}"
    exit 1
fi

echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         Database is ready for benchmarks!                       ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"

echo -e "\n${YELLOW}Useful commands:${NC}"
echo -e "  ${GREEN}Access database:${NC} docker exec -it $DB_CONTAINER psql -U $DB_USER -d $DB_NAME"
echo -e "  ${GREEN}Stop database:${NC} docker stop $DB_CONTAINER"
echo -e "  ${GREEN}Start database:${NC} docker start $DB_CONTAINER"
echo -e "  ${GREEN}Remove database:${NC} docker rm -f $DB_CONTAINER"
echo -e "  ${GREEN}View logs:${NC} docker logs $DB_CONTAINER -f"