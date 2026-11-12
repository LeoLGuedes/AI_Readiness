# Prontidão IA: monitoramento da prontidão de times para IA generativa

Artefato de Design Science Research (6º período). É um sistema web que
acompanha, ciclo a ciclo, a prontidão de equipes para usar IA generativa no
trabalho. Ele combina duas fontes de dado:

- **desempenho**: atividades de letramento em IA, com acerto, erro e tempo por área;
- **percepção**: um check-in semanal curto, com confiança, receio de
  substituição e dificuldades.

Ao fim de cada ciclo, o sistema calcula indicadores por time, aplica regras de
alerta e recomenda intervenções à gestão. O caso central é o time que **sabe
usar a IA, mas não se sente pronto**: o técnico sobe enquanto a confiança cai.

Esta versão atende à etapa de **demonstração** da DSR, com dados simulados de
uma seguradora fictícia (Seguradora Aurora), 3 times de 8 pessoas e 6 ciclos
semanais.

| Documento | Conteúdo |
|---|---|
| `docs/especificacao.md` | especificação original (seções 0 a 12) |
| `docs/fundamentacao.md` | base teórica de cada pergunta, indicador e regra, com referências |
| `docs/roteiro-demonstracao.md` | passo a passo do UC01, clique a clique, com o nome de cada print |
| `docs/decisoes.md` | toda decisão tomada onde a especificação era omissa (D01 a D52) |
| `docs/plano-inicial.md` | plano aprovado antes da implementação |

---

## Como rodar

**Pré-requisitos:** JDK 21 e Docker Desktop (aberto).

A pasta do projeto fica em `C:\!Faculdade\...`, e o `!` no caminho quebra o
carregamento de classes do Java (decisão D25). Por isso os comandos rodam a
partir de uma unidade mapeada. Quem clonar em um caminho sem `!` pode pular o
`subst`.

```powershell
subst P: "C:\!Faculdade\6° Período\Pesquisa Aplicada\Artefato"
cd P:\
docker compose up -d
.\gradlew.bat bootRun --args='--spring.profiles.active=demo'
```

Abra **http://localhost:8080**. No primeiro uso, com o banco vazio, o sistema
já sobe com o cenário da seção 8.2 montado: ciclos 1 a 3 fechados e ciclo 4
aberto, esperando a Carla.

No Linux e no macOS: `docker compose up -d` e
`./gradlew bootRun --args='--spring.profiles.active=demo'`.

**Versão congelada para apresentar:** `.\gradlew.bat bootJar` gera
`build\libs\prontidao-ia-0.1.0.jar`. Copie o arquivo para outra pasta e rode
`java -jar prontidao-ia-0.1.0.jar --spring.profiles.active=demo`. Essa versão
não reinicia quando o código muda.

**Testes:** `.\gradlew.bat test`. São 82 testes, incluindo as 10 invariantes
da seção 8.5. Os testes sobem o próprio PostgreSQL com Testcontainers e
precisam só do Docker aberto.

## Usuários de demonstração

Todas as senhas são **`demo123`**. A tela de login lista os usuários quando
o perfil `demo` está ativo.

| Pessoa | E-mail | Papel |
|---|---|---|
| Renata Guimarães | `renata.guimaraes@aurora.com.br` | gestora de pessoas, gestora dos três times |
| Carla Menezes | `carla.menezes@aurora.com.br` | atendente, time Atendimento; faz o roteiro ao vivo |
| Demais 23 colaboradores | `nome.sobrenome@aurora.com.br` | nomes em `simulacao/PessoasDemo.java` |

O **Painel** (`/demo`, só no perfil `demo`) restaura o cenário, preenche o
roteiro da Carla, gera respostas, fecha e avança ciclos.

---

## Arquitetura por pacote

Monólito Spring Boot 3 + Thymeleaf + PostgreSQL 16 (Flyway), organizado por
funcionalidade. Raiz: `src/main/java/br/com/prontidaoia/`.

