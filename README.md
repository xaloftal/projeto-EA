# CatchIt


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

### Seed automático

No arranque do backend, o projeto popula automaticamente as tabelas `route`, `stop` e `stopschedule` apenas se estiverem vazias. Os dados são lidos a partir dos CSVs em [backend/src/main/resources/seed](backend/src/main/resources/seed), para poderes alterá-los sem mexer no código Java.

Se já houver dados nessas tabelas, o seed é ignorado para evitar duplicados.

Ficheiros usados:
- [routes.csv] 
- [stops.csv]
- [stop_schedules.csv]

## Docker (DB + Backend + Payment Service)

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
- Payment Service em `localhost:8081`
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
- `PAYMENT_SERVICE_PORT` (opcional, default: `8081`)
- `JWT_SECRET` (obrigatória)
- `JWT_EXPIRATION` (obrigatória, em milissegundos)

### Escalabilidade do pagamento

O pagamento foi isolado num microserviço dedicado (`payment-service`) e o backend passou a comunicar via HTTP no endpoint `/api/payments/authorize`.

No `payment-service`, a autorização usa padrão Strategy com seleção por prefixo de `paymentMethodId`:
- `balance-...` -> `BalancePaymentStrategy`
- Qualquer outro valor -> rejeitado por `FallbackDeclinePaymentStrategy`

Com esta separação, podes escalar apenas o serviço de pagamento sem escalar o backend inteiro, por exemplo:

```bash
docker compose up --build --scale payment-service=3
```

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

## Mapa e GeoJSON

Para acelerar o carregamento do mapa, o projeto passou a usar GeoJSON no backend e um cache em Redis para servir os stops de forma rápida.

### GeoJSON generator

O serviço [GeoJsonGeneratorService](backend/src/main/java/PSM/Location/api/stop/GeoJsonGeneratorService.java) é o ponto central da geração do mapa. Ele:
- lê os `Stop` da base de dados,
- converte cada stop para uma `Feature` GeoJSON,
- monta uma `FeatureCollection`,
- guarda o resultado em Redis com TTL,
- e volta a gerar o ficheiro em caso de cache miss.

A cache é inicializada quando a aplicação termina o arranque, depois do seed automático do backend. Se não houver dados seedados, o serviço continua a funcionar e gera o GeoJSON normalmente quando for necessário.

### Integração com Redis

O Redis passou a ser a store principal para o GeoJSON dos stops:
- chave: `catchit:geojson:stops`
- tipo: `RedisTemplate<String, Object>`
- expiração: 24 horas

O backend já traz o serviço Redis no `docker compose`, por isso não é preciso configurar nada extra para esta cache funcionar.

### Mudanças no MapView

O frontend foi otimizado para renderizar o mapa mais depressa e reutilizar o que já foi carregado:
- o mapa abre logo centrado no Porto,
- o zoom inicial deixa de mostrar todos os stops ao mesmo tempo,
- os stops GeoJSON são carregados em memória no frontend para evitar pedidos repetidos,
- o mapa usa `L.geoJSON(...)` para desenhar os stops de forma mais eficiente,
- a rota do mapa ficou em `keep-alive`, para o ecrã não ser destruído quando mudas de página e voltas,
- o carregamento das rotas foi separado do carregamento dos stops, para o mapa aparecer primeiro e o resto vir depois.

Em conjunto, estas alterações reduzem o tempo de espera e fazem o mapa ficar pronto mais cedo quando entras na página.

