# Versão mínima funcional — Monitoramento da prontidão para IA

Você vai construir a primeira versão funcional de um sistema web que acompanha, ciclo a ciclo, a prontidão de equipes para o uso de IA generativa no trabalho. Leia este documento inteiro antes de escrever qualquer código.

## 0. Como trabalhar

1. **Comece por um plano, não por código.** Entregue primeiro: estrutura de pacotes, lista de rotas/telas, ordem de implementação, os wireframes pedidos na seção 7 e qualquer ponto deste documento que você considere ambíguo. Espere aprovação antes de implementar.
2. **Implemente em fatias verticais** na ordem da seção 10. Cada fatia termina com testes passando, instruções de verificação manual e um commit.
3. **Não invente requisitos.** Quando faltar informação, escolha a opção mais simples e registre em `docs/decisoes.md` (decisão, alternativa descartada, motivo).
4. **Clareza acima de esperteza.** Este é um projeto acadêmico de Design Science Research. O grupo vai defender o sistema oralmente, então cada regra de negócio precisa estar num lugar óbvio, com nome claro e um teste que a documente.
5. Toda a interface e a documentação são em **português do Brasil**.

## 1. Contexto

Empresas avaliam a prontidão das equipes para IA de forma pontual (um questionário único), o que impede ver onde e quando as pessoas travam. O artefato combina duas fontes de dado:

- **Desempenho:** atividades de letramento em IA (acerto, erro e tempo por área).
- **Percepção:** um check-in curto a cada ciclo (confiança, receio de substituição e dificuldades).

Ao fim de cada ciclo, o sistema calcula indicadores por time, aplica regras de alerta e recomenda intervenções ao gestor. O caso central é detectar um time que **sabe usar a IA, mas não se sente pronto**: o técnico sobe enquanto a confiança cai.

Esta versão serve para a etapa de **demonstração** da DSR. Ela precisa executar o caso de uso UC01 (seção 9) de ponta a ponta, com dados simulados, e as telas vão virar as evidências de um relatório. Por isso, **fidelidade ao cenário e às regras deste documento é o requisito principal**.

## 2. Stack

- Java 21, Spring Boot 3.x, Gradle. Monólito de um módulo, organizado por funcionalidade.
- Spring MVC + Thymeleaf (renderização no servidor). HTMX é permitido para interações pequenas. Gráficos com Chart.js.
- Spring Security com login por formulário, papéis `COLABORADOR` e `GESTOR`. Senhas com BCrypt.
- PostgreSQL 16 via `docker-compose.yml`, migrations com Flyway, acesso com Spring Data JPA.
- Testes: JUnit 5. Regras de negócio testadas como código puro (sem banco). Integração com Testcontainers.
- Perfil Spring `demo` habilita o painel de simulação (seção 8).

## 3. Cenário

Uma seguradora fictícia de médio porte liberou um assistente de IA generativa para os funcionários. A área de pessoas acompanha a adoção durante **6 ciclos semanais**, com **3 times de 8 pessoas**:

| Time | Padrão ao longo dos ciclos |
|---|---|
| Atendimento | Acerto nas atividades sobe a cada ciclo; confiança cai, receio de substituição sobe e o engajamento cai no ciclo 4. Depois da intervenção (ciclo 5), a confiança volta a subir. |
| Financeiro | Começa com confiança baixa e desempenho médio; melhora de forma consistente. Nunca gera alerta. |
| Marketing | Confiança alta e respostas rápidas desde o início, mas erra muito na verificação das respostas da IA. |

**Personagens:**
- **Renata:** gestora de pessoas, gestora dos três times.
- **Carla:** atendente, membro do Atendimento. É quem faz o check-in e as atividades ao vivo na demonstração.

Os demais 23 colaboradores recebem nomes brasileiros fictícios.

## 4. Modelo de dados

Crie o schema na migration `V1`. Os nomes das tabelas e colunas estão definidos; tipos e índices adicionais ficam a seu critério. Registre as escolhas em `docs/decisoes.md`.

**Estrutura**
- `usuario` (id, nome, email único, senha_hash, papel `COLABORADOR|GESTOR`, time_id nulo). Restrição: se o papel for `COLABORADOR`, `time_id` é obrigatório.
- `time` (id, nome, gestor_id → usuario)
- `ciclo` (id, numero único, inicio, fim, status `ABERTO|FECHADO`). No máximo um ciclo aberto (índice único parcial).

