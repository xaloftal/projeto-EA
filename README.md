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

- `spring.datasource.url=jdbc:postgresql://localhost:5432/catchitdb`
- `spring.datasource.username=postgres`
- `spring.datasource.password=1234`

Se o teu utilizador ou palavra-passe do PostgreSQL forem diferentes, ajusta esse ficheiro.

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

