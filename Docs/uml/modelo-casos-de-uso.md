# Modelo de Casos de Uso (MCU) — SentinelTrade

> **Base:** `pesquisaAudiencia.md` e `requisitos.md`
>
> **Referência metodológica:** BEZERRA, Eduardo. *Princípios de Análise e Projeto de Sistemas com UML*, cap. 4 (Modelagem de casos de uso).
>
> **Arquivo do diagrama:** `./assets/mcu.svg`. Ele precisa estar na mesma pasta deste `.md` para a imagem aparecer.

---

## 1. Diagrama

![Diagrama de casos de uso do SentinelTrade](./assets/mcu.svg)

O diagrama mostra **27 casos de uso** e **9 atores** (6 humanos, 2 sistemas externos e o Tempo). Os casos de uso estão organizados em três colunas dentro da fronteira do sistema:

| Coluna | Conteúdo |
|---|---|
| Esquerda | O que o **investidor** faz diretamente: acesso, consulta e ordens. |
| Centro | Comportamento **reutilizado** (`«include»`) ou **opcional** (`«extend»`), mais os casos acionados por sistemas externos. |
| Direita | O que a **equipe da Orion** faz e o que é disparado por **eventos externos** (bolsa e tempo). |

---

## 2. Atores

Segundo Bezerra, ator é qualquer elemento **externo** ao sistema que interage com ele: uma pessoa, outro sistema, um equipamento ou uma organização. O ator representa um **papel**, não um indivíduo. Por isso as quatro personas da pesquisa (Lucas, Dona Marta, Rafael e Juliana) aparecem como **um único ator**, Investidor.

| Ator | Tipo | Papel no sistema | Origem na pesquisa |
|---|---|---|---|
| **Investidor** | Humano, primário | Consulta mercado e carteira, envia e acompanha ordens | Personas P1–P4 |
| **Suporte** | Humano, primário | Consulta histórico do cliente (dados mascarados) para atender reclamações | Stakeholder "Atendimento / Suporte" |
| **Operador de mesa** | Humano, primário | Mantém cadastro, monitora ordens, registra ordem em contingência | Stakeholder "Backoffice" + D1, D10 |
| **Analista de risco** | Humano, primário | Define limites e acompanha ordens bloqueadas | Stakeholder "Risco" + D7, D9 |
| **Auditor (compliance)** | Humano, primário | Consulta a trilha de auditoria | Stakeholder "Compliance" + D4, D10 |
| **Administrador** | Humano, primário | Ativa contingência e mantém ativos e calendário | Stakeholder "SRE / TI" + D1 |
| **Bolsa simulada** | Sistema externo | Recebe ordens, devolve aceites, execuções e rejeições | Briefing ("integrar-se a uma Bolsa simulada") |
| **Provedor de cotações** | Sistema externo | Envia cotações em tempo quase real | Briefing ("provedor externo") |
| **Tempo** | Ator temporal | Dispara tarefas agendadas (reconciliação e expiração) | RF-21, RNF-RES-06 |

---

## 3. Lista de casos de uso

| ID | Caso de uso | Ator primário | Ator(es) secundário(s) | Requisitos |
|---|---|---|---|---|
| UC01 | Autenticar com MFA | Investidor | — | RF-03 |
| UC02 | Gerenciar dispositivos e sessões | Investidor | — | RF-04 |
| UC03 | Definir perfil de investidor | Investidor | — | RF-02 |
| UC04 | Consultar cotações | Investidor | — | RF-09, RF-10 |
| UC05 | Consultar carteira e saldo | Investidor | — | RF-07, RF-08 |
| UC06 | Enviar ordem | Investidor | — | RF-12, RF-13, RF-20 |
| UC07 | Alterar ordem | Investidor | — | RF-18 |
| UC08 | Cancelar ordem | Investidor | — | RF-17 |
| UC09 | Acompanhar status da ordem | Investidor | — | RF-19 |
| UC10 | Consultar histórico de operações | Investidor | Suporte (também primário) | RF-26 |
| UC11 | Configurar notificações | Investidor | — | RF-25 |
| UC12 | Confirmar operação fora do perfil | (extensão de UC06) | — | RF-15 |
| UC13 | Validar ordem | (incluído) | — | RF-14, RF-16, RF-33 |
| UC14 | Transmitir à bolsa | (incluído) | Bolsa simulada | RNF-RES-01..04 |
| UC15 | Emitir comprovante | (extensão de UC10) | — | RF-27 |
| UC16 | Notificar investidor | (incluído) | — | RF-23, RF-24 |
| UC17 | Atualizar cotações | Provedor de cotações | — | RF-09, RNF-DES-02 |
| UC18 | Manter investidores | Operador de mesa | — | RF-01 |
| UC19 | Registrar ordem em contingência | Operador de mesa | — | RF-22 |
| UC20 | Monitorar ordens | Operador de mesa | Analista de risco | RF-31 |
| UC21 | Manter limites de risco | Analista de risco | — | RF-30 |
| UC22 | Consultar trilha de auditoria | Auditor | — | RF-29 |
| UC23 | Ativar modo de contingência | Administrador | — | RF-32, RNF-RES-05 |
| UC24 | Manter ativos e calendário de mercado | Administrador | — | RF-11, RF-33 |
| UC25 | Processar retorno da bolsa | Bolsa simulada | — | RF-19, RF-23 |
| UC26 | Reconciliar ordens | Tempo | Bolsa simulada | RF-21 |
| UC27 | Expirar ordens vencidas | Tempo | — | RF-12 (validade) |

