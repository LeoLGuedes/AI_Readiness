# Plano inicial — Monitoramento da prontidão para IA

> Documento de aprovação. Nenhum código será escrito antes do "ok" do grupo.
> Spec de origem: enunciado "Versão mínima funcional — Monitoramento da prontidão para IA" (seções 0 a 12).

---

## 1. Bloqueio de ambiente (precisa de decisão antes da Fatia 1)

Verificação feita nesta máquina:

| Item exigido pela spec | Situação encontrada |
|---|---|
| Java 21 (JDK) | Apenas **JRE 1.8.0_401**, sem JDK |
| Gradle | **não instalado** (o wrapper resolve, mas precisa de JDK 21) |
| Docker / docker compose | **não instalado** |
| Node (para Playwright, opcional) | v24.14.1 — OK |

Consequências diretas:

- Sem JDK 21 não há `./gradlew bootRun` nem compilação de teste algum.
- Sem Docker não há PostgreSQL 16 (§2) nem Testcontainers (§2), e a seção 12 exige `docker compose up -d`.

**Opções (escolha uma):**

- **(A) Instalar as ferramentas** — Temurin JDK 21 + Docker Desktop. É o caminho fiel à spec e o único que cumpre a seção 12 sem ressalvas. Recomendado.
- **(B) Instalar só o JDK 21 e desenvolver contra H2 em modo PostgreSQL**, mantendo `docker-compose.yml` e as migrations Flyway prontos para o Postgres. Os testes de integração rodariam em H2 em vez de Testcontainers, com uma nota no README. Desvio pequeno, mas é desvio.
- **(C) Trocar a stack** (por exemplo Node/TypeScript). Não recomendado: a spec fixa Java/Spring e a defesa oral vai citar a stack.

Enquanto isso não estiver resolvido, consigo escrever código, migrations, testes e templates, **mas não consigo executá-los nem provar que passam** — e a seção 0.2 exige que cada fatia termine com testes passando. Não vou declarar fatia concluída sem execução real.

---

## 2. Correção de caminho

A spec (§7) aponta `docs/referencia-visual/styles.css`. O arquivo está em `referencia-visual/styles.css`, na raiz. Na Fatia 1 ele será movido para `docs/referencia-visual/styles.css` e copiado para `src/main/resources/static/css/styles.css` (o `docs/` fica como referência do grupo; o `static/` é o que a aplicação serve). Acréscimos de classe vão para `static/css/app.css`, sem tocar na identidade.

---

## 3. Estrutura de pacotes

Monólito de um módulo, **organizado por funcionalidade** (não por camada técnica). Raiz: `br.com.prontidaoia`.

