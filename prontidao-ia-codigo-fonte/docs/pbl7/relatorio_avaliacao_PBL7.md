# Relatório de Avaliação — PBL 7 | DSR Etapa 5
## Artefato Prontidão IA — versão de trabalho (preencher após executar o protocolo)

**Data da análise do pacote:** 15/10/2026  

**Versão declarada em build.gradle:** 0.1.0 — Java 21 / Spring Boot 3.3.5 / Gradle 8.10.2 / PostgreSQL 16.  
**Estado deste relatório:** parcial, com resultados históricos do pacote e método proposto. NÃO É laudo de execução dos novos testes.

## 1. Problema de pesquisa e objetivo

Organizações que avaliam a prontidão para IA de forma pontual deixam de perceber divergências entre competência técnica, segurança e confiança. O Prontidão IA combina resultados de atividades de IA generativa com check-ins semanais para produzir indicadores por equipe, detectar divergências, recomendar ações e acompanhar seis ciclos.

O objetivo desta avaliação é verificar a aderência funcional aos requisitos, robustez das restrições de segurança e privacidade, reprodutibilidade da demonstração, tempos observados e capacidade dos perfis COLABORADOR e GESTOR de compreender e usar a aplicação.

## 2. Metodologia

Avaliação mista com quatro frentes:

1. **Testes técnicos já existentes** — JUnit 5, MockMvc, Spring Security Test e Testcontainers PostgreSQL: regressão, regras, UC01.
2. **Testes técnicos complementares PBL 7** — proteção CSRF, autorização negativa, parser de formulários e reprodutibilidade da restauração. Classes adicionadas apenas em `src/test`.
3. **Medição de desempenho local** — trinta GETs sequenciais na rota pública `/login`, após três aquecimentos; média, p50, p95, falhas HTTP. Medição exploratória não substitui teste de carga e não caracteriza outras rotas.
4. **Avaliação de usabilidade/utilidade** — sessões com 6–10 participantes voluntários, por conveniência; tarefas controladas por perfil e questionário padronizado SUS opcional.

Para os critérios funcionais do caso UC01, utilizar como referência primária os **dez invariantes da seção 8.5** de `docs/especificacao.md`.

## 3. Evidência técnica anterior disponível

Nos arquivos XML do pacote em `build/test-results/test`, foram identificadas **16 suites XML contendo 82 testes, 0 falhas, 0 erros e 0 ignorados**. Essa contagem demonstra que uma execução anterior da versão empacotada foi aprovada. Ela não comprova que os testes adicionais deste kit passaram, nem representa a experiência de usuários reais.

As fotos e a demonstração no navegador evidenciam que o perfil `demo` inicializa e apresenta o Painel. É evidência de execução local, mas não substitui ensaios completos com coleta de métricas.

## 4. Resultados técnicos — preencher após executar

| Indicador | Origem | Resultado | Critério de avaliação |
|---|---|---|---|
| Testes JUnit novos e existentes | `build/reports/tests/test/index.html` e CSV | PENDENTE DE EXECUÇÃO | 100% críticos sem falhas; registrar qualquer falha |
| Proteção CSRF nas ações demo | `AvaliacaoSegurancaPbl7Test` | PENDENTE | HTTP 403 sem CSRF |
| Bloqueio de ações gestoras a colaborador | `AvaliacaoSegurancaPbl7Test` | PENDENTE | HTTP 403 |
| Entradas inválidas no formulário | `AvaliacaoEntradaPbl7Test` | PENDENTE | Não aceitação como resposta válida |
| Repetição do cenário | `AvaliacaoReprodutibilidadePbl7Test` | PENDENTE | 9 fotografias iguais e ciclo 4 aberto |
| Média / p50 / p95 `/login` | `avaliacao/resultados/pbl7_resposta_login.csv` | PENDENTE | Registrar valores, sem teto arbitrário retroativo |
| Tempo do fluxo colaborador | `coleta_participantes.csv` | PENDENTE | Registrar mediana e variação |
| Conclusão de tarefas sem ajuda | `coleta_participantes.csv` | PENDENTE | Referência exploratória >=80% |
| SUS por participante (opcional) | `respostas_sus.csv` | PENDENTE | Referência aproximada de 68 pontos, com ressalvas |
| Interpretação do alerta Atendimento | Tarefa G01 | PENDENTE | >=80% sem ajuda (meta planejada) |

