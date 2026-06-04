# Exemplo de Dependências para pom.xml

Adicione estas linhas na secção `<dependencies>` do seu `pom.xml`:

```xml
<!-- WebSocket Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Spring Messaging -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-messaging</artifactId>
</dependency>

<!-- SockJS (para fallback em navegadores antigos) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## Localização no pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    ...
    <dependencies>
        <!-- Spring Boot Starters existentes -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- ADICIONAR AQUI: WebSocket e Messaging -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-messaging</artifactId>
        </dependency>

        <!-- Resto das dependências ... -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- etc... -->
    </dependencies>

    ...
</project>
```

## Após Adicionar

1. Salvar o ficheiro
2. No IDE/Terminal:
   ```bash
   mvn clean install
   ```

3. Aguardar download das dependências (pode levar 1-2 minutos)

4. Verificar se não há erros de compilação

---

✅ Pronto para usar WebSocket Notifications!