```
src/main/java/br/com/prontidaoia/
├── ProntidaoIaApplication.java
│
├── config/
│   ├── SecurityConfig.java              login por formulário, BCrypt, papéis
│   ├── LimiaresProperties.java          @ConfigurationProperties("prontidao.limiares")
│   ├── DemoProperties.java              total de ciclos, nomes de demonstração
│   ├── AgendadorFechamento.java         @Scheduled: fecha ciclo cujo "fim" passou
│   └── FaixaDemoAdvice.java             @ControllerAdvice: injeta a faixa "dados simulados"
│
├── comum/
│   ├── Arredondamento.java              meia para cima; 1 casa (banco) e inteiro (tela)
│   ├── excecao/                         RegraVioladaException, RecursoNaoEncontradoException
│   └── web/TratadorDeErros.java
│
├── usuario/
│   ├── Usuario.java  Papel.java  UsuarioRepository.java
│   ├── TimeEntity.java  TimeRepository.java
│   └── UsuarioDetailsService.java
│
├── ciclo/
│   ├── Ciclo.java  StatusCiclo.java  CicloRepository.java
│   └── CicloService.java                ciclo aberto, abertura do próximo
│
├── checkin/
│   ├── Checkin.java  Dificuldade.java  CheckinDificuldade.java
│   ├── CheckinRepository.java  DificuldadeRepository.java
│   ├── CheckinService.java              regra "um por usuário/ciclo"
│   └── CheckinController.java           GET/POST /checkin
│
├── atividade/
│   ├── Area.java  Atividade.java  Questao.java
│   ├── AtividadeLiberada.java  Tentativa.java  Resposta.java
│   ├── (repositórios correspondentes)
│   ├── ExecucaoAtividadeService.java    inicia tentativa, grava respostas, finaliza
│   ├── CorrecaoService.java             gabarito comentado
│   └── AtividadeController.java         /atividades/{id}, /finalizar, /resultado
│
├── diagnostico/
│   ├── indicador/
│   │   ├── IndicadorTimeCiclo.java  IndicadorRepository.java
│   │   ├── CalculoTecnico.java          ← §5.1 linha 1  (código puro)
│   │   ├── CalculoConfianca.java        ← §5.1 linha 2  (código puro)
│   │   ├── CalculoResistencia.java      ← §5.1 linha 3  (código puro)
│   │   ├── CalculoEngajamento.java      ← §5.1 linha 4  (código puro)
│   │   ├── CalculoProdutividade.java    ← §5.1 linha 5  (código puro)
│   │   ├── CalculoErroPorArea.java      ← §5.1 linha 6  (leitura, sem snapshot)
│   │   ├── AmostraMinima.java           ← §5.1 "amostra mínima"
│   │   └── CalculadoraIndicadores.java  orquestra as classes acima
│   ├── alerta/
│   │   ├── Alerta.java  TipoAlerta.java  PrioridadeAlerta.java  StatusAlerta.java
│   │   ├── DetalheAlerta.java           payload jsonb tipado
│   │   ├── RegraDivergenciaTecnicoConfianca.java   ← §5.2 A  (código puro)
│   │   ├── RegraConfiancaSemVerificacao.java       ← §5.2 B  (código puro)
│   │   ├── RegraPrioridade.java                    ← §5.2 prioridade
│   │   ├── RegraEncerramentoDivergencia.java       ← §5.2 encerramento A
│   │   ├── RegraEncerramentoConfianca.java         ← §5.2 encerramento B
│   │   └── AlertaService.java           persistência, dedupe, transições
│   ├── recomendacao/
│   │   ├── Recomendacao.java  Intervencao.java  StatusRecomendacao.java
│   │   ├── ContagemDificuldades.java    ← §5.3 "duas mais marcadas" (código puro)
│   │   ├── GeradorRecomendacoes.java    ← §5.3
│   │   └── RecomendacaoService.java     aceitar / ajustar / adiar / agendar
│   ├── escopo/
│   │   └── EscopoService.java           ← §5.4 troca de atividade liberada
│   └── fechamento/
│       └── FechamentoCicloService.java  ← §5.5 (transacional, 6 etapas)
│
├── colaborador/
│   ├── InicioController.java            /inicio
│   └── HistoricoController.java         /meu-historico
│
├── gestao/
│   ├── GestaoController.java            /gestao, /gestao/times/{id}
│   ├── RecomendacaoController.java      /gestao/alertas/{id}/recomendacao
│   ├── EscopoController.java            /gestao/times/{id}/escopo
│   ├── consulta/VisaoGeralQuery.java    projeções agregadas (nunca linha de checkin)
│   ├── consulta/DetalheTimeQuery.java
│   └── PrivacidadeGuard.java            ← §4 regra de privacidade, testada
│
└── simulacao/                           (perfil demo; separado do domínio)
    ├── SeedCatalogos.java               áreas, dificuldades, intervenções, questões
    ├── SeedPessoas.java                 Renata, Carla + 23 nomes fictícios
    ├── PerfilTime.java                  curvas-alvo por time (§8.4)
    ├── GeradorDeterministico.java       semente fixa; gera respostas individuais
    ├── RoteiroCarla.java                ← §8.3
    ├── CenarioInicial.java              ← §8.2 (usa o serviço de fechamento)
    ├── RegistroAcoesDemo.java           log das ações do painel
    └── DemoController.java              /demo e os 5 botões
```

**Por que assim:** cada linha da tabela §5.1 e cada regra da §5.2 tem **uma classe própria, com nome que repete o texto da spec**, sem dependência de Spring ou JPA — recebem *records* de entrada e devolvem números. Na defesa oral, "onde está a regra X" tem resposta de uma linha, e o teste unitário ao lado documenta os números da §8.

---

## 4. Rotas e telas

