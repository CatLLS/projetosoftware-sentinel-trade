# Tabela de Casos de Uso — SentinelTrade

> **Base:** `MCU.md` (diagrama e descrições expandidas) e `requisitos.md`
>
> **Referência:** BEZERRA, Eduardo. *Princípios de Análise e Projeto de Sistemas com UML*, cap. 4.
>
> **Convenções:** prioridade MoSCoW (M = Must, S = Should), herdada dos requisitos rastreados. Os casos UC12–UC16 não têm ator primário próprio, porque só acontecem dentro de outro caso de uso (`«include»` ou `«extend»`).

---

## 1. Visão geral

| ID | Caso de uso | Ator primário | Ator(es) secundário(s) | Objetivo (sumário) | Relações | Requisitos | Prior. |
|---|---|---|---|---|---|---|---|
| UC01 | Autenticar com MFA | Investidor (também todos os funcionários) | — | Entrar no sistema com senha e segundo fator; pedir novo fator em ações sensíveis. | — | RF-03, RF-06, RNF-SEG-01/02 | M |
| UC02 | Gerenciar dispositivos e sessões | Investidor | — | Ver onde a conta está conectada e encerrar sessões suspeitas. | — | RF-04 | M |
| UC03 | Definir perfil de investidor | Investidor | — | Responder ao questionário de suitability e obter o perfil vigente. | — | RF-02 | S |
| UC04 | Consultar cotações | Investidor | — | Ver preço, variação e horário da última atualização; montar lista de acompanhamento. | — | RF-09, RF-10 | M |
| UC05 | Consultar carteira e saldo | Investidor | — | Ver saldo disponível e bloqueado, posições, preço médio e resultado. | — | RF-07, RF-08 | M |
| UC06 | Enviar ordem | Investidor | — | Comprar ou vender um ativo com confirmação e proteção contra duplicidade. | inclui UC13, UC14; estendido por UC12 | RF-12, RF-13, RF-16, RF-20 | M |
| UC07 | Alterar ordem | Investidor | — | Mudar preço ou quantidade de uma ordem ainda aberta. | inclui UC13, UC14 | RF-18 | S |
| UC08 | Cancelar ordem | Investidor | — | Pedir o cancelamento de uma ordem aberta ou parcialmente executada. | inclui UC14 | RF-17 | M |
| UC09 | Acompanhar status da ordem | Investidor | — | Ver o estado atual e a linha do tempo da ordem, com execuções parciais. | — | RF-19 | M |
| UC10 | Consultar histórico de operações | Investidor, Suporte | — | Filtrar e exportar ordens e execuções passadas (o suporte vê dados mascarados). | estendido por UC15 | RF-26, RNF-SEG-12 | M |
| UC11 | Configurar notificações | Investidor | — | Escolher canais e tipos de aviso; alertas de segurança não podem ser desligados. | — | RF-25 | S |
| UC12 | Confirmar operação fora do perfil | — (extensão) | — | Registrar a ciência do investidor quando a ordem não combina com o perfil. | estende UC06 | RF-15 | S |
| UC13 | Validar ordem | — (incluído) | — | Checar saldo, posição, limites, ativo, horário, lote, tick e perfil. | incluído por UC06, UC07, UC19 | RF-14, RF-16, RF-33 | M |
| UC14 | Transmitir à bolsa | — (incluído) | Bolsa simulada | Levar a solicitação à bolsa sem duplicar e sem perder (fila, timeout, idempotência). | incluído por UC06, UC07, UC08, UC19 | RNF-RES-01 a 04 | M |
| UC15 | Emitir comprovante | — (extensão) | — | Gerar o comprovante de uma ordem com toda a linha do tempo e hash de verificação. | estende UC10 | RF-27 | S |
| UC16 | Notificar investidor | — (incluído) | — | Avisar sobre execução, rejeição, cancelamento, expiração, segurança e contingência. | incluído por UC25, UC27 | RF-23, RF-24 | M |
| UC17 | Atualizar cotações | Provedor de cotações | — | Receber cotações e marcar como desatualizadas as que pararem de chegar. | — | RF-09, RNF-DES-02 | M |
| UC18 | Manter investidores | Operador de mesa | — | Incluir, alterar, bloquear e inativar investidores (sem exclusão física). | — | RF-01 | M |
| UC19 | Registrar ordem em contingência | Operador de mesa | — | Enviar ordem em nome do cliente quando o canal digital está fora do ar. | inclui UC13, UC14 | RF-22 | S |
| UC20 | Monitorar ordens | Operador de mesa | Analista de risco | Ver ordens por estado, rejeições por motivo e pendências de reconciliação. | — | RF-31 | S |
| UC21 | Manter limites de risco | Analista de risco | — | Definir e alterar limites por conta e bloquear ativos. | — | RF-30 | M |
| UC22 | Consultar trilha de auditoria | Auditor | — | Pesquisar e exportar eventos de auditoria e verificar a integridade da cadeia. | — | RF-29, RNF-AUD-01 | M |
| UC23 | Ativar modo de contingência | Administrador | — | Colocar a plataforma em indisponibilidade segura e avisar os usuários. | — | RF-32, RNF-RES-05 | M |
| UC24 | Manter ativos e calendário de mercado | Administrador | — | Cadastrar ativos negociáveis e as fases do pregão por dia. | — | RF-11, RF-33 | M |
| UC25 | Processar retorno da bolsa | Bolsa simulada | — | Aplicar aceites, execuções, rejeições e cancelamentos informados pela bolsa. | inclui UC16 | RF-19, RF-23 | M |
| UC26 | Reconciliar ordens | Tempo | Bolsa simulada | Resolver ordens com resultado incerto e detectar divergências. | — | RF-21 | M |
| UC27 | Expirar ordens vencidas | Tempo | — | Encerrar ordens cuja validade acabou e liberar as reservas. | inclui UC16 | RF-12 (validade) | M |