---

## 4. Relacionamentos entre casos de uso

| Relação | De → Para | Por que esse tipo de relação |
|---|---|---|
| `«include»` | UC06 Enviar → UC13 Validar | A validação sempre acontece e é **obrigatória**. Nenhuma ordem segue sem ela. |
| `«include»` | UC07 Alterar → UC13 Validar | Alterar preço ou quantidade muda o risco, então a ordem é validada de novo. |
| `«include»` | UC19 Contingência → UC13 Validar | A ordem da mesa obedece às **mesmas** regras da ordem do app. A contingência não é um atalho para burlar limites. |
| `«include»` | UC06, UC07, UC08, UC19 → UC14 Transmitir | Todo envio, alteração ou cancelamento passa pelo mesmo caminho até a bolsa, com timeout, idempotência e fila. |
| `«include»` | UC25 Retorno → UC16 Notificar | Toda execução, rejeição ou cancelamento confirmado gera aviso (RF-23). |
| `«include»` | UC27 Expirar → UC16 Notificar | O investidor precisa saber que a ordem expirou. |
| `«extend»` | UC12 Fora do perfil → UC06 Enviar | Só acontece **sob condição**: quando a ordem não é adequada ao perfil. O ponto de extensão está anotado no diagrama. |
| `«extend»` | UC15 Comprovante → UC10 Histórico | É **opcional**: o usuário consulta o histórico e, se quiser, gera o comprovante de uma ordem. |

A regra usada para escolher entre os dois foi a distinção que Bezerra faz:

- **`«include»`** quando o comportamento é **sempre executado** e se repete em mais de um caso de uso. A inclusão evita repetir a mesma descrição em vários lugares.
- **`«extend»`** quando o comportamento é **opcional ou condicional** e o caso base faz sentido sem ele. O caso base não "sabe" da extensão; ele só expõe um ponto de extensão.

---

## 5. Descrições dos casos de uso críticos

Bezerra destaca que o diagrama sozinho não define o comportamento. O **MCU é o diagrama mais as descrições textuais**. Abaixo estão as descrições expandidas dos três casos de uso de maior risco: os que tocam diretamente as dores D1, D2 e D7. Os demais seguem o mesmo modelo e podem ser descritos na próxima etapa.

### UC06 — Enviar ordem

| Campo | Conteúdo |
|---|---|
| **Identificador** | UC06 |
| **Importância** | Risco alto, prioridade alta |
| **Sumário** | O investidor envia uma ordem de compra ou venda de um ativo. |
| **Ator primário** | Investidor |
| **Atores secundários** | Bolsa simulada (via UC14) |
| **Precondições** | Investidor autenticado (UC01) com sessão válida; conta `ATIVA`; perfil de investidor vigente. |
| **Pós-condições (sucesso)** | Ordem registrada com ID único e número sequencial; saldo ou posição reservados; ordem `ENVIADA` ou na fila para envio; evento gravado na auditoria. |
| **Regras de negócio** | RF-14 (validações), RF-16 (reserva), RF-20 (idempotência), RF-33 (fases do mercado). |

