# Protocolo de avaliação de usabilidade — PBL 7 / Prontidão IA

## Pergunta de pesquisa operacional
Os perfis COLABORADOR e GESTOR conseguem executar as tarefas centrais e compreender os indicadores para a tomada de decisão sem auxílio?

## Amostragem proposta (ainda não realizada)
- Convidar de 6 a 10 voluntários, idealmente distribuídos entre os dois perfis (ex.: 4 colaboradores simulados + 4 gestores simulados). É amostra exploratória por conveniência; não há representatividade estatística garantida.
- Preferir pessoas que NÃO participaram do desenvolvimento. Registrar familiaridade prévia com IA (baixa/média/alta), mas não coletar dados pessoais desnecessários.
- Todos devem consentir com participação e gravação de tela, caso haja gravação. Sem consentimento, registrar apenas métricas anonimizadas.
- Usar o cenário fictício da Seguradora Aurora; **não utilizar dados reais de funcionários**.

## Preparação
1. Iniciar Docker Desktop e PostgreSQL; executar aplicação no perfil `demo`.
2. Antes de **cada sessão** que necessite do estado inicial, restaurar o cenário usando `/demo` em uma sessão controlada do pesquisador. Não permitir que o participante restaure durante sua tarefa comum.
3. Cuidado: as tarefas alteram o banco de demonstração. Não executar testes de usabilidade e gravar outro vídeo simultaneamente.
4. Configurar cronômetro, tarefa impressa, ficha `coleta_participantes.csv`. Não apresentar o roteiro de cliques antes das tarefas.
5. Com poucos usuários fictícios, executar os participantes **sequencialmente** e restaurar entre eles para evitar sobreposição de respostas já concluídas.

## Tarefas (ver `tarefas_usabilidade.csv`)
COLABORADOR:
- C01 — Entrar com credenciais fornecidas.
- C02 — Encontrar atividade, responder as 10 questões e localizar pontuação.
- C03 — Fazer check-in informando percepção e dificuldades.
- C04 — Encontrar seu histórico e explicar o que mostra.

GESTOR:
- G01 — Identificar a equipe cujo técnico sobe e confiança cai; explicar o problema.
- G02 — Identificar a dificuldade de verificação no Marketing.
- G03 — Localizar alerta, abrir recomendação e explicar por que faz sentido.
- G04 — Aceitar uma recomendação, identificar mudança no escopo, acompanhar cenário posterior (o pesquisador controla a passagem do tempo).

**Importante:** Para G01–G04, o avaliador pode preparar o cenário já com o ciclo 4 fechado e com o alerta adequado. Essa preparação deve ser padronizada entre participantes e não contabilizada no tempo de execução de tarefas do gestor.

## Métricas (recolher por tarefa)
- sucesso: 1 se alcançou resultado correto, 0 caso contrário;
- tempo: diferença fim–início em segundos; medir de forma padronizada e registrar desistências;
- erros: contagem de ações equivocadas observáveis;
- auxílio: 1 se precisou receber orientação para concluir; 0 caso contrário;
- interpretação correta do problema: 1 quando gestor consegue explicar a divergência sem sugestão; 0 caso contrário.

### Critérios planejados para discussão, não resultados
- Conclusão >= 80% das tarefas sem ajuda.
- Identificação correta do Atendimento como divergência em >= 80% dos gestores participantes.
- Erros críticos de acesso indevido: zero.
- SUS médio >= 68 pontos como referência externa, **não** como prova de usabilidade, principalmente em amostra pequena.
- Tempos de execução: registrar mediana e maior valor observado; usar as referências temporais da planilha como *planejamento*, não limites universais.

## Pós-tarefa: questionário
Se escolher utilizar o **System Usability Scale (SUS)**, aplicar a versão padronizada de dez itens (Likert 1–5), sem reformular os itens, e registrar q1–q10 em `respostas_sus.csv`. O escore é 0–100, não é porcentagem. Itens ímpares: resposta - 1; itens pares: 5 - resposta; soma × 2,5. Ao acrescentar perguntas específicas do domínio, coloque-as fora do escore SUS.

Perguntas complementares (escala 1–5, sem entrar no SUS):
1. Consegui identificar facilmente o que deveria fazer no sistema.
2. A tela de resultado do quiz me ajudou a identificar meus erros.
3. (Gestor) Os indicadores mostraram diferenças relevantes entre equipes.
4. (Gestor) Entendi por que a recomendação foi apresentada.
5. Eu utilizaria uma ferramenta como esta em um contexto organizacional real (com as devidas proteções).

Pergunta aberta: "Qual parte foi mais difícil ou confusa? O que você melhoraria primeiro?"

## Análise
- Por tarefa: média e mediana dos tempos, n de acertos, proporção de sucesso, proporção de auxílio e erros frequentes.
- Por perfil: comparar tarefas e relatórios separadamente; evitar agregações que ocultem a experiência do gestor.
- SUS: pontuar individualmente e mostrar média, intervalo e tamanho da amostra; não chamar SUS de 80% de satisfação.
- Reportar TODOS os resultados, inclusive falhas, ausências, desistências e comentários negativos; não inventar participantes nem respostas.
- Encerrar com limitações: amostra pequena, ambiente controlado, dados simulados e efeito de aprendizagem.