### Autenticação
| Método | Rota | Papel | Tela / efeito |
|---|---|---|---|
| GET | `/login` | público | Login (mostra usuários de demonstração no perfil `demo`) |
| POST | `/login` | público | Spring Security |
| POST | `/logout` | autenticado | — |
| GET | `/` | autenticado | Redireciona: `COLABORADOR`→`/inicio`, `GESTOR`→`/gestao` |

### Colaborador
| Método | Rota | Tela / efeito |
|---|---|---|
| GET | `/inicio` | Ciclo atual, pendências, progresso |
| GET | `/checkin` | Formulário (3 perguntas + aviso de privacidade) |
| POST | `/checkin` | Grava e volta a `/inicio` com confirmação |
| GET | `/atividades/{idLiberada}` | 10 questões + cronômetro (cria a tentativa) |
| POST | `/atividades/{idLiberada}/finalizar` | Exige todas respondidas; grava respostas e `finalizada_em` |
| GET | `/atividades/{idLiberada}/resultado` | Acertos, tempo, gabarito comentado |
| GET | `/meu-historico` | Gráfico próprio (acerto × confiança) + tabela |

### Gestor
| Método | Rota | Tela / efeito |
|---|---|---|
| GET | `/gestao?ciclo=N` | Quadro 1 — visão geral dos times |
| GET | `/gestao/times/{id}?de=1&ate=N` | Quadro 2 — detalhe do time |
| GET | `/gestao/times/{id}/series.json` | Séries para o Chart.js |
| GET | `/gestao/alertas/{id}/recomendacao` | Quadro 3 — recomendação |
| POST | `/gestao/alertas/{id}/recomendacao/aceitar` | `ACEITA` → redireciona ao escopo |
| POST | `/gestao/alertas/{id}/recomendacao/ajustar` | FA1: `AJUSTADA` + `descricao_ajustada` |
| POST | `/gestao/alertas/{id}/recomendacao/adiar` | `ADIADA` |
| GET | `/gestao/times/{id}/escopo` | Escopo atual × proposto + intervenções humanas |
| POST | `/gestao/times/{id}/escopo` | Salva escopo, alerta → `EM_ACOMPANHAMENTO` |
| POST | `/gestao/recomendacoes/{id}/agendar` | `data_prevista` da intervenção humana |

### Demonstração (só com perfil `demo`)
| Método | Rota | Efeito |
|---|---|---|
| GET | `/demo` | Ciclo aberto, pendências por time, registro de ações |
| POST | `/demo/restaurar` | Restaurar cenário inicial (§8.2) |
| POST | `/demo/carla` | Preencher pendências da Carla conforme o roteiro (§8.3) |
| POST | `/demo/gerar-respostas` | Gerar respostas simuladas do ciclo aberto |
| POST | `/demo/fechar-ciclo` | Fechar ciclo aberto |
| POST | `/demo/avancar-ciclo` | Gerar + fechar |

---

## 5. Wireframes (§7)

### 5.1 `/gestao` — visão geral dos times (Quadro 1)

```
+------------------------------------------------------------------------------+
| [#] Prontidao IA        Ambiente de demonstracao: dados simulados     [RG v]  |
+--------------+---------------------------------------------------------------+
| GESTAO       |  Visao geral dos times                   Ciclo: [ 4 de 6  v ]  |
| > Visao geral|  Ciclo 4 - 22/09 a 28/09 - fechado                             |
|   Times      |                                                                |
|   Alertas    |  +-- Maior erro da empresa no ciclo 4 --------------------+    |
|              |  | (!) Verificacao das respostas da IA - 42% de erro      |    |
| DEMONSTRACAO |  +-------------------------------------------------------+    |
|   Painel     |                                                                |
|              |  +-- Atendimento ------------ [ALTA] Sabe usar, mas nao -+     |
|              |  |                                   se sente pronto     |     |
|              |  |  Tecnico    Confianca   Resistencia   Engajamento     |     |
|              |  |   72 ^+5      47 v-6      63 ^+10       79 v-13       |     |
|              |  |  Tempo medio 10,9 min - 7 de 8 check-ins              |     |
|              |  |                            [ Ver detalhe do time -> ] |     |
|              |  +-------------------------------------------------------+     |
|              |  +-- Financeiro ------------------------- sem alertas ---+     |
|              |  |   69 ^+4      53 ^+6      31 v-3        92 ^+4        |     |
|              |  |  Tempo medio 13,0 min - 8 de 8 check-ins  [ Detalhe ] |     |
|              |  +-------------------------------------------------------+     |
|              |  +-- Marketing ------ [NORMAL] Confianca alta sem senso -+     |
|              |  |                            critico                    |     |
|              |  |   67 ^+1      84  = 0     13  = 0       96 v-4        |     |
|              |  |  Tempo medio 9,4 min - 8 de 8 check-ins   [ Detalhe ] |     |
|              |  +-------------------------------------------------------+     |
|              |  +-- Maior ganho de confianca desde o ciclo 1 -----------+     |
|              |  | Financeiro: +19 pontos (34 -> 53)                     |     |
|              |  +-------------------------------------------------------+     |
+--------------+---------------------------------------------------------------+
```