---

## 2. Pré-condições, pós-condições e fluxos resumidos

> Os casos UC06, UC14 e UC26 têm descrição expandida completa em `MCU.md` (seção 5). Esta tabela traz a versão resumida de todos.
> **Pré-condição comum** a todos os casos de uso de atores humanos, exceto UC01: usuário autenticado, com sessão válida.

| ID | Pré-condições | Fluxo principal (resumo) | Pós-condições (sucesso) | Exceções principais |
|---|---|---|---|---|
| UC01 | Usuário cadastrado e não bloqueado. | Informa e-mail e senha → sistema confere o hash → pede o 2º fator → valida → cria sessão. | Sessão ativa; evento de login auditado; aviso se o dispositivo é novo. | Senha ou fator inválido (bloqueio progressivo); conta bloqueada; recuperação de acesso com carência (RF-06). |
| UC02 | — | Lista sessões e dispositivos → usuário escolhe uma sessão → sistema a encerra. | Token da sessão invalidado na hora; evento auditado. | Tentativa de encerrar a sessão atual pede confirmação. |
| UC03 | Conta ativa. | Responde ao questionário → sistema calcula o perfil → mostra o resultado → usuário confirma. | Perfil e validade (24 meses) registrados. | Questionário incompleto: conta fica "sem perfil" e só pode consultar. |
| UC04 | — | Escolhe o ativo ou a lista → sistema mostra a cotação com horário. | — | Cotação desatualizada: aviso visível e ordem a mercado bloqueada. |
| UC05 | Conta ativa. | Abre a carteira → sistema mostra saldos e posições com resultado. | — | Dados de mercado indisponíveis: valores marcados com "última atualização às HH:MM". |
| UC06 | Conta ativa; perfil vigente; mercado aceitando ordens. | Preenche a ordem → revisa o resumo → confirma → sistema cria, valida (UC13), reserva e transmite (UC14). | Ordem com ID e número sequencial; saldo ou posição reservados; auditado. | Validação falhou (`REJEITADA`); modo de contingência ativo; requisição duplicada devolve a ordem existente. |
| UC07 | Ordem em `ABERTA`. | Altera preço/quantidade → sistema revalida (UC13) → transmite (UC14). | Ordem atualizada; valores antes e depois auditados. | Ordem já executada; nova validação falhou. |
| UC08 | Ordem em `ABERTA` ou `PARCIALMENTE_EXECUTADA`. | Pede cancelamento → ordem vai para `CANCELAMENTO_SOLICITADO` → transmite (UC14). | Pedido registrado; estado final vem da bolsa (UC25). | Executou antes do cancelamento: termina em `EXECUTADA` e o investidor é avisado. |
| UC09 | Existe ao menos uma ordem. | Escolhe a ordem → sistema mostra estado, linha do tempo e execuções. | — | — |
| UC10 | Investidor: dono da conta. Suporte: perfil `SUPORTE`. | Filtra por período, ativo, tipo e estado → sistema lista → opcionalmente exporta. | Consulta do suporte auditada. | Ponto de extensão: emitir comprovante (UC15). |
| UC11 | — | Liga/desliga tipos e canais → sistema salva. | Preferências atualizadas. | Tentativa de desligar alerta de segurança é recusada. |
| UC12 | Ordem fora do perfil durante UC06. | Sistema mostra o aviso de inadequação → investidor aceita. | Aceite gravado e ligado à ordem. | Investidor recusa: a ordem não é criada. |
| UC13 | Ordem em `CRIADA`. | Verifica saldo/posição, limite, ativo, fase do mercado, lote/tick e perfil. | Ordem `VALIDADA`; reserva feita. | Qualquer regra falha: `REJEITADA` com código e mensagem simples. |
| UC14 | Ordem `VALIDADA`, ou pedido de alteração/cancelamento. | Publica na fila (outbox) → envia com ID da ordem → bolsa confirma. | Ordem `ENVIADA`/`ABERTA`. | Recusa da bolsa; timeout → `PENDENTE_RECONCILIACAO`; disjuntor aberto → fila e contingência. |
| UC15 | Ordem selecionada no histórico. | Sistema monta o comprovante a partir da auditoria → usuário baixa. | Comprovante com hash de verificação. | — |
| UC16 | Evento que exige aviso. | Aplica as preferências (UC11) → monta a mensagem → envia. | Notificação registrada com status de envio. | Falha no canal: nova tentativa; mensagem nunca pede senha ou código. |
| UC17 | Conexão com o provedor ativa. | Recebe a cotação → atualiza o último preço e o horário. | Cotação atual disponível em até 1 s. | Sem atualização por mais de 5 s: cotação marcada como desatualizada. |
| UC18 | Perfil `OPERADOR`. | Inclui / altera / bloqueia / inativa investidor. | Cadastro atualizado e auditado. | Exclusão de investidor com ordens é recusada; sugere inativação. |
| UC19 | Perfil `OPERADOR`; canal digital indisponível ou cliente pediu pela mesa. | Identifica o cliente → registra a ordem → valida (UC13) → transmite (UC14). | Ordem com `canal=MESA` e emissor = operador. | Mesmas da UC13 e UC14. |
| UC20 | Perfil `OPERADOR` ou `RISCO`. | Abre o painel → filtra por estado, cliente ou ativo. | — | Destaque automático de ordens paradas há mais de N minutos. |
| UC21 | Perfil `RISCO`. | Escolhe a conta ou o ativo → define o limite ou o bloqueio. | Limite vigente para novas ordens; valor anterior e novo auditados. | Alteração sensível exige aprovação de um segundo analista (RNF-SEG-10). |
| UC22 | Perfil `AUDITOR`. | Filtra por usuário, ordem, período ou tipo → exporta → verifica integridade. | Consulta auditada. | Quebra na cadeia de hashes gera alerta imediato. |
| UC23 | Perfil `ADMIN`, ou disparo automático pelo disjuntor. | Ativa o modo → sistema recusa novas ordens e notifica (UC16). | Plataforma em modo seguro; consultas continuam funcionando. | — |
| UC24 | Perfil `ADMIN`. | Cadastra ou altera ativos e fases do pregão. | Regras usadas pela UC13 atualizadas. | Mudança de ativo com ordens abertas exige confirmação. |
| UC25 | Mensagem válida da bolsa. | Localiza a ordem pelo ID → aplica a transição → atualiza posição e saldo → notifica (UC16). | Estado, posição e saldo consistentes; auditado. | Mensagem duplicada é ignorada (idempotência); ordem não encontrada vai para UC26. |
| UC26 | Ordens em `PENDENTE_RECONCILIACAO` ou horário da reconciliação diária. | Consulta cada ordem na bolsa → confirma ou rejeita → libera reservas. | Nenhuma ordem pendente além de 5 min. | Divergência sem solução automática: alerta no UC20. |
| UC27 | Fim da validade da ordem. | Identifica as ordens vencidas → marca como `EXPIRADA` → libera a reserva → notifica (UC16). | Reservas liberadas; auditado. | Ordem executada no mesmo instante: prevalece a execução. |

