# Sentinel-Trade-System (SentinelTrade — Orion Capital)

## Integrantes

- Cauê Rodrigues de Santana, 10736791;
- Catarina Silva e Meirelles, 10239324.

## Briefing

- Trabalho de projeto de software focado em arquitetura, modelagem UML/UMLsec e implementação de um sistema de trade de alta criticidade (**SentinelTrade**, para a corretora fictícia **Orion Capital**);
- Para entendimento do contexto do mercado brasileiro (B3, CVM 30/35, BSM/MRP, LGPD) e melhor compreensão dos requisitos, utilizamos pesquisa secundária (*desk research*) apoiada por ferramentas de pesquisa, sempre validando dados e referências oficiais.

---

## Leitura dos Documentos e Rastreabilidade

Recomenda-se seguir a ordem de leitura abaixo para compreender a evolução completa do projeto — da pesquisa de audiência até a implementação segura com UMLsec:

1. [x] [Pesquisa de Audiência](./docs/evidencias/pesquisa-audiencia.md) — Contexto B3, 4 personas (Lucas, Dona Marta, Rafael e Juliana), stakeholders, 10 dores mapeadas (`D1`–`D10`) e regulação (CVM 30, CVM 35, MRP/BSM, LGPD);
2. [x] [Requisitos Funcionais e Não Funcionais](./docs/requisitos/Requisitos.md) — 33 RFs e 32 RNFs (Segurança, Resiliência e Qualidade) com matriz de rastreabilidade;
3. [x] [Requisitos e Diretrizes DevSecOps](./docs/requisitos/Requisitos-Sec.md) — Checklist DevSecOps, sanitização de entradas e prevenção de vulnerabilidades;
4. [x] [Tabela de Casos de Uso](./docs/uml/tabela-casos-de-uso.md) — Visão consolidada dos 27 casos de uso (`UC01`–`UC27`) e matriz de segregação de funções;
5. [x] [Modelo de Casos de Uso (MCU)](./docs/uml/uml-casos-de-uso.md) — Diagrama de casos de uso ([mcu.svg](./docs/extra-e-refs-aula/assets/mcu.svg)) e descrições expandidas dos fluxos críticos (`UC06`, `UC14`, `UC26`);
6. [x] [Modelo de Classes de Domínio](./docs/uml.md) — Visão estrutural das entidades de domínio ([uml.svg](./docs/extra-e-refs-aula/assets/uml.svg));
7. [x] [Modelo de Classes de Análise (BCE)](./docs/modelo-classes-analise.md) — Decomposição *Boundary-Control-Entity* ([classes-analise.svg](./docs/extra-e-refs-aula/assets/classes-analise.svg));
8. [x] [Modelagem de Interações](./docs/uml/uml-interacoes.md) — Diagramas de Sequência ([interacoes-ordem.svg](./docs/extra-e-refs-aula/assets/interacoes-ordem.svg)) e de Comunicação ([interacoes-comunicacao.svg](./docs/extra-e-refs-aula/assets/interacoes-comunicacao.svg));
9. [x] [Modelo de Classes de Projeto](./docs/modelo-classes-projeto.md) — Camadas, portas e adaptadores ([classes-projeto.svg](./docs/extra-e-refs-aula/assets/classes-projeto.svg));
10. [x] [Modelagem de Estados da Ordem](./docs/uml/uml-estados.md) — Máquina de estados formal com 11 estados ([estados-ordem.svg](./docs/extra-e-refs-aula/assets/estados-ordem.svg));
11. [x] [Arquitetura do Sistema](./docs/uml/uml-arquitetura.md) — Arquitetura Lógica em camadas/subsistemas ([arquitetura-sistema.svg](./docs/extra-e-refs-aula/assets/arquitetura-sistema.svg)) e Implantação Física ([arquitetura-implantacao.svg](./docs/extra-e-refs-aula/assets/arquitetura-implantacao.svg));
12. [x] [Plano de Implementação](./PLAN.md) — Diretrizes arquiteturais da aplicação Java 21 + Spring Boot 3.5 + MySQL 8.4;
13. [x] [Modelagem UMLsec da Implementação](./docs/umlsec/umlsec.md) — Mapeamento dos estereótipos UMLsec diretamente no código e nos diagramas renderizados:
    - [Diagrama de Classes UMLsec](./docs/umlsec/assets/classes-umlsec.svg) ([fonte PlantUML](./docs/umlsec/plantuml/classes-umlsec.puml))
    - [Diagrama de Sequência UMLsec](./docs/umlsec/assets/sequencia-ordem-umlsec.svg) ([fonte PlantUML](./docs/umlsec/plantuml/sequencia-ordem-umlsec.puml))
    - [Diagrama de Implantação UMLsec](./docs/umlsec/assets/implantacao-umlsec.svg) ([fonte PlantUML](./docs/umlsec/plantuml/implantacao-umlsec.puml))
