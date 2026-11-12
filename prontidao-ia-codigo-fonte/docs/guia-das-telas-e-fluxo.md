# Guia das telas e do fluxo do sistema

Este guia explica, em linguagem de conversa, o que cada tela do Prontidão IA
faz, por que ela existe e como uma ação numa tela provoca mudanças nas
outras. Ele foi escrito para quem vai apresentar o sistema, escrever o
relatório ou responder perguntas na defesa oral sem precisar abrir o código.

Se você quer só o passo a passo da demonstração, use
`docs/roteiro-demonstracao.md`. Se quer saber de onde vem cada pergunta e cada
regra na literatura, use `docs/fundamentacao.md`. Este documento fica entre
os dois: conta a história do sistema funcionando.

---

## 1. A ideia em poucas palavras

Imagine uma seguradora que liberou um assistente de IA generativa para os
funcionários. A área de pessoas quer saber se as equipes estão prontas para
usar essa ferramenta. O jeito tradicional seria aplicar um questionário uma
única vez. O problema é que um questionário único tira uma foto de um momento
e não mostra **onde** nem **quando** as pessoas travam.

O Prontidão IA faz diferente. Ele acompanha as equipes **semana a semana**
(cada semana é um **ciclo**) e cruza duas fontes de informação:

- **O que as pessoas conseguem fazer.** Toda semana, cada colaborador faz
  duas atividades curtas, de 10 questões cada, sobre o uso da IA no trabalho:
  como pedir as coisas à IA, como conferir o que ela responde, como proteger
  dados e como aplicar a IA nas tarefas do setor. Isso é o **desempenho**.
- **Como as pessoas se sentem.** Toda semana, cada colaborador responde um
  **check-in** de três perguntas: quão seguro se sente, quanto teme ser
  substituído pela IA e que dificuldades teve. Isso é a **percepção**.

No fim de cada semana, o sistema **fecha o ciclo**. Ele junta as respostas de
cada time, calcula indicadores, procura padrões preocupantes e, quando acha
um, **avisa a gestora e sugere o que fazer**. A gestora decide se aceita a
sugestão, e o sistema acompanha nas semanas seguintes para ver se a situação
melhorou.

O caso que o sistema foi feito para pegar é o do time que **sabe usar a IA,
mas não se sente pronto**. O desempenho sobe semana após semana, mas a
confiança das pessoas cai. Um questionário único nunca veria isso, porque é
uma tendência que só aparece quando se compara uma semana com a outra.

---

## 2. Quem usa o sistema

O sistema tem dois tipos de pessoa, e cada um enxerga um mundo diferente.

**A colaboradora (papel COLABORADOR).** No cenário, a personagem principal é a
**Carla Menezes**, atendente do time de Atendimento. A colaboradora faz o
check-in, faz as atividades, vê o resultado e acompanha o próprio histórico.
Ela **nunca** vê dados de outras pessoas nem dos times.

**A gestora (papel GESTOR).** No cenário, é a **Renata Guimarães**, gestora de
pessoas e responsável pelos três times (Atendimento, Financeiro e Marketing).
A gestora vê os indicadores de cada time, os alertas e as recomendações, e
toma as decisões. Ela **nunca** vê a resposta de uma pessoa específica.

Essa separação é uma regra central, e o sistema a garante em mais de um
lugar. Se a Renata digitar o endereço de uma tela de colaboradora, recebe
"Acesso não permitido". O mesmo acontece com a Carla nas telas da gestão.

Além dos dois papéis, existe o **Painel de demonstração**. Ele não faz parte
do produto: é uma ferramenta para conduzir a apresentação, que simula o
passar das semanas. Qualquer pessoa logada o vê, mas só quando o sistema roda
no modo de demonstração.

---

## 3. O coração do sistema: o ciclo semanal

Tudo gira em torno do ciclo. Entender o ciclo é entender o sistema.

```
  ┌──────────────── CICLO ABERTO (uma semana) ────────────────┐
  │                                                            │
  │  Cada colaborador tem 3 pendências:                        │
  │    • 1 check-in                                            │
  │    • 2 atividades (ex.: 4A e 4B)                           │
  │                                                            │
  │  As pessoas respondem quando quiserem durante a semana.    │
  └──────────────────────────┬─────────────────────────────────┘
                             │  a semana acaba
                             ▼
  ┌──────────────── FECHAMENTO DO CICLO ──────────────────────┐
  │  1. O ciclo é marcado como fechado                         │
  │  2. O sistema confere se cada time teve respostas          │
  │     suficientes (pelo menos 5 check-ins)                   │
  │  3. Calcula e grava os indicadores de cada time            │
  │  4. Verifica se algum alerta antigo pode ser encerrado     │
  │     e depois procura alertas novos                         │
  │  5. Para cada alerta novo, gera as recomendações           │
  │  6. Abre o próximo ciclo com as atividades da semana       │
  └──────────────────────────┬─────────────────────────────────┘
                             │
                             ▼
               A gestora vê tudo nas telas de gestão,
             decide sobre as recomendações e, se aceitar,
                 muda as atividades do time (escopo)
                             │
                             ▼
                  O próximo ciclo começa ...
```

Três detalhes importam:

- **Só existe um ciclo aberto por vez.** Enquanto o ciclo 4 está aberto, as
  pessoas só respondem o ciclo 4. Quando ele fecha, abre o 5.
- **O que não foi feito até o fechamento fica como não feito.** Se alguém não
  respondeu o check-in da semana, não dá para responder depois. Isso aparece
  no indicador de engajamento.