**Percepção**
- `checkin` (id, usuario_id, ciclo_id, confianca 1–5, receio_substituicao 1–5, respondido_em). Único por (usuario_id, ciclo_id).
- `dificuldade` (id, codigo, descricao, fator, intervencao_id). É um catálogo.
- `checkin_dificuldade` (checkin_id, dificuldade_id), com chave composta.

**Desempenho**
- `area` (id, codigo, nome). Quatro áreas: `FORMULACAO` (formulação de pedidos à IA), `VERIFICACAO` (verificação das respostas da IA), `DADOS` (uso seguro de dados), `APLICACAO` (aplicação em tarefas do setor).
- `atividade` (id, codigo, titulo, nivel, descricao)
- `questao` (id, atividade_id, area_id, ordem, enunciado, alternativas jsonb, indice_correto, comentario)
- `atividade_liberada` (id, time_id, ciclo_id, atividade_id, recomendacao_id nulo). Única por (time_id, ciclo_id, atividade_id). **É o escopo do time no ciclo.**
- `tentativa` (id, usuario_id, atividade_liberada_id, iniciada_em, finalizada_em nulo). Única por (usuario_id, atividade_liberada_id).
- `resposta` (id, tentativa_id, questao_id, alternativa_escolhida, correta, tempo_segundos)

**Diagnóstico**
- `indicador_time_ciclo` (id, time_id, ciclo_id, tecnico, confianca nulo, resistencia nulo, engajamento, tempo_medio_min, n_checkins, n_membros, calculado_em). Única por (time_id, ciclo_id). Guarde os valores com uma casa decimal. **É uma fotografia gravada no fechamento do ciclo e nunca é recalculada depois.**
- `alerta` (id, time_id, ciclo_id, tipo `DIVERGENCIA_TECNICO_CONFIANCA|CONFIANCA_SEM_VERIFICACAO`, prioridade `NORMAL|ALTA`, status `ABERTO|EM_ACOMPANHAMENTO|RESOLVIDO`, ciclo_resolucao_id nulo, detalhe jsonb com os valores que dispararam a regra)
- `intervencao` (id, codigo único, descricao, executor, tipo `HUMANA|PLATAFORMA`). É um catálogo.
- `recomendacao` (id, alerta_id, intervencao_id, dificuldade_id nulo, n_respostas, total_respostas, status `SUGERIDA|ACEITA|AJUSTADA|ADIADA`, descricao_ajustada nulo, data_prevista nulo, ciclo_aplicacao_id nulo, decidido_por nulo, decidido_em nulo)

**Regra de privacidade (obrigatória e testada):** nenhuma rota ou consulta acessível ao `GESTOR` pode devolver dados de check-in de uma pessoa específica. Para o gestor, percepção só aparece agregada por time, e somente quando o time tem pelo menos 5 check-ins no ciclo. O colaborador vê o próprio histórico completo.

## 5. Regras de negócio

Implemente cada regra numa classe própria, com o limiar em `application.yml`, e com testes unitários que reproduzam os números da seção 8.

### 5.1 Indicadores (por time, no fechamento do ciclo)

| Indicador | Cálculo |
|---|---|
| Técnico | % de respostas corretas nas tentativas finalizadas no ciclo. |
| Confiança | média de (confianca − 1) / 4 × 100 nos check-ins do ciclo. |
| Resistência | média de (receio_substituicao − 1) / 4 × 100 nos check-ins do ciclo. |
| Engajamento | concluídas / esperadas × 100. Esperadas = n_membros × (1 + atividades liberadas ao time no ciclo). Concluídas = check-ins + tentativas finalizadas. |
| Produtividade | tempo médio (finalizada_em − iniciada_em) das tentativas finalizadas, em minutos. Lido junto com o técnico, que representa a qualidade. |
| Erro por área | 100 − % de acerto por área. É calculado na leitura, sem snapshot. |

**Amostra mínima:** se `n_checkins < 5`:
- confiança e resistência ficam nulas;
- as dificuldades não são exibidas ao gestor;
- as regras A e B não são avaliadas para aquele time naquele ciclo.

