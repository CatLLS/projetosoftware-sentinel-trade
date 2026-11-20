# Roteiro de Apresentação — SentinelTrade (~10 Minutos)

> **Duração estimada:** ~10 minutos (aprox. 1.450 palavras em ritmo natural de fala: 140–150 palavras/minuto).
> **Objetivo:** Explicar de ponta a ponta o núcleo arquitetural, a modelagem UML/UMLsec, as decisões de domínio e segurança e a implementação/testes da plataforma **SentinelTrade (Orion Capital)**, referenciando explicitamente os arquivos de cada tópico.

---

## Sumário de Tópicos e Tempo

1. **[00:00 – 01:15] Tópico 1 — Contexto de Mercado, Personas, Dores Reais e Regulação** (1 min 15 s)
2. **[01:15 – 02:30] Tópico 2 — Engenharia de Requisitos e Modelo de Casos de Uso** (1 min 15 s)
3. **[02:30 – 04:15] Tópico 3 — Modelagem UML: Domínio, Análise BCE, Interações, Estados e Arquitetura** (1 min 45 s)
4. **[04:15 – 05:45] Tópico 4 — Segurança desde o Design: Perfil UMLsec e Infraestrutura Isolada** (1 min 30 s)
5. **[05:45 – 07:00] Tópico 5 — Autenticação MFA em Duas Etapas (TOTP), Criptografia e RBAC** (1 min 15 s)
6. **[07:00 – 08:45] Tópico 6 — Núcleo Financeiro: Domínio, Risco Pré-Trade, Idempotência e Reconciliação** (1 min 45 s)
7. **[08:45 – 10:00] Tópico 7 — Auditoria Imutável, Prevenção de Vazamento (`<<no down-flow>>`) e Suíte de Testes** (1 min 15 s)

---

## [00:00 – 01:15] Tópico 1 — Contexto de Mercado, Personas, Dores Reais e Regulação

### Arquivos deste tópico
- `README.md`
- `docs/evidencias/pesquisa-audiencia.md`

### Texto de Fala
"Olá a todos. Nesta apresentação vamos percorrer o núcleo do **SentinelTrade**, uma plataforma de negociação de ativos de renda variável — ações, ETFs e fundos imobiliários — projetada para a corretora fictícia **Orion Capital**.

Em vez de partir direto para código ou telas, nosso projeto começou com uma pesquisa profunda de audiência documentada em `docs/evidencias/pesquisa-audiencia.md`. Olhando para os dados reais da B3 e da ANBIMA, vemos mais de 6,4 milhões de contas de pessoas físicas movimentando cerca de 747 bilhões de reais por ano. Mas esse público não é homogêneo: convivem na mesma plataforma desde o **Lucas**, investidor iniciante que opera pelo celular com medo de errar um dígito ou não entender o status da ordem; passando pela **Dona Marta**, que representa o público 60+ — hoje detentor de 46% de todo o estoque de renda variável e alvo preferencial de engenharia social; até o **Rafael**, trader ativo que sofre com travamentos na abertura e no fechamento do pregão; e a **Juliana**, que busca transparência e adequação ao seu perfil.

Dessa pesquisa mapeamos dez dores críticas, de `D1` a `D10`, e quatro pilares regulatórios reais: a **Resolução CVM 35**, que exige numeração sequencial, controle pré-negociação de limites e rastreabilidade do emissor; a **Resolução CVM 30** de *suitability*; a **LGPD**; e o **Mecanismo de Ressarcimento de Prejuízos (MRP) da BSM/B3**, que indeniza falhas de execução mas rejeita 90% dos pedidos quando o investidor não consegue provar o que aconteceu. A conclusão central da nossa pesquisa foi clara: em uma plataforma de trade, **confiança é o produto**. O sistema precisa executar exatamente o que foi pedido, uma única vez, e ser capaz de provar cada passo depois."

---

## [01:15 – 02:30] Tópico 2 — Engenharia de Requisitos e Modelo de Casos de Uso

