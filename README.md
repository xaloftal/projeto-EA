# CatchIt

## Regras de Colaboração

### Padrão de branches

As branches de desenvolvimento devem seguir o formato:

`iniciais/feat-nome_feature`

Exemplos:
- `ad/feat-class-diagram`
- `rr/feat-import-bus-data`
- `gc/feat-route-validation`
- `cr/feat-payment-integration`


Notas:
- `iniciais`: iniciais da pessoa que cria a branch (minúsculas).
- `feat`: tipo de trabalho (feature).
- `nome_feature`: descrição curta, em minúsculas, separada por hífen.

### Pull Requests para a `main`

Para fazer merge na branch `main`, o Pull Request deve cumprir:

- Pelo menos **1 aprovação** de outra pessoa da equipa.
- **Não fazer merge direto na `main` sem PR.**

Fluxo recomendado:
1. Criar branch com o padrão definido.
2. Fazer commits da feature.
3. Abrir PR para `main`.
4. Pedir revisão.
5. Fazer merge apenas após pelo menos 1 aprovação.

(ps: que try harder)

## Backend

O backend está dentro da pasta [backend](backend). Para o correr, é preciso ter:

- Java 17 ou +
- Maven
- PostgreSQL a correr localmente

### 1. Criar a base de dados

O projeto espera uma base chamada `catchitdb`.

No `psql`, executa:

```sql
CREATE DATABASE catchitdb;
```

ou no Windows

```powershell
createdb -U postgres catchitdb
```

### 2. Confirmar as credenciais

O backend usa estas definições no ficheiro [backend/src/main/resources/application.properties](backend/src/main/resources/application.properties):

- `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/catchitdb}`
- `spring.datasource.username=${DB_USER:postgres}`
- `spring.datasource.password=${DB_PASSWORD}`

Antes de arrancar, define as variáveis de ambiente (por exemplo num ficheiro `.env` na raiz do projeto):

```bash
cp .env.example .env
```

Depois edita o `.env` com os teus valores.

### 3. Arrancar o backend

Abre um terminal na pasta `backend` e corre:

```powershell
mvn spring-boot:run
```

Se quiseres ignorar testes durante o arranque:

```powershell
mvn -DskipTests spring-boot:run
```

### 4. Validar que subiu

Quando estiver certo, deves ver no terminal algo como:

- `Tomcat started on port 8080`
- `Started CatchItApplication`

Se a base ainda não existir, o Spring Boot vai falhar antes de criar as tabelas.

## Docker (DB + Backend)

Se quiseres subir a infraestrutura com um comando, usa Docker Compose.

Pré-requisitos:
- Docker
- Docker Compose

Na raiz do projeto, corre:

```bash
docker compose up --build
```

Isto sobe:
- PostgreSQL em `localhost:5433`
- Backend Spring Boot em `localhost:8080`
- Frontend web em `localhost:9000`

Para parar:

```bash
docker compose down
```

Para parar e apagar também os dados da base:

```bash
docker compose down -v
```

Nota sobre persistência de dados:
- `docker compose down` mantém os dados (volume persistente)
- `docker compose down -v` apaga os volumes e os dados
- O backend está com `ddl-auto=update` para não recriar tabelas a cada arranque

Antes do `docker compose up --build`, cria o `.env` na raiz:

```bash
cp .env.example .env
```

Variáveis necessárias:
- `DB_PASSWORD` (obrigatória)
- `DB_USER` (opcional, default: `postgres`)
- `DB_NAME` (opcional, default: `catchitdb`)
- `DB_PORT` (opcional, default: `5433`)

### Aceder à base de dados via Docker

Se quiseres abrir o `psql` dentro do container da base de dados:

```bash
docker exec -it catchit-db psql -U postgres -d catchitdb
```

Depois podes correr comandos como:

```sql
\dt
select id, email, name, balance from users;
```

Se preferires ligar a partir do teu terminal ao PostgreSQL exposto pelo Docker, usa:

```bash
psql -h localhost -p ${DB_PORT:-5433} -U postgres -d catchitdb
```

## Mobile + Docker

O emulador Android/iOS continua fora do Docker. O fluxo recomendado fica:
1. `docker compose up --build` (DB + backend + frontend web)
2. `npx quasar dev -m capacitor -T android` (app mobile)

No frontend mobile, define `VITE_API_BASE_URL` para:
- Android Emulator: `http://10.0.2.2:8080`
- Dispositivo físico: `http://<ip-da-tua-maquina>:8080`

