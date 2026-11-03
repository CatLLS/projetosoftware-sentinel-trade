# Pesquisa de Audiência — SentinelTrade (Orion Capital)

> **Objetivo:** entender quem vai usar (e quem será afetado por) uma plataforma de trade de mercado de capitais no Brasil, quais são as dores reais dessa audiência e que restrições regulatórias e de contexto moldam o produto. Esta pesquisa é a base para o documento `requisitos.md`.
>
> **Data da pesquisa:** outubro de 2026
> **Método:** pesquisa secundária (desk research) em dados oficiais da B3, ANBIMA/Datafolha, CVM, BSM e FINRA, estudos acadêmicos (FGV EESP), imprensa especializada e relatos públicos de investidores (Reclame Aqui, reportagens). Todas as fontes estão listadas na seção 9.
>
> **Limitação:** não houve entrevistas primárias com usuários. As personas abaixo são *proto-personas* derivadas de dados secundários e devem ser validadas com usuários reais da Orion Capital quando possível.

---

## Sumário

1. Contexto do mercado brasileiro
2. Quem é a audiência (dados quantitativos)
3. Segmentação e personas
4. Stakeholders internos e externos
5. Dores e problemas mapeados
6. Ambiente regulatório que afeta o produto
7. Jornada do investidor e momentos críticos
8. Conclusões e implicações para o produto
9. Fontes

---

## 1. Contexto do mercado brasileiro

O investidor pessoa física (PF) é hoje um agente relevante — não marginal — da bolsa brasileira:

- A B3 registrou cerca de **6,23 milhões de contas PF em custódia em 2025**, das quais aproximadamente 74% de homens e 26% de mulheres. A própria B3 alerta que o número conta CPFs por agente de custódia, ou seja, o mesmo investidor pode aparecer mais de uma vez se tiver conta em mais de uma corretora.
- Em julho de 2026, um relatório do Itaú BBA (via Exame) apontou **6,45 milhões de contas PF, maior nível em cinco anos**, contra um piso de 3,79 milhões cinco anos antes. Investidores individuais já detêm **19,5% das ações em circulação (free float)**, acima da média de 10 anos de 14,9%.
- Em 2025, investidores PF movimentaram **R$ 517,3 bilhões em ações** no mercado à vista da B3; incluindo BDRs, ETFs, FIIs e outros instrumentos, o volume chegou a **R$ 747,7 bilhões**.
- Na população geral, a ANBIMA/Datafolha (9ª edição do Raio X do Investidor) aponta que **36% dos brasileiros (60,6 milhões de pessoas) têm algum investimento**, embora a maior parte esteja fora da renda variável (poupança, renda fixa).

**Implicação:** o público de uma corretora digital é grande, heterogêneo e crescente. O sistema precisa suportar picos de carga e usuários com níveis de conhecimento muito diferentes.

---

## 2. Quem é a audiência (dados quantitativos)

### 2.1 Tamanho e engajamento

| Indicador | Valor | Fonte |
|---|---|---|
| Contas PF em custódia na B3 (2025) | ~6,23 milhões | B3 – Perfil de Investidores |
| Contas PF (jul/2026) | ~6,45 milhões | Exame / Itaú BBA |
| PF que operam ações ao menos 1x por mês | ~1,7 milhão (+6% a/a) | B3 via Traders Union |
| Investidores PF em ETFs | ~862 mil (+34% vs 2T25) | B3 via Traders Union |
| Investidores PF em FIIs | ~2,6 milhões (detêm ~76% do saldo de FIIs) | Book PF B3 1T2024 |
| Saldo **mediano** em ações por investidor | ~R$ 1,8 mil | Book PF B3 1T2024 |

**Leitura:** a maioria das contas tem **saldo pequeno** e **opera pouco**; uma minoria (≈1,7 mi) é ativa mensalmente. Isso indica dois padrões de uso muito diferentes coexistindo na mesma plataforma.

### 2.2 Perfil demográfico