### Arquivos deste tópico
- `docs/requisitos/Requisitos.md`
- `docs/requisitos/Requisitos-Sec.md`
- `docs/uml/tabela-casos-de-uso.md`
- `docs/uml/uml-casos-de-uso.md`
- `docs/extra-e-refs-aula/assets/mcu.svg`

### Texto de Fala
"A partir dessas dores, estruturamos a engenharia de requisitos em `docs/requisitos/Requisitos.md` e `docs/requisitos/Requisitos-Sec.md`. Definimos 33 Requisitos Funcionais e 32 Requisitos Não Funcionais divididos em Segurança, Resiliência e Qualidade, todos amarrados por três princípios sistêmicos: primeiro, **a ordem é o centro do sistema**; segundo, **exatamente uma vez e provável depois**; e terceiro, **quando falhar, falhar de forma segura e transparente**, preferindo bloquear uma ordem que não pode ser validada a aceitar uma operação inconsistente.

No Modelo de Casos de Uso, detalhado em `docs/uml/uml-casos-de-uso.md` e `docs/uml/tabela-casos-de-uso.md` com o diagrama `mcu.svg`, mapeamos 27 casos de uso e 9 atores. Uma decisão arquitetural fundamental aqui foi representar os papéis internos da corretora — `Suporte`, `Operador de Mesa`, `Analista de Risco`, `Auditor` e `Administrador` — como atores separados, sem generalização artificial. Isso torna visível no próprio diagrama a **segregação de funções**: quem audita apenas lê a trilha de auditoria (`UC22`), quem dá suporte apenas consulta histórico mascarado (`UC10`), e toda ordem enviada — seja pelo app do investidor no `UC06`, seja na alteração `UC07` ou pela mesa em contingência no `UC19` — inclui obrigatoriamente os casos de uso `UC13 Validar Ordem` e `UC14 Transmitir à Bolsa`, além da reconciliação disparada pelo ator Tempo no `UC26`."

---

## [02:30 – 04:15] Tópico 3 — Modelagem UML: Domínio, Análise BCE, Interações, Estados e Arquitetura

### Arquivos deste tópico
- `docs/uml.md` (`docs/extra-e-refs-aula/assets/uml.svg`)
- `docs/modelo-classes-analise.md` (`docs/extra-e-refs-aula/plantuml/classes-analise.puml`, `docs/extra-e-refs-aula/assets/classes-analise.svg`)
- `docs/modelo-classes-projeto.md` (`docs/extra-e-refs-aula/plantuml/classes-projeto.puml`, `docs/extra-e-refs-aula/assets/classes-projeto.svg`)
- `docs/uml/uml-interacoes.md` (`docs/extra-e-refs-aula/plantuml/interacoes-ordem.puml`, `docs/extra-e-refs-aula/plantuml/interacoes-comunicacao.puml`)
- `docs/uml/uml-estados.md` (`docs/extra-e-refs-aula/plantuml/estados-ordem.puml`, `docs/extra-e-refs-aula/assets/estados-ordem.svg`)
- `docs/uml/uml-arquitetura.md` (`docs/extra-e-refs-aula/plantuml/arquitetura-sistema.puml`, `docs/extra-e-refs-aula/plantuml/arquitetura-implantacao.puml`)

### Texto de Fala
"Seguindo a metodologia de análise e projeto orientados a objetos do professor Eduardo Bezerra, evoluímos o modelo em cinco visões complementares.

Em `docs/uml.md` e `docs/modelo-classes-analise.md`, construímos o modelo de domínio e a decomposição **BCE** (*Boundary, Control, Entity*). Separamos claramente as fronteiras de contato externo, os controladores de coordenação e as entidades que guardam invariantes financeiras, como `Conta`, `Posicao`, `Ordem` e `TrilhaAuditoria`.