## 5. Mapeamento de qualidade / atendimento aos objetivos

| Objetivo funcional | Teste ou evidência |
|---|---|
| Coletar competência técnica | Quiz, correção, tempo, resultado do colaborador (T04–T05) |
| Coletar percepção | Check-in válido, inválido, duplicado (T06) |
| Consolidar indicadores sem expor indivíduos | T07–T08 e testes negativos de autorização |
| Identificar divergências | Regras A/B, limites, prioridades (T09) |
| Sugerir intervenções rastreáveis | Recomendação, justificativa, aceite, ajuste e adiamento (T10) |
| Acompanhar ciclos | Fechamento e mudança de escopo (T11) |
| Reproduzir demonstração | T12 |
| Ser compreensível | C01–C04, G01–G04 e SUS opcional |

## 6. Ponto de atenção: permissões do Painel demo

`SecurityConfig` exige login para `/demo`, mas não restringe a página ao papel GESTOR. `DemoController`, ativo somente no perfil `demo`, contém ações como restaurar e avançar ciclos. Um colaborador autenticado pode acessar esse painel no perfil de demonstração. Isso é uma **limitação de segurança do modo acadêmico**; não apresentar como apropriado a produção. O teste de caracterização registra o comportamento sem mudar regras aprovadas.

## 7. Pontos fortes já sustentados pelos arquivos

- Separação de papéis em `/gestao/**` e rotas de colaborador.
- Privacidade por amostra mínima de 5 check-ins.
- Regras A/B parametrizadas em YAML.
- Banco com PostgreSQL, migrações Flyway e serviço de fechamento transacional.
- UC01 determinístico, seis ciclos e relatórios automatizados anteriores com 82 testes sem falhas.

## 8. Limitações científicas e técnicas

- A simulação programa comportamentos posteriores à intervenção: melhora dos indicadores **não demonstra causalidade**.
- A amostra de usabilidade sugerida é pequena e de conveniência; resultados não generalizáveis.
- Tempo de `/login` em localhost não representa desempenho de toda a aplicação, usuários simultâneos ou disponibilidade em produção.
- Privacidade por agregação e mínimo de amostra não substitui avaliação completa LGPD, riscos de inferência ou anonimização real.
- Modo `demo` contém ações privilegiadas acessíveis a usuários autenticados; não deve ser exposto publicamente.
- Sem avaliação com equipes reais nem integração com ferramentas externas de IA nesta versão.

## 9. Análise crítica — completar com resultados reais

**O que funcionou:** [inserir casos comprovados, tabelas e telas].  
**O que falhou:** [inserir erros reais, incluindo testes não executados].  
**Melhorias prioritárias:** [classificar por gravidade + impacto + esforço].  
**Avaliação da utilidade percebida:** [dados de observação e questionário].

## 10. Conclusão parcial

O pacote mostra um artefato executável com evidências de testes técnicos aprovados anteriormente, além de mecanismos de diagnóstico e simulação coerentes com a especificação. A **Etapa 5 só estará concluída** após executar novamente a suíte, aplicar o protocolo de usabilidade com participantes, coletar tempos, analisar falhas e preencher esta avaliação com resultados verificáveis.

## 11. Referências sugeridas

- Especificação e fundamentação internas: `docs/especificacao.md`, `docs/fundamentacao.md`.
- ISO/IEC 25010:2023: https://www.iso.org/standard/78176.html
- OWASP ASVS (controle de acesso): https://github.com/OWASP/ASVS/blob/master/4.0/pt/0x12-V4-Access-Control.md
- System Usability Scale (SUS), Brooke (1996); pontuação e referências: https://www.sciencedirect.com/topics/computer-science/system-usability-scale