- **Gênero:** a bolsa ainda é majoritariamente masculina (~74% das contas PF são de homens), enquanto na população investidora geral (incluindo renda fixa) as mulheres são 51,5% (ANBIMA 8ª ed.). Há espaço de crescimento do público feminino na renda variável.
- **Idade:**
  - Historicamente, metade dos novos entrantes na bolsa tinha entre **25 e 39 anos** (B3, 2021).
  - O público **60+** cresce rapidamente: 535 mil investidores em 2025 (+91% em 5 anos). São cerca de **10% dos investidores, mas detêm 46% de todo o estoque** em renda variável (R$ 292 bi).
  - A idade média do investidor brasileiro (todas as classes de ativo) é de **43 anos** (ANBIMA 9ª ed.).
- **Classe social e região (investidores em geral, ANBIMA 8ª ed.):** classe C é 47% dos investidores; Sudeste concentra 43%.
- **Ticket de entrada:** em 2021, a mediana do primeiro investimento em bolsa era de **R$ 352** — reforçando que o produto precisa funcionar bem para valores pequenos e frações.

### 2.3 Canal e comportamento digital

- Nos EUA, o uso de **aplicativo móvel para executar trades** subiu de 30% (2018) para 44% (2021) e 46% (2024) (FINRA Foundation). O Brasil segue tendência semelhante: o conhecimento de bancos e carteiras digitais saltou de 24% para 46% entre 2022 e 2025 (ANBIMA).
- A FINRA passou a vigiar especificamente apps com **recursos "gamificados"** que influenciam o comportamento do cliente — um alerta de design.

### 2.4 Contexto de risco comportamental

- Estudo da FGV EESP feito **a pedido da CVM**, com dados de todos os day traders de minicontratos entre 2012 e 2017: **92,1% desistiram em menos de um ano**, e entre os que persistiram por 300+ pregões, **97% perderam dinheiro**.
- Levantamento mais recente (Grana Capital, ago/2024–jul/2025, ~34 mil investidores): **~72% dos que fizeram day trade perderam dinheiro**.
- **17% da população fez apostas online (bets) em 2025** (ANBIMA), e parte da população confunde aposta com investimento.

### 2.5 Contexto de segurança

- **34% dos brasileiros** relatam já ter passado por ao menos uma situação de golpe ou fraude financeira (ANBIMA 9ª ed.).
- O Brasil é o **2º país do mundo em tentativas de golpes e fraudes bancárias**, atrás apenas da China (Painel de Fraudes Bancárias Digitais, MJSP/Febraban, dez/2025).
- Golpes predominantes envolvem **engenharia social**: falsa central, falso gerente, clonagem de WhatsApp para obter códigos e senhas (Febraban).

---

## 3. Segmentação e personas

A partir dos dados acima, a audiência de investidores foi dividida em quatro segmentos. Os nomes são fictícios.

### Persona 1 — **Lucas, o investidor iniciante de longo prazo**

| | |
|---|---|
| **Idade** | 27 anos, analista júnior, São Paulo |
| **Representa** | A maior parte das contas: saldo pequeno (~R$ 1–5 mil), opera poucas vezes ao mês, compra ETFs e ações "boas pagadoras" |
| **Canal** | Celular, quase exclusivamente |
| **Objetivo** | Construir patrimônio aos poucos, aportar todo mês |
| **Conhecimento** | Baixo a médio; aprende com vídeos e redes sociais |

**Dores:**
- Não entende bem o **status da ordem** ("enviada" é o mesmo que "executada"? Por que só comprou parte?).
- Medo de **errar a quantidade ou o preço** (digitar 1000 em vez de 100).
- Não sabe o que aconteceu quando a ordem é **rejeitada** — mensagens técnicas e genéricas.
- Recebe mensagens de golpe se passando pela corretora e não sabe distinguir notificações legítimas.
- Quer ver de forma simples **quanto tem e quanto ganhou/perdeu**.

**O que precisa do sistema:** confirmação clara antes de enviar, mensagens de rejeição compreensíveis, estado da ordem em linguagem simples, notificações confiáveis e identificáveis, carteira fácil de ler.

---

### Persona 2 — **Dona Marta, a investidora de renda (60+)**

| | |
|---|---|
| **Idade** | 66 anos, aposentada, Belo Horizonte |
| **Representa** | O segmento 60+: ~10% dos investidores, mas ~46% do estoque em renda variável |
| **Canal** | Computador e celular, com dificuldade em telas pequenas |
| **Objetivo** | Renda mensal com FIIs e dividendos; preservar patrimônio |
| **Conhecimento** | Médio sobre os ativos; baixo sobre tecnologia |