Em `docs/uml/uml-interacoes.md`, modelamos os diagramas de Sequência e de Comunicação para o fluxo crítico da ordem. Aplicamos a **Lei de Demeter** e a **Regra do Todo-Parte**: o controlador não manipula saldos diretamente; ele delega às operações de negócio das entidades, mantendo alta coesão e baixo acoplamento. Em `docs/modelo-classes-projeto.md`, refinamos esse desenho introduzindo Portas (interfaces do domínio) e Adaptadores de infraestrutura.

Um dos artefatos mais importantes do projeto está em `docs/uml/uml-estados.md`: a **Máquina de Estados da Ordem**, com **11 estados**. Além dos estados clássicos — `CRIADA`, `VALIDADA`, `ENVIADA`, `ABERTA`, `PARCIALMENTE_EXECUTADA`, `EXECUTADA`, `REJEITADA`, `CANCELADA` e `EXPIRADA` — introduzimos dois estados indispensáveis para a integridade financeira:
1. `CANCELAMENTO_SOLICITADO`: porque pedir cancelamento não significa que a ordem foi cancelada; se a execução na bolsa vencer a corrida, a ordem termina como `EXECUTADA`;
2. `PENDENTE_RECONCILIACAO`: quando ocorre *timeout* na comunicação com a bolsa, o sistema não sabe se a ordem chegou. Reenviar às cegas duplicaria a operação; por isso ela entra em reconciliação.

Por fim, em `docs/uml/uml-arquitetura.md`, consolidamos a arquitetura lógica em quatro camadas e seis subsistemas de domínio acíclicos, junto com a topologia física *multi-tier*."

---

## [04:15 – 05:45] Tópico 4 — Segurança desde o Design: Perfil UMLsec e Infraestrutura Isolada