- **Os indicadores são uma fotografia.** No fechamento, o sistema calcula e
  **grava** os números de cada time. Depois disso eles nunca mais são
  recalculados. O que a gestora viu no ciclo 4 continua igual para sempre, e
  é isso que torna as telas confiáveis como evidência.

O acompanhamento tem **6 ciclos** no total. Quando o ciclo 6 fecha, nenhum
outro é aberto.

**Quem fecha o ciclo?** Na vida real, um **agendador automático** confere de
hora em hora se a data de fim do ciclo já passou e, se passou, fecha. Na
demonstração, a gestora também pode fechar na hora pelo botão "Fechar ciclo
aberto" do Painel. Os dois caminhos usam exatamente o mesmo processo.

---

## 4. Telas comuns a todos

### 4.1 Tela de login (`/login`)

É a porta de entrada, a mesma para os dois papéis. A pessoa digita o e-mail e
a senha e clica em entrar.

- **O que acontece depois do login:** o sistema olha o papel da pessoa e a
  leva para o lugar certo. A colaboradora vai para a tela **Início**, e a
  gestora vai para a **Visão geral dos times**.
- **Se a senha estiver errada:** a tela volta com uma mensagem de erro e
  ninguém entra.
- **No modo de demonstração:** a tela mostra uma caixa com os usuários de
  exemplo (Renata e Carla) e a senha padrão, `demo123`. Assim, quem assiste à
  apresentação consegue entrar sem precisar consultar nada.

📷 Print de referência: `docs/evidencias/02a-login.png`

### 4.2 A moldura de todas as telas

Depois do login, toda tela tem a mesma moldura:

- **Barra superior:** a marca "Prontidão IA · Seguradora Aurora" à esquerda.
  À direita, as iniciais e o nome de quem está logado, com o time (ou "Gestora
  de pessoas") e o botão **Sair**.
- **Faixa rosa "Ambiente de demonstração: dados simulados":** aparece em
  todas as páginas quando o sistema roda no modo de demonstração. Ela deixa
  claro, em qualquer print, que os dados não são reais.
- **Menu lateral:** muda conforme o papel. A colaboradora vê "Minha semana"
  (Início, Check-in, Meu histórico). A gestora vê "Gestão" (Visão geral). No
  modo de demonstração, os dois também veem "Demonstração → Painel".

No celular, o menu lateral some e as telas se reorganizam em uma coluna.

---

## 5. As telas da colaboradora

### 5.1 Início (`/inicio`)

É a tela que a Carla vê assim que entra. Pense nela como a lista de tarefas
da semana.

**O que aparece:**

- Um cumprimento ("Olá, Carla") e qual é o ciclo atual, com as datas
  ("Ciclo 4 de 6 · 21/09 a 27/09").
- Um botão **Meu histórico**, no canto.
- O cartão **Progresso do ciclo**, com uma barra e o texto "1 de 3
  pendências concluídas".
- O cartão **Pendências da semana**, com três linhas:
  - **Check-in semanal**, com o selo "Pendente" (âmbar) ou "Concluído" (verde)
    e o botão **Fazer check-in** enquanto estiver pendente.
  - **Atividade 4A · Formulação e verificação** e **Atividade 4B · Dados e
    aplicação no setor**. Cada uma tem um selo de situação ("Não iniciada",
    "Em andamento" ou "Concluída") e um botão que muda conforme a situação:
    **Começar**, **Continuar** ou **Ver resultado**. Depois de concluída, a
    linha também mostra quantos acertos a pessoa teve ("8 de 10 acertos").

**Como ela se conecta com o resto:** as três pendências são exatamente o que
o indicador de **engajamento** do time conta no fechamento. Se a Carla deixar
uma atividade sem fazer, isso pesa no engajamento do Atendimento.

**Quando não há ciclo aberto** (por exemplo, depois do ciclo 6), a tela avisa
que não há pendências no momento.

Depois de enviar o check-in, a pessoa volta para esta tela e vê, no canto
inferior, um aviso rápido: "Check-in enviado. Obrigado por responder!"

📷 `docs/evidencias/02b-inicio.png`

### 5.2 Check-in (`/checkin`)

É o questionário semanal de percepção. Foi pensado para levar menos de um
minuto.

**As três perguntas:**

1. **"Quão seguro(a) você se sente para usar o assistente de IA nas suas
   tarefas?"** Cinco opções clicáveis, de 1 a 5. A ponta do 1 diz "Nada
   seguro" e a ponta do 5 diz "Totalmente seguro".
2. **"Quanto você concorda com a frase: a IA pode substituir parte do meu
   trabalho?"** A mesma escala, de "Discordo totalmente" a "Concordo
   totalmente".
3. **"Quais dificuldades você teve com a IA nesta semana?"** Várias opções de
   marcar, e dá para marcar mais de uma:
   - Receio de substituição
   - Não sei quando posso confiar na resposta
   - Falta de tempo ou suporte para praticar
   - O assistente não se encaixa nas minhas tarefas
   - Meu time ou minha liderança não usa
   - **Nenhuma**

   "Nenhuma" é exclusiva: marcá-la desmarca as outras, e marcar outra
   desmarca "Nenhuma".

**O aviso de privacidade.** Logo acima do botão fica sempre o texto: *"Sua
gestora vê apenas resultados do time, nunca as suas respostas, e só quando o
time tem pelo menos 5 respostas."* Não é só um texto bonito: o sistema cumpre
isso de verdade (veja a seção 8).

**O botão "Enviar check-in".** Se faltar alguma resposta, a tela não é
enviada: a pergunta que falta fica com a borda vermelha e uma mensagem logo
abaixo ("Escolha uma opção de 1 a 5" ou "Marque as dificuldades que você teve
ou 'nenhuma'"). Quando tudo está respondido, o check-in é gravado e a pessoa
volta para o Início.

**Um check-in por semana.** Se a pessoa já respondeu e abre a tela de novo,
vê "Você já fez o check-in deste ciclo" e não consegue responder outra vez.
Mesmo que alguém tente enviar duas vezes ao mesmo tempo, o banco de dados
barra a segunda resposta.

**Para onde vão essas respostas:**

- A pergunta 1 vira o indicador de **confiança** do time.
- A pergunta 2 vira o indicador de **resistência** do time.
- A pergunta 3 alimenta as **recomendações**: as dificuldades mais marcadas
  definem quais intervenções o sistema sugere à gestora.

📷 `docs/evidencias/02c-checkin.png`

### 5.3 Atividade (`/atividades/{id}`)

É a prova curta da semana. Cada atividade tem **10 questões de múltipla
escolha**, sempre sobre situações de uma seguradora: resumir um relatório de
sinistros, conferir um prazo de reembolso, proteger dados de saúde de um
segurado, responder um cliente sobre franquia, e assim por diante.

**Como é a tela:**

- À esquerda, as 10 questões, uma embaixo da outra. Cada questão mostra o
  número ("Questão 3 de 10") e a área que ela avalia ("Verificação das
  respostas da IA"), seguidos das quatro alternativas.
- À direita, um cartão fixo com o **cronômetro**, o contador "7 de 10
  respondidas", uma barra de progresso e o botão **Finalizar atividade**.

**O que acontece quando a pessoa abre a atividade:** o sistema registra na
mesma hora que ela **começou** (isso é a "tentativa"). O cronômetro conta a
partir desse momento. Se a pessoa fechar a página e voltar depois, o
cronômetro continua de onde estava, sem zerar. Isso importa porque o tempo
gasto vira o indicador de **produtividade**.

**Enquanto a pessoa responde:** o sistema anota, sem mostrar, quantos
segundos ela levou em cada questão. Esse dado aparece depois no gabarito e
fica disponível para análises futuras.

**O botão "Finalizar atividade" exige todas as questões respondidas.** Se
faltar alguma, aparece uma caixa vermelha no topo dizendo quais faltam
("Faltam: 2, 9") e a tela rola até ela. Essa conferência é feita no navegador
e repetida no servidor, então não dá para burlar.

**Depois de finalizar:** o sistema corrige, grava cada resposta com
"acertou/errou" e registra o horário de término. Isso acontece **uma única
vez**: não dá para refazer a atividade. Se a pessoa tentar abri-la de novo,
vai direto para o resultado.

**E se alguém tentar abrir a atividade de outro time?** O sistema responde
"página não encontrada". Ele não diz "essa atividade é de outro time", porque
isso já revelaria alguma coisa.

As duas atividades da semana têm papéis diferentes:

- **Atividade A ("Formulação e verificação"):** 5 questões sobre como
  formular pedidos à IA e 5 sobre como conferir o que ela responde.
- **Atividade B ("Dados e aplicação no setor"):** 5 questões sobre uso seguro
  de dados e 5 sobre aplicar a IA nas tarefas do setor.

📷 `docs/evidencias/02d-atividade-4A.png` e `02d-atividade-4B.png`

### 5.4 Resultado da atividade (`/atividades/{id}/resultado`)

É a tela que aparece logo depois de finalizar. Tem duas partes.

**Os números:** dois cartões grandes, **Acertos** ("8 de 10") e **Tempo**
("11 min 5 s").

**O gabarito comentado:** as 10 questões de novo, agora corrigidas. Em cada
uma:

- um selo **Acertou** (verde) ou **Errou** (vermelho), sempre com o texto
  escrito, e não só a cor;
- a alternativa certa pintada de verde, com a etiqueta "Resposta certa";
- se a pessoa errou, a alternativa que ela marcou pintada de vermelho, com a
  etiqueta "Sua resposta";
- um parágrafo **"Por quê:"** explicando a resposta.

A ideia é que a atividade também ensine. É justamente isso que a "Trilha de
verificação" faz quando a gestora a libera para um time com dificuldade (seção
6.4): situações de baixo risco com retorno imediato.

📷 `docs/evidencias/02e-resultado-4A.png` e `02e-resultado-4B.png`

### 5.5 Meu histórico (`/meu-historico`)

É o espelho pessoal da colaboradora ao longo das semanas.

**O que aparece:**

- Uma caixa no topo: **"Só você vê estes dados. A gestão vê apenas resultados
  do time."**
- Um **gráfico de linhas** com duas séries por ciclo: o **acerto nas
  atividades** (em bordô) e a **confiança** (em verde), as duas na escala de
  0 a 100. É a versão pessoal da pergunta central do sistema: "estou
  acertando mais, e me sinto mais segura?"
- Uma **tabela dos ciclos**, com o período, o acerto ("65% (13 de 20)"), a
  confiança marcada (1 a 5), o receio de substituição (1 a 5) e a situação do
  ciclo ("Em andamento" ou "Fechado").

**Por que existe:** a regra de privacidade limita o que a **gestora** vê, não
o que a própria pessoa vê. Esta é a **única** tela do sistema com a percepção
de uma pessoa individualmente, e ela só mostra os dados de quem está logado.
Mesmo que alguém coloque o identificador de outra pessoa no endereço da
página, o sistema ignora e mostra os dados de quem está logado.

📷 `docs/evidencias/02f-meu-historico.png`

---

## 6. As telas da gestora

### 6.1 Visão geral dos times (`/gestao`), o "Quadro 1"

É a primeira tela da Renata e responde à pergunta "como estão meus times
nesta semana?".

**No topo:** o título, o ciclo mostrado com as datas ("Ciclo 4 · 21/09 a
27/09 · fechado") e um **seletor de ciclo**. A tela mostra por padrão o
**último ciclo fechado**, porque um ciclo ainda aberto está incompleto e seus
números enganariam. Pelo seletor dá para voltar a qualquer ciclo anterior.

**Faixa amarela "Maior erro da empresa no ciclo 4":** mostra a área em que a
empresa inteira mais errou nas atividades ("Verificação das respostas da IA ·
42% de erro"). O cálculo junta as respostas de todos os times.

**Um cartão por time**, cada um com:

- **O nome do time e os selos de alerta.** Um alerta de prioridade alta
  aparece em **vermelho**, um de prioridade normal em **âmbar**, sempre com o
  nome escrito ("Sabe usar, mas não se sente pronto · Alta"). Se o alerta já
  estiver em acompanhamento ou resolvido, isso também aparece no selo. Um
  time sem alertas mostra "Sem alertas".
- **Os quatro indicadores**, em números grandes e inteiros:
  - **Técnico**: o percentual de acerto nas atividades;
  - **Confiança**: a média da pergunta 1 do check-in, convertida para 0 a
    100;
  - **Resistência**: a média da pergunta 2, também de 0 a 100;
  - **Engajamento**: o percentual das pendências da semana que foram
    cumpridas.
- **A variação** de cada indicador em relação ao ciclo anterior, embaixo do
  número: "↑ +5", "↓ −7" ou "= 0". A cor diz se a mudança é boa (verde) ou
  ruim (vermelho). Na **resistência** a lógica é invertida, porque ali subir
  é ruim: mais gente com medo de ser substituída.
- **"amostra insuficiente"** no lugar do número, quando o time teve menos de
  5 check-ins no ciclo. Isso vale para confiança e resistência (seção 8).
- **O rodapé**, com o tempo médio por atividade e quantos check-ins o time
  respondeu ("Tempo médio 10,9 min · 7 de 8 check-ins"), e o botão **Ver
  detalhe do time →**.

**Faixa verde no final, "Maior ganho de confiança desde o ciclo 1":** destaca o
time que mais evoluiu ("Financeiro: +19 pontos (34 → 53)"). É uma forma de
mostrar também o que está dando certo, e não só os problemas.

📷 `docs/evidencias/04a-visao-geral.png`

### 6.2 Detalhe do time (`/gestao/times/{id}`), o "Quadro 2"

É a tela de investigação: "o que está acontecendo com o Atendimento?".

**No topo:** o link "← Visão geral", o nome do time, quantos membros ele tem,
o ciclo em foco e o seletor de **intervalo** ("ciclos 1 a 4"). O intervalo
controla até que ciclo o gráfico mostra dados e qual ciclo os blocos de baixo
detalham.

**A caixa do alerta** (uma para cada alerta que valia naquele ciclo):

- a borda esquerda colorida pela prioridade, o selo "Prioridade Alta", o nome
  do alerta e a situação ("Aberto", "Em acompanhamento" ou "Resolvido");
- "Gerado no fechamento do ciclo 4" e, se já foi resolvido, "resolvido no
  fechamento do ciclo 6";
- **a regra explicada com os números que a dispararam**: *"Técnico subiu de
  61 para 67 e 72; confiança caiu de 59 para 53 e 46."* Essa frase foi gravada
  no momento do fechamento e nunca muda depois;
- quando a prioridade é alta, o motivo: *"Engajamento caiu 13 pontos →
  prioridade alta."*;
- o botão **Ver recomendação →**.

**O gráfico "Indicadores por ciclo":** quatro linhas, uma por indicador,
sempre com as mesmas cores em todo o sistema (técnico em bordô, confiança em
verde, resistência em vermelho, engajamento em âmbar), com legenda.
- **Tooltip:** ao passar o mouse, aparecem os valores daquele ciclo.
- **Marcador de intervenção:** quando a gestora já aplicou uma intervenção,
  uma **linha vertical tracejada** com a palavra "intervenção" marca o ciclo em
  que ela começou a valer. É o que permite enxergar o "antes e depois" no
  próprio gráfico.
- **Eixo:** mostra sempre os 6 ciclos, mesmo os que ainda não aconteceram,
  para o marcador ficar sempre no mesmo lugar.

**Erro por área:** quatro barras (formulação, verificação, dados, aplicação),
cada uma com o percentual de erro do time naquela área no ciclo em foco. É
onde se descobre em que ponto o time trava.

**Dificuldades relatadas:** uma barra para cada dificuldade do check-in, com
a contagem e o percentual ("5 (71% de 7 respostas)"). Esse bloco **só
aparece quando o time teve pelo menos 5 check-ins**. Com menos, ele é
substituído por uma caixa amarela explicando que a amostra é insuficiente e
que as dificuldades não são mostradas para proteger quem respondeu.

**Produtividade:** o tempo médio por atividade no ciclo 1 e no ciclo em foco,
com a diferença ("Ciclo 4: 10,9 min (−3,1)"). Embaixo, um lembrete: *"Ler
junto com o técnico (72): mais rápido só é ganho se o acerto não cair."* Ficar
mais rápido errando mais não é melhora.

**Amostra:** quantos check-ins o time respondeu no ciclo e qual é o mínimo
("7 de 8 check-ins respondidos (mínimo de 5)").

**Comparar antes e depois:** este bloco só aparece depois que uma intervenção
foi aplicada. É uma tabela com o ciclo **anterior** à intervenção ao lado dos
ciclos seguintes. Por exemplo, "C4 (antes)", "C5" e "C6", com técnico,
confiança, resistência e engajamento em cada coluna. É a prova, em números, de
como o time evoluiu depois da decisão da gestora.

📷 `docs/evidencias/04b-detalhe-atendimento.png`,
`06c-detalhe-em-acompanhamento.png` e `08-atendimento-ciclos-1-a-6.png`

### 6.3 Recomendação (`/gestao/alertas/{id}/recomendacao`), o "Quadro 3"

É a tela onde o sistema diz "eu sugiro isto" e a gestora responde.

**O título** resume tudo: *"Sabe usar, mas não se sente pronto – Atendimento –
ciclo 4"*, com os selos de prioridade e de situação.

**Um cartão por recomendação.** No caso do Atendimento, são dois:

1. **"Comunicação da liderança, em reunião de time, sobre a IA como apoio ao
   trabalho, e não como substituição"**, com o selo **Ação humana**, a
   dificuldade ligada ("Receio de substituição · 5 de 7 respostas"), o
   executor ("Liderança") e a situação ("Sugerida").
2. **"Trilha 'Verificação de respostas': atividades práticas de baixo risco,
   com situações do próprio setor e feedback comentado"**, com o selo
   **Plataforma**, a dificuldade ligada ("Não sei quando posso confiar na
   resposta · 3 de 7 respostas") e o executor ("Plataforma").

A diferença entre os dois selos é importante:
- uma **ação humana** é algo que pessoas precisam fazer (uma reunião, uma
  oficina). O sistema só registra a decisão e a data;
- uma intervenção de **plataforma** o próprio sistema executa, trocando as
  atividades que o time recebe.

**O expansor "Por que esta recomendação?"** abre uma explicação:
- que percentual do time marcou aquela dificuldade;
- que ela está entre as duas mais marcadas;
- qual regra disparou o alerta ("Regra A: o técnico subiu em dois ciclos
  seguidos e a confiança caiu pelo menos 5 pontos em cada um deles");
- os valores que a dispararam.

A gestora não precisa confiar no sistema às cegas: consegue ver o raciocínio.

**Os três botões de decisão:**

- **Aceitar e ajustar escopo:** marca as recomendações como aceitas e leva
  para a tela de escopo.
- **Ajustar recomendação:** abre uma caixa de texto em cada item, com o texto
  atual, para a gestora reescrever. Por exemplo, trocar a comunicação
  genérica por "Reunião da gerente com o time na segunda-feira". O item
  editado fica "Ajustado", os não editados ficam "Aceitos", e a gestora segue
  para o escopo. Esse é o "fluxo alternativo" da especificação.
- **Adiar:** marca as recomendações como adiadas e volta ao detalhe do time.
  O alerta continua aberto, e a gestora pode decidir depois.

Cada decisão guarda **quem** decidiu e **quando**. Depois de decidido, os
botões somem e a tela mostra "A decisão sobre este alerta já foi tomada", com
um atalho para o escopo.

**No rodapé, sempre:** *"As recomendações apoiam a decisão da gestão e não a
substituem."* É coerente com o próprio tema do artefato, que é justamente não
confiar cegamente numa resposta automática.

📷 `docs/evidencias/05-recomendacao.png`

### 6.4 Escopo de atividades (`/gestao/times/{id}/escopo`)

"Escopo" é o conjunto de atividades que um time recebe no ciclo. Normalmente
são a "A" e a "B" da semana (5A e 5B, por exemplo). Esta tela é onde a
decisão da gestora vira uma mudança concreta.

**Dois cartões lado a lado:**

- **Escopo atual:** as atividades que o time tem agora no ciclo aberto (5A e
  5B).
- **Escopo proposto:** como o escopo fica se a recomendação de plataforma for
  aplicada. A atividade trocada aparece destacada: a 5A dá lugar à **"Trilha
  verificação – situações do Atendimento"**, uma atividade com o mesmo nível e
  a mesma divisão de áreas da atividade A, mas com as questões de verificação
  montadas em situações reais de atendimento ao cliente de seguros.

**A caixa de marcar "Trocar 5A → Trilha…"** já vem **marcada**, porque é a
troca sugerida. A gestora pode desmarcar se preferir acompanhar o time só com
as ações humanas.

**Uma frase explica quando a troca vale.** Há duas situações possíveis:

- se **ninguém do time começou** as atividades do ciclo aberto, a troca vale
  **já neste ciclo** ("a troca vale já no ciclo aberto");
- se **alguém já começou** uma atividade, trocar agora seria injusto e
  confuso, então a troca fica guardada e vale **a partir do próximo ciclo**
  ("a troca vale a partir do ciclo 6"). O sistema aplica automaticamente
  quando o próximo ciclo abrir.

**O nível da atividade nunca muda na troca.** Se a trilha fosse mais fácil, o
técnico do time subiria por causa da prova, e não por aprendizado, e a
comparação entre os ciclos perderia o sentido.

**O botão "Salvar escopo"** confirma a decisão:
- as atividades do time são trocadas, agora ou no próximo ciclo;
- o sistema registra a partir de qual ciclo a intervenção valeu, e esse
  ciclo vira o marcador tracejado do gráfico;
- **o alerta muda para "Em acompanhamento"**;
- a gestora volta ao detalhe do time com a mensagem **"Escopo salvo"**.

**Intervenções humanas.** Embaixo, cada ação humana aceita (a comunicação da
liderança) aparece com um campo de data e o botão **Marcar como agendada**. A
gestora escolhe a data prevista, e o item ganha o selo "Agendada para
02/10/2026". O sistema **registra** a intervenção, mas não a executa: a
reunião acontece fora dele. A data pode ser marcada antes ou depois de salvar
o escopo, uma coisa não depende da outra.

📷 `docs/evidencias/06a-escopo.png` e `06b-escopo-salvo-e-agendado.png`

---

## 7. O que acontece "por trás" de cada fechamento

Quando o ciclo fecha, o sistema faz as contas descritas a seguir. Todos os
limites (5 pontos, 75, 50%, 10 pontos, 2 ciclos, 5 check-ins) ficam num único
arquivo de configuração, o `application.yml`, e podem ser discutidos como
parâmetros de projeto.

### 7.1 Os indicadores de cada time

| Indicador | Como é calculado |
|---|---|
| **Técnico** | percentual de respostas corretas nas atividades finalizadas no ciclo |
| **Confiança** | média da pergunta 1, convertida de 1–5 para 0–100 (1 vira 0, 3 vira 50, 5 vira 100) |
| **Resistência** | média da pergunta 2, com a mesma conversão |
| **Engajamento** | pendências cumpridas ÷ pendências esperadas × 100; cada pessoa tem 1 check-in + 1 por atividade liberada |
| **Produtividade** | tempo médio entre abrir e finalizar uma atividade, em minutos |
| **Erro por área** | 100 − acerto em cada área; é calculado na hora de mostrar, e não gravado |

Os valores são guardados com uma casa decimal (por exemplo, 46,4). As telas
mostram números inteiros, arredondando a metade para cima (46,5 vira 47).

Um exemplo de engajamento: o Atendimento tem 8 pessoas e 2 atividades, então
espera 8 × (1 + 2) = 24 pendências. No ciclo 4, foram 7 check-ins e 12
atividades, 19 de 24, ou 79%.

### 7.2 Regra A: "Sabe usar, mas não se sente pronto"

O alerta aparece quando, **ao mesmo tempo**:

- o técnico **subiu duas vezes seguidas** (ciclo 2 → 3 → 4);
- a confiança **caiu pelo menos 5 pontos em cada uma** dessas duas passagens.

No cenário, o Atendimento tem técnico 61 → 67 → 72 e confiança 59 → 53 → 46.
A regra dispara no ciclo 4. No ciclo 3 ela **não** dispara, porque do ciclo 1
para o 2 a confiança caiu só 3 pontos (62,5 para 59,4). Exigir duas quedas
seguidas evita alarme falso por uma semana ruim isolada.

### 7.3 Regra B: "Confiança alta sem senso crítico"

O alerta aparece quando, em **dois ciclos seguidos**, o time tem:

- confiança de **pelo menos 75**;
- acerto **abaixo de 50%** nas questões de verificação das respostas da IA.

É o problema contrário ao da regra A: gente confiante demais, que aceita o que
a IA diz sem conferir. No cenário, o Marketing tem confiança acima de 78 e
verificação por volta de 40% desde o ciclo 1, e a regra dispara a partir do
ciclo 2.

### 7.4 Prioridade

O alerta **nasce** com prioridade **alta** se o engajamento do time caiu pelo
menos 10 pontos do ciclo anterior para o atual. Caso contrário, nasce com
prioridade normal. A prioridade não muda depois. No cenário, o Atendimento cai
de 92 para 79 (13 pontos) e o alerta nasce alto.

### 7.5 Sem alertas repetidos

Se a regra vale de novo num ciclo seguinte e o time já tem um alerta daquele
tipo ainda não resolvido, o sistema **não cria outro**: só atualiza os valores
do alerta existente. Por isso o Marketing tem **um único** alerta B do ciclo 2
até o fim, e não um por ciclo.

### 7.6 Como os alertas se encerram

- **Alerta A:** depois que a gestora salvou o escopo, o alerta fica "Em
  acompanhamento". Ele passa a **Resolvido** quando a confiança do time
  **sobe em dois ciclos seguidos** a partir do ciclo em que a intervenção
  começou a valer. No cenário, a intervenção vale no ciclo 5, e a confiança vai
  de 46 (C4) para 50 (C5) e 56 (C6). Depois do ciclo 5 houve só uma subida, e
  o alerta continua em acompanhamento. No ciclo 6 veio a segunda, e o alerta é
  resolvido.
- **Alerta B:** é resolvido quando a condição (confiança alta com
  verificação baixa) **deixa de valer por dois ciclos seguidos**.
- **Um ciclo sem amostra mínima não conta** para encerrar nada. Um alerta não
  pode sumir só porque o time parou de responder.

### 7.7 Como as recomendações são escolhidas

- **Alerta A:** o sistema conta quantas vezes cada dificuldade foi marcada no
  check-in do time naquele ciclo. Pega as **duas mais marcadas** e sugere, para
  cada uma, a intervenção que o catálogo liga a ela. Em caso de empate, vale a
  ordem da lista. Dificuldades que ninguém marcou não viram recomendação.

  | Dificuldade | Intervenção sugerida | Quem executa |
  |---|---|---|
  | Receio de substituição | Comunicação da liderança sobre a IA como apoio | Liderança (humana) |
  | Não sei quando posso confiar na resposta | Trilha "Verificação de respostas" | Plataforma |
  | Falta de tempo ou suporte para praticar | Horário reservado e ponto focal de suporte | Gestor (humana) |
  | O assistente não se encaixa nas minhas tarefas | Oficina de casos de uso do setor | Liderança e TI (humana) |
  | Meu time ou minha liderança não usa | Liderança passa a usar e compartilhar exemplos | Liderança (humana) |

- **Alerta B:** uma única recomendação, a trilha de verificação **com
  checklist de revisão** das respostas da IA.

---

## 8. A regra de privacidade, explicada

Esta é uma regra obrigatória da especificação e aparece em várias telas.

- **A gestora nunca vê a resposta de uma pessoa.** Não existe nenhuma tela ou
  endereço da gestão que mostre "a Carla respondeu 2". A percepção só chega à
  gestão como média ou contagem do time inteiro.
- **E só quando o time tem pelo menos 5 check-ins no ciclo.** Com poucas
  respostas, a média deixaria de proteger as pessoas. Se só 2 pessoas
  responderam, a gestora poderia deduzir quem disse o quê. Com menos de 5:
  - confiança e resistência ficam **em branco** ("amostra insuficiente");
  - as dificuldades **não aparecem**;
  - as regras A e B **nem são avaliadas** para aquele time naquele ciclo.
- **A colaboradora vê o próprio histórico completo**, e só o dela.

No código, existe uma peça chamada `PrivacidadeGuard`: é a única porta por onde
as telas da gestão chegam a dados de check-in, e ela aplica o limite de 5
respostas. Um teste automático percorre **todas** as telas da gestão e
confere que nenhuma mostra o nome de um colaborador.

Por que isso importa para o sistema funcionar: se as pessoas achassem que a
chefe vê suas respostas, responderiam o que acham que a chefe quer ouvir, e os
dados de confiança e receio perderiam o valor.

---

## 9. O Painel de demonstração (`/demo`)

Esta tela não faz parte do produto. Ela existe para a apresentação da DSR,
porque ninguém vai esperar seis semanas reais durante uma defesa oral. Ela só
existe no modo de demonstração; fora dele, o endereço dá "página não
encontrada".

**À esquerda, "Ciclo aberto":** qual é o ciclo aberto, com as datas, e uma
tabela por time com quantos check-ins e atividades já foram feitos ("6 de 8",
"10 de 16").

**À direita, os cinco botões:**

| Botão | O que faz |
|---|---|
| **Restaurar cenário inicial** | Apaga todo o movimento (respostas, ciclos, alertas) e remonta o cenário do zero: ciclos 1, 2 e 3 já fechados, ciclo 4 aberto com as respostas de 23 colaboradores. Faltam só as da Carla, e um colega do Atendimento pulou o check-in de propósito. As pessoas e os catálogos continuam. |
| **Preencher pendências da Carla conforme o roteiro** | Faz pela Carla o check-in (confiança 2, receio 4, as duas dificuldades) e as duas atividades (8 de 10 e 7 de 10, com cerca de 11 e 10 minutos). Só preenche o que ainda estiver pendente. |
| **Gerar respostas simuladas do ciclo aberto** | Gera as respostas de quem ainda não respondeu no ciclo aberto. |
| **Fechar ciclo aberto** | Faz o fechamento completo da seção 7, exatamente como o agendador faria quando a semana acaba. |
| **Avançar um ciclo** | Os dois anteriores juntos: gera as respostas que faltam e fecha o ciclo. |

**Embaixo, o "Registro de ações":** uma lista do que foi feito, com horário,
incluindo o que cada fechamento produziu ("Alerta novo: Sabe usar, mas não se
sente pronto – Atendimento (ALTA)", "Alerta mantido: …", "Alerta resolvido:
…"). Ele fica só na memória e some quando o sistema é reiniciado.

**Um ponto importante:** nenhum botão do painel grava indicador ou alerta
diretamente. Os indicadores e alertas sempre saem do mesmo processo de
fechamento que funcionaria na vida real. A simulação só produz as
**respostas** das pessoas.

📷 `docs/evidencias/01-painel-cenario-restaurado.png`,
`03-painel-ciclo-4-fechado.png` e `07-painel-ciclos-5-e-6.png`

---

## 10. A história completa do cenário, ciclo a ciclo

Juntando tudo, esta é a história que a demonstração conta.

**Ciclos 1 a 3 (já prontos ao restaurar).**
- **Atendimento:** acerta cada vez mais (55 → 61 → 67), mas a confiança
  começa a escorregar (63 → 59 → 53) e o receio de ser substituído cresce.
- **Financeiro:** começa inseguro (confiança 34) e melhora de forma estável.
- **Marketing:** está muito confiante desde o início (78 → 84) e responde
  rápido, mas erra muito as questões de verificação (em torno de 40%). No
  fechamento do ciclo 2, nasce o alerta B do Marketing, "Confiança alta sem
  senso crítico", com prioridade normal.

**Ciclo 4 (aberto, ao vivo).**
- A Carla entra, faz o check-in dizendo que se sente pouco segura (2) e que
  teme ser substituída (4), marca as duas dificuldades e faz as atividades.
- Ao fechar o ciclo, o técnico do Atendimento chegou a 72 e a confiança caiu
  para 46. É a segunda queda seguida de mais de 5 pontos, e **a regra A
  dispara**. O engajamento caiu 13 pontos, porque um colega pulou o check-in e
  várias atividades ficaram sem fazer, então o alerta nasce com **prioridade
  alta**.
- O sistema olha as dificuldades do Atendimento: receio de substituição (5 de
  7) e "não sei quando confiar" (3 de 7) foram as mais marcadas. Sugere a
  **comunicação da liderança** e a **trilha de verificação**.
- O alerta B do Marketing continua valendo e é atualizado, sem duplicar.

**A decisão da Renata.**
- Ela vê o alerta na visão geral, investiga o detalhe do Atendimento, abre a
  recomendação, entende o "por quê" e **aceita**.
- Na tela de escopo, salva a troca: no ciclo 5, o Atendimento recebe a trilha
  de verificação no lugar da atividade 5A. Ela também agenda a reunião da
  liderança.
- O alerta passa a **em acompanhamento**, e o gráfico ganha o marcador de
  intervenção no ciclo 5.

**Ciclos 5 e 6.**
- A confiança do Atendimento volta a subir (46 → 50 → 56), e o receio diminui.
- No fechamento do ciclo 5 houve só uma subida, e o alerta continua em
  acompanhamento. No fechamento do ciclo 6 veio a segunda subida seguida, e o
  alerta é **resolvido**.
- A tabela "Comparar antes e depois" mostra lado a lado o ciclo 4 (antes) e os
  ciclos 5 e 6.
- O Financeiro termina sem nenhum alerta. O Marketing termina com o alerta B
  ainda aberto, porque ninguém tomou decisão sobre ele no roteiro e a
  verificação continua baixa.

**Um aviso honesto para o relatório:** no cenário, a melhora do Atendimento
nos ciclos 5 e 6 está **programada** nos dados simulados e aconteceria mesmo
se a Renata tivesse adiado a recomendação. A demonstração prova que o sistema
**detecta, recomenda, registra e acompanha**. Ela não prova que a intervenção
**causou** a melhora; isso exigiria uma avaliação com pessoas reais.

---

## 11. Perguntas que costumam aparecer

**"E se a Carla não fizer o check-in?"**
Se o ciclo fechar sem o check-in dela, isso conta como pendência não cumprida:
o engajamento do Atendimento cai. Os indicadores do time continuam sendo
calculados com as respostas que existem, desde que haja pelo menos 5.

**"E se um time tiver só 4 respostas na semana?"**
Confiança e resistência ficam em branco, as dificuldades não aparecem para a
gestora e as regras A e B não são avaliadas para aquele time naquela semana.
Técnico, engajamento e produtividade continuam aparecendo, porque não são
percepção de ninguém.

**"A gestora é obrigada a aceitar a recomendação?"**
Não. Ela pode aceitar, ajustar o texto ou adiar. O sistema apoia a decisão,
não decide por ela.

**"O que acontece se a gestora salvar o escopo no meio da semana, com gente já
fazendo as atividades?"**
A troca não é aplicada no meio da semana. Fica guardada e é aplicada
automaticamente quando o próximo ciclo abrir. A tela avisa isso antes de
salvar.

**"A confiança do exemplo da especificação era 47, e a tela mostra 46.
Por quê?"**
Com 7 check-ins de respostas inteiras de 1 a 5, os valores possíveis perto de
47 são 46,4 e 50,0. 47 não existe aritmeticamente. O sistema usa o valor mais
próximo, 46,4, que está dentro da tolerância de 2 pontos da especificação.

**"Por que a produtividade da Carla sai diferente quando ela responde ao
vivo?"**
Porque o sistema mede o tempo real entre abrir e finalizar a atividade. Numa
apresentação, a Carla responde em um ou dois minutos, e não nos 10 a 11
minutos do roteiro. Para reproduzir o número exato, use o botão "Preencher
pendências da Carla" no Painel.

**"Posso repetir a demonstração?"**
Sim, quantas vezes quiser: Painel → **Restaurar cenário inicial**. O cenário é
determinístico, isto é, restaurar duas vezes dá exatamente os mesmos números.

**"O sistema executa a reunião da liderança?"**
Não. Ele registra a decisão e a data prevista. A reunião acontece fora do
sistema, com as pessoas.

---

## 12. Mapa rápido das telas

| Tela | Endereço | Quem vê | Para quê |
|---|---|---|---|
| Login | `/login` | todos | entrar no sistema |
| Início | `/inicio` | colaboradora | ver as pendências da semana |
| Check-in | `/checkin` | colaboradora | responder as 3 perguntas de percepção |
| Atividade | `/atividades/{id}` | colaboradora | fazer as 10 questões com cronômetro |
| Resultado | `/atividades/{id}/resultado` | colaboradora | ver os acertos, o tempo e o gabarito comentado |
| Meu histórico | `/meu-historico` | colaboradora | acompanhar o próprio acerto e a própria confiança |
| Visão geral (Quadro 1) | `/gestao` | gestora | comparar os times num ciclo |
| Detalhe do time (Quadro 2) | `/gestao/times/{id}` | gestora | investigar um time e ver o alerta |
| Recomendação (Quadro 3) | `/gestao/alertas/{id}/recomendacao` | gestora | aceitar, ajustar ou adiar |
| Escopo | `/gestao/times/{id}/escopo` | gestora | trocar atividades e agendar intervenções |
| Painel de demonstração | `/demo` | todos (só no modo demo) | simular o passar das semanas |