**Dores:**
- Alto patrimônio exposto → **alvo preferencial de golpes** (falsa central, troca de celular, acesso indevido).
- Teme fazer uma operação **sem querer** ou duas vezes.
- Precisa de **histórico e comprovantes** claros (para imposto de renda e para conferência).
- Fontes pequenas, fluxos longos e MFA confuso geram abandono ou pedidos de ajuda a terceiros (o que aumenta o risco de fraude).

**O que precisa do sistema:** MFA robusto, porém compreensível; alertas de login/dispositivo novo; proteção contra ordens duplicadas; histórico e extratos exportáveis; acessibilidade (contraste, tamanho de fonte, leitores de tela).

---

### Persona 3 — **Rafael, o trader ativo**

| | |
|---|---|
| **Idade** | 34 anos, autônomo, Curitiba |
| **Representa** | O núcleo de ~1,7 mi que opera mensalmente, incluindo quem faz day trade/swing trade |
| **Canal** | Desktop com várias telas + app no celular |
| **Objetivo** | Aproveitar oscilações de curto prazo |
| **Conhecimento** | Alto sobre a ferramenta, mas sujeito a vieses comportamentais |

**Dores:**
- **Indisponibilidade em momentos críticos** (abertura, fechamento, leilões, alta volatilidade) — o relato mais recorrente em reclamações e reportagens.
- **Cotação atrasada ou incorreta** que leva a decisões erradas.
- **Comportamento errático do sistema**: relatos públicos de investidores que enviaram ordem de venda e o sistema comprou, ou dobrou a posição.
- Quando a plataforma volta, encontra **posições zeradas automaticamente** com prejuízo e não sabe o que aconteceu.
- Para pedir ressarcimento, precisa **provar** a falha (prints, horários, registros) — e o sistema raramente ajuda com isso.
- Latência entre o clique e a confirmação.

**O que precisa do sistema:** baixa latência, disponibilidade alta no pregão, sinalização explícita de dado desatualizado, idempotência e integridade de ordens, estado da ordem em tempo quase real, registro auditável que o próprio investidor possa consultar, comunicação clara de incidentes.

**Atenção de design:** dado o histórico de perdas no day trade, o sistema **não** deve estimular operação compulsiva (gamificação, confetes, "streaks"). Limites de risco e alertas fazem parte do cuidado com esse usuário.

---

### Persona 4 — **Juliana, a nova investidora diversificando**

| | |
|---|---|
| **Idade** | 39 anos, gerente de projetos, Recife |
| **Representa** | Mulheres e investidores vindos da renda fixa migrando para ETFs/FIIs (segmento em crescimento) |
| **Canal** | Celular e notebook |
| **Objetivo** | Diversificar a reserva que hoje está em renda fixa |
| **Conhecimento** | Bom em finanças pessoais, novo na bolsa |

**Dores:**
- Desconfiança: "e se a corretora quebrar ou errar?". Quer **transparência** sobre o que acontece com a ordem e o dinheiro.
- Precisa saber se um produto é **adequado ao seu perfil** (suitability) antes de comprar.
- Quer **notificações relevantes**, não spam.

**O que precisa do sistema:** perfil de investidor registrado e verificado antes de ordens incompatíveis, explicação clara dos estados da ordem e do saldo (disponível x bloqueado), notificações configuráveis.

---

## 4. Stakeholders internos e externos

O sistema não serve só ao investidor. Os atores abaixo também têm necessidades que viram requisitos.