| Pacote | Responsabilidade |
|---|---|
| `usuario` | pessoas, times, papéis e login (`UsuarioAutenticado`) |
| `ciclo` | ciclos semanais; abertura do ciclo seguinte copiando o escopo (`CicloService`) |
| `checkin` | check-in semanal e catálogo de dificuldades |
| `atividade` | atividades, questões, tentativas, respostas e correção |
| `colaborador` | telas da colaboradora: início e histórico pessoal |
| `diagnostico.indicador` | cálculo dos indicadores (seção 5.1) |
| `diagnostico.alerta` | regras de alerta e encerramento (seção 5.2) |
| `diagnostico.recomendacao` | recomendações e decisões da gestão (seções 5.3 e 5.4) |
| `diagnostico.escopo` | troca de escopo (seção 5.4) |
| `diagnostico.fechamento` | fechamento do ciclo (seção 5.5), o único que grava indicador e alerta |
| `gestao` | telas da gestão (Quadros 1, 2 e 3, escopo) e a regra de privacidade |
| `simulacao` | cenário, gerador determinístico e painel `/demo`; separado do domínio |
| `config` | segurança, limiares, agendador de fechamento |
| `comum` | arredondamento, exceções e tratamento de erros |

Os limiares das regras ficam todos em `src/main/resources/application.yml`
(`prontidao.limiares`). Nenhuma classe de regra guarda número no código.

## Onde fica cada regra

As regras são **código puro**, sem banco e sem Spring. Cada uma tem uma
classe própria e testes com os números da seção 8.

| Regra (seção) | Classe | Teste |
|---|---|---|
| Técnico (5.1) | `diagnostico/indicador/CalculoTecnico.java` | `CalculoIndicadoresTest` |
| Confiança (5.1) | `diagnostico/indicador/CalculoConfianca.java` | `CalculoIndicadoresTest` |
| Resistência (5.1) | `diagnostico/indicador/CalculoResistencia.java` | `CalculoIndicadoresTest` |
| Engajamento (5.1) | `diagnostico/indicador/CalculoEngajamento.java` | `CalculoIndicadoresTest` |
| Produtividade (5.1) | `diagnostico/indicador/CalculoProdutividade.java` | `CalculoIndicadoresTest` |
| Erro por área (5.1) | `diagnostico/indicador/CalculoErroPorArea.java` | `CalculoIndicadoresTest` |
| Amostra mínima (5.1) | `diagnostico/indicador/AmostraMinima.java` | `CalculoIndicadoresTest`, `InvariantesDoCenarioTest` |
| Arredondamento (5.1) | `comum/Arredondamento.java` | `ArredondamentoTest` |
| Regra A: sabe usar, mas não se sente pronto (5.2) | `diagnostico/alerta/RegraDivergenciaTecnicoConfianca.java` | `RegrasDeAlertaTest` |
| Regra B: confiança alta sem senso crítico (5.2) | `diagnostico/alerta/RegraConfiancaSemVerificacao.java` | `RegrasDeAlertaTest` |
| Prioridade (5.2) | `diagnostico/alerta/RegraPrioridade.java` | `RegrasDeAlertaTest` |
| Sem duplicatas (5.2) | `diagnostico/alerta/AlertaService.java` + índice único `uq_alerta_nao_resolvido` | `FechamentoCicloServiceTest` |
| Encerramento do A (5.2) | `diagnostico/alerta/RegraEncerramentoDivergencia.java` | `RegrasDeAlertaTest` |
| Encerramento do B (5.2) | `diagnostico/alerta/RegraEncerramentoConfianca.java` | `RegrasDeAlertaTest` |
| Duas dificuldades mais marcadas (5.3) | `diagnostico/recomendacao/ContagemDificuldades.java` | `ContagemDificuldadesTest` |
| Recomendações dos alertas A e B (5.3) | `diagnostico/recomendacao/GeradorRecomendacoes.java` | `FechamentoCicloServiceTest` |
| Aceitar, ajustar, adiar, agendar (5.4) | `diagnostico/recomendacao/RecomendacaoService.java` | `TelasDaGestaoTest` |
| Troca de escopo, agora ou no próximo ciclo (5.4) | `diagnostico/escopo/EscopoService.java` | `TelasDaGestaoTest` |
| Fechamento em 6 etapas (5.5) | `diagnostico/fechamento/FechamentoCicloService.java` | `FechamentoCicloServiceTest` |
| Fechamento automático (5.5) | `config/AgendadorFechamento.java` | — |
| Um check-in por pessoa e ciclo | `checkin/CheckinService.java` | `CheckinServiceTest` |
| Finalizar só com tudo respondido | `atividade/Correcao.java` | `CorrecaoTest` |
| `finalizada_em` gravado uma vez | `atividade/Tentativa.java` | `ExecucaoAtividadeServiceTest` |
| **Privacidade** (4): gestor nunca vê check-in individual | `gestao/PrivacidadeGuard.java` + rotas separadas em `config/SecurityConfig.java` | `InvariantesDoCenarioTest` (invariante 9), `TelasDaGestaoTest` |

