# Modelagem UMLsec — SentinelTrade (implementação)

> **Base:** `PLAN.md` (seção 2) e o código em `src/main/java/com/sentineltrade`.
>
> **Diagramas (UML-as-a-Code):**
> - Classes: [classes-umlsec.puml](./plantuml/classes-umlsec.puml)
> - Sequência do fluxo crítico de ordem: [sequencia-ordem-umlsec.puml](./plantuml/sequencia-ordem-umlsec.puml)
> - Implantação: [implantacao-umlsec.puml](./plantuml/implantacao-umlsec.puml)

---

## 1. Estereótipos e onde estão no código

| Estereótipo | Onde aparece | Implementação |
|---|---|---|
| `<<critical>>` `{secrecy}` | `User.passwordHash`, `User.mfaSecret` | BCrypt para a senha; `SecretConverter` cifra o segredo TOTP com AES-256-GCM no banco. |
| `<<critical>>` `{integrity}` | `Order`, `Account`, `Position` | Sem *setters* públicos; estado só muda por métodos de negócio; `@Version` (lock otimista); `CHECK` no SQL impede saldo/quantidade negativos. |
| `<<secure links>>` | `docker-compose.yml` | Rede `backend` com `internal: true`; a porta 3306 não é publicada; só a API fica exposta. TLS é ativado por `SERVER_SSL_*` fora do ambiente de desenvolvimento. |
| `<<role-based access>>` | `OrderController`, `AuditLogController` | `@PreAuthorize` com `ROLE_TRADER`, `ROLE_AUDITOR` e `ROLE_ADMIN`; um trader só enxerga as próprias ordens (as dos outros respondem 404). |
| `<<guarded access>>` | `SecurityConfig` | Todo `/api/v1/**` exige a autoridade `MFA_VERIFIED`, que só existe no token emitido depois do TOTP. |
| `<<no down-flow>>` / `<<data security>>` | `GlobalExceptionHandler`, DTOs | Respostas RFC 7807 sem *stack trace*; a API só expõe DTOs; o padrão de log mascara `password=`, `secret=`, `token=` e `code=`. |
| `<<audit>>` / `<<fair exchange>>` | `AuditService`, `OrderService` | Cada transição de ordem grava o `audit_logs` na mesma transação ACID; o repositório só tem `save`/`find`, e *triggers* MySQL bloqueiam `UPDATE`/`DELETE`. |

---

## 2. Decisões em relação aos documentos de análise

- **Máquina de estados:** segue os 11 estados de `uml-estados.md` (nomes em inglês no código: `CREATED`, `VALIDATED`, `SENT`, `OPEN`, `PENDING_RECONCILIATION`, `PARTIALLY_EXECUTED`, `CANCEL_REQUESTED`, `EXECUTED`, `REJECTED`, `CANCELLED`, `EXPIRED`).
- **Bolsa e cotações simuladas** em memória (`SimulatedExchangeAdapter`, `SimulatedMarketQuoteAdapter`) atrás das portas `ExchangeGateway` e `MarketQuoteProvider`.
- **Sem broker/outbox:** a chamada à bolsa acontece entre duas transações curtas. Se a resposta se perde, a ordem vai para `PENDING_RECONCILIATION` e o `OrderReconciler` consulta a bolsa pelo ID, sem reenviar.
- **Alteração (`PATCH`)** só para ordem limitada `OPEN` sem execuções; a reserva é recalculada na mesma transação.
- **Banco:** MySQL (conforme `PLAN.md`), e não PostgreSQL como no diagrama de implantação original.