| Stakeholder | Papel | Necessidades / dores |
|---|---|---|
| **Operador de mesa / Backoffice (Orion)** | Acompanha ordens, trata exceções, atende clientes em contingência | Visão unificada do ciclo de vida das ordens (hoje os sistemas são pouco integrados); capacidade de agir quando o canal digital cai (a regulação exige meio alternativo de envio de ordens) |
| **Analista de risco (Orion)** | Define e monitora limites | Configurar limites por cliente/ativo; ver ordens bloqueadas e o motivo; agir sobre ordens que excedem limites operacionais |
| **Compliance / Auditoria interna** | Garante aderência regulatória | Logs imutáveis e consultáveis; reconstruir "quem fez o quê, quando, de onde"; relatórios para CVM/BSM |
| **Atendimento / Suporte** | Responde reclamações | Consultar histórico do cliente sem acesso a dados sensíveis desnecessários; ver o motivo exato de uma rejeição |
| **Administrador de sistema / SRE** | Mantém a plataforma no ar | Observabilidade, alertas, modo degradado controlado, recuperação após falha |
| **Provedor de cotações (externo)** | Fornece market data | Integração com tolerância a atraso/queda |
| **Bolsa/Corretora simulada (externo)** | Executa ordens | Protocolo de envio com confirmação, idempotência, reconciliação |
| **Reguladores (CVM, BSM/B3)** — indiretos | Fiscalizam | Registro de ordens com data/hora e numeração sequencial; regras de atuação publicadas; evidência para o MRP |

---

## 5. Dores e problemas mapeados

As dores foram agrupadas por tema e classificadas por **impacto** (financeiro/regulatório/reputacional) e **frequência** observada nas fontes.

### D1 — Indisponibilidade e instabilidade da plataforma ⚠️ Alta
- Relatos públicos recorrentes de corretoras grandes fora do ar **durante o pregão** (fechamento, leilões, circuit breaker), com investidores sem conseguir zerar posições.
- Instabilidade de plataforma está **entre as principais reclamações recebidas pela CVM**.
- Em caso de indisponibilidade, a corretora **deve informar o cliente sobre meios alternativos para envio de ordens**.
- Em incidentes recentes, as corretoras não informaram causa nem prazo de normalização — aumentando a ansiedade do usuário.

### D2 — Integridade da ordem (duplicidade, execução errada) ⚠️ Alta
- Relatos de ordem de venda que virou compra, posição dobrada, ordens abertas sem o cliente acompanhar.
- Dupla submissão é um risco natural em apps móveis (rede instável → usuário toca de novo → duas ordens).
- O MRP cobre explicitamente **"inexecução ou infiel execução de ordens"**, ou seja, esse tipo de falha tem custo direto para a corretora.

### D3 — Falta de transparência sobre o estado da ordem 🟠 Média-alta
- Usuários iniciantes não distinguem "enviada", "aceita", "parcialmente executada", "executada".
- Rejeições aparecem com mensagens genéricas.
- Sem rastreabilidade ponta a ponta, nem o suporte consegue explicar o que aconteceu.

### D4 — Dificuldade de provar falhas e obter ressarcimento 🟠 Média-alta
- O MRP ressarce até **R$ 200 mil por ocorrência**, com prazo de 18 meses, mas **não é automático**: o investidor precisa indicar datas, horários, ativos e comprovar o prejuízo.
- Reportagem da Exame indica que a maioria dos pedidos é rejeitada; para casos de instabilidade, é necessário comprovar a falha com prints, gravações e e-mails.
- Em 2024 a BSM recebeu 299 pedidos ao MRP.
- **Oportunidade:** se o sistema mantém registro auditável e fornece comprovantes ao investidor, reduz conflito e custo de atendimento.

### D5 — Fraude, golpes e acesso indevido ⚠️ Alta
- 1 em cada 3 brasileiros já passou por golpe/fraude financeira; o Brasil é o 2º país em tentativas.
- A engenharia social mira justamente **senhas e códigos de verificação**.
- Público 60+ (com maior patrimônio) é alvo preferencial.

### D6 — Cotação desatualizada ou incorreta 🟠 Média
- Decisões tomadas sobre preço velho geram prejuízo e reclamação.
- Provedores externos podem atrasar ou cair; o usuário raramente é avisado de que o dado está "velho".

### D7 — Validação insuficiente antes da ordem 🟠 Média
- Ordem enviada sem saldo, sem posição (venda a descoberto não intencional), fora do horário ou acima do limite gera rejeição na bolsa, custo operacional e frustração.
- A regulação exige monitoramento e controle das ordens que excedam limites operacionais por cliente.

