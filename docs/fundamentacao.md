# Fundamentação teórica dos instrumentos e das regras

Este documento liga cada pergunta, indicador e regra do artefato
(`docs/especificacao.md`) a um construto ou modelo já publicado. Ele serve de
base para a seção de fundamentação do relatório de DSR e para a defesa oral:
para cada "por que isso?", há uma fonte.

> **Antes de citar no relatório:** confira cada referência na fonte original
> (página, volume, ano da edição que o grupo usar). As referências abaixo são
> as publicações de origem de cada construto, não uma revisão sistemática.

---

## 1. Visão geral

O artefato mede a **prontidão para IA generativa** por duas fontes, como diz a
seção 1 da especificação:

| Fonte | O que mede | Modelo de referência |
|---|---|---|
| **Desempenho** (atividades) | letramento em IA: formular, verificar, proteger dados, aplicar | Long & Magerko (2020); Ng et al. (2021) |
| **Percepção** (check-in) | autoeficácia, ansiedade de substituição, barreiras de adoção | Compeau & Higgins (1995); Wang & Wang (2022); Venkatesh et al. (2003); Moore & Benbasat (1991) |

A ideia de "prontidão para tecnologia" como combinação de atitudes que
impulsionam e que inibem a adoção vem do *Technology Readiness Index*
(Parasuraman & Colby, 2015). O artefato difere dele em dois pontos: mede de
forma **repetida** (ciclos semanais), e não num questionário único, e cruza a
percepção com o **desempenho observado**. É exatamente a lacuna descrita na
seção 1 da especificação.

---

## 2. Check-in (percepção)

### Pergunta 1: confiança

> "Quão seguro(a) você se sente para usar o assistente de IA nas suas tarefas?"
> Escala de 1 (nada seguro) a 5 (totalmente seguro).

- **Construto:** autoeficácia no uso de tecnologia, ou seja, a crença da
  pessoa na própria capacidade de usar a ferramenta para realizar a tarefa
  (Bandura, 1977; Compeau & Higgins, 1995).
- **Por que importa:** a autoeficácia prevê o uso efetivo da tecnologia de
  forma independente da habilidade real. Por isso ela pode divergir do
  desempenho, e essa divergência é o que a regra A procura.
- **Indicador:** `Confiança = média de (resposta − 1) / 4 × 100`.

### Pergunta 2: receio de substituição

> "Quanto você concorda com a frase: a IA pode substituir parte do meu
> trabalho?" Escala de 1 (discordo totalmente) a 5 (concordo totalmente).

- **Construto:** ansiedade em relação à IA, na dimensão *job replacement
  anxiety* da AI Anxiety Scale (Wang & Wang, 2022).
- **Por que importa:** o receio de substituição é uma das principais
  barreiras afetivas à adoção. No cenário, ele sobe no Atendimento junto com
  a queda de confiança.
- **Indicador:** `Resistência = média de (resposta − 1) / 4 × 100`.

### Pergunta 3: dificuldades da semana

> "Quais dificuldades você teve com a IA nesta semana?" Múltipla escolha com o
> catálogo, mais a opção "nenhuma".

Cada item do catálogo (seção 5.3) corresponde a um fator de um modelo de adoção
conhecido:

| Dificuldade | Fator | Origem do fator |
|---|---|---|
| Receio de substituição | deslocamento | *Job replacement anxiety* (Wang & Wang, 2022) |
| Não sei quando posso confiar na resposta | confiança (calibração) | *Trust in automation* (Lee & See, 2004) |
| Falta de tempo ou suporte para praticar | condições facilitadoras | UTAUT (Venkatesh et al., 2003) |
| O assistente não se encaixa nas minhas tarefas | compatibilidade | Difusão de Inovações (Rogers, 2003; Moore & Benbasat, 1991) |
| Meu time ou minha liderança não usa | normas subjetivas / influência social | UTAUT (Venkatesh et al., 2003) |

A pergunta pede **as dificuldades da semana**, e não uma opinião geral. Isso
reduz o viés de memória e permite ligar cada dificuldade a uma intervenção no
ciclo seguinte.

### Escala e transformação para 0–100

As perguntas 1 e 2 usam escala do tipo Likert de 5 pontos (Likert, 1932). A
transformação `(x − 1) / 4 × 100` é o método **POMP**, *percent of maximum
possible* (Cohen et al., 1999): ela leva o mínimo da escala a 0 e o máximo a
100. Assim, confiança e resistência ficam na mesma unidade do técnico e do
engajamento, e os quatro podem aparecer no mesmo gráfico.

### Limitação a declarar no relatório

Cada construto é medido por **um único item**, adaptado das escalas citadas, e
não pela escala validada completa. A escolha é deliberada: o check-in é semanal
e precisa levar menos de um minuto, e escalas longas repetidas toda semana
geram fadiga e abandono. Há evidência de que medidas de item único são
aceitáveis para construtos estreitos e bem definidos (Wanous, Reichers & Hudy,
1997). Mesmo assim, é uma limitação de validade de construto e deve constar no
relatório.

---

## 3. Atividades (desempenho)

### As quatro áreas

As áreas da seção 4 seguem as competências de letramento em IA descritas por
Long & Magerko (2020) e agrupadas por Ng et al. (2021) em conhecer e entender,
usar e aplicar, avaliar e criar, e ética:

| Área do artefato | Competência de letramento em IA |
|---|---|
| `FORMULACAO`: formulação de pedidos à IA | usar e aplicar |
| `VERIFICACAO`: verificação das respostas da IA | avaliar criticamente |
| `DADOS`: uso seguro de dados | ética, privacidade e segurança |
| `APLICACAO`: aplicação em tarefas do setor | usar e aplicar no contexto de trabalho |

As questões usam situações do dia a dia de uma seguradora, o que aproxima a
avaliação do uso real (princípio de avaliação autêntica). As questões da área
`DADOS` refletem as exigências da LGPD (Lei nº 13.709/2018), como necessidade e
minimização de dados pessoais.

### Indicadores de desempenho

- **Técnico:** percentual de acerto nas tentativas finalizadas. É o nível 2
  ("aprendizagem") do modelo de avaliação de treinamentos de Kirkpatrick
  (Kirkpatrick & Kirkpatrick, 2006).
- **Erro por área:** 100 − acerto da área. Mostra *onde* o time trava, o que um
  questionário único não mostra.
- **Produtividade:** tempo médio por atividade. É lida **junto** com o técnico
  porque rapidez sem acerto não é ganho, e a relação entre as duas medidas é a
  clássica troca entre velocidade e precisão.

---

## 4. Engajamento

`Engajamento = pendências concluídas / pendências esperadas × 100`

Corresponde ao **engajamento comportamental**, isto é, a participação efetiva
nas atividades propostas (Fredricks, Blumenfeld & Paris, 2004). É uma medida de
comportamento observado, e não de percepção. Por isso não depende de amostra
mínima.

---

## 5. Regras de alerta

As duas regras detectam os dois lados do mesmo problema: **confiança mal
calibrada**. Lee & See (2004) definem confiança apropriada como aquela que
corresponde à capacidade real do sistema e do usuário. Existem dois desvios:

| Regra | Desvio | Referência |
|---|---|---|
| **A**: "Sabe usar, mas não se sente pronto" | **subconfiança**: o desempenho sobe e a confiança cai. A pessoa tem a habilidade, mas não a crença na própria capacidade, e tende a abandonar a ferramenta. | Lee & See (2004); Bandura (1977) |
| **B**: "Confiança alta sem senso crítico" | **sobreconfiança / viés de automação**: a confiança é alta, mas a pessoa não verifica o que a IA produz. | Parasuraman & Manzey (2010) |

### Por que a regra A exige três ciclos e duas quedas de 5 pontos

Uma única medição não separa tendência de ruído. Exigir que o técnico suba em
dois intervalos seguidos, e que a confiança caia pelo menos 5 pontos nos dois,
é uma regra conservadora: ela troca sensibilidade por menos alarmes falsos. Os
limiares ficam em `application.yml` para que o grupo possa discuti-los como
parâmetros de projeto, e não como verdades.

### Prioridade por queda de engajamento

A queda de engajamento é o sinal comportamental de que a percepção já está
virando abandono. Quando os dois aparecem juntos, a ação é mais urgente.

---

## 6. Recomendações e intervenções

Cada dificuldade aponta para uma intervenção que ataca o fator correspondente:

| Fator | Intervenção | Fundamento |
|---|---|---|
| Deslocamento | Comunicação da liderança sobre a IA como apoio | reduzir a ansiedade de substituição (Wang & Wang, 2022); a etapa de conscientização do modelo ADKAR (Hiatt, 2006) |
| Confiança | Trilha de verificação com feedback comentado | calibrar a confiança com experiências de baixo risco e retorno imediato (Lee & See, 2004); experiências de êxito aumentam a autoeficácia (Bandura, 1977) |
| Condições facilitadoras | Horário reservado e ponto focal de suporte | condições facilitadoras do UTAUT (Venkatesh et al., 2003) |
| Compatibilidade | Oficina de casos de uso do setor | compatibilidade percebida (Rogers, 2003) |
| Normas subjetivas | Liderança passa a usar e compartilhar exemplos | influência social do UTAUT (Venkatesh et al., 2003) |

O sistema **recomenda** e a gestora **decide** (seção 5.4). O rodapé da tela de
recomendação diz isso de forma explícita, o que é coerente com o próprio tema
do artefato: não se pede confiança cega numa recomendação automática.

### "Comparar antes e depois"

A tabela do Quadro 2 compara o ciclo anterior à intervenção com os ciclos
seguintes. É o desenho de avaliação de Kirkpatrick para mudança de
comportamento (nível 3). **Limitação:** no cenário simulado, a melhora dos
ciclos 5 e 6 é programada (seção 8.2), então a demonstração mostra que o
artefato *registra e exibe* o efeito, não que a intervenção o *causou*.

---

## 7. Privacidade e amostra mínima

- **Mínimo de 5 check-ins por time** para exibir percepção ao gestor: segue a
  lógica do *k-anonimato* (Sweeney, 2002). Com grupos pequenos, a média deixa
  de proteger o indivíduo, porque a gestora poderia deduzir quem respondeu o
  quê. Limites entre 3 e 5 respondentes são a prática usual em pesquisas de
  clima organizacional.