Notas de implementação: cartão por time = `.card` + `.grid-2`/`.grid-5`; deltas com `.delta.up` / `.delta.down`; selo de alerta = `.badge` (vermelho `--danger` para ALTA, âmbar `--warning` para NORMAL) **sempre acompanhado do nome escrito**; números com `font-variant-numeric: tabular-nums`.

### 5.2 `/gestao/times/{id}` — detalhe do time (Quadro 2)

```
+------------------------------------------------------------------------------+
| [#] Prontidao IA        Ambiente de demonstracao: dados simulados     [RG v]  |
+--------------+---------------------------------------------------------------+
| GESTAO       | <- Visao geral                                                 |
|   Visao geral| Atendimento - 8 membros - ciclo 4 de 6                          |
| > Times      |                                                                |
|   Alertas    | +-- [ALTA] Sabe usar, mas nao se sente pronto - ABERTO -----+   |
|              | | Gerado no fechamento do ciclo 4                           |   |
| DEMONSTRACAO | | "Tecnico subiu de 61 para 67 e 72; confianca caiu de 59    |   |
|   Painel     | |  para 53 e 47." Engajamento caiu 13 pontos -> prioridade   |   |
|              | |  alta.                             [ Ver recomendacao -> ]|   |
|              | +-----------------------------------------------------------+   |
|              | +-- Indicadores por ciclo ------ intervalo [ ciclos 1 a 4 v]+   |
|              | |100 |                                 :                    |   |
|              | |    |        ___ tecnico ______/      : intervencao        |   |
|              | | 50 |  \___ confianca ___             :                    |   |
|              | |    |  ___ resistencia ___/           :                    |   |
|              | |  0 +--C1----C2----C3----C4----C5----C6--+                 |   |
|              | | (o) Tecnico (o) Confianca (o) Resistencia (o) Engajamento |   |
|              | +-----------------------------------------------------------+   |
|              | +-- Erro por area - ciclo 4 --+ +-- Dificuldades relatadas -+   |
|              | | Formulacao  ####-----  23%  | | Receio de subst. ### 5    |   |
|              | | Verificacao ######---  33%  | |   (71% de 7 respostas)    |   |
|              | | Dados       ####-----  25%  | | Nao sei confiar   ##  3   |   |
|              | | Aplicacao   ######---  33%  | | Falta de tempo    #   2   |   |
|              | +-----------------------------+ | Nao se encaixa    #   1   |   |
|              |                                 +---------------------------+   |
|              | +-- Produtividade ------------+ +-- Amostra ----------------+   |
|              | | Ciclo 1: 14,0 min por ativ. | | (i) 7 de 8 check-ins      |   |
|              | | Ciclo 4: 10,9 min  (-3,1)   | |     respondidos           |   |
|              | | Ler junto com o tecnico (72)| |     (minimo de 5)         |   |
|              | +-----------------------------+ +---------------------------+   |
|              | +-- Comparar antes e depois - (so com intervencao) ---------+   |
|              | | Indicador     C4 (antes)      C5          C6              |   |
|              | | Tecnico           72          73          75              |   |
|              | | Confianca         47          50          56              |   |
|              | | Resistencia       63          59          53              |   |
|              | | Engajamento       79          83          92              |   |
|              | +-----------------------------------------------------------+   |
+--------------+---------------------------------------------------------------+
```