### D8 — Usabilidade e acessibilidade 🟡 Média
- Usuários com pouco conhecimento financeiro e/ou digital, uso predominante em celular, público 60+ crescente.
- Erros de digitação (quantidade/preço) são uma causa clássica de prejuízo.

### D9 — Estímulo a comportamento de risco 🟡 Média
- Dados de perdas no day trade e popularidade das bets mostram que a interface pode empurrar o usuário para operar em excesso.
- Reguladores (FINRA) já fiscalizam recursos "gamificados".

### D10 — Sistemas legados pouco integrados (dor da Orion) ⚠️ Alta
- Descrita no próprio briefing: dificuldade de rastrear ordens, controlar risco e auditar.
- Impacto regulatório direto: a corretora precisa manter registro de todas as ordens e regras internas documentadas.

---

## 6. Ambiente regulatório que afeta o produto

> O SentinelTrade é um projeto acadêmico com dados simulados, mas a arquitetura deve ser **compatível** com as obrigações abaixo, porque são elas que definem o que é "correto" para uma corretora real.

| Norma / mecanismo | O que exige (resumo) | Impacto no sistema |
|---|---|---|
| **Resolução CVM 35/2021** (intermediação) | Regras internas sobre tipos de ordens aceitas, horário de recebimento, prazo de validade, registro, cancelamento/alteração; registro de cada ordem com data/hora de recepção e numeração sequencial e cronológica; identificação do emissor da ordem; sistema que permita monitorar e adequar ordens que excedam limites operacionais por cliente; prioridade de ordens de clientes sobre carteira própria | Máquina de estados da ordem; ID sequencial; timestamps confiáveis; motor de limites; auditoria; regras de horário/validade parametrizáveis |
| **Resolução CVM 30/2021** (suitability) | Verificar adequação de produtos e operações ao perfil do cliente; revisão do perfil periodicamente (no mínimo a cada 24 meses, segundo materiais de mercado); registro de ciência quando o cliente opera fora do perfil | Cadastro de perfil de investidor; alerta/aceite explícito para operação fora do perfil; histórico de aceites |
| **MRP – BSM/B3** | Ressarcimento de até R$ 200 mil por ocorrência para falhas de intermediação, inclusive inexecução ou infiel execução de ordens; prazo de 18 meses | Retenção e imutabilidade de logs; comprovantes ao investidor |
| **LGPD (Lei 13.709/2018)** | Proteção de dados pessoais, minimização, segurança, direitos do titular | Criptografia, controle de acesso por perfil, mascaramento em logs, segregação de dados sensíveis |
| **Boas práticas de segurança** (Febraban, ISO 27001) | MFA, educação contra engenharia social, nunca solicitar senha/código pelo canal de atendimento | MFA, notificações de segurança, alerta de novo dispositivo |

---

## 7. Jornada do investidor e momentos críticos

```
[1] Cadastro/Onboarding → [2] Login (MFA) → [3] Consulta de cotação/carteira
      → [4] Montagem da ordem → [5] Validação → [6] Envio à bolsa
      → [7] Acompanhamento (parcial/total/rejeição) → [8] Notificação
      → [9] Atualização de carteira/saldo → [10] Histórico/comprovante
```

| Etapa | Momento crítico | Dor relacionada | Risco se falhar |
|---|---|---|---|
| 1 | Definição do perfil de investidor | D7, regulatório | Operação inadequada ao perfil |
| 2 | MFA, dispositivo novo | D5 | Conta invadida, ordens fraudulentas |
| 3 | Cotação exibida | D6 | Decisão sobre dado velho |
| 4 | Digitação de qtd/preço | D8, D2 | Erro "fat finger" |
| 5 | Saldo, posição, limite, horário | D7 | Rejeição na bolsa / exposição indevida |
| 6 | Envio com rede instável | D2, D1 | **Ordem duplicada** ou perdida |
| 7 | Execução parcial | D3 | Usuário não entende a posição |
| 8 | Aviso de execução/rejeição | D3, D5 | Desinformação / phishing |
| 9 | Reconciliação de posição | D2, D10 | Carteira inconsistente |
| 10 | Consulta posterior/prova | D4, D10 | Não auditável, ressarcimento negado |