**Fluxo principal**
1. O investidor escolhe o ativo, o lado (compra/venda), o tipo (mercado/limitada), a quantidade, o preço (se limitada) e a validade.
2. O sistema mostra o resumo da ordem: valor estimado e saldo resultante (RF-13).
3. O investidor confirma. O app envia a ordem junto com uma **chave de idempotência**.
4. O sistema registra a ordem como `CRIADA`.
5. O sistema executa **UC13 Validar ordem**.
6. O sistema reserva o saldo (compra) ou a quantidade (venda).
7. O sistema muda a ordem para `VALIDADA` e informa o investidor.
8. O sistema executa **UC14 Transmitir à bolsa**.
9. O caso de uso termina. O acompanhamento continua no UC09.

**Fluxos alternativos**
- **A1 — Preço ou quantidade fora do padrão (passo 2):** o sistema pede **confirmação reforçada**. Se o investidor desistir, o caso termina sem criar a ordem.
- **A2 — Operação fora do perfil (passo 5):** ponto de extensão. Executa **UC12**. Se o investidor aceitar, o aceite é registrado e o fluxo volta ao passo 6. Se recusar, o caso termina sem ordem.
- **A3 — Requisição repetida (passo 3):** chega uma requisição com a mesma chave de idempotência. O sistema **devolve a ordem já existente** e não cria outra.

**Fluxos de exceção**
- **E1 — Validação falhou (passo 5):** a ordem vai para `REJEITADA` com código e mensagem em linguagem simples (ex.: "Saldo insuficiente: faltam R$ 120,00"). Nada é reservado.
- **E2 — Modo de contingência ativo (passo 4):** o sistema recusa a ordem, explica o motivo e mostra o canal alternativo (mesa de operações).
- **E3 — Cotação desatualizada e ordem a mercado (passo 1):** o sistema impede o envio e sugere ordem limitada (RF-10).

### UC14 — Transmitir à bolsa (incluído)

| Campo | Conteúdo |
|---|---|
| **Sumário** | Leva uma ordem validada, uma alteração ou um cancelamento até a bolsa simulada, sem duplicar e sem perder. |
| **Ator secundário** | Bolsa simulada |
| **Precondições** | Ordem em `VALIDADA` (envio) ou em `ABERTA`/`PARCIALMENTE_EXECUTADA` (alteração ou cancelamento). |
| **Pós-condições** | Ordem em `ENVIADA`/`ABERTA`, ou em `PENDENTE_RECONCILIACAO` se o resultado for incerto. |

**Fluxo principal**
1. O sistema publica a solicitação em uma fila durável, junto com o registro da ordem (padrão *outbox*).
2. O sistema envia a solicitação à bolsa usando o ID da ordem como identificador do cliente.
3. A bolsa confirma o recebimento dentro do *timeout*.
4. O sistema muda a ordem para `ENVIADA`/`ABERTA` e registra o evento.

**Fluxos de exceção**
- **E1 — Bolsa recusa:** a ordem vai para `REJEITADA`, a reserva é liberada e o sistema executa UC16.
- **E2 — Timeout sem resposta:** a ordem **não é reenviada às cegas**. Ela vai para `PENDENTE_RECONCILIACAO` e o UC26 resolve depois.
- **E3 — Disjuntor aberto (bolsa indisponível):** a solicitação fica na fila. Se a indisponibilidade passar do limite, o sistema entra em modo de contingência (UC23).

### UC26 — Reconciliar ordens

| Campo | Conteúdo |
|---|---|
| **Sumário** | Periodicamente, compara as ordens e execuções internas com as da bolsa e resolve as incertezas. |
| **Ator primário** | Tempo |
| **Ator secundário** | Bolsa simulada |
| **Precondições** | Existe ao menos uma ordem em `PENDENTE_RECONCILIACAO`, ou é o horário da reconciliação diária. |

**Fluxo principal**
1. O sistema lista as ordens pendentes de reconciliação.
2. Para cada uma, o sistema consulta a bolsa pelo ID da ordem.
3. Se a ordem existir na bolsa, o sistema a atualiza para o estado informado (`ABERTA`, `EXECUTADA`...).
4. Se a ordem não existir, o sistema a marca como `REJEITADA` e libera a reserva.
5. O sistema registra cada decisão na auditoria e notifica o investidor quando o estado muda.

