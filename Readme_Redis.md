# CatchIt - Redis (Carrinho e Blacklist JWT)

Este documento resume o papel do Redis no backend do projeto, cobrindo os dois fluxos principais: sessao de carrinho de compras e revogacao de tokens JWT.

## Objetivo

O Redis foi adotado para armazenar dados volateis com acesso rapido e latencia baixa.

Responsabilidades principais:
- Manter o estado do carrinho por utilizador.
- Aplicar TTL para limpeza automatica de carrinhos inativos.
- Guardar tokens JWT revogados em blacklist ate expirarem.
- Permitir validacao de seguranca em memoria durante autenticacao.

## Arquitetura

Componentes envolvidos:
- frontend: consulta e atualiza carrinho via endpoints do backend.
- backend: aplica regras de negocio e serializacao/deserializacao.
- redis: persistencia temporaria de carrinhos e blacklist JWT.


## Modelo de Chaves no Redis

Chaves utilizadas:
- carrinho por utilizador: `cart:user:{UUID}`
- blacklist de token: `jwt:blacklist:{token}`

Politica de expiracao:
- Carrinho: TTL configuravel para limpar carrinhos abandonados.
- JWT blacklist: TTL alinhado com o tempo restante de expiracao do token.

## Fluxo de Carrinho (Inicio ao Fim)

1. Frontend chama endpoint de carrinho no backend.
2. Backend identifica utilizador autenticado.
3. `CartController` delega operacao para `CartService`.
4. `CartService` le chave `cart:user:{UUID}` no Redis.
5. Se existir JSON valido, converte para lista de itens e devolve resposta.
6. Se nao existir ou estiver vazio, devolve carrinho vazio.
7. Em operacoes de update, backend regrava o JSON e renova TTL.

## Fluxo de Logout e Blacklist JWT

1. Utilizador faz logout (ou outra acao de revogacao).
2. Backend extrai token JWT da requisicao.
3. Token e gravado no Redis com prefixo `jwt:blacklist:`.
4. TTL da chave e definido com base no `exp` do token.
5. Em cada pedido autenticado, filtro de seguranca verifica se token esta na blacklist.
6. Se estiver, pedido e rejeitado; se nao estiver, autenticacao continua.
7. Quando TTL termina, Redis remove automaticamente a entrada.

## Integracao Frontend

Exemplo de consumo do carrinho:

```ts
const fetchCart = async () => {
  if (!currentUser.value) return

  const response = await catchitApi.getCart()

  if (response.success && response.data) {
    applyBackendCart(response.data)
    return
  }

  error.value = response.error || 'Unable to load cart'
}
```

Cliente API para backend:

```ts
async getCart(): Promise<ApiResponse<BackendCartResponse>> {
  return requestJson<BackendCartResponse>('/api/cart')
}
```

## Processamento no Backend

Exemplo de operacao no servico:

```java
public CartResponseDTO getCart(UUID userId) {
    List<CartItemDTO> items = loadItems(userId);
    return toResponse(items);
}
```

Leitura de dados no Redis com fallback seguro:

```java
private List<CartItemDTO> loadItems(UUID userId) {
    String json = redisTemplate.opsForValue().get(getCartKey(userId));
    if (json == null || json.isBlank()) {
        return new ArrayList<>();
    }

    try {
        return objectMapper.readValue(json, new TypeReference<List<CartItemDTO>>() {
        });
    } catch (Exception e) {
        throw new IllegalStateException("Unable to read cart from Redis", e);
    }
}
```

## Seguranca e Consistencia

Decisoes aplicadas:
- JWT permanece stateless, mas a revogacao e stateful no Redis.
- Chaves com prefixo facilitam observabilidade e debug operacional.
- TTL evita acumulacao de lixo e reduz custo operacional.
- Falha de parse do carrinho gera erro de dominio explicito.

## Operacoes de Diagnostico

Comandos uteis para inspecao:

```bash
docker exec catchit-redis redis-cli KEYS "cart:user:*"
docker exec catchit-redis redis-cli KEYS "jwt:blacklist:*"
docker exec catchit-redis redis-cli TTL "cart:user:{UUID}"
docker exec catchit-redis redis-cli TTL "jwt:blacklist:{token}"
docker exec catchit-redis redis-cli GET "cart:user:{UUID}"
```

Exemplo observado de blacklist:

```bash
jwt:blacklist:eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5MGNiN2M2Ny1hZmY4LTQzZGQtOThkNC01NWFhMTU4NDkyOGUiLCJlbWFpbCI6Im11bHRpLWxvZ291dEBleGFtcGxlLmNvbSIsImlhdCI6MTc3NzEzNjg4NywiZXhwIjoxNzc3MTM4Njg3fQ.1_LF5Iu2mD-hqeZcse6s0ddNjMr30Z7MN8JhRkUQ1kM
```

## Checklist de Validacao (Manual)

1. Subir stack:
- `docker compose up --build`
2. Autenticar utilizador e obter JWT valido.
3. Executar logout.
4. Confirmar token na blacklist:
- `docker exec catchit-redis redis-cli KEYS "jwt:blacklist:*"`
5. Validar bloqueio de token revogado em endpoint protegido.
6. Criar/atualizar carrinho no frontend.
7. Confirmar existencia da chave:
- `docker exec catchit-redis redis-cli KEYS "cart:user:*"`
8. Confirmar expiracao com TTL apos periodo de inatividade.

## Limites Atuais Conhecidos

- Operacoes de debug com `KEYS` sao adequadas em ambiente de desenvolvimento, mas em producao o recomendado e usar `SCAN`.
- Nao existe historico permanente de carrinho no Redis (por desenho).
- Revogacao depende da disponibilidade do Redis durante validacao de token.

## Evolucao Recomendada

- Introduzir metrica de taxa de revogacao e tamanho da blacklist.
- Adicionar health checks especificos para Redis no backend.
- Definir politica de fallback explicita quando Redis estiver indisponivel na validacao JWT.