Cores fixas por indicador em **todos** os gráficos: Técnico `#8A0638` (primária), Confiança `#177245`, Resistência `#B42318`, Engajamento `#A15C00`. Legenda sempre visível. Sem amostra (`n_checkins < 5`), o cartão de dificuldades é substituído pela caixa `.alert.warning` com o texto de amostra insuficiente.

---

## 6. Ordem de implementação (fatias verticais)

Cada fatia termina com: testes executados e passando, roteiro de verificação manual no corpo do commit e **um commit**. Nada é dado como pronto sem saída de teste real.

### Fatia 1 — Esqueleto
Projeto Gradle + Spring Boot 3, `docker-compose.yml` (Postgres 16), migration `V1__esquema.sql` com todas as tabelas da §4 (incluindo o `CHECK` de `COLABORADOR ⇒ time_id NOT NULL` e o índice único parcial de ciclo aberto), `SecurityConfig` com os dois papéis e BCrypt, layout Thymeleaf base (barra superior + menu lateral + faixa demo) a partir do `styles.css`.
**Testes:** contexto sobe; `/gestao` nega `COLABORADOR`; `/inicio` nega `GESTOR`; login válido e inválido.
**Verificação manual:** login com um usuário de cada papel.

### Fatia 2 — Coleta (colaborador)
`/inicio`, `/checkin`, `/atividades/{id}`, `/resultado`, `/meu-historico`.
**Testes:** check-in duplicado é rejeitado; finalizar com questão em branco é rejeitado; correção conta acertos e `tempo_segundos`; `finalizada_em` só é gravado uma vez; `/meu-historico` de um usuário nunca devolve dados de outro.

### Fatia 3 — Diagnóstico
As classes de cálculo, as 5 regras, o gerador de recomendações e o `FechamentoCicloService`.
**Testes unitários (código puro, sem banco)** reproduzindo os números da §8.4: a regra A dispara no ciclo 4 e não no 3; a regra B dispara no Marketing a partir do ciclo 2; prioridade ALTA por queda de 13 no engajamento; encerramento A em dois ciclos de alta; `n_checkins < 5` anula confiança/resistência e suspende as regras.

### Fatia 4 — Simulação
Seed de catálogos, pessoas, atividades 1A…6B e a trilha; gerador determinístico; `/demo` com os cinco botões e o registro de ações; teste de integração das 10 invariantes da §8.5.

### Fatia 5 — Gestão
`/gestao`, detalhe do time, recomendação, escopo, "comparar antes e depois".
**Testes:** as 3 decisões do gestor; troca de escopo no ciclo aberto × próximo ciclo (conforme haja tentativa iniciada); `PrivacidadeGuard` — nenhuma rota do gestor devolve check-in individual.

### Fatia 6 — Roteiro e evidências
`docs/roteiro-demonstracao.md`, script Playwright opcional gravando em `docs/evidencias/`, README final e `docs/decisoes.md` fechado.

---

## 7. Pontos ambíguos e decisões propostas

Cada item abaixo vira uma entrada em `docs/decisoes.md` (decisão, alternativa descartada, motivo). Marque os que quiser mudar.

**Consistência dos dados da §8**

1. **`±2` de tolerância × regras de limiar exato.** A regra A exige confiança(C1) − confiança(C2) **< 5** para *não* disparar no ciclo 3 (alvos 63 e 59 → 4). Com tolerância de ±2 a simulação poderia produzir 64 e 58 (= 6) e quebrar a invariante 1. **Decisão:** as distribuições de check-in por time/ciclo serão **tabelas fixas de respostas individuais**, não sorteio dentro da tolerância; a semente aleatória governa só o que não afeta limiar (quais questões cada pessoa erra, segundos de cada tentativa). A tolerância vira margem de conferência, não de geração.
2. **Atendimento, ciclo 4: "acerto em verificação 68%" (§8.4) × "erro em verificação 33%" (§8.4, tabela de erro por área, logo 67% de acerto).** Divergência de 1 ponto, dentro da tolerância. **Decisão:** para o ciclo 4 vale a tabela de erro por área (é ela que a invariante 6 e o Quadro 2 usam); para os demais ciclos vale a linha de acerto em verificação.
3. **"Erro médio da empresa" (invariante 6 e faixa do Quadro 1).** **Decisão:** agrupado sobre todas as respostas de todos os times no ciclo (*pooled*), não média das três porcentagens. Com times do mesmo tamanho os dois dão o mesmo resultado; o agrupado é o mais defensável.
4. **Conferência aritmética já feita:** engajamento sempre sobre 24 pendências (8 × (1 + 2)) — 96% = 23/24, 92% = 22/24, 79% = 19/24 etc. ✔ E o técnico bate com a média das quatro áreas no ciclo 4 (Atendimento 71,5 ≈ 72; Financeiro 68,5 ≈ 69; Marketing 66,5 ≈ 67). ✔ As tabelas da §8 são internamente consistentes.