---

## 3. Matriz atores × casos de uso

✔ = ator primário · ○ = ator secundário

| Caso de uso | Investidor | Suporte | Operador | Risco | Auditor | Admin | Bolsa | Provedor | Tempo |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| UC01–UC09, UC11 | ✔ | | | | | | | | |
| UC10 Consultar histórico | ✔ | ✔ | | | | | | | |
| UC14 Transmitir à bolsa | | | | | | | ○ | | |
| UC17 Atualizar cotações | | | | | | | | ✔ | |
| UC18 Manter investidores | | | ✔ | | | | | | |
| UC19 Ordem em contingência | | | ✔ | | | | | | |
| UC20 Monitorar ordens | | | ✔ | ○ | | | | | |
| UC21 Manter limites | | | | ✔ | | | | | |
| UC22 Consultar auditoria | | | | | ✔ | | | | |
| UC23 Modo de contingência | | | | | | ✔ | | | |
| UC24 Ativos e calendário | | | | | | ✔ | | | |
| UC25 Retorno da bolsa | | | | | | | ✔ | | |
| UC26 Reconciliar ordens | | | | | | | ○ | | ✔ |
| UC27 Expirar ordens | | | | | | | | | ✔ |

A matriz mostra a **segregação de funções** (RNF-SEG-10): nenhum papel interno opera a conta do cliente pelo canal normal, e o Auditor só tem acesso de leitura.