14. [x] [Roteiro de Apresentação (~10 min)](./SCRIPT.md) — Explicação falada do núcleo da plataforma dividida por tópicos e arquivos correspondentes.

---

## Visão Geral da Implementação (`/src`)

A aplicação implementa o núcleo crítico do **SentinelTrade** em **Java 21** e **Spring Boot 3.5.6**, seguindo uma arquitetura em camadas com portas e adaptadores (Clean/Hexagonal simplificada) e segurança desde o design (**UMLsec**):

### 1. Segurança com UMLsec

| Estereótipo UMLsec | Aplicação no Código | Arquivos Principais |
|---|---|---|
| `<<critical>>` `{secrecy}` | Hash de senha com **BCrypt** (e *dummy hash* contra enumeração de usuário por *timing*) e segredo TOTP cifrado em repouso no MySQL com **AES-256-GCM** (IV aleatório de 12 bytes). | `User.java`, `AuthService.java`, `SecretConverter.java`, `V1__init_schema.sql` |
| `<<critical>>` `{integrity}` | Entidades financeiras sem *setters* públicos; mutações apenas via métodos de negócio que validam invariantes; controle de concorrência otimista (`@Version`) e `CHECK` constraints no SQL. | `Order.java`, `LimitOrder.java`, `MarketOrder.java`, `Account.java`, `Position.java`, `Settlement.java` |
| `<<guarded access>>` | Autenticação em 2 etapas: `/api/v1/auth/login` emite token `PRE_AUTH` (válido apenas para `/api/v1/auth/mfa/verify`); após validar o TOTP (RFC 6238), emite token `MFA_VERIFIED`, exigido em todo `/api/v1/**`. | `SecurityConfig.java`, `TokenService.java`, `TotpMfaVerifier.java`, `SecurityAuditHandler.java` |
| `<<role-based access>>` | Segregação de funções via `@PreAuthorize` (`ROLE_TRADER`, `ROLE_AUDITOR`, `ROLE_ADMIN`) e verificação estrita de titularidade da conta (ordens de terceiros retornam `404 Not Found`). | `OrderController.java`, `AuditLogController.java`, `CurrentUser.java`, `OrderService.java` |
| `<<no down-flow>>` / `<<data security>>` | Exposição exclusiva de DTOs imutáveis (`record`), tratamento centralizado de erros no padrão **RFC 7807 (`ProblemDetail`)** sem *stack traces* e mascaramento de segredos nos logs. | `GlobalExceptionHandler.java`, `api/dto/*`, `application.yml` |
| `<<audit>>` / `<<fair exchange>>` | Cada transição de ordem publica um `AuditEvent` gravado na mesma transação ACID pelo `AuditService`. A tabela `audit_logs` é *append-only* (`@Immutable`, repositório sem `delete`/`update` e *triggers* MySQL bloqueando `UPDATE` e `DELETE`). | `AuditService.java`, `AuditEvent.java`, `AuditLog.java`, `AuditLogRepository.java`, `V1__init_schema.sql` |
| `<<secure links>>` | Isolamento do MySQL na rede Docker interna `backend` (`internal: true`), sem publicação da porta `3306` no host e container da API rodando com usuário não-root. | `docker-compose.yml`, `Dockerfile` |

### 2. Ciclo de Vida da Ordem, Idempotência e Reconciliação

- **Herança e Polimorfismo (`Order` → `MarketOrder` / `LimitOrder`):** mapeadas em tabela única (`SINGLE_TABLE`) com discriminador `order_type`.
- **Máquina de 11 Estados (`OrderStatus`):** `CREATED`, `VALIDATED`, `SENT`, `OPEN`, `PENDING_RECONCILIATION`, `PARTIALLY_EXECUTED`, `CANCEL_REQUESTED`, `EXECUTED`, `REJECTED`, `CANCELLED` e `EXPIRED`.
- **Motor Pré-Negociação (`RiskRule` — Strategy Pattern):**
  - `BalanceSufficiencyRule`: garante saldo disponível suficiente para ordens de compra (`BUY`);
  - `AssetPositionRule`: garante custódia livre (`quantity - reservedQuantity`) suficiente para vendas (`SELL`), impedindo venda a descoberto não autorizada;
  - `OrderExposureLimitRule`: verifica se o volume financeiro negociado no dia somado à nova ordem respeita o `dailyRiskLimit` da conta.