As regras usam os valores armazenados (uma casa decimal). As telas mostram números inteiros, arredondando metade para cima.

### 5.2 Regras de alerta (aplicadas no fechamento do ciclo `c`)

- **A, divergência técnico × confiança:** técnico(c) > técnico(c−1) > técnico(c−2), **e** confiança(c−2) − confiança(c−1) ≥ 5, **e** confiança(c−1) − confiança(c) ≥ 5. Nome exibido: “Sabe usar, mas não se sente pronto”.
- **B, confiança sem verificação:** confiança ≥ 75 **e** acerto na área `VERIFICACAO` < 50%, no ciclo `c` e no `c−1`. Nome exibido: “Confiança alta sem senso crítico”.
- **Prioridade:** o alerta nasce com prioridade `ALTA` se engajamento(c−1) − engajamento(c) ≥ 10.
- **Sem duplicatas:** se o time já tem um alerta não resolvido do mesmo tipo, atualize o `detalhe` em vez de criar outro.
- **Encerramento do tipo A:** um alerta `EM_ACOMPANHAMENTO` passa a `RESOLVIDO` quando a confiança sobe em dois ciclos seguidos a partir do ciclo de aplicação da intervenção, comparando cada ciclo com o anterior.
- **Encerramento do tipo B:** quando a condição B deixa de valer por dois ciclos seguidos.

### 5.3 Recomendações (geradas junto com o alerta)

- **Tipo A:** conte as dificuldades marcadas nos check-ins do time no ciclo. Pegue as **duas mais marcadas** (empate resolvido pela ordem do catálogo) e crie uma recomendação para cada uma, com a intervenção ligada à dificuldade, `n_respostas` e `total_respostas`.
- **Tipo B:** uma recomendação com a intervenção `TRILHA_VERIFICACAO_CHECKLIST`.

**Catálogo** (seed):

| Dificuldade (fator) | Intervenção | Executor / tipo |
|---|---|---|
| Receio de substituição (deslocamento) | Comunicação da liderança, em reunião de time, sobre a IA como apoio ao trabalho, e não como substituição | Liderança / HUMANA |
| Não sei quando posso confiar na resposta (confiança) | Trilha “Verificação de respostas”: atividades práticas de baixo risco, com situações do próprio setor e feedback comentado | Plataforma / PLATAFORMA |
| Falta de tempo ou suporte para praticar (condições facilitadoras) | Horário reservado para prática e ponto focal de suporte no time | Gestor / HUMANA |
| O assistente não se encaixa nas minhas tarefas (compatibilidade) | Oficina de casos de uso do setor | Liderança e TI / HUMANA |
| Meu time ou minha liderança não usa (normas subjetivas) | Liderança passa a usar o assistente e a compartilhar exemplos de uso | Liderança / HUMANA |
| *(alerta B)* | Trilha “Verificação de respostas” e checklist de revisão das respostas da IA | Plataforma / PLATAFORMA |

### 5.4 Decisão do gestor e escopo

- **“Aceitar e ajustar escopo”:** marca as recomendações do alerta como `ACEITA` e leva à tela de escopo.
- **“Ajustar recomendação” (fluxo alternativo FA1):** permite editar o texto de cada item e grava `AJUSTADA` com `descricao_ajustada`.
- **“Adiar”:** marca os itens como `ADIADA`.
- **Salvar o escopo:**
  - Troca a atividade liberada conforme a intervenção de plataforma. A troca vale para o ciclo aberto se nenhum membro do time ainda iniciou tentativa nele; caso contrário, vale a partir do próximo ciclo.
  - Grava `recomendacao_id` na nova `atividade_liberada` e `ciclo_aplicacao_id` na recomendação.
  - Muda o alerta para `EM_ACOMPANHAMENTO`.
- **Intervenções humanas:** são marcadas como agendadas com uma data (`data_prevista`). O sistema registra a intervenção, mas não a executa.
- **O nível de dificuldade não muda na troca de escopo.** A trilha contextualiza as questões, mas mantém o nível; senão o técnico subiria por causa da mudança e a comparação entre ciclos perderia valor.

### 5.5 Fechamento do ciclo

É um serviço transacional, disparado por um agendador (quando `fim` passou) e pelo painel de demonstração. Etapas, em ordem:

1. Marca o ciclo como `FECHADO`. Pendências não concluídas ficam como não realizadas.
2. Confere a amostra de check-ins de cada time.
3. Calcula e grava `indicador_time_ciclo` de cada time.
4. Avalia o encerramento de alertas existentes e depois aplica as regras A e B.
5. Gera as recomendações dos alertas novos.
6. Se ainda houver ciclos (total de 6, configurável), abre o próximo e cria as `atividade_liberada` copiando o escopo vigente de cada time.

**Nunca insira indicadores ou alertas direto no banco, nem no seed.** Tudo passa por este serviço.

## 6. Telas

Todas as telas seguem o plano de design da seção 7. Cada tela indica a evidência do relatório que ela produz.

### Colaborador

**`/login`** (tela única para os dois papéis)
- Formulário de login.
- Com o perfil `demo` ativo, mostra os usuários de demonstração.

**`/inicio`** (tela inicial da colaboradora)
- Ciclo atual (“Ciclo 4 de 6”, com datas).
- Lista de pendências (check-in e atividades) com status.
- Progresso do ciclo e link para “Meu histórico”.

**`/checkin`** (formulário do check-in)
- **Pergunta 1:** “Quão seguro(a) você se sente para usar o assistente de IA nas suas tarefas?” Cinco opções clicáveis com rótulos nas pontas (nada seguro / totalmente seguro).
- **Pergunta 2:** “Quanto você concorda com a frase: a IA pode substituir parte do meu trabalho?” Mesma escala (discordo totalmente / concordo totalmente).
- **Pergunta 3:** “Quais dificuldades você teve com a IA nesta semana?” Múltipla escolha com o catálogo, mais a opção “nenhuma”.
- Aviso fixo de privacidade: “Sua gestora vê apenas resultados do time, nunca as suas respostas, e só quando o time tem pelo menos 5 respostas.”
- Botão “Enviar check-in”, com validação inline se faltar resposta. Depois do envio, mostra a confirmação e volta ao início.

**`/atividades/{id}`** (execução da atividade)
- 10 questões de múltipla escolha e um cronômetro visível.
- Botão “Finalizar atividade”, que exige todas as questões respondidas.

**`/atividades/{id}/resultado`** (resultado da atividade)
- Acertos (ex.: 8 de 10) e tempo.
- Gabarito comentado, questão a questão.

**`/meu-historico`** (histórico pessoal)
- Gráfico do próprio acerto e da própria confiança por ciclo.
- Tabela dos ciclos.
- Nota: “Só você vê estes dados.”

### Gestor

**`/gestao`** (visão geral dos times, Quadro 1 do relatório)
- Seletor de ciclo; o padrão é o último ciclo fechado.
- Um painel por time com os quatro indicadores e a variação em relação ao ciclo anterior (↑ ↓ =).
- Selo de alerta: vermelho para prioridade alta, âmbar para normal, sempre com o nome do alerta escrito.
- Faixa com a área de maior erro da empresa no ciclo (%).
- Destaque para o time com maior ganho de confiança desde o ciclo 1.

**`/gestao/times/{id}`** (detalhe do time, Quadro 2)
- **Gráfico de linhas** dos quatro indicadores por ciclo:
  - seletor de intervalo (“ciclos 1 a N”);
  - linha vertical tracejada marcando a intervenção;
  - tooltip com os valores.
- **Caixa do alerta:**
  - nome, prioridade, status e ciclo em que foi gerado;
  - regra em linguagem simples com os valores que a dispararam (ex.: “Técnico subiu de 61 para 67 e 72; confiança caiu de 59 para 53 e 47”);
  - botão “Ver recomendação”.
- **Barras de erro por área** no ciclo.
- **Barras de dificuldades relatadas**, com contagem e % sobre o total de respostas. Se faltar amostra, mostra o aviso de amostra insuficiente.
- **Bloco de produtividade:** tempo médio por atividade no ciclo 1 e no ciclo atual.
- **Aviso de amostra** (“7 de 8 check-ins respondidos, mínimo de 5”).
- **“Comparar antes e depois”**, visível quando há intervenção: tabela com o ciclo anterior à intervenção ao lado dos ciclos seguintes.