**Horários de maior risco:** abertura do pregão, leilões de fechamento e eventos de alta volatilidade (ex.: circuit breaker). É quando a carga é maior **e** quando a indisponibilidade custa mais caro.

---

## 8. Conclusões e implicações para o produto

1. **Confiança é o produto.** Para todas as personas, a principal expectativa não é ter mais funcionalidades, e sim que o sistema faça exatamente o que o usuário pediu, uma única vez, e consiga provar isso depois.
2. **Dois perfis de uso convivem:** a maioria opera pouco e com valores pequenos (precisa de clareza e proteção contra erro); uma minoria opera muito (precisa de baixa latência e disponibilidade no pregão). A arquitetura deve atender ao pico do trader sem complicar a experiência do iniciante.
3. **Disponibilidade nos momentos críticos importa mais que disponibilidade média.** Um sistema 99,9% disponível que cai justamente no fechamento falha com a audiência.
4. **Quando falhar, falhe de forma segura e transparente:** bloquear novas ordens em vez de aceitar ordens que não podem ser validadas, avisar o usuário, informar canal alternativo e nunca deixar estado inconsistente.
5. **Segurança tem que considerar engenharia social**, não só ataque técnico: MFA, alertas de novo dispositivo, notificações que nunca peçam códigos.
6. **Auditoria serve também ao investidor:** histórico e comprovantes detalhados reduzem disputas e apoiam pedidos ao MRP.
7. **Design responsável:** limites de risco, confirmação explícita e ausência de gamificação protegem o usuário mais vulnerável.
8. **Para a Orion,** a dor central é integração e rastreabilidade: um ID único de ordem que atravesse todos os serviços e um log imutável resolvem boa parte do problema descrito no briefing.

---

## 9. Fontes

**Dados de mercado e perfil do investidor**
1. B3 — Perfil de Investidores PF por gênero (2025). https://sistemaswebb3-listados.b3.com.br/investorProfilePage/genre?language=pt-br
2. B3 / Bora Investir — "Investidor pessoa física movimentou mais de R$ 517,3 bilhões em ações na B3 em 2025". https://borainvestir.b3.com.br/tipos-de-investimentos/renda-variavel/acoes/investidor-pessoa-fisica-movimentou-mais-de-r-5173-bilhoes-em-acoes-na-b3-em-2025/
3. Exame — "B3 alcança 6,45 milhões de investidores pessoa física, maior nível em 5 anos" (jul/2026). https://exame.com/invest/mercados/b3-alcanca-645-milhoes-de-investidores-pessoa-fisica-maior-nivel-em-5-anos/
4. Traders Union — "B3 destaca avanço da base de investidores pessoa física" (ago/2026). https://tradersunion.com/pt/news/institutions/show/3108978-b3-investidores-diversificacao-mercado-capitais/
5. B3 — Book Pessoa Física 1T2024. https://www.b3.com.br/data/files/79/94/4E/9F/52CAF8105391B9F8AC094EA8/Book%20Pessoa%20F%C3%ADsica%20-%201T2024%20(v2).pdf
6. B3 — "Total de investidor pessoa física cresce 43% no primeiro semestre" (2021). https://www.b3.com.br/pt_br/noticias/porcentagem-de-investidores-pessoa-fisica-cresce-na-b3.htm
7. B3 — Apresentação Book PF 2021 (mediana do primeiro investimento). https://www.b3.com.br/data/files/1C/85/3E/FC/CB63B71027085EA7AC094EA8/BookPF_Apresentacao%20para%20Coletiva%20de%20Imprensa.pdf
8. ANBIMA — Raio X do Investidor Brasileiro (9ª edição). https://www.anbima.com.br/pt_br/especial/raio-x-do-investidor-brasileiro.htm
9. ANBIMA — "Anbima lança a nona edição do Raio X do Investidor Brasileiro". https://www.anbima.com.br/pt_br/noticias/anbima-lanca-a-nona-edicao-do-raio-x-do-investidor-brasileiro-36-da-populacao-aplica-em-produtos-financeiros.htm
10. Investalk BB / Broadcast — dados regionais e de classe social da 8ª edição do Raio X. https://investalk.bb.com.br/noticias/mercado/pais-deve-ganhar-4-milhoes-de-novos-investidores-aponta-pesquisa-anbima-datafolha
11. FINRA Investor Education Foundation — Investors in the United States (2024 NFCS Investor Survey). https://www.finrafoundation.org/sites/finrafoundation/files/2025-11/NFCS_Investor_Survey_Report_White_Paper.pdf
12. InvestmentNews — "Finra zeroes in on online brokerage apps". https://www.investmentnews.com/fintech/finra-zeroes-in-on-online-brokerage-apps/202213