- **Idempotência (`X-Idempotency-Key`):** reenvios com a mesma chave e mesmo payload retornam a ordem original (`200 OK`) e registram `ORDER_DUPLICATE_REPLAYED`; se o payload ou a conta divergir, bloqueiam com `409 Conflict` e auditam `ORDER_DUPLICATE_BLOCKED`.
- **Duas Transações Curtas + Reconciliação Sem Reenvio Cego:** a validação, reserva e mudança para `SENT` ocorrem na **Transação ACID 1**; a chamada ao `ExchangeGateway` ocorre fora da transação de banco; o retorno é aplicado na **Transação ACID 2**. Se houver *timeout* (`ExchangeTimeoutException`), a ordem vai para `PENDING_RECONCILIATION` e o `OrderReconciler` consulta o estado real na bolsa por ID (`query`/`cancel`), sem jamais reenviar a ordem.

---

## Endpoints da API REST

| Método | Rota | Autoridade / Role | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Público | Etapa 1: valida usuário/senha e emite JWT `PRE_AUTH`. |
| `POST` | `/api/v1/auth/mfa/verify` | `PRE_AUTH` | Etapa 2: valida código TOTP de 6 dígitos e emite JWT `MFA_VERIFIED`. |
| `POST` | `/api/v1/orders` | `MFA_VERIFIED` + `ROLE_TRADER` | Cria, valida, reserva e envia ordem (exige header `X-Idempotency-Key`). |
| `GET` | `/api/v1/orders` | `MFA_VERIFIED` | Lista ordens paginadas (do próprio trader, ou todas para `AUDITOR`/`ADMIN`). |
| `GET` | `/api/v1/orders/{id}` | `MFA_VERIFIED` | Consulta ordem por ID (valida titularidade para `TRADER`). |
| `PATCH` | `/api/v1/orders/{id}` | `MFA_VERIFIED` + `ROLE_TRADER` | Altera preço limite de ordem `LIMIT` em estado `OPEN` sem execuções, recalculando a reserva. |
| `POST` | `/api/v1/orders/{id}/cancel` | `MFA_VERIFIED` + `ROLE_TRADER` | Solicita cancelamento (`CANCEL_REQUESTED` → `CANCELLED`) e estorna reserva remanescente. |
| `GET` | `/api/v1/audit-logs` | `MFA_VERIFIED` + `ROLE_AUDITOR`/`ADMIN` | Consulta paginada da trilha de auditoria imutável (`audit_logs`). |

---

## Como Executar

Requisitos: Docker com Docker Compose.

```bash
cp .env.example .env          # valores apenas para desenvolvimento
docker compose up --build
```

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **Usuários de demonstração:** `trader`, `auditor` e `admin`, todos com a senha `DEMO_PASSWORD` do `.env`.
- **MFA:** cadastre `DEMO_MFA_SECRET` (base32) num app autenticador (Google Authenticator, Aegis...) ou gere o código com `oathtool --totp -b JBSWY3DPEHPK3PXP`.

Fluxo no Swagger:

1. `POST /api/v1/auth/login` → token `PRE_AUTH`;
2. **Authorize** com esse token e `POST /api/v1/auth/mfa/verify` com o código TOTP → token `MFA_VERIFIED`;
3. **Authorize** com o token final e use `/api/v1/orders` (header `X-Idempotency-Key` obrigatório na criação).

Com `EXCHANGE_TIMEOUT_RATE=0.1`, 10% dos envios "perdem" a resposta da bolsa simulada e passam por `PENDING_RECONCILIATION`, resolvida pelo reconciliador a cada 5 s.

---

## Suíte de Testes Automatizados

Para executar toda a suíte de testes:

```bash
mvn test
```

A suíte cobre **todas as camadas** da aplicação em `src/test/java/com/sentineltrade`:

| Classe de Teste | Camada | O que é testado |
|---|---|---|
| `OrderTest` | Domínio (`domain.order`) | Máquina de 11 estados (`Order`, `LimitOrder`, `MarketOrder`), execuções parciais/totais, corrida entre cancelamento e execução, expiração, reconciliação, alteração de preço limite e bloqueio de transições inválidas. |
| `AccountTest` | Domínio (`domain.account`) | Reserva, liberação, liquidação de compra, crédito de venda e proteção contra saldos negativos (`InsufficientBalanceException`, `IllegalStateException`). |
| `PositionTest` | Domínio (`domain.account`) | Custódia por ativo, cálculo de preço médio ponderado em múltiplas compras, reserva/liberação de quantidade e baixa em vendas (`InsufficientPositionException`). |
| `SettlementTest` | Domínio (`domain.account`) | Serviço de domínio que coordena reservas, liquidações parciais/totais e estornos para ordens de compra (`BUY`) e venda (`SELL`). |
| `RiskRulesTest` | Domínio (`domain.risk`) | Estratégias `BalanceSufficiencyRule`, `AssetPositionRule` e `OrderExposureLimitRule` (incluindo posições reservadas e limite diário acumulado). |
| `UserAndAuditDomainTest` | Domínio (`domain.user` / `audit`) | Entidades `User`, autoridades de `Role` (`ROLE_TRADER`, `ROLE_AUDITOR`, `ROLE_ADMIN`) e imutabilidade de `AuditLog`. |
| `OrderServiceTest` | Aplicação (`application.order`) | Fluxos completos de `OrderService` (compra/venda a mercado e limitada, idempotência, bloqueio de payload divergente, rejeição pré-trade e na bolsa, alteração, cancelamento, *timeout* e reconciliação), `PlaceOrderCommand` e `OrderReconciler`. |
| `AuthServiceTest` | Aplicação (`application.auth`) | Login em duas etapas (`PRE_AUTH` → `MFA_VERIFIED`), proteção contra enumeração de usuários via *dummy hash*, bloqueio de usuários inativos e auditoria de falhas. |
| `AuditServiceTest` | Aplicação (`application.audit`) | Gravação transacional de `AuditEvent` com extração automática do `actorId` via JWT no `SecurityContext` e do IP no `RequestContextHolder`. |
| `TotpMfaVerifierTest` | Infraestrutura (`infrastructure.security`) | Algoritmo TOTP (RFC 6238), vetores de teste oficiais, tolerância de ±1 janela de 30 s (*clock drift*), rejeição de códigos expirados/malformados e segredos Base32 inválidos. |
| `SecurityInfrastructureTest` | Infraestrutura (`infrastructure.security`) | Criptografia AES-256-GCM com IV aleatório e detecção de adulteração (`SecretConverter`), emissão/validação de JWT (`TokenService`, `SecurityConfig`) e respostas RFC 7807 em falhas 401/403 (`SecurityAuditHandler`). |
| `SimulationAdaptersTest` | Infraestrutura (`infrastructure.simulation`) | Provedor de cotações B3 com e sem volatilidade (`SimulatedMarketQuoteAdapter`) e motor de bolsa simulada (`SimulatedExchangeAdapter`: execução parcial/total, banda de preço, expiração por TTL, cancelamento, alteração e *timeout* com reconciliação). |
| `DemoDataSeederTest` | Infraestrutura (`infrastructure.seed`) | Criação condicional dos usuários (`trader`, `auditor`, `admin`), conta e posições iniciais em ambiente de desenvolvimento. |
| `ApiControllersAndExceptionHandlerTest` | Interface (`api`) | Controladores REST (`AuthController`, `OrderController`, `AuditLogController`), validação de DTOs e mapeamento completo de exceções para `ProblemDetail` RFC 7807 sem vazamento de detalhes internos (`GlobalExceptionHandler`). |
| `OrderFlowIntegrationTest` | Integração E2E | Fluxo HTTP completo com **Testcontainers MySQL 8.4** (ignorados automaticamente quando não há daemon Docker ativo). |

---

## Estrutura do Código

| Pacote | Responsabilidade |
|---|---|
| `api` | Controllers REST (`OrderController`, `AuthController`, `AuditLogController`), DTOs validados (`api/dto/*`), resolução de usuário autenticado (`CurrentUser`) e tratamento de erros RFC 7807 (`GlobalExceptionHandler`). |
| `application` | Casos de uso e orquestração transacional: `OrderService`, `OrderReconciler`, `AuthService`, `AuditService` e `AuditEvent`. |
| `domain` | Entidades e regras de negócio puras: `Order` (`MarketOrder`, `LimitOrder`, `OrderStatus`), `Account`, `Position`, `Settlement`, `User`, `AuditLog`, `RiskRule`s (`BalanceSufficiencyRule`, `AssetPositionRule`, `OrderExposureLimitRule`) e portas (`ExchangeGateway`, `MarketQuoteProvider`, `MfaVerifier`). |
| `infrastructure` | Persistência JPA (`OrderRepository`, `AccountRepository`, `PositionRepository`, `UserRepository`, `AuditLogRepository`), segurança (`SecurityConfig`, `TokenService`, `TotpMfaVerifier`, `SecretConverter`, `SecurityAuditHandler`), simulações (`SimulatedExchangeAdapter`, `SimulatedMarketQuoteAdapter`) e *seed* (`DemoDataSeeder`). |