### Arquivos deste tópico
- `PLAN.md`
- `docs/umlsec/umlsec.md`
- `docs/umlsec/plantuml/classes-umlsec.puml` (`docs/umlsec/assets/classes-umlsec.svg`)
- `docs/umlsec/plantuml/sequencia-ordem-umlsec.puml` (`docs/umlsec/assets/sequencia-ordem-umlsec.svg`)
- `docs/umlsec/plantuml/implantacao-umlsec.puml` (`docs/umlsec/assets/implantacao-umlsec.svg`)
- `Dockerfile`
- `docker-compose.yml`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V1__init_schema.sql`

### Texto de Fala
"Para transformar toda essa modelagem em software executável, definimos o plano em `PLAN.md` e aplicamos o perfil **UMLsec** documentado em `docs/umlsec/umlsec.md`, com seus três diagramas PlantUML e SVGs renderizados em `docs/umlsec/assets/`: classes, sequência e implantação.

Na infraestrutura, o estereótipo `<<secure links>>` está implementado diretamente em `docker-compose.yml` e `Dockerfile`. O container da aplicação Spring Boot é compilado em multi-stage build com JDK 21 e executa sobre uma imagem Alpine mínima com usuário não-root. O banco de dados MySQL 8.4 reside exclusivamente na rede bridge `backend`, configurada com `internal: true`, e **não publica a porta 3306** para o host. Apenas a API na porta 8080 é acessível externamente pela rede `frontend`.

No banco de dados, gerenciado deterministicamente pelo Flyway em `src/main/resources/db/migration/V1__init_schema.sql`, aplicamos os estereótipos `<<critical>> {integrity}` e `<<audit>>` no próprio DDL SQL:
- As tabelas `accounts`, `positions` e `orders` possuem cláusulas `CHECK` que tornam impossível no nível do SGBD gravar saldo disponível negativo, saldo reservado negativo ou quantidade executada maior que a quantidade da ordem;
- Todas possuem coluna `version` para *Optimistic Locking*;
- E a tabela `audit_logs` possui dois *triggers* nativos — `audit_logs_no_update` e `audit_logs_no_delete` — que abortam com erro SQLSTATE `45000` qualquer tentativa de alterar ou apagar um registro de auditoria."

---

## [05:45 – 07:00] Tópico 5 — Autenticação MFA em Duas Etapas (TOTP), Criptografia e RBAC

### Arquivos deste tópico
- `src/main/java/com/sentineltrade/domain/user/User.java`
- `src/main/java/com/sentineltrade/domain/user/Role.java`
- `src/main/java/com/sentineltrade/domain/user/MfaVerifier.java`
- `src/main/java/com/sentineltrade/application/auth/AuthService.java`
- `src/main/java/com/sentineltrade/infrastructure/security/SecurityConfig.java`
- `src/main/java/com/sentineltrade/infrastructure/security/TokenService.java`
- `src/main/java/com/sentineltrade/infrastructure/security/TotpMfaVerifier.java`
- `src/main/java/com/sentineltrade/infrastructure/security/SecretConverter.java`
- `src/main/java/com/sentineltrade/infrastructure/security/SecurityAuditHandler.java`
- `src/main/java/com/sentineltrade/api/AuthController.java`

### Texto de Fala
"Passando ao código da aplicação em `/src`, vamos ver como os estereótipos `<<critical>> {secrecy}`, `<<guarded access>>` e `<<role-based access>>` protegem o acesso.

Na entidade `User.java`, a senha nunca é guardada em texto claro — apenas o hash BCrypt. Já o segredo do segundo fator (`mfaSecret`) utiliza o conversor JPA `SecretConverter.java`, que cifra o valor em repouso no banco com **AES-256-GCM** e vetor de inicialização aleatório de 12 bytes a cada gravação, garantindo confidencialidade e autenticidade criptográfica.

O login em `AuthController.java` e `AuthService.java` ocorre obrigatoriamente em duas etapas:
1. No `POST /api/v1/auth/login`, validamos usuário e senha. Mesmo quando o usuário não existe, o `AuthService` executa a verificação BCrypt contra um `dummyHash` pré-computado, eliminando ataques de enumeração de usuários por diferença de tempo de resposta. Se a senha estiver correta, o `TokenService.java` emite um JWT curto contendo apenas a autoridade `PRE_AUTH`.
2. Como configurado em `SecurityConfig.java`, um token `PRE_AUTH` é bloqueado em todas as rotas de negócio — ele só dá permissão para chamar `POST /api/v1/auth/mfa/verify`. Nessa segunda chamada, o `TotpMfaVerifier.java` calcula o código TOTP padrão **RFC 6238** (HMAC-SHA1 em janelas de 30 segundos, tolerando ±1 janela de *clock drift*) e compara os códigos em tempo constante usando `MessageDigest.isEqual`.
3. Só após o TOTP válido o `TokenService` emite o JWT final com a autoridade `MFA_VERIFIED` e o papel do usuário (`ROLE_TRADER`, `ROLE_AUDITOR` ou `ROLE_ADMIN`). Qualquer tentativa de acessar `/api/v1/orders` sem `MFA_VERIFIED` é interceptada pelo `SecurityAuditHandler.java`, gravada na auditoria como `ACCESS_DENIED` e respondida com `403 Forbidden`."

---

## [07:00 – 08:45] Tópico 6 — Núcleo Financeiro: Domínio, Risco Pré-Trade, Idempotência e Reconciliação

### Arquivos deste tópico
- `src/main/java/com/sentineltrade/domain/order/Order.java`
- `src/main/java/com/sentineltrade/domain/order/LimitOrder.java`
- `src/main/java/com/sentineltrade/domain/order/MarketOrder.java`
- `src/main/java/com/sentineltrade/domain/order/OrderStatus.java`
- `src/main/java/com/sentineltrade/domain/account/Account.java`
- `src/main/java/com/sentineltrade/domain/account/Position.java`
- `src/main/java/com/sentineltrade/domain/account/Settlement.java`
- `src/main/java/com/sentineltrade/domain/risk/RiskRule.java`
- `src/main/java/com/sentineltrade/domain/risk/BalanceSufficiencyRule.java`
- `src/main/java/com/sentineltrade/domain/risk/AssetPositionRule.java`
- `src/main/java/com/sentineltrade/domain/risk/OrderExposureLimitRule.java`
- `src/main/java/com/sentineltrade/application/order/OrderService.java`
- `src/main/java/com/sentineltrade/application/order/PlaceOrderCommand.java`
- `src/main/java/com/sentineltrade/application/order/OrderReconciler.java`
- `src/main/java/com/sentineltrade/infrastructure/simulation/SimulatedExchangeAdapter.java`
- `src/main/java/com/sentineltrade/infrastructure/simulation/SimulatedMarketQuoteAdapter.java`

### Texto de Fala
"Chegamos ao coração do sistema: o domínio financeiro e a orquestração de ordens.

Em `Order.java`, aplicamos abstração, herança e polimorfismo com as subclasses `MarketOrder.java` e `LimitOrder.java`. Nenhum atributo financeiro ou de estado possui *setter* público. Toda mudança passa pelo método privado `transitionTo`, que consulta o grafo de transições permitidas em `OrderStatus.java`. Qualquer transição ilegal — como cancelar uma ordem já executada ou alterar o preço de uma ordem a mercado — lança `InvalidOrderStateException`. Da mesma forma, `Account.java` e `Position.java` encapsulam saldo disponível, saldo reservado, quantidade em custódia e cálculo de preço médio ponderado com `BigDecimal`, coordenados pelo serviço de domínio `Settlement.java`.

Quando uma ordem chega ao `OrderService.java`:
1. **Idempotência:** verificamos o header obrigatório `X-Idempotency-Key`. Se a chave já existir com o mesmo payload e mesma conta, devolvemos a ordem original com `200 OK` e auditamos `ORDER_DUPLICATE_REPLAYED`, sem nunca duplicar a operação. Se alguém reutilizar a chave com parâmetros diferentes, bloqueamos com `409 Conflict` e auditamos `ORDER_DUPLICATE_BLOCKED`.
2. **Transação ACID 1 (Validação e Reserva):** criamos a ordem e executamos o motor de risco pré-trade baseado no *Strategy Pattern* (`RiskRule.java`): `BalanceSufficiencyRule` valida saldo disponível em compras; `AssetPositionRule` valida posição livre em vendas; e `OrderExposureLimitRule` soma o volume negociado no dia e compara com o limite diário da conta. Se qualquer regra falhar, a ordem é gravada como `REJECTED` junto com seu log de auditoria na mesma transação. Se passar, o recurso é reservado atomicamente e a ordem avança para `VALIDATED` e `SENT`.
3. **Chamada à Bolsa Fora da Transação + Transação ACID 2:** para não segurar locks no banco durante I/O externo, chamamos a porta `ExchangeGateway` (`SimulatedExchangeAdapter.java`) entre duas transações curtas. Se a bolsa responder, abrimos a segunda transação para liquidar a execução ou liberar reservas. Se ocorrer *timeout* (`ExchangeTimeoutException`), a ordem vai para `PENDING_RECONCILIATION` mantendo o saldo reservado. Em background, o `OrderReconciler.java` consulta a bolsa periodicamente pelo ID da ordem (`exchange.query(orderId)`) e converge o estado real sem jamais reenviar a ordem."

---

## [08:45 – 10:00] Tópico 7 — Auditoria Imutável, Prevenção de Vazamento (`<<no down-flow>>`) e Suíte de Testes

### Arquivos deste tópico
- `src/main/java/com/sentineltrade/domain/audit/AuditLog.java`
- `src/main/java/com/sentineltrade/application/audit/AuditService.java`
- `src/main/java/com/sentineltrade/application/audit/AuditEvent.java`
- `src/main/java/com/sentineltrade/infrastructure/persistence/AuditLogRepository.java`
- `src/main/java/com/sentineltrade/api/OrderController.java`
- `src/main/java/com/sentineltrade/api/AuditLogController.java`
- `src/main/java/com/sentineltrade/api/CurrentUser.java`
- `src/main/java/com/sentineltrade/api/GlobalExceptionHandler.java`
- `src/test/java/com/sentineltrade/domain/order/OrderTest.java`
- `src/test/java/com/sentineltrade/domain/account/AccountTest.java`
- `src/test/java/com/sentineltrade/domain/account/PositionTest.java`
- `src/test/java/com/sentineltrade/domain/account/SettlementTest.java`
- `src/test/java/com/sentineltrade/domain/risk/RiskRulesTest.java`
- `src/test/java/com/sentineltrade/domain/user/UserAndAuditDomainTest.java`
- `src/test/java/com/sentineltrade/application/order/OrderServiceTest.java`
- `src/test/java/com/sentineltrade/application/auth/AuthServiceTest.java`
- `src/test/java/com/sentineltrade/application/audit/AuditServiceTest.java`
- `src/test/java/com/sentineltrade/infrastructure/security/TotpMfaVerifierTest.java`
- `src/test/java/com/sentineltrade/infrastructure/security/SecurityInfrastructureTest.java`
- `src/test/java/com/sentineltrade/infrastructure/simulation/SimulationAdaptersTest.java`
- `src/test/java/com/sentineltrade/infrastructure/seed/DemoDataSeederTest.java`
- `src/test/java/com/sentineltrade/api/ApiControllersAndExceptionHandlerTest.java`
- `src/test/java/com/sentineltrade/OrderFlowIntegrationTest.java`

### Texto de Fala
"Para fechar o ciclo de segurança e qualidade, implementamos os estereótipos `<<audit>>`, `<<fair exchange>>` e `<<no down-flow>>`.

Em `AuditService.java`, o listener de eventos roda sincronicamente dentro da mesma transação ACID da regra de negócio: uma mudança de estado na ordem e seu registro em `AuditLog.java` confirmam ou sofrem rollback juntos. Além disso, `AuditLogRepository.java` estende a interface base `Repository` do Spring Data expondo apenas `save` e consultas `find`, sem herdar `delete` de `JpaRepository`, e `AuditLogController.java` restringe a leitura aos perfis `AUDITOR` e `ADMIN`. Já em `OrderController.java`, se um trader tentar consultar, alterar ou cancelar o ID de uma ordem de outro cliente, o serviço retorna `404 Not Found` para não vazar sequer a existência do recurso.

No `GlobalExceptionHandler.java`, toda exceção de domínio, validação ou banco é traduzida para o formato padrão **RFC 7807 (`ProblemDetail`)**, suprimindo *stack traces* e detalhes internos de SQL, enquanto o padrão de log em `application.yml` mascara automaticamente chaves como `password=`, `secret=`, `token=` e `code=`.

Finalmente, validamos toda a plataforma com uma suíte completa de **mais de 100 testes automatizados** em `src/test/java/com/sentineltrade`, cobrindo 15 classes de teste:
- No **Domínio**, `OrderTest`, `AccountTest`, `PositionTest`, `SettlementTest`, `RiskRulesTest` e `UserAndAuditDomainTest` provam cada transição dos 11 estados, cálculos de preço médio e regras de risco;
- Na **Aplicação**, `OrderServiceTest`, `AuthServiceTest` e `AuditServiceTest` testam compras, vendas, idempotência, bloqueio de duplicatas, *timeouts*, reconciliação, MFA e auditoria;
- Na **Infraestrutura e API**, `TotpMfaVerifierTest`, `SecurityInfrastructureTest`, `SimulationAdaptersTest`, `DemoDataSeederTest` e `ApiControllersAndExceptionHandlerTest` verificam vetores RFC 6238, criptografia AES-256-GCM contra adulteração, JWT, motor de bolsa simulada e respostas RFC 7807;
- E no **Teste de Integração** (`OrderFlowIntegrationTest`), exercitamos o fluxo HTTP ponta a ponta contra um MySQL 8.4 real via Testcontainers.

Com isso, o SentinelTrade demonstra uma rastreabilidade completa: da dor real do investidor brasileiro na pesquisa de audiência, passando pelos diagramas UML e UMLsec, até cada linha de código e teste automatizado. Muito obrigado!"