**Comportamento e risco**
13. FGV EESP — "Day trade é cassino, muito mais sorte do que técnica, diz pesquisador". https://eesp.fgv.br/noticia/day-trade-e-cassino-muito-mais-sorte-do-que-tecnica-diz-pesquisador
14. TradingView News — levantamento Grana Capital sobre day trade (2024–2025). https://br.tradingview.com/news/cointelegraph:9e0172bfdbc81:0/

**Dores com plataformas**
15. Exame — "Prejuízo por erro da corretora: 90% dos recursos são rejeitados". https://exame.com/invest/onde-investir/prejuizo-por-erro-da-corretora-90-dos-recursos-sao-rejeitados-entenda/
16. CNN Brasil — "O que fazer quando o site da corretora trava na hora de realizar operações?". https://www.cnnbrasil.com.br/economia/investimentos/o-que-fazer-quando-o-site-da-corretora-trava-na-hora-de-realizar-operacoes/
17. Arena do Pavini — "Slowboys: sistema de corretora trava várias vezes e clientes pedem indenização". https://www.arenadopavini.com.br/acoes-na-arena/sistema-de-corretora-trava-varias-vezes-e-clientes-pedem-indenizacao-por-perdas
18. Seu Crédito Digital — "XP Investimentos fora do ar" (mai/2025). https://seucreditodigital.com.br/xp-investimentos-fora-do-ar/
19. Reclame Aqui — "Instabilidade na plataforma" (Clear Corretora). https://www.reclameaqui.com.br/clear-corretora-de-valores/instabilidade-na-plataforma_rpag8ByLyOllrDO5/

**Regulação**
20. CVM — Resolução CVM 35/2021 (texto consolidado). https://conteudo.cvm.gov.br/export/sites/cvm/legislacao/resolucoes/anexos/001/resol035consolid.pdf
21. B3 — Roteiro do Programa de Qualificação Operacional (PQO), vigência 2024. https://www.b3.com.br/data/files/42/E1/3F/E1/F96B98101DBF7498AC094EA8/Roteiro%20PQO%20-%20Vigencia%20_02012024.pdf
22. CVM — Relatório de ARR sobre Suitability (Resolução CVM 30). https://www.gov.br/cvm/pt-br/centrais-de-conteudo/publicacoes/estudos/arr-suitability.pdf
23. AAWZ Partners — "Suitability na Consultoria CVM" (prazos de revisão e guarda). https://aawzpartners.com/suitability-consultoria-cvm/
24. BSM Supervisão de Mercados — "O que é MRP?". https://www.bsmsupervisao.com.br/o-que-e-mrp
25. B3 / Bora Investir — "Entenda como funciona o Mecanismo de Ressarcimento de Prejuízos". https://borainvestir.b3.com.br/noticias/entenda-como-funciona-o-mecanismo-de-ressarcimento-de-prejuizos/
26. Portal do Investidor (gov.br) — "Riscos e o Mecanismo de Ressarcimento de Prejuízos". https://www.gov.br/investidor/pt-br/investir/como-investir/como-funciona-a-bolsa/riscos-e-o-mecanismo-de-ressarcimento-de-prejuizos

**Segurança e fraude**
27. O Tempo — "Governo e Febraban lançam plano de combate a fraudes bancárias digitais" (dez/2025). https://www.otempo.com.br/politica/governo/2025/12/3/governo-e-febraban-lancam-plano-de-combate-a-fraudes-bancarias-digitais
28. Febraban — "Febraban faz alerta sobre o golpe do falso gerente". https://portal.febraban.org.br/noticia/4431/pt-br/
29. Febraban — "FEBRABAN relança campanha nacional antifraudes". https://portal.febraban.org.br/noticia/3836/pt-br/