**Fluxo de exceção**
- **E1 — Divergência que o sistema não resolve sozinho** (ex.: execução na bolsa sem ordem correspondente): o sistema abre um alerta no UC20 para o operador.

---

## 6. Justificativa das decisões de modelagem

### 6.1 Um único ator "Investidor" para as quatro personas
As personas têm necessidades diferentes, mas **interagem com o sistema da mesma forma**: os mesmos casos de uso e as mesmas permissões. O que muda entre elas é **como** o sistema deve se comportar (latência para o Rafael, acessibilidade para a Dona Marta, clareza para o Lucas). Essas diferenças são requisitos não funcionais, não atores diferentes. Criar um ator "Trader" separado, por exemplo, sugeriria permissões que não existem.

### 6.2 Atores internos separados, sem generalização
Seria possível criar um ator abstrato "Funcionário Orion" e fazer Suporte, Operador, Risco, Auditor e Admin herdarem dele. **Não fizemos isso** porque os cinco não compartilham nenhum caso de uso, exceto Operador e Risco em UC20. A generalização só se justifica quando os filhos herdam interações do pai. Aqui ela só deixaria o diagrama mais pesado e esconderia a **segregação de funções** (RNF-SEG-10): quem audita não opera, e quem administra não vê dados financeiros. Ver cada papel ligado só aos seus casos de uso torna essa separação visível.

### 6.3 Suporte ligado apenas a "Consultar histórico"
O suporte precisa explicar ao cliente o que aconteceu, mas **não pode operar** a conta (RF-05). Associá-lo a um único caso de uso, de leitura, comunica essa restrição no próprio diagrama. O mascaramento de dados (RNF-SEG-12) fica na descrição do UC10.

### 6.4 Sistemas externos e Tempo como atores
- **Bolsa simulada** e **Provedor de cotações** estão fora da fronteira porque o SentinelTrade não os controla. É exatamente por isso que precisam de timeout, retentativa e disjuntor. Modelá-los como atores deixa claro onde ficam as integrações frágeis.
- A Bolsa é **ator primário** no UC25 (ela inicia a interação ao devolver uma execução) e **secundário** no UC14 e no UC26 (o SentinelTrade inicia).
- **Tempo** foi usado como ator para os casos disparados por agenda (UC26, UC27). Sem ele, esses casos de uso ficariam sem ninguém que os iniciasse e pareceriam "soltos" no diagrama. Eles existem justamente para resolver as dores D2 e D10.

### 6.5 "Validar ordem" como caso de uso incluído
A validação é o **coração do controle de risco** (RF-14) e aparece em três lugares: envio, alteração e contingência. Torná-la um caso de uso incluído:
- evita descrever a mesma regra três vezes;
- **deixa explícito que a mesa de operações passa pela mesma validação** que o app. Isso responde a uma preocupação regulatória: a contingência não pode virar uma porta para ignorar limites.

### 6.6 "Transmitir à bolsa" como caso de uso incluído
Envio, alteração, cancelamento e contingência chegam à bolsa pelo **mesmo caminho**, onde ficam os mecanismos de resiliência (timeout, idempotência, fila e reconciliação). Isolar esse caminho em um caso de uso mostra que a proteção contra duplicidade (dor D2) vale para todas as operações, não só para o envio.

### 6.7 Operação fora do perfil como extensão
A confirmação fora do perfil **não acontece sempre**: só quando a ordem é inadequada ao perfil (CVM 30). O caso UC06 é completo sem ela. Esse é o cenário típico de `«extend»`, com o ponto de extensão anotado no diagrama.

### 6.8 Comprovante como extensão do histórico
O comprovante (RF-27) é uma ação **opcional** a partir do histórico. Ele não ganhou ligação direta com o ator porque o usuário sempre chega a ele pelo histórico. Assim fica claro que o comprovante é uma visão detalhada de uma ordem já consultada.

### 6.9 "Notificar investidor" como caso incluído, sem ligação direta ao ator
A notificação é **consequência** de outros eventos (execução, rejeição, expiração), não algo que o investidor inicia. Por isso ela é incluída pelos casos que a disparam (UC25 e UC27). As notificações de segurança (login em dispositivo novo) e de contingência estão descritas no UC01 e no UC23. Não desenhamos essas ligações para não poluir o diagrama. Ligar o Investidor diretamente ao UC16 daria a impressão de que ele "pede" notificações, quando na verdade quem decide quais notificações recebe é o UC11.