**`/gestao/alertas/{id}/recomendacao`** (recomendação, Quadro 3)
- Título no formato “Sabe usar, mas não se sente pronto – Atendimento – ciclo 4”.
- Um item por recomendação, cada um com:
  - a intervenção;
  - a dificuldade ligada, com contagem (“5 de 7 respostas”);
  - o executor e o selo “ação humana” ou “plataforma”;
  - um expansor “Por que esta recomendação?”, que mostra o % de respostas e a regra ativa.
- Botões “Aceitar e ajustar escopo”, “Ajustar recomendação” e “Adiar”.
- Rodapé: “As recomendações apoiam a decisão da gestão e não a substituem.”

**`/gestao/times/{id}/escopo`** (escopo de atividades)
- Escopo atual ao lado do escopo proposto, com a troca sugerida já selecionada.
- Botão “Salvar escopo”.
- Lista de intervenções humanas, cada uma com “Marcar como agendada” e campo de data.
- Depois de salvar, volta ao detalhe do time com a confirmação “Escopo salvo” e o alerta em acompanhamento.

### Painel de demonstração: `/demo`

Só existe com o perfil `demo`. Enquanto esse perfil estiver ativo, todas as páginas mostram a faixa “Ambiente de demonstração: dados simulados”. O painel mostra o ciclo aberto e as pendências por time, e tem os botões:

- “Restaurar cenário inicial”
- “Preencher pendências da Carla conforme o roteiro” (alternativa ao preenchimento manual)
- “Gerar respostas simuladas do ciclo aberto” (para quem ainda não respondeu)
- “Fechar ciclo aberto”
- “Avançar um ciclo” (gerar e fechar)

O painel também mantém um registro das ações executadas.

## 7. Referência visual e direção de design

O grupo definiu a identidade visual do sistema em `docs/referencia-visual/styles.css`. Use esse arquivo como folha de estilo base:
- mantenha a paleta (bordô `#8A0638` como cor principal e os tons de apoio), a tipografia, os raios e os componentes que ele já define: barra superior, menu lateral, cards de indicador, badges, caixas de aviso, escala Likert, opções de quiz, tabelas e toast;
- adapte e acrescente classes conforme as telas da seção 6 precisarem, sem mudar a identidade.

O arquivo define só a aparência. O layout de cada tela, o conteúdo, os textos, os dados e o fluxo seguem esta especificação.

**Qualidade mínima:**
- números com algarismos tabulares;
- cada indicador com a mesma cor em todos os gráficos, com legenda;
- estados de alerta com cor e texto, nunca só cor;
- responsivo e com foco de teclado visível;
- verbos ativos e a mesma palavra para a mesma ação em todo o fluxo (“Salvar escopo” gera “Escopo salvo”).

No plano inicial, proponha o layout da visão geral e do detalhe do time em wireframes ASCII.

## 8. Dados simulados

O código de simulação fica num pacote `simulacao`, separado do domínio, e é **determinístico** (semente fixa). Ele gera respostas **individuais** (escala de cada pessoa, acerto de cada questão, tempo de cada tentativa) cujas agregações atingem as metas abaixo. Os indicadores sempre saem do serviço de fechamento.

### 8.1 Catálogos e conteúdo

- As quatro áreas, as cinco dificuldades e as intervenções da seção 5.3.
- A cada ciclo, cada time recebe duas atividades de 10 questões:
  - **A**, “Formulação e verificação” (5 questões + 5);
  - **B**, “Dados e aplicação no setor” (5 + 5).
- Códigos das atividades por ciclo: “1A”, “1B” … “6A”, “6B”.
- Crie também a atividade da trilha, “Trilha verificação – situações do Atendimento”: mesmo nível e mesma divisão por área da atividade A, com as questões de verificação usando situações de atendimento ao cliente de seguros.
- As questões devem ser realistas, em pt-BR, no contexto de uma seguradora. É permitido reaproveitar enunciados entre ciclos.
- A atividade 4A deve conter esta questão: uma resposta do assistente cita um prazo de reembolso e a pergunta é o que fazer antes de repassá-la ao cliente.

### 8.2 Estado inicial (“Restaurar cenário inicial”)