**Catálogo e regras**

5. **Duas intervenções de trilha.** A dificuldade "não sei quando posso confiar" liga à trilha "Verificação de respostas"; o alerta B usa `TRILHA_VERIFICACAO_CHECKLIST`. **Decisão:** são duas intervenções distintas no catálogo (`TRILHA_VERIFICACAO` e `TRILHA_VERIFICACAO_CHECKLIST`), ambas `PLATAFORMA`; as duas apontam para a mesma atividade de trilha no escopo.
6. **`alerta.detalhe` (jsonb).** **Decisão:** formato fixo `{regra, ciclo, valores:{tecnico:[c2,c1,c], confianca:[...], engajamento:[c1,c], verificacao:[c1,c]}, frase}` — a `frase` é exatamente o texto exibido no Quadro 2, gerado no fechamento e nunca recalculado na tela.
7. **Recomendação de intervenção humana não troca escopo**, só recebe `data_prevista` — e "Salvar escopo" não exige que a data já esteja preenchida.
8. **Alerta B não gera troca de escopo automática** neste roteiro (o Marketing não recebe decisão da gestora na §9). A recomendação é criada e fica `SUGERIDA`.
9. **Tentativas não finalizadas** não entram em técnico nem em produtividade, e contam como pendência não cumprida no engajamento.
10. **Empate na contagem de dificuldades** resolvido pela ordem do catálogo, que é a ordem da tabela da §5.3.

**Produto e conteúdo**

11. **Nome da seguradora** não consta na spec. **Decisão:** "Seguradora Aurora"; e-mails `nome.sobrenome@aurora.com.br`.
12. **Senhas de demonstração:** todas `demo123`, exibidas em `/login` e `/demo` só com o perfil `demo`.
13. **Cronômetro e `tempo_segundos`.** **Decisão:** o cronômetro da tela é JavaScript; `tempo_segundos` por questão é o intervalo entre marcar uma alternativa e a anterior, enviado em campo oculto. A produtividade da §5.1 usa `finalizada_em − iniciada_em`, não a soma por questão (a spec manda assim) — `tempo_segundos` serve para o gabarito e para análise futura.
14. **"Nenhuma" no check-in** grava o check-in com zero dificuldades ligadas e é mutuamente exclusiva com as demais.
15. **Total de ciclos** em `application.yml` (`prontidao.ciclos.total: 6`), exibido como "Ciclo 4 de 6".
16. **Renata é gestora dos três times**; o papel `GESTOR` tem `time_id` nulo.
17. **`/demo` exige autenticação** (qualquer papel) e existe só com o perfil `demo`; fora dele, 404.
18. **Gráfico do `/meu-historico`** mostra acerto pessoal e confiança pessoal por ciclo — é o único lugar do sistema com percepção individual, e só para a própria pessoa.

**Escopo**

19. **Playwright** é a fatia opcional §9. Node 24 está disponível, então recomendo fazer: as evidências passam a ser reprodutíveis, o que ajuda o relatório de DSR.

---

## 8. O que preciso de você agora

1. Escolha da opção **A / B / C** da seção 1 (bloqueio de ambiente).
2. "Ok" nos wireframes da seção 5 e nas decisões da seção 7 — ou as correções que quiser.
3. Confirmação do nome da seguradora (item 11) e se quer o Playwright (item 19).

Com isso, começo pela Fatia 1.
