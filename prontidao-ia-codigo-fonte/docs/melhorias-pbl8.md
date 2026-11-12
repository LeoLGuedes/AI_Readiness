# Melhorias do artefato (PBL 8, DSR etapa 5)

Na DSRM, a avaliação pode devolver o artefato ao desenvolvimento. A seção 8 do
relatório de avaliação listou seis melhorias possíveis. O grupo escolheu a
segunda, de prioridade alta:

| Melhoria | Motivo | Prioridade |
|---|---|---|
| Teste do limite exato: 4 check-ins ocultam, 5 liberam | Cobrir a regra central de privacidade | Alta |

Este documento registra a limitação, o que mudou e as evidências da evolução.

## M1: Teste do limite exato da amostra mínima

### A regra

Seção 4 e seção 5.1 da especificação: o gestor só vê os dados de percepção de
um time (confiança, resistência e dificuldades relatadas) quando pelo menos
**5** pessoas responderam o check-in do ciclo. Com 4 ou menos, a média deixaria
de esconder quem respondeu o quê. A mesma condição desliga as regras de
alerta A e B para o time naquele ciclo.

A regra inteira cabe numa comparação, em `AmostraMinima.suficiente`:

```java
return checkins >= minimoDeCheckins;   // minimoDeCheckins = 5 (application.yml)
```

Trocar `>=` por `>` ou errar o limite em 1 muda quem fica protegido. Por isso
o ponto exato da fronteira, 4 e 5, precisa de teste próprio.

### A limitação encontrada

Antes da melhoria, a fronteira aparecia em testes espalhados, cada um com
outro objetivo:

- `CalculoIndicadoresTest` testava 4 e 5 check-ins só no cálculo, sem banco e
  sem tela;
- `TelasDaGestaoTest.privacidadeSemAmostra` testava 4 check-ins na tela do
  gestor;
- os testes do fechamento e das invariantes usavam o cenário completo, com 7
  ou 8 check-ins.

**Nenhum teste abria a tela do gestor com exatamente 5 check-ins.** O lado
"5 liberam" nunca era conferido onde a gestora de fato olha. E a tela não
lê a `AmostraMinima` direto: ela passa pelo `PrivacidadeGuard`, que é a única
porta da gestão para os dados de check-in. Um erro só no guarda passaria
despercebido.

### O que foi feito

Dois testes novos. **O código de produção não mudou**: a regra estava certa,
o que faltava era a prova.

**`diagnostico/indicador/AmostraMinimaTest`** (código puro, 10 testes):

- `0`, `1` e `4` check-ins não formam amostra; `5`, `6` e `8` formam;
- com 4 check-ins unânimes (confiança e receio máximos), confiança e
  resistência saem nulas; com 5, saem 100;
- do cálculo à regra B, sem atalho: com confiança 100 e acerto em verificação
  de 20% nos dois ciclos, a regra B **não** é avaliada com 4 check-ins e
  **dispara** com 5.

**`gestao/LimiteDaAmostraNaGestaoTest`** (banco real e tela, 3 testes): o
mesmo time (Marketing, 8 pessoas), no mesmo ciclo, com as mesmas respostas;
muda só o número de check-ins.

| Camada | 4 check-ins | 5 check-ins |
|---|---|---|
| Fotografia gravada no fechamento | confiança e resistência nulas | confiança 75,0 e resistência 25,0 |
| `PrivacidadeGuard` | sem amostra; nenhuma contagem | com amostra; "Receio de substituição" marcada 5 vezes (100%) |
| Tela `/gestao/times/{id}` | "Amostra insuficiente: 4 de 8 check-ins, mínimo de 5", sem a dificuldade | "5 de 8 check-ins respondidos (mínimo de 5)", com a dificuldade |

Um terceiro teste confere que o `application.yml` ainda diz 5. Se alguém mudar
o limiar, os números acima perdem o sentido e o teste avisa.

### Evidências da evolução

**1. Suíte de testes.** Antes: 82 testes. Depois: 95 testes, todos passando
(`.\gradlew.bat test` a partir de `P:\`).

**2. Teste de mutantes.** Para medir se os testes novos pegam erros que os
antigos deixavam passar, o script
`ferramentas/mutantes/testar-limite-da-amostra.ps1` injeta, um por vez,
quatro defeitos plausíveis no limite. Para cada um, ele roda a suíte inteira,
anota o que falhou e devolve o código ao estado do git. Resultado de
2026-10-20, sobre o commit `42d5b37`:

| Defeito injetado | Efeito | Suíte antiga (82) | Testes novos (13) |
|---|---|---|---|
| M1. `AmostraMinima`: `>=` vira `>` | o 5º check-in deixa de liberar | 3 falham | 4 falham |
| M2. `AmostraMinima`: limite vira 4 | o 4º check-in passa a liberar | 4 falham | 4 falham |
| M3. `PrivacidadeGuard`: `>=` vira `>` | só a tela do gestor oculta os dados com 5 | **0 falham** | 1 falha |
| M4. `PrivacidadeGuard`: limite vira 4 | só a tela do gestor mostra as dificuldades com 4 | 1 falha | 1 falha |

O ponto que importa é o **M3**. Com a suíte antiga, um erro que escondesse as
dificuldades da gestora num time com exatamente 5 respostas passaria com
todos os testes verdes. A tela continuaria funcionando para 7 ou 8 check-ins,
que são os números do cenário de demonstração, e ninguém notaria. Agora o
defeito quebra o teste `cincoCheckinsLiberam`.

O M1 também mostra uma fragilidade: na suíte antiga, dois dos três testes que
o pegavam eram testes de cenário (fechamento com dados simulados), que
falham por acaso dos números escolhidos, e não por testarem o limite. Os
testes novos falham com uma mensagem que diz o que quebrou: "5 check-ins
liberam".

Para refazer:

```powershell
subst P: "C:\!Faculdade\6° Período\Pesquisa Aplicada\Artefato"
cd P:\
powershell -ExecutionPolicy Bypass -File ferramentas\mutantes\testar-limite-da-amostra.ps1
```

Leva uns 5 minutos e grava o relatório em `build\mutantes-limite-da-amostra.md`.

### O que esta melhoria não cobre

- O limite continua sendo 5, valor da especificação. A outra melhoria da
  tabela ("o mínimo de 5 não basta sozinho", inferência por combinação de
  filtros) fica para um ciclo futuro.
- Os mutantes foram escritos à mão, só para a regra de privacidade. Não é uma
  análise de mutação do sistema inteiro.