- **Ciclos 1 a 3 fechados**, com dados completos. Os indicadores devem ser produzidos pelo serviço de fechamento.
- **Ciclo 4 aberto**, com as respostas dos 23 colaboradores já geradas, exceto as da Carla. Um membro do Atendimento (não a Carla) pula o check-in do ciclo 4, para que o time feche com 7 de 8.
- **Ciclos 5 e 6** são gerados pelo botão “Avançar um ciclo”, seguindo o padrão do cenário.
- **A melhora do Atendimento nos ciclos 5 e 6 é programada e não depende da intervenção.** Documente isso no README.

### 8.3 Roteiro da Carla no ciclo 4

**Check-in:**
- confiança 2 e receio 4;
- dificuldades “receio de substituição” e “não sei quando posso confiar na resposta”.

**Atividades:**
- 4A: 8 de 10 acertos, cerca de 11 minutos;
- 4B: 7 de 10 acertos, cerca de 10 minutos.

**Histórico:** no ciclo 3, o acerto pessoal dela foi de 65%.

### 8.4 Metas por time e ciclo

Tolerância de ±2 pontos nos indicadores e de ±0,5 min no tempo. Os valores do ciclo 4 já consideram a Carla seguindo o roteiro.

| Time | Indicador | C1 | C2 | C3 | C4 | C5 | C6 |
|---|---|---|---|---|---|---|---|
| Atendimento | Técnico | 55 | 61 | 67 | 72 | 73 | 75 |
| | Confiança | 63 | 59 | 53 | 47 | 50 | 56 |
| | Resistência | 44 | 47 | 53 | 63 | 59 | 53 |
| | Engajamento | 96 | 92 | 92 | 79 | 83 | 92 |
| | Acerto em verificação (%) | 60 | 63 | 65 | 68 | 73 | 78 |
| | Tempo médio (min) | 14,0 | 12,8 | 11,6 | 10,9 | 10,6 | 10,2 |
| Financeiro | Técnico | 58 | 61 | 65 | 69 | 72 | 75 |
| | Confiança | 34 | 41 | 47 | 53 | 59 | 63 |
| | Resistência | 41 | 38 | 34 | 31 | 28 | 28 |
| | Engajamento | 83 | 88 | 88 | 92 | 92 | 96 |
| | Acerto em verificação (%) | 53 | 55 | 60 | 65 | 68 | 73 |
| | Tempo médio (min) | 15,5 | 14,6 | 13,8 | 13,0 | 12,4 | 11,9 |
| Marketing | Técnico | 64 | 65 | 66 | 67 | 68 | 69 |
| | Confiança | 78 | 81 | 84 | 84 | 88 | 88 |
| | Resistência | 16 | 16 | 13 | 13 | 13 | 9 |
| | Engajamento | 100 | 96 | 100 | 96 | 100 | 100 |
| | Acerto em verificação (%) | 38 | 40 | 40 | 43 | 43 | 45 |
| | Tempo médio (min) | 9,8 | 9,6 | 9,5 | 9,4 | 9,4 | 9,3 |

**Erro por área no ciclo 4 (%):**

| Área | Atendimento | Financeiro | Marketing |
|---|---|---|---|
| Formulação | 23 | 30 | 18 |
| Verificação | 33 | 35 | 58 |
| Dados | 25 | 23 | 33 |
| Aplicação | 33 | 38 | 25 |

**Dificuldades do Atendimento no ciclo 4** (7 check-ins): receio de substituição 5, não sei quando confiar 3, falta de tempo ou suporte 2, não se encaixa 1, liderança não usa 0.

**Engajamento abaixo de 100%:** vem de pendências não cumpridas, e o número de pendências faltantes precisa bater com o engajamento. No ciclo 4 do Atendimento: 7 check-ins e 12 de 16 atividades, ou seja, 19 de 24 pendências.

### 8.5 Invariantes (critérios de aceite exatos)

Crie um teste de integração que restaura o cenário, executa o roteiro da Carla e avança os ciclos, verificando:

1. A regra A dispara para o Atendimento no fechamento do ciclo 4, e não no ciclo 3. Nunca dispara para Financeiro ou Marketing.
2. O alerta A nasce com prioridade `ALTA`.
3. A regra B mantém um único alerta aberto para o Marketing desde o fechamento do ciclo 2. Nunca dispara para os outros times.
4. O Financeiro não tem alerta em nenhum ciclo.
5. No ciclo 1, a confiança do Atendimento é maior que a do Financeiro. No ciclo 4, é menor.
6. No ciclo 4, `VERIFICACAO` é a área com maior erro médio da empresa, e o Marketing é o time que mais erra nela.
7. A recomendação do alerta A tem exatamente dois itens: receio de substituição (5 de 7) → comunicação da liderança; e “não sei quando confiar” (3 de 7) → trilha de verificação.
8. Se o escopo for salvo no início do ciclo 5 e os ciclos 5 e 6 forem avançados, o alerta A fica `EM_ACOMPANHAMENTO` depois do ciclo 5 e `RESOLVIDO` no fechamento do ciclo 6, e não antes.
9. Nenhuma rota do gestor devolve check-in individual.
10. Com menos de 5 check-ins, confiança e resistência ficam nulas e as regras não são avaliadas (teste com dados próprios).

## 9. Roteiro de demonstração (UC01)

Escreva `docs/roteiro-demonstracao.md` com o passo a passo clique a clique, o resultado esperado de cada passo e o nome do print a capturar. O sistema precisa permitir exatamente esta sequência:

1. Restaurar o cenário no painel `/demo`.
2. Carla entra, faz o check-in e as atividades 4A e 4B conforme o roteiro, e abre “Meu histórico”. Prints: login, início, check-in, atividade, resultado e histórico.
3. No painel, “Fechar ciclo aberto” (ciclo 4). Resultado: alerta A no Atendimento e alerta B mantido no Marketing.
4. Renata entra, abre a visão geral (print) e o detalhe do Atendimento (print), incluindo os gráficos de erro por área e de dificuldades.
5. Renata abre a recomendação (print), expande “Por que esta recomendação?” e clica em “Aceitar e ajustar escopo”.
6. Na tela de escopo (print), salva o escopo e agenda a comunicação da liderança. O detalhe do time mostra o alerta em acompanhamento e o marcador de intervenção.
7. No painel, “Avançar um ciclo” duas vezes (ciclos 5 e 6).
8. Renata abre o detalhe do Atendimento com o intervalo “ciclos 1 a 6” e usa “Comparar antes e depois” (print). O alerta aparece como resolvido.
9. Renata abre o detalhe do Financeiro e do Marketing (um print de cada).

**Fatia opcional, mas recomendada:** um script Playwright que executa este roteiro e salva os prints em `docs/evidencias/`, com nomes numerados por passo. Assim, as evidências podem ser reproduzidas.

## 10. Ordem de implementação

1. **Esqueleto:** projeto, `docker-compose.yml`, migration `V1`, segurança com os dois papéis, layout base (barra superior e menu lateral) a partir de `docs/referencia-visual/styles.css`. *Verificação:* login funciona para os dois papéis.
2. **Coleta:** check-in, atividades, resultado e histórico do colaborador, com testes.
3. **Diagnóstico:** indicadores, regras de alerta, recomendações e o serviço de fechamento. Testes unitários com os números da seção 8.
4. **Simulação:** seed, gerador, painel `/demo` e o teste das invariantes da seção 8.5.
5. **Gestão:** visão geral, detalhe do time, recomendação, escopo e “comparar antes e depois”.
6. **Roteiro e evidências:** `docs/roteiro-demonstracao.md`, o script Playwright (opcional) e o README final.

## 11. Fora do escopo

- Várias empresas.
- Integração com ferramentas de IA reais.
- E-mail ou notificações.
- CRUD de catálogos (tudo vem do seed).
- Pseudonimização no nível do banco.
- Aplicativo móvel.
- Internacionalização.

## 12. Definição de pronto

- O sistema sobe com `docker compose up -d` e `./gradlew bootRun --args='--spring.profiles.active=demo'`.
- Todos os testes passam, incluindo o de invariantes.
- `README.md` em pt-BR com:
  - como rodar;
  - usuários de demonstração;
  - arquitetura por pacote;
  - **onde fica cada regra** (classe e arquivo);
  - como a simulação atinge as metas;
  - limitações (dados programados, produtividade medida só na plataforma, intervenções humanas não executadas pelo sistema).
- `docs/decisoes.md` e `docs/roteiro-demonstracao.md` completos.