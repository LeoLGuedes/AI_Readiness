# Decisões de projeto

Registro exigido pela seção 0.3 da especificação: sempre que faltou informação,
escolhemos a opção mais simples e anotamos aqui **a decisão, a alternativa
descartada e o motivo**.

Cada entrada tem a fatia em que foi tomada.

---

## Dados do cenário simulado

### D01 — Distribuições de check-in são tabelas fixas, não sorteio (fatia 1)

**Decisão:** as respostas individuais de check-in por time e ciclo vêm de
tabelas fixas no pacote `simulacao`. A semente aleatória governa apenas o que
não afeta limiar: quais questões cada pessoa erra e os segundos de cada
tentativa.

**Alternativa descartada:** sortear os valores dentro da tolerância de ±2
pontos da seção 8.4.

**Motivo:** a regra A só **não** dispara no ciclo 3 porque
confiança(C1) − confiança(C2) = 63 − 59 = 4, abaixo do limiar de 5. Sorteando
dentro de ±2, a simulação poderia produzir 64 e 58 (diferença 6) e disparar o
alerta no ciclo errado, quebrando a invariante 1 da seção 8.5. A tolerância
passa a ser margem de conferência, não margem de geração.

### D02 — No ciclo 4 vale a tabela de erro por área (fatia 1)

**Decisão:** para o ciclo 4, o acerto em VERIFICAÇÃO do Atendimento é 67%
(erro 33%, da tabela "Erro por área no ciclo 4"). Para os demais ciclos vale a
linha "Acerto em verificação (%)" da tabela de metas.

**Alternativa descartada:** usar 68% no ciclo 4, como diz a linha de acerto em
verificação.

**Motivo:** as duas tabelas da seção 8.4 divergem em 1 ponto no ciclo 4, dentro
da tolerância declarada. A tabela de erro por área é a que alimenta a
invariante 6 e o gráfico do Quadro 2, então é ela que manda.

### D03 — "Erro médio da empresa" é agrupado, não média de médias (fatia 1)

**Decisão:** a faixa de maior erro da empresa e a invariante 6 usam o erro
calculado sobre **todas as respostas de todos os times** no ciclo.

**Alternativa descartada:** média aritmética das três porcentagens de time.

**Motivo:** com times do mesmo tamanho os dois resultados coincidem, e o
agrupado continua correto se um time tiver menos respostas.

### D04 — Nome da empresa fictícia (fatia 1)

**Decisão:** "Seguradora Aurora"; e-mails no formato
`primeiro.ultimo@aurora.com.br`, sem acentos.

**Alternativa descartada:** deixar a empresa sem nome.

**Motivo:** a especificação descreve "uma seguradora fictícia de médio porte"
sem nomeá-la, e as telas precisam de um nome para a barra superior e para os
enunciados das questões.

### D05 — Senha única de demonstração (fatia 1)

**Decisão:** todos os usuários simulados usam `demo123`, exibida em `/login` e
em `/demo` apenas com o perfil `demo` ativo.

**Alternativa descartada:** senhas distintas por pessoa.

**Motivo:** quem assiste à defesa precisa entrar como Carla ou Renata sem
consultar o código.

### D06 — Quem falta no ciclo 4 (fatia 1)

**Decisão:** Marcos Vinícius Lima é o membro do Atendimento que pula o check-in
do ciclo 4, fazendo o time fechar com 7 de 8.

**Alternativa descartada:** sortear quem falta.

**Motivo:** a seção 8.2 exige que não seja a Carla e que o número seja exato;
fixar a pessoa mantém o cenário reproduzível entre restaurações.

---

## Modelo de dados

### D07 — Enums do domínio viram `varchar` + `CHECK` (fatia 1)

**Decisão:** `papel`, `status`, `tipo` e `prioridade` são `varchar` com
restrição `CHECK`.

**Alternativa descartada:** tipos `ENUM` do PostgreSQL.

**Motivo:** acrescentar um valor novo passa a ser uma migration de texto, não
uma alteração de tipo; e o mapeamento com `@Enumerated(STRING)` fica direto.

### D08 — Indicadores em `numeric(4,1)` (fatia 1)

**Decisão:** `tecnico`, `confianca`, `resistencia` e `engajamento` são
`numeric(4,1)`; `tempo_medio_min` é `numeric(5,1)`.

**Alternativa descartada:** `double precision`.

**Motivo:** a seção 5.1 manda guardar os valores com uma casa decimal, e as
regras comparam esses valores armazenados. Ponto flutuante binário tornaria a
comparação `≥ 5` dependente de arredondamento invisível.