- **O gestor nunca vê check-in individual:** princípios de necessidade e
  minimização da LGPD (art. 6º, III). A gestão precisa da tendência do time
  para decidir a intervenção, não da resposta de cada pessoa.
- **Efeito sobre a qualidade do dado:** a garantia de anonimato, exibida no
  próprio formulário, reduz o viés de desejabilidade social nas respostas de
  confiança e receio.

---

## 8. Posição na Design Science Research

O artefato é uma **instanciação** (Hevner et al., 2004), e esta versão atende à
etapa de **demonstração** do DSRM (Peffers et al., 2007): mostrar que o
artefato resolve uma instância do problema, no caso o UC01 com dados
simulados. A etapa de **avaliação** com usuários reais fica como trabalho
futuro e deve aparecer nas limitações.

---

## Referências

- BANDURA, A. Self-efficacy: toward a unifying theory of behavioral change.
  *Psychological Review*, v. 84, n. 2, p. 191–215, 1977.
- BRASIL. Lei nº 13.709, de 14 de agosto de 2018. Lei Geral de Proteção de
  Dados Pessoais (LGPD).
- COHEN, P.; COHEN, J.; AIKEN, L. S.; WEST, S. G. The problem of units and the
  circumstance for POMP. *Multivariate Behavioral Research*, v. 34, n. 3,
  p. 315–346, 1999.
- COMPEAU, D. R.; HIGGINS, C. A. Computer self-efficacy: development of a
  measure and initial test. *MIS Quarterly*, v. 19, n. 2, p. 189–211, 1995.
- FREDRICKS, J. A.; BLUMENFELD, P. C.; PARIS, A. H. School engagement:
  potential of the concept, state of the evidence. *Review of Educational
  Research*, v. 74, n. 1, p. 59–109, 2004.
- HEVNER, A. R.; MARCH, S. T.; PARK, J.; RAM, S. Design science in information
  systems research. *MIS Quarterly*, v. 28, n. 1, p. 75–105, 2004.
- HIATT, J. M. *ADKAR: a model for change in business, government and our
  community*. Loveland: Prosci, 2006.
- KIRKPATRICK, D. L.; KIRKPATRICK, J. D. *Evaluating training programs: the
  four levels*. 3. ed. San Francisco: Berrett-Koehler, 2006.
- LEE, J. D.; SEE, K. A. Trust in automation: designing for appropriate
  reliance. *Human Factors*, v. 46, n. 1, p. 50–80, 2004.
- LIKERT, R. A technique for the measurement of attitudes. *Archives of
  Psychology*, n. 140, 1932.
- LONG, D.; MAGERKO, B. What is AI literacy? Competencies and design
  considerations. In: *Proceedings of the 2020 CHI Conference on Human Factors
  in Computing Systems*. New York: ACM, 2020.
- MOORE, G. C.; BENBASAT, I. Development of an instrument to measure the
  perceptions of adopting an information technology innovation. *Information
  Systems Research*, v. 2, n. 3, p. 192–222, 1991.
- NG, D. T. K.; LEUNG, J. K. L.; CHU, S. K. W.; QIAO, M. S. Conceptualizing AI
  literacy: an exploratory review. *Computers and Education: Artificial
  Intelligence*, v. 2, 100041, 2021.
- PARASURAMAN, A.; COLBY, C. L. An updated and streamlined technology readiness
  index: TRI 2.0. *Journal of Service Research*, v. 18, n. 1, p. 59–74, 2015.
- PARASURAMAN, R.; MANZEY, D. H. Complacency and bias in human use of
  automation: an attentional integration. *Human Factors*, v. 52, n. 3,
  p. 381–410, 2010.
- PEFFERS, K.; TUUNANEN, T.; ROTHENBERGER, M. A.; CHATTERJEE, S. A design
  science research methodology for information systems research. *Journal of
  Management Information Systems*, v. 24, n. 3, p. 45–77, 2007.
- ROGERS, E. M. *Diffusion of innovations*. 5. ed. New York: Free Press, 2003.
- SWEENEY, L. k-anonymity: a model for protecting privacy. *International
  Journal of Uncertainty, Fuzziness and Knowledge-Based Systems*, v. 10, n. 5,
  p. 557–570, 2002.
- VENKATESH, V.; MORRIS, M. G.; DAVIS, G. B.; DAVIS, F. D. User acceptance of
  information technology: toward a unified view. *MIS Quarterly*, v. 27, n. 3,
  p. 425–478, 2003.
- WANG, Y.-Y.; WANG, Y.-S. Development and validation of an artificial
  intelligence anxiety scale: an initial application in predicting motivated
  learning behavior. *Interactive Learning Environments*, v. 30, n. 4,
  p. 619–634, 2022.
- WANOUS, J. P.; REICHERS, A. E.; HUDY, M. J. Overall job satisfaction: how
  good are single-item measures? *Journal of Applied Psychology*, v. 82, n. 2,
  p. 247–252, 1997.
