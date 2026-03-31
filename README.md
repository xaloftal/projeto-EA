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