### D09 — A tabela `time` é citada entre aspas (fatia 1)

**Decisão:** a tabela se chama `time`, como manda a seção 4, mas é escrita
`"time"` no SQL e `` `time` `` no mapeamento JPA. A classe se chama
`TimeEntity`.

**Alternativa descartada:** renomear a tabela para `equipe`.

**Motivo:** `TIME` é palavra-chave do SQL e `Time` colidiria com
`java.sql.Time`; a especificação fixa o nome da tabela, então a adaptação fica
só na citação e no nome da classe Java.

### D10 — A "ordem do catálogo" é a ordem de id (fatia 1)

**Decisão:** o desempate da seção 5.3 ("empate resolvido pela ordem do
catálogo") usa a ordem crescente de `dificuldade.id`, que é a ordem de inserção
do seed.

**Alternativa descartada:** acrescentar uma coluna `ordem` em `dificuldade`.

**Motivo:** a seção 4 fixa as colunas da tabela; uma coluna a mais iria além do
que "tipos e índices adicionais ficam a seu critério" permite.

### D11 — O "sem duplicatas" da regra de alerta é garantido pelo banco (fatia 1)

**Decisão:** índice único parcial `uq_alerta_nao_resolvido (time_id, tipo)
where status <> 'RESOLVIDO'`.

**Alternativa descartada:** garantir apenas no serviço.

**Motivo:** a seção 5.2 trata isso como invariante, não como conveniência; o
índice impede que uma corrida entre o agendador e o painel de demonstração crie
dois alertas do mesmo tipo.

### D12 — Duas intervenções de trilha no catálogo (fatia 1)

**Decisão:** o catálogo tem `TRILHA_VERIFICACAO` (ligada à dificuldade "não sei
quando posso confiar na resposta") e `TRILHA_VERIFICACAO_CHECKLIST` (usada pelo
alerta B). Ambas são do tipo `PLATAFORMA` e apontam para a mesma atividade de
trilha na troca de escopo.

**Alternativa descartada:** uma única intervenção servindo aos dois casos.

**Motivo:** a seção 5.3 descreve as duas com textos diferentes — a segunda
acrescenta o checklist de revisão — e a seção 5.3 nomeia explicitamente
`TRILHA_VERIFICACAO_CHECKLIST` como a intervenção do alerta B.

### D13 — Formato do `alerta.detalhe` (fatia 1)

**Decisão:** `{regra, ciclo, valores:{...}, frase}`, onde `frase` é exatamente
o texto exibido no Quadro 2, gerado no fechamento.

**Alternativa descartada:** montar a frase na tela, a partir dos indicadores.

**Motivo:** a seção 4 diz que o indicador é "uma fotografia gravada no
fechamento e nunca recalculada depois". A explicação do alerta precisa da mesma
garantia: o que a gestora leu na defesa é o que ficou gravado.

---

## Produto e telas

### D14 — Intervenção humana não bloqueia o salvamento do escopo (fatia 1)

**Decisão:** "Salvar escopo" funciona mesmo sem a data prevista da intervenção
humana preenchida; a data pode ser marcada depois.

**Alternativa descartada:** exigir a data antes de salvar.

**Motivo:** a seção 5.4 separa as duas coisas — o escopo troca a atividade de
plataforma, e a intervenção humana apenas "é marcada como agendada".

### D15 — O alerta B não troca escopo automaticamente (fatia 1)

**Decisão:** a recomendação do alerta B é criada com status `SUGERIDA` e só
muda por decisão da gestora.

**Alternativa descartada:** aplicar a trilha ao Marketing no fechamento.

**Motivo:** a seção 5.4 é clara em que a troca de escopo decorre da decisão do
gestor, e o roteiro da seção 9 não prevê decisão sobre o Marketing.

### D16 — Tentativa não finalizada não conta em nada, exceto no engajamento (fatia 1)

**Decisão:** o técnico e a produtividade usam só tentativas finalizadas; uma
tentativa iniciada e não finalizada conta como pendência não cumprida.

**Alternativa descartada:** contar as respostas já dadas numa tentativa aberta.

**Motivo:** a seção 5.1 diz "tentativas finalizadas" nas duas linhas, e a seção
5.5 diz que "pendências não concluídas ficam como não realizadas".

### D17 — `tempo_segundos` é por questão; a produtividade é por tentativa (fatia 1)

**Decisão:** `resposta.tempo_segundos` guarda o intervalo entre marcar uma
alternativa e a anterior, enviado por campo oculto. A produtividade da seção
5.1 usa `finalizada_em − iniciada_em`.

**Alternativa descartada:** somar os tempos por questão para obter o tempo da
atividade.

**Motivo:** a seção 5.1 define a produtividade pelos carimbos da tentativa. O
tempo por questão fica disponível para o gabarito e para análise futura, sem
competir com a definição oficial.

### D18 — "Nenhuma" no check-in é exclusiva (fatia 1)

**Decisão:** marcar "nenhuma" desmarca as demais dificuldades e grava o
check-in com zero ligações.

**Alternativa descartada:** permitir "nenhuma" junto com outras.

**Motivo:** a contagem da seção 5.3 usa `total_respostas` como o número de
check-ins do ciclo; uma resposta contraditória distorceria o "5 de 7".

### D19 — `/demo` exige autenticação (fatia 1)

**Decisão:** `/demo` é acessível aos dois papéis, mas só a quem já entrou, e só
existe com o perfil `demo`; fora dele, 404.

**Alternativa descartada:** deixar `/demo` público.

**Motivo:** o painel restaura e fecha ciclos; deixá-lo aberto tornaria possível
destruir o cenário no meio da defesa a partir de outra aba.

### D20 — O único lugar com percepção individual é `/meu-historico` (fatia 1)

**Decisão:** o gráfico de `/meu-historico` mostra acerto e confiança pessoais
por ciclo, e a rota só devolve dados do próprio usuário autenticado.

**Alternativa descartada:** omitir a confiança do histórico pessoal.

**Motivo:** a seção 4 garante ao colaborador "o próprio histórico completo"; a
regra de privacidade limita o **gestor**, não a pessoa.

---

## Stack e infraestrutura

### D21 — A referência visual passou para `docs/` (fatia 1)

**Decisão:** `styles.css` foi movido de `referencia-visual/` para
`docs/referencia-visual/styles.css` (caminho que a seção 7 cita) e copiado para
`src/main/resources/static/css/styles.css`. Os acréscimos ficam em
`static/css/app.css`.

**Alternativa descartada:** editar o arquivo original do grupo.

**Motivo:** a seção 7 manda manter a identidade; separar "referência do grupo"
de "acréscimos do sistema" deixa visível o que foi adicionado.

### D22 — Spring Boot 3.3.5 e Gradle com DSL Groovy (fatia 1)

**Decisão:** `org.springframework.boot` 3.3.5, `build.gradle` em Groovy,
toolchain Java 21.

**Alternativa descartada:** Gradle Kotlin DSL.

**Motivo:** a seção 0.4 pede clareza acima de esperteza; a DSL Groovy é a que o
grupo encontra na maioria dos tutoriais que vai consultar durante a defesa.

### D23 — Um contêiner PostgreSQL para toda a suíte de testes (fatia 1)

**Decisão:** `TesteComBanco` usa o padrão "singleton container": um único
PostgreSQL 16 sobe uma vez e é reaproveitado por todas as classes de teste.

**Alternativa descartada:** `@Testcontainers` com `@Container` estático por
classe.

**Motivo:** subir um banco por classe tornaria o ciclo de fatias verticais
lento demais para o ritmo de trabalho.

### D24 — Testcontainers fixado em 1.21.4 (fatia 1)

**Decisão:** `build.gradle` sobrescreve `testcontainers.version` para 1.21.4.

**Alternativa descartada:** manter a 1.19.8 que o Spring Boot 3.3.5 traz, ou
subir o Spring Boot inteiro de versão.

**Motivo:** o Docker Engine 29 recusa (erro 400) a versão da API usada pela
1.19.8, e nenhum teste de banco sobe. Trocar só a biblioteca de teste resolve
sem mexer na versão do Spring que o grupo vai citar na defesa.

### D25 — Rodar o Gradle fora de caminhos com `!` (fatia 1)

**Decisão:** nesta máquina o projeto fica em `C:\!Faculdade\...`, e o `!` quebra
o carregamento das classes de teste (`ClassNotFoundException`), porque o Java
usa `!` como separador em URLs de classpath. Os testes rodam a partir de uma
unidade mapeada: `subst P: "C:\!Faculdade\6° Período\Pesquisa Aplicada\Artefato"`
e depois `P:\> gradlew test`.

**Alternativa descartada:** mover o `build/` para fora da pasta do projeto via
`build.gradle`.

**Motivo:** o problema é do caminho desta máquina, não do projeto; quem clonar
em um caminho sem `!` não precisa de nada. Mudar o `build.gradle` por causa
disso esconderia a causa real.

---

## Fatia 2: coleta do colaborador

### D26: Áreas, dificuldades e intervenções ficam numa migration (fatia 2)

**Decisão:** `V2__catalogos.sql` insere as 4 áreas, as 5 dificuldades e as
6 intervenções da seção 5.3.

**Alternativa descartada:** semeá-las pelo pacote `simulacao`, junto com os
dados do perfil `demo`.

**Motivo:** as regras dependem desses códigos (por exemplo,
`TRILHA_VERIFICACAO_CHECKLIST` no alerta B). São dados de referência, não dados
simulados, e precisam existir em qualquer perfil.

### D27: Banco de questões num JSON gerado por script (fatia 2)

**Decisão:** as 45 questões originais (10 por área e mais 5 de verificação em
situações de atendimento) ficam em `ferramentas/gerar_atividades.py`, que gera
`src/main/resources/catalogo/atividades.json`. As atividades reaproveitam
enunciados, como a seção 8.1 permite: no ciclo *k*, cada área usa 5 questões
consecutivas do seu banco de 10, começando na posição 2(*k*−1). A posição da
alternativa certa é embaralhada com semente fixa. A 4A contém a questão do
prazo de reembolso, e o script falha se ela não estiver lá.

**Alternativa descartada:** escrever 130 questões distintas diretamente em SQL.

**Motivo:** o grupo revisa o texto num lugar só, em Python legível, e o
resultado é reproduzível. **Limitação conhecida:** em várias questões a
alternativa certa é a mais longa, um vício clássico de itens de múltipla
escolha. Para a demonstração isso não importa, porque o acerto é simulado, mas
deve ser revisto antes de qualquer uso com pessoas reais.

### D28: Todas as atividades têm nível 1 (fatia 2)

**Decisão:** `atividade.nivel = 1` para 1A…6B e para a trilha.

**Alternativa descartada:** subir o nível a cada ciclo.

**Motivo:** a seção 5.4 exige que a troca de escopo mantenha o nível, e a
seção 8.4 atribui a subida do técnico ao aprendizado do time. Com o nível
constante, a comparação entre ciclos não se mistura com a dificuldade da prova.

### D29: Até a fatia 4, o perfil demo abre o ciclo 1 ao subir (fatia 2)

**Decisão:** se não houver ciclo nenhum, o `IniciadorDemo` abre o ciclo 1 com
início na data de hoje e libera 1A e 1B aos três times.

**Alternativa descartada:** esperar pelo "Restaurar cenário inicial".

**Motivo:** as telas do colaborador precisam de um ciclo aberto para serem
usadas e testadas agora. O cenário da seção 8.2 substitui esse estado na
fatia 4.

### D30: Abrir a atividade cria a tentativa; reabrir não reinicia o tempo (fatia 2)

**Decisão:** o primeiro `GET /atividades/{id}` grava `iniciada_em`. Reabrir a
página devolve a mesma tentativa, e o cronômetro é contado a partir desse
instante. Uma atividade finalizada redireciona para o resultado.

**Alternativa descartada:** iniciar a tentativa só no primeiro clique numa
alternativa.

**Motivo:** a produtividade da seção 5.1 usa `finalizada_em − iniciada_em`, e
ler as questões faz parte do tempo da atividade. A seção 5.4 também usa "algum
membro já iniciou tentativa" para decidir quando a troca de escopo vale, e isso
precisa ser um fato gravado.

### D31: Atividade de outro time responde 404, não 403 (fatia 2)

**Decisão:** quem tenta abrir uma atividade liberada a outro time recebe
"não encontrada".

**Alternativa descartada:** 403.

**Motivo:** um 403 confirmaria que aquele id existe e é de outra equipe. O
time da pessoa vem da sessão, nunca da URL.

### D32: A escala Likert continua navegável por teclado (fatia 2)

**Decisão:** `app.css` sobrescreve o `display:none` que a referência aplica ao
radio da escala Likert. O radio fica invisível, mas pode receber foco.

**Alternativa descartada:** manter o `display:none`.

**Motivo:** com `display:none` o radio sai da ordem de tabulação, e a seção 7
exige foco de teclado visível. A aparência da escala não muda.

### D33: Chart.js servido pela própria aplicação (fatia 2)

**Decisão:** `static/js/chart.umd.min.js` (versão 4.4.4) fica no repositório.

**Alternativa descartada:** carregar de uma CDN.

**Motivo:** a defesa oral pode acontecer sem internet. Um gráfico que não
aparece no meio da demonstração é um risco que não precisamos correr.

---

## Fatia 3: diagnóstico

### D34: A regra B lê o acerto em verificação das respostas (fatia 3)

**Decisão:** a regra B calcula o acerto em `VERIFICACAO` de c e de c−1 a
partir das respostas das tentativas finalizadas, arredondado a uma casa
decimal, como os indicadores gravados.

**Alternativa descartada:** acrescentar uma coluna de acerto em verificação a
`indicador_time_ciclo`.

**Motivo:** a seção 4 fixa as colunas da tabela, e a seção 5.1 diz que o
erro por área "é calculado na leitura, sem snapshot". Isso é seguro porque as
respostas de um ciclo fechado não mudam mais: só se finaliza tentativa no
ciclo aberto.

### D35: "Copiar o escopo vigente" avança o número das atividades (fatia 3)

**Decisão:** ao abrir o ciclo c+1, cada atividade do escopo padrão avança de
número ("4A" vira "5A", "4B" vira "5B"). Uma atividade vinda de troca de
escopo, como a trilha de verificação, continua a mesma no ciclo seguinte e
leva junto o `recomendacao_id`.

**Alternativa descartada:** copiar as mesmas atividades ("4A" de novo no
ciclo 5).

**Motivo:** a seção 8.1 dá atividades próprias a cada ciclo, e a troca de
escopo precisa sobreviver aos ciclos seguintes para que "Comparar antes e
depois" faça sentido. **Pendente para a fatia 5:** quando a troca só vale a
partir do próximo ciclo (algum membro já começou uma tentativa), a abertura
do ciclo precisará aplicar a troca guardada.

### D36: Sem tentativa finalizada, técnico e tempo médio valem 0 (fatia 3)

**Decisão:** um time que não finalizou nenhuma atividade no ciclo grava
técnico 0 e tempo médio 0.

**Alternativa descartada:** deixar as colunas nulas.

**Motivo:** a seção 4 declara `tecnico` e `tempo_medio_min` como obrigatórios
(só confiança e resistência podem ser nulas). O caso não acontece no cenário
da seção 8, e o engajamento baixo já denuncia a situação.

### D37: Como a regra B se encerra, ciclo a ciclo (fatia 3)

**Decisão:** "a condição B deixa de valer" é avaliado ciclo a ciclo:
confiança ≥ 75 e acerto em verificação < 50 **naquele ciclo**. O alerta B se
encerra quando isso é falso em dois ciclos seguidos. Um ciclo sem amostra
mínima não conta como "deixou de valer". E quando o ciclo que está sendo
fechado não tem amostra, nenhuma regra do time é avaliada, nem as de
encerramento.

**Alternativa descartada:** usar a regra B inteira (dois ciclos) como
condição, ou tratar a falta de amostra como condição falsa.

**Motivo:** a seção 5.1 manda não avaliar as regras sem amostra. Deixar um
alerta se encerrar porque o time parou de responder seria o oposto do que o
alerta quer mostrar.

### D38: Recomendações sem dificuldade marcada (fatia 3)

**Decisão:** no alerta A, uma dificuldade que ninguém marcou não vira
recomendação, mesmo que sobre vaga entre as "duas mais marcadas". A
recomendação do alerta B, que não vem de check-in, grava
`n_respostas = total_respostas = 0` e `dificuldade_id` nulo.

**Alternativa descartada:** completar sempre dois itens no alerta A e
preencher o alerta B com o total de check-ins.

**Motivo:** a recomendação mostra "5 de 7 respostas" como justificativa, e
"0 de 7" não justifica nada. No alerta B, a justificativa é a própria regra.

### D39: "Sem duplicatas" atualiza só o detalhe (fatia 3)

**Decisão:** quando a regra volta a valer para um time que já tem alerta não
resolvido do mesmo tipo, só o `detalhe` é atualizado. O ciclo de origem e a
prioridade continuam os do nascimento.

**Alternativa descartada:** recalcular a prioridade a cada ciclo.

**Motivo:** a seção 5.2 diz que o alerta "nasce" com a prioridade, e o
Quadro 2 mostra "gerado no fechamento do ciclo N": os dois são fatos do
nascimento.

### D40: O alerta A só se encerra depois da intervenção aplicada (fatia 3)

**Decisão:** o encerramento do tipo A só é avaliado para alertas
`EM_ACOMPANHAMENTO` que tenham recomendação com `ciclo_aplicacao_id`. A
contagem de subidas começa nesse ciclo.

**Alternativa descartada:** encerrar também alertas `ABERTO` quando a
confiança sobe sozinha.

**Motivo:** a seção 5.2 define o encerramento "a partir do ciclo de aplicação
da intervenção". Sem intervenção não há ciclo de aplicação, e o alerta fica
aberto esperando a decisão da gestão.

### D41: Agendador de hora em hora, desligável (fatia 3)

**Decisão:** `AgendadorFechamento` confere a cada hora, no minuto 5, se a data
de fim do ciclo aberto passou, e dispara o mesmo `FechamentoCicloService` do
painel. A chave `prontidao.ciclos.fechamento-automatico` desliga o agendador,
e os testes o desligam.

**Alternativa descartada:** agendar para meia-noite.

**Motivo:** conferir de hora em hora fecha o ciclo no máximo uma hora depois
do vencimento, mesmo que a aplicação tenha ficado desligada à meia-noite.
Desligar nos testes impede que um ciclo feche no meio de uma verificação.

---

## Fatia 4: simulação

### D42: O gerador completa até a meta e desconta o que já existe (fatia 4)

**Decisão:** para cada time e ciclo, as metas da seção 8.4 viram totais
inteiros: a soma das respostas de confiança e de receio, o número de
tentativas, os acertos por área e os segundos totais. O gerador desconta o que
já foi respondido e o que está reservado (a Carla no ciclo 4) e reparte só o
restante entre quem ainda não respondeu. A semente decide quem erra cada
questão, quem deixa pendência e os tempos individuais, nunca os totais.

**Alternativa descartada:** gerar cada ciclo inteiro de uma vez, ignorando
quem já respondeu.

**Motivo:** a Carla responde ao vivo, e o botão "Gerar respostas simuladas"
serve "para quem ainda não respondeu" (seção 6). Com os totais fechados, os
números do time batem da mesma forma se a Carla responder antes, depois ou
pelo botão do roteiro.

### D43: Dificuldades fora do Atendimento/ciclo 4 foram escolhidas (fatia 4)

**Decisão:** a seção 8.4 só fixa as dificuldades do Atendimento no ciclo 4
(5, 3, 2, 1, 0). As contagens dos demais times e ciclos estão em
`PerfilTime` e foram escolhidas para serem plausíveis: o Financeiro relata
mais falta de tempo, e o Marketing quase nenhuma dificuldade.

**Alternativa descartada:** deixar os outros ciclos sem dificuldades.

**Motivo:** um time que nunca marca dificuldade pareceria artificial no
histórico. Nenhuma dessas contagens afeta uma regra, porque só o alerta A usa
dificuldades, e ele só dispara no Atendimento, no ciclo 4.

### D44: Áreas fora do ciclo 4 derivam do técnico e da verificação (fatia 4)

**Decisão:** fora do ciclo 4, a meta de verificação vem da seção 8.4, e as
outras três áreas recebem acertos que fazem o total bater com o técnico
(formulação um pouco acima, aplicação um pouco abaixo). No ciclo 4 vale a
tabela de erro por área, e o total é ajustado ao técnico sem afastar nenhuma
área mais de 1,9 ponto da tabela.

**Alternativa descartada:** as quatro áreas iguais ao técnico.

**Motivo:** a regra B depende da verificação, então ela nunca é ajustada. As
diferenças entre as outras áreas deixam o gráfico de erro por área
informativo em todos os ciclos.

### D45: Decisões da gestão e troca de escopo implementadas na fatia 4 (fatia 4)

**Decisão:** `RecomendacaoService` (aceitar, ajustar, adiar, agendar) e
`EscopoService` (salvar escopo) foram escritos nesta fatia, sem tela. "Aceitar"
não desfaz um item AJUSTADO, que também conta como aceito. O ciclo de
aplicação é gravado em todas as recomendações aceitas do alerta, humanas e de
plataforma, e é a partir dele que o alerta A é encerrado. Quando a troca só
vale no próximo ciclo, o fechamento a aplica logo depois de abrir esse ciclo
(`aplicarPendentes`), o que resolve a pendência registrada em D35.

**Alternativa descartada:** deixar tudo para a fatia 5.

**Motivo:** a invariante 8 da seção 8.5 exige salvar o escopo no início do
ciclo 5, e o teste das invariantes é desta fatia. A fatia 5 acrescenta só as
telas.

### D46: O perfil demo sobe com o cenário montado (fatia 4)

**Decisão:** com o banco sem ciclos, o perfil `demo` monta o cenário da seção
8.2 ao subir (`prontidao.demo.restaurar-ao-iniciar: true`). Com ciclos já
existentes, não mexe em nada. Os testes desligam a chave e partem do ciclo 1
vazio. Isso substitui a decisão D29.

**Alternativa descartada:** exigir o clique em "Restaurar cenário inicial" a
cada instalação nova.

**Motivo:** a seção 12 diz que o sistema sobe com dois comandos. Quem abrir o
sistema pela primeira vez já encontra o ciclo 4 pronto para o roteiro.

### D47: O ciclo 4 começa três dias antes de hoje (fatia 4)

**Decisão:** ao restaurar, os ciclos são datados para que o ciclo 4 comece
três dias atrás e termine três dias à frente. Os ciclos 5 e 6, gerados por
"Avançar um ciclo", ficam com datas futuras, mas as respostas deles recebem
horários recentes.

**Alternativa descartada:** datas fixas no passado.

**Motivo:** com o ciclo 4 já vencido, o agendador (D41) o fecharia sozinho no
meio da demonstração.

### D48: Limites aritméticos das metas (fatia 4)

**Decisão:** aceitamos os valores mais próximos que a aritmética permite. Com
7 check-ins, a confiança do Atendimento no ciclo 4 só pode ser 46,4 ou 50,0,
e escolhemos 46,4. Por isso a frase do alerta diz "caiu de 59 para 53 e 46", e
não "47" como no exemplo da seção 6. Todos os valores ficam dentro da
tolerância de ±2 da seção 8.4, e o teste `metasDentroDaTolerancia` confere
isso em todos os ciclos. **Atenção:** se a Carla fizer as atividades ao vivo,
o tempo real dela entra na produtividade do Atendimento no ciclo 4. Para
reproduzir o 10,9 min, use o botão do roteiro.

**Alternativa descartada:** forçar os valores exatos da tabela.

**Motivo:** a especificação fixa a média de respostas inteiras de 1 a 5, e
nem toda meta é atingível com 7 ou 8 pessoas. Os limiares das regras foram
conferidos contra os valores atingíveis.

---

## Fatia 5: gestão

### D49: FA1 aceita os itens não editados (fatia 5)

**Decisão:** em "Ajustar recomendação", cada item cujo texto foi alterado
passa a AJUSTADA com `descricao_ajustada`. Os itens deixados como estavam são
aceitos, e a gestão segue para a tela de escopo, como em "Aceitar e ajustar
escopo".

**Alternativa descartada:** manter os itens não editados como SUGERIDA.

**Motivo:** a seção 5.4 trata o ajuste como um fluxo alternativo ao aceite, e
não como uma terceira decisão. Deixar metade dos itens sem decisão impediria
salvar o escopo, que exige recomendação aceita.

### D50: A troca sugerida pode ser desmarcada (fatia 5)

**Decisão:** a tela de escopo mostra a troca "5A → trilha" já marcada, como
pede a seção 6. Desmarcada, o escopo é salvo sem trocar a atividade: o alerta
passa a EM_ACOMPANHAMENTO e o ciclo de aplicação é registrado, para o
encerramento funcionar.

**Alternativa descartada:** troca obrigatória.

**Motivo:** "já selecionada" implica que dá para tirar a seleção. A decisão
continua com a gestão (seção 5.4), inclusive a de acompanhar o time só com as
intervenções humanas.

### D51: Quadros mostram o ciclo fechado; alertas "vigentes" no ciclo (fatia 5)

**Decisão:** a visão geral e o detalhe do time mostram, por padrão, o último
ciclo fechado. Ao escolher um ciclo anterior, aparecem os alertas vigentes
naquele ciclo: os que nasceram até ele e não tinham sido resolvidos antes
dele. O gráfico do detalhe tem sempre os 6 ciclos no eixo, e o seletor
"ciclos 1 a N" limita até onde os pontos aparecem.

**Alternativa descartada:** mostrar sempre os alertas de hoje.

**Motivo:** o relatório compara o estado de cada ciclo. Um alerta resolvido no
ciclo 6 precisa continuar aparecendo quando a gestora volta ao ciclo 4. O eixo
fixo deixa o marcador de intervenção no mesmo lugar em qualquer intervalo.

### D52: Comentários de template não vão para o navegador (fatia 5)

**Decisão:** os comentários dos templates usam a sintaxe do Thymeleaf
(`<!--/* ... */-->`), que é removida na renderização.

**Alternativa descartada:** comentários HTML comuns.

**Motivo:** um comentário HTML escrito "Comparar antes e depois" aparecia no
código da página mesmo quando o bloco não existia. Isso confunde os testes e
quem inspeciona a página.

---

## Fatia 6: roteiro e evidências

### D53: As evidências são geradas pelo roteiro real, na tela (fatia 6)

**Decisão:** `ferramentas/evidencias/gravar-evidencias.js` (Playwright, com o
Chrome ou o Edge da máquina) executa os 9 passos da seção 9 clicando nas
telas, e salva um print por passo em `docs/evidencias/`. Nos passos da Carla,
o script lê o gabarito em `catalogo/atividades.json` e responde como o roteiro
da seção 8.3 manda: acerta as primeiras questões de cada área e erra as
restantes, chegando a 8 de 10 na 4A e 7 de 10 na 4B. Os PNGs não entram no
repositório, porque qualquer pessoa os refaz com um comando.

**Alternativa descartada:** usar o botão "Preencher pendências da Carla" nos
prints, ou versionar os PNGs.

**Motivo:** as telas de check-in, atividade e resultado precisam aparecer
preenchidas, como no uso real, para servirem de evidência. Os prints mudam a
cada ajuste visual, e versioná-los só aumentaria o repositório. Como a Carla
do script responde em segundos, a produtividade do Atendimento no ciclo 4 sai
abaixo de 10,9 min nesses prints (ver D48).

### D54: O `gradlew` é executável no repositório (fatia 6)

**Decisão:** o arquivo `gradlew` foi marcado como executável no git
(`git update-index --chmod=+x`).

**Alternativa descartada:** pedir a cada pessoa que rode `chmod +x gradlew`.

**Motivo:** no Windows o git gravou o arquivo sem permissão de execução, e
`./gradlew` falharia com "permission denied" na máquina de um colega com
Linux ou macOS.

---

## Melhoria: hierarquia de departamentos

### D55: Três níveis de acesso: gerente de RH, líder e colaborador (hierarquia)

**Decisão:** cada time passa a ser um **departamento** (Atendimento,
Financeiro e Marketing) com um **líder** (papel novo `LIDER`). O papel
`GESTOR` passa a significar a **gerente de RH** (Renata), que vê todos os
departamentos e faz a análise geral. O líder vê apenas o próprio
departamento: visão geral, detalhe do time, recomendações e escopo. Um id de
outro time, alerta ou recomendação na URL devolve 403
(`gestao/AcessoDepartamento`).

**Alternativa descartada:** renomear o enum `GESTOR` para `RH`.

**Motivo:** o valor já está gravado no banco e em vários testes; a mudança de
nome não altera o comportamento. O texto da tela diz "Gerente de RH".

### D56: O líder se liga ao departamento por `time.gestor_id` e não é membro do time (hierarquia)

**Decisão:** o líder tem `time_id` nulo (restrição `ck_usuario_lider_sem_time`)
e o departamento aponta para ele pela coluna `gestor_id`, que já existia.

**Alternativa descartada:** colocar o líder no time com `time_id`.

**Motivo:** ele entraria na contagem de membros e no cálculo dos indicadores
(engajamento, amostra mínima), mudando os números do cenário da seção 8.

### D57: Líder cria colaboradores só no próprio departamento; RH em qualquer um (hierarquia)

**Decisão:** a tela `/gestao/equipe` cria contas sempre com o papel
`COLABORADOR`. O serviço (`ContaColaboradorService.cadastrarPelaLideranca`)
lê do banco o papel do autor e o departamento que ele lidera; se o líder
enviar outro departamento no formulário, nada é criado e a tela explica a
regra. A coluna `criado_por_id` guarda quem criou a conta.

**Alternativa descartada:** confiar no campo travado do formulário.

**Motivo:** o navegador pode ser adulterado; a regra precisa valer no servidor.

### D58: A tela de equipe mostra contagens, não a lista de pessoas (hierarquia)

**Decisão:** a tela de equipe mostra, por departamento, o líder, o número de
colaboradores ativos e quantos o usuário criou. Ela não lista nomes.

**Alternativa descartada:** listar os colaboradores do departamento.

**Motivo:** a invariante 9 (seção 4) proíbe que qualquer rota de `/gestao`
mostre dado de uma pessoa específica, e o teste `invariante9` percorre todas
essas rotas.

### D59: O "maior erro" e o "maior ganho" usam só os times visíveis (hierarquia)

**Decisão:** para o líder, a faixa vira "Maior erro do departamento" e o
"maior ganho de confiança entre os times" some (só há um time).

**Motivo:** o maior erro da empresa inteira revelaria ao líder dados de
outros departamentos.