## Como a simulação atinge as metas

O código de simulação fica no pacote `simulacao`, separado do domínio.

1. **As metas da seção 8.4** estão transcritas em `PerfilTime.java`, por
   time e ciclo.
2. **O gerador (`GeradorDeterministico`) trabalha com totais fixos, e não com
   médias sorteadas.** Uma confiança de 63 com 8 check-ins vira "a soma de
   (resposta − 1) é 20", que dá 62,5. O mesmo vale para o receio, o número de
   atividades (que sai do engajamento), os acertos por área e os segundos
   totais.
3. **Ele gera respostas individuais**: a escala de cada pessoa, o acerto de
   cada questão e o tempo de cada tentativa. A semente fixa
   (`prontidao.demo.semente`) decide só quem erra cada questão, quem deixa
   pendência e os tempos; nunca os totais (decisão D01).
4. **Ele desconta o que já existe e o que está reservado.** No ciclo 4, as
   pendências da Carla ficam reservadas com os valores do roteiro (seção
   8.3). Por isso o total do time fecha tanto quando a Carla responde pela
   tela quanto pelo botão do painel (D42).
5. **Os indicadores nunca são gravados pela simulação.** "Restaurar cenário
   inicial" gera os ciclos 1 a 3 e os fecha pelo `FechamentoCicloService`, o
   mesmo caminho do agendador.

O teste `InvariantesDoCenarioTest` confere as 10 invariantes da seção 8.5,
as metas de todos os times nos 6 ciclos dentro da tolerância (±2 pontos,
±0,5 min), o acerto de 65% da Carla no ciclo 3 e o determinismo: restaurar
duas vezes dá exatamente os mesmos números.

## Limitações

- **Os dados são programados.** A melhora do Atendimento nos ciclos 5 e 6 está
  na tabela de metas e **não depende da intervenção**: acontece mesmo que a
  gestão adie a recomendação. A demonstração mostra que o sistema detecta,
  recomenda, registra e acompanha. Ela não prova que a intervenção causou a
  melhora (seção 8.2).
- **A produtividade é medida só na plataforma:** é o tempo entre abrir e
  finalizar a atividade, e não o tempo de trabalho real no dia a dia da
  pessoa.
- **As intervenções humanas não são executadas pelo sistema.** Ele registra a
  decisão e a data prevista (seção 5.4); a reunião da liderança acontece fora
  dele.
- **Cada construto do check-in tem um único item**, e não as escalas validadas
  completas. A escolha é deliberada, para o check-in levar menos de um minuto
  (ver `docs/fundamentacao.md`).
- **Os valores atingíveis têm limite aritmético.** Com 7 check-ins, a
  confiança do Atendimento no ciclo 4 só pode ser 46,4 ou 50,0; o sistema
  mostra 46, e não o 47 do exemplo da especificação (D48). Se a Carla fizer as
  atividades ao vivo, o tempo real dela muda a produtividade do ciclo 4.
- **O banco de questões tem um vício:** em várias questões a alternativa certa
  é a mais longa (D27). Isso não afeta a demonstração, mas deve ser revisto
  antes de qualquer uso com pessoas reais.
- Fora do escopo (seção 11): várias empresas, integração com IA real, e-mail,
  CRUD de catálogos, pseudonimização no banco, app móvel e internacionalização.

## Evidências

`docs/roteiro-demonstracao.md` descreve os 9 passos do UC01. O script
`ferramentas/evidencias/gravar-evidencias.js` executa o roteiro no Chrome da
máquina e salva os prints numerados em `docs/evidencias/`:

```powershell
cd P:\ferramentas\evidencias
npm install
node gravar-evidencias.js
```

O script restaura o cenário no passo 1. Os prints não vão para o
repositório, porque qualquer pessoa os refaz com esse comando.

## Estrutura de apoio

- `ferramentas/gerar_atividades.py`: banco das 45 questões originais. Gera
  `src/main/resources/catalogo/atividades.json` (as atividades 1A a 6B e a
  trilha).
- `src/main/resources/db/migration/`: `V1__esquema.sql` (tabelas da seção 4)
  e `V2__catalogos.sql` (áreas, dificuldades e intervenções da seção 5.3).
- `docs/referencia-visual/styles.css`: identidade visual do grupo; os
  acréscimos ficam em `src/main/resources/static/css/app.css`.