### 6.10 Auditoria não aparece como caso de uso de registro
O requisito RF-28 (registrar auditoria) **não virou caso de uso**. Registrar log não é um objetivo de nenhum ator: é um comportamento transversal, que acontece dentro de quase todos os casos de uso. Se fosse desenhado, teria `«include»` partindo de 20 casos de uso e o diagrama ficaria ilegível sem informação nova. A auditoria aparece de duas formas:
- como **caso de uso de consulta** (UC22), que é um objetivo real do Auditor;
- como **pós-condição** nas descrições ("evento gravado na auditoria").

No modelo de classes ela aparece como entidade própria (`TrilhaAuditoria`).

### 6.11 Autenticação como precondição, não como `«include»`
Todos os casos de uso do investidor exigem login, mas **não** foram ligados ao UC01 por `«include»`. Autenticar é um objetivo próprio do usuário, que acontece uma vez por sessão, e não um passo repetido dentro de cada operação. Por isso ele aparece como **precondição** nas descrições. As ações sensíveis que pedem um novo fator (RF-03) estão descritas no próprio UC01.

### 6.12 Casos de uso de cadastro agrupados em "Manter"
Cadastrar, alterar, consultar e inativar investidores **não** viraram quatro casos de uso. Seguindo a recomendação de agrupar operações CRUD de uma mesma entidade, eles foram reunidos em **UC18 Manter investidores**, e o mesmo vale para UC21 e UC24. As variações (incluir, alterar, inativar) ficam como fluxos alternativos na descrição. Isso mantém a granularidade no nível de **objetivos do ator**, e não de telas.

### 6.13 Modo de contingência como caso de uso próprio
A indisponibilidade segura poderia ficar só como requisito não funcional. Ela virou o caso de uso **UC23** porque a pesquisa mostra que a **comunicação** em incidentes é uma dor própria (D1): investidores ficam sem saber causa nem prazo. Existe, portanto, uma ação do Administrador com efeito visível para o usuário. O disparo automático pelo disjuntor está descrito no UC14 (E3).

### 6.14 Organização visual em três colunas
A disposição em colunas (investidor, comportamento compartilhado, operação/eventos) foi escolhida para que:
- as linhas de comunicação de cada grupo de atores não se cruzem;
- os relacionamentos `«include»` e `«extend»` fiquem concentrados no centro, onde é fácil ver o que é reaproveitado.

A numeração (UCxx) mantém a rastreabilidade com `requisitos.md`.

---

## 7. Cobertura dos requisitos funcionais

| Requisito | Caso(s) de uso | Requisito | Caso(s) de uso |
|---|---|---|---|
| RF-01 | UC18 | RF-18 | UC07 |
| RF-02 | UC03 | RF-19 | UC09, UC25 |
| RF-03 | UC01 | RF-20 | UC06 (A3), UC14 |
| RF-04 | UC02 | RF-21 | UC26 |
| RF-05 | todos (atores × casos de uso) | RF-22 | UC19 |
| RF-06 | UC01 (fluxo alternativo de recuperação) | RF-23 | UC16, UC25 |
| RF-07 | UC05 | RF-24 | UC16 (via UC01 e UC23) |
| RF-08 | UC05 | RF-25 | UC11 |
| RF-09 | UC04, UC17 | RF-26 | UC10 |
| RF-10 | UC04, UC06 (E3) | RF-27 | UC15 |
| RF-11 | UC24 | RF-28 | pós-condição de todos (ver 6.10) |
| RF-12 | UC06 | RF-29 | UC22 |
| RF-13 | UC06 (passo 2, A1) | RF-30 | UC21 |
| RF-14 | UC13 | RF-31 | UC20 |
| RF-15 | UC12 | RF-32 | UC23 |
| RF-16 | UC06 (passo 6), UC13 | RF-33 | UC24, UC13 |
| RF-17 | UC08 | | |

Todos os 33 requisitos funcionais estão cobertos por ao menos um caso de uso ou por uma descrição.

---

## 8. Próximos passos

- Escrever as descrições expandidas dos demais casos de uso, no mesmo formato da seção 5.
- Validar os fluxos de exceção do UC06 com a Orion, em especial os textos de mensagem ao usuário.
- Usar os substantivos das descrições como ponto de partida para o modelo de classes (`uml.md`).
