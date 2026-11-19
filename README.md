# Sentinel-Trade-System

## Integrantes

- Cauê Rodrigues de Santana, 10736791;
- Catarina Silva e Meirelles, 10239324.

## Briefing

- Trabalho de projeto de software focado em arquitetura de software de um sistema de trade;
- Para entendimento do contexto e melhor compreensão dos requerimentos, usamos a ferramenta _Deep Research_ do Claude, mas sempre validando o que nos era retornado.

## Leitura dos Documentos

- Recomenda-se que a ordem de leitura a seguir seja seguida para melhor compreensão do projeto:
    1. [ ] [Pesquisa de Audiência](./Docs/pesquisa-audiencia.md);
    2. [ ] [Requisitos](./Docs/requisitos.md);
    3. [ ] [Tabela de Casos de Uso](./Docs/tabela-casos-de-uso.md);
    4. [ ] [Modelo de Casos de Uso (diagrama)](./Docs/modelo-casos-de-uso.md);
    5. [ ] [UML](./Docs/uml.md);
    6. [ ] [UMLsec da implementação](./Docs/umlsec/umlsec.md).

## Como executar

Requisitos: Docker com Docker Compose.

```bash
cp .env.example .env          # valores apenas para desenvolvimento
docker compose up --build
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Usuários de demonstração: `trader`, `auditor` e `admin`, todos com a senha `DEMO_PASSWORD` do `.env`.
- MFA: cadastre `DEMO_MFA_SECRET` (base32) num app autenticador (Google Authenticator, Aegis...) ou gere o código com `oathtool --totp -b JBSWY3DPEHPK3PXP`.

Fluxo no Swagger:

1. `POST /api/v1/auth/login` → token `PRE_AUTH`;
2. **Authorize** com esse token e `POST /api/v1/auth/mfa/verify` com o código TOTP → token `MFA_VERIFIED`;
3. **Authorize** com o token final e use `/api/v1/orders` (header `X-Idempotency-Key` obrigatório na criação).

Com `EXCHANGE_TIMEOUT_RATE=0.1`, 10% dos envios "perdem" a resposta da bolsa simulada e passam por `PENDING_RECONCILIATION`, resolvida pelo reconciliador a cada 5 s.

## Testes

```bash
mvn test
```

Os testes de integração (`OrderFlowIntegrationTest`) sobem um MySQL via Testcontainers e são ignorados automaticamente quando não há Docker.

## Estrutura do código

| Pacote | Responsabilidade |
|---|---|
| `api` | Controllers REST, DTOs validados e tratamento de erros RFC 7807. |
| `application` | Casos de uso: `OrderService`, `OrderReconciler`, `AuthService`, `AuditService`. |
| `domain` | Entidades e regras: `Order` (+ `MarketOrder`, `LimitOrder`), `Account`, `Position`, `RiskRule`s e portas. |
| `infrastructure` | Persistência (Spring Data), segurança (JWT, TOTP, criptografia), bolsa/cotações simuladas e seed. |
