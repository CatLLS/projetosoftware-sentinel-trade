## 1. Visão Arquitetural e Stack Tecnológica

O sistema deve seguir uma arquitetura em camadas (ou Hexagonal/Clean Architecture simplificada) para garantir isolamento estrito entre exposição de API, regras de domínio, persistência e integrações externas.

Considere todos os arquivos em `docs/*` (exceto `docs/extra-e-refs-aula/classes-documents/*`) como base de criação do projeto. Lá você encontra todas as informações necessárias para modelagem.

* **Linguagem & Framework:** Java 21 + Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security, Bean Validation).
* **Banco de Dados:** MySQL 8.x com gerenciamento de migrações via **Flyway** (garantindo versionamento determinístico do DDL SQL).
* **Documentação de API:** Springdoc OpenAPI 3 (**Swagger UI**).
* **Infraestrutura & Ambiente:** Docker + Docker Compose (`docker-compose.yml` orquestrando o container da aplicação e o container do MySQL com volumes persistentes e *healthchecks*).
* **Segurança (UMLsec):** Aplicação explícita de estereótipos do perfil UMLsec desde a modelagem até o código.

---

## 2. Diretrizes de Modelagem UMLsec

O Antigravity deve projetar e documentar o sistema aplicando os seguintes estereótipos de **UMLsec** nos diagramas de classes, implantação e sequência, refletindo-os diretamente na implementação:

| Estereótipo UMLsec | Aplicação Arquitetural | Implementação Técnica no Spring / MySQL |
| --- | --- | --- |
| **`<<critical>>`** | Classes/entidades que manipulam saldo, custódia de ativos, ordens financeiras e credenciais (`Account`, `Order`, `Position`, `AuditLog`). | Tag `{secrecy}` em campos sensíveis (hashes de senha, segredos TOTP/MFA) e `{integrity}` em saldos, quantidades e estados de ordem. |
| **`<<secure links>>`** | Comunicação entre cliente/Swagger, API Spring e banco MySQL. | Isolamento em rede interna Docker (`bridge` dedicada), bloqueio de exposição externa direta da porta `3306` em produção e enforcement de TLS/HTTPS nos endpoints. |
| **`<<role-based access>>`** | Controle de acesso aos endpoints de criação, consulta e cancelamento de ordens. | Spring Security com RBAC (`ROLE_TRADER`, `ROLE_AUDITOR`, `ROLE_ADMIN`) e validação de propriedade do recurso (um usuário só acessa/cancela suas próprias ordens). |
| **`<<guarded access>>`** | Proteção de operações críticas de movimentação financeira e alteração de estado. | Interceptação via filtros/AOP exigindo sessão autenticada + validação do segundo fator (MFA) antes da execução da regra de negócio. |
| **`<<no down-flow>>` / `<<data security>>**` | Prevenção de vazamento de informações internas e logs higienizados. | Uso rigoroso de DTOs de entrada/saída (nunca expor entidades JPA), `@ControllerAdvice` global que suprime *stack traces* em respostas HTTP e mascaramento de dados sensíveis nos logs. |
| **`<<audit>>` / `<<fair exchange>>**` | Rastreabilidade completa e atomicidade na execução/cancelamento de ordens. | Transações ACID (`@Transactional`) acopladas à gravação imutável (append-only) na tabela `audit_logs`. |

---

## 3. Modelagem Orientada a Objetos e Domínio

Para atender aos requisitos de POO (encapsulamento, abstração, herança e polimorfismo), o domínio deve ser estruturado da seguinte forma:

* **Abstração e Herança de Ordens:**
* Classe abstrata base `Order` (mapeada via `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` ou `JOINED`) contendo atributos comuns: `id`, `idempotencyKey`, `account`, `assetSymbol`, `side` (`BUY`/`SELL`), `quantity`, `status`, `createdAt`.
* Especializações concretas: `MarketOrder` (execução imediata ao preço de mercado) e `LimitOrder` (exige `limitPrice` positivo e validação de faixa).


* **State Pattern / Máquina de Estados Encapsulada:**
* Os estados da ordem (`PENDING`, `VALIDATED`, `EXECUTED`, `CANCELLED`, `REJECTED`) devem ser controlados por métodos de domínio na própria entidade/agregado (ex: `order.cancel()`, `order.execute()`, `order.reject(reason)`), impedindo mutação direta via *setters* públicos e lançando exceções de domínio em transições inválidas (ex: cancelar uma ordem já `EXECUTED`).


* **Interfaces de Estratégia e Integração:**
* Interface `RiskRule` (Strategy Pattern) com implementações específicas: `BalanceSufficiencyRule`, `AssetPositionRule`, `OrderExposureLimitRule`.
* Interface `MarketQuoteProvider` na camada de domínio com implementação simulada (`SimulatedMarketQuoteAdapter`) na camada de infraestrutura, isolando integrações externas.
* Interface `MfaVerifier` para desacoplar a lógica de verificação do segundo fator.



---

## 4. Modelagem Relacional SQL (MySQL)

O schema SQL (gerenciado via `V1__init_schema.sql` no Flyway) deve garantir integridade referencial e travas contra inconsistências financeiras:

1. **`users`**: `id`, `username`, `password_hash`, `mfa_secret`, `role`, `active`, `created_at`.
2. **`accounts`**: `id`, `user_id` (FK), `available_balance` (`DECIMAL(19,4)`), `reserved_balance` (`DECIMAL(19,4)`), `daily_risk_limit` (`DECIMAL(19,4)`), `version` (`BIGINT` para *Optimistic Locking* `@Version`).
3. **`positions`**: `id`, `account_id` (FK), `asset_symbol`, `quantity` (`DECIMAL(19,6)`), `reserved_quantity` (`DECIMAL(19,6)`), `average_price` (`DECIMAL(19,4)`), `version` (`BIGINT`), com constraint `UNIQUE(account_id, asset_symbol)`.
4. **`orders`**: `id`, `idempotency_key` (`VARCHAR(64) UNIQUE NOT NULL`), `account_id` (FK), `order_type`, `asset_symbol`, `side`, `quantity`, `limit_price`, `status`, `rejection_reason`, `created_at`, `updated_at`.
5. **`audit_logs`**: `id`, `event_type`, `actor_id`, `entity_name`, `entity_id`, `previous_state`, `new_state`, `metadata_json`, `ip_address`, `created_at` (sem operações de `UPDATE` ou `DELETE` na camada de repositório).

---

## 5. Roteiro de Execução em Fases (Plano de Trabalho)

### Fase 1: Infraestrutura, Docker e Banco de Dados

* Criar `Dockerfile` multi-stage (build com Maven/JDK 21, runtime com JRE 21 Alpine).
* Criar `docker-compose.yml` com serviços `sentinel-db` (MySQL 8) e `sentinel-api` (Spring Boot), configurando variáveis de ambiente via `.env.example` e rede isolada (`<<secure links>>`).
* Configurar Spring Boot (`application.yml`), Springdoc Swagger (`/swagger-ui.html`) e criar as migrations SQL iniciais do Flyway.

### Fase 2: Segurança, Autenticação com MFA e Auditoria

* Implementar fluxo de autenticação em dois passos:
1. `POST /api/v1/auth/login`: Valida credenciais e retorna um token temporário pré-MFA (`PRE_AUTH`).
2. `POST /api/v1/auth/mfa/verify`: Valida o código TOTP (ou OTP controlado simulado) e emite o JWT final com *claims* de role e status `MFA_VERIFIED`.


* Configurar filtro de segurança para exigir `MFA_VERIFIED` em todas as rotas `/api/v1/orders/**`.
* Implementar `AuditService` assíncrono/transacional disparado via eventos do Spring (`ApplicationEventPublisher`) para registrar logins, falhas de MFA, criação, atualização, rejeição e cancelamento de ordens.

### Fase 3: Domínio, Regras de Negócio e Idempotência

* Implementar as entidades OO (`Order`, `MarketOrder`, `LimitOrder`, `Account`, `Position`) com encapsulamento estrito e controle de concorrência (`@Version`).
* Implementar o motor de validação de regras de negócio:
* **Prevenção de Duplicidade:** Verificar `idempotency_key` (enviada via header `X-Idempotency-Key` ou payload) antes de processar a ordem; retornar a ordem existente caso a chave já tenha sido processada.
* **Saldo e Posição:** Em ordens de compra (`BUY`), validar `available_balance >= valor_estimado` e bloquear/reservar o valor. Em ordens de venda (`SELL`), validar `quantity - reserved_quantity >= ordem.quantity`.
* **Limite de Risco:** Verificar se o volume financeiro da ordem não ultrapassa o `daily_risk_limit` da conta.


* Implementar endpoints RESTful:
* `POST /api/v1/orders` (Criação)
* `GET /api/v1/orders` e `GET /api/v1/orders/{id}` (Consulta com paginação e filtros)
* `PATCH /api/v1/orders/{id}` (Atualização permitida apenas para ordens em estado elegível, como ajuste de preço limite em `PENDING`)
* `DELETE /api/v1/orders/{id}` ou `POST /api/v1/orders/{id}/cancel` (Cancelamento com estorno de saldo/posição reservada)



### Fase 4: Tratamento Seguro de Exceções e Validação

* Aplicar anotações Jakarta Validation (`@NotNull`, `@Positive`, `@Pattern`, `@Size`) em todos os DTOs de requisição.
* Criar `@RestControllerAdvice` centralizado mapeando exceções de negócio (`InsufficientBalanceException`, `RiskLimitExceededException`, `InvalidOrderStateException`, `DuplicateOrderException`) para respostas padronizadas **RFC 7807 (`ProblemDetail`)**, garantindo ausência de vazamento de detalhes internos (`<<no down-flow>>`).

### Fase 5: Suíte de Testes Automatizados

* **Testes Unitários (JUnit 5 + Mockito):**
* Cobertura completa da máquina de estados da classe `Order` (todas as transições válidas e tentativas inválidas).
* Testes isolados para cada implementação de `RiskRule` (saldo insuficiente, posição descoberta, estouro de limite de risco).
* Testes de validação de MFA e idempotência.


* **Testes de Integração (Spring Boot Test + Testcontainers MySQL):**
* Ciclo completo de criação, consulta, atualização e cancelamento via HTTP, verificando persistência no MySQL, bloqueio/estorno de saldo e geração dos registros correspondentes na tabela `audit_logs`.



---

## 6. Critérios de Aceite (Checklist de Entrega)

* [ ] Ambiente sobe integralmente com um único comando (`docker compose up --build`).
* [ ] Swagger UI acessível e documentando todos os schemas, códigos HTTP e requisitos de autenticação Bearer + header de idempotência.
* [ ] Diagramas UMLsec (Classes, Sequência do fluxo crítico de ordem e Implantação) entregues refletindo fielmente os estereótipos definidos.
* [ ] Tentativas de submeter ordens sem MFA validado, com saldo insuficiente, acima do limite de risco ou com `idempotency_key` repetida são bloqueadas e devidamente registradas em `audit_logs`.
* [ ] Todos os testes unitários e de integração executam e passam via `mvn test`.
