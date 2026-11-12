# Roteiro de demonstração: UC01

Passo a passo, clique a clique, da demonstração exigida na seção 9 da
especificação. Cada passo diz o resultado esperado e o nome do print. Os nomes
são os mesmos que o script `ferramentas/evidencias/gravar-evidencias.js` gera
em `docs/evidencias/`.

**Antes de começar:** o sistema precisa estar no ar com o perfil `demo`
(veja "Como rodar" no `README.md`), em http://localhost:8080. Todas as senhas
são `demo123`.

| Pessoa | E-mail | Papel |
|---|---|---|
| Renata Guimarães | `renata.guimaraes@aurora.com.br` | gestora dos três times |
| Carla Menezes | `carla.menezes@aurora.com.br` | atendente, time Atendimento |

> Dica para gravar a tela: navegador em tela cheia (`F11`) e gravação com
> `Win + Shift + R` (Ferramenta de Captura do Windows 11).

---

## Passo 1: restaurar o cenário

1. Entre como **Renata**.
2. No menu lateral, clique em **Painel** (seção "Demonstração").
3. Clique em **Restaurar cenário inicial**.

**Resultado esperado:** mensagem "Restaurar cenário inicial: feito." O
painel mostra **Ciclo 4 de 6**. Na tabela, o Atendimento tem **6 de 8
check-ins** e **10 de 16 atividades**: falta só a Carla, e um colega pulou o
check-in de propósito. Financeiro e Marketing aparecem completos, dentro do
cenário. O registro de ações mostra a restauração.

📷 `01-painel-cenario-restaurado.png`

---

## Passo 2: a Carla faz o check-in, as atividades e abre o histórico

1. Clique em **Sair** e entre como **Carla**.
   📷 `02a-login.png` (tela de login, com a lista dos usuários de demonstração)
2. **Início:** aparece "Olá, Carla", "Ciclo 4 de 6" com as datas, a barra
   "0 de 3 pendências concluídas" e as pendências check-in, 4A e 4B.
   📷 `02b-inicio.png`
3. Clique em **Fazer check-in** e responda:
   - Pergunta 1 (quão segura se sente): **2**
   - Pergunta 2 (a IA pode substituir parte do meu trabalho): **4**
   - Pergunta 3: **Receio de substituição** e **Não sei quando posso confiar
     na resposta**

   Mostre o aviso fixo de privacidade no fim do formulário.
   📷 `02c-checkin.png` (formulário preenchido)

   *Opcional:* antes de preencher, clique em "Enviar check-in" com tudo
   vazio para mostrar a validação em cada pergunta.
4. Clique em **Enviar check-in**. **Esperado:** volta ao início com o aviso
   "Check-in enviado", e o check-in aparece como concluído.
5. Na linha **Atividade 4A**, clique em **Começar**. Aparecem 10 questões e o
   cronômetro. Responda as 10; o roteiro prevê **8 acertos**. A questão sobre
   o prazo de reembolso citado pelo assistente está nesta atividade.
   📷 `02d-atividade-4A.png`

   *Opcional:* clique em "Finalizar atividade" com questões em branco para
   mostrar o bloqueio ("Faltam: …").
6. Clique em **Finalizar atividade**. **Esperado:** "8 de 10", o tempo e o
   gabarito comentado questão a questão.
   📷 `02e-resultado-4A.png`
7. Volte ao **Início** e faça a **Atividade 4B** do mesmo jeito; o roteiro
   prevê **7 acertos**.
   📷 `02d-atividade-4B.png` e `02e-resultado-4B.png`
8. Clique em **Meu histórico**. **Esperado:** gráfico do próprio acerto e da
   própria confiança por ciclo, tabela dos ciclos (o ciclo 3 mostra **65%**) e
   a nota "Só você vê estes dados."
   📷 `02f-meu-historico.png`

> **Atalho:** o botão **Preencher pendências da Carla conforme o roteiro**
> do Painel faz os itens 3 a 7 de uma vez, com os tempos do roteiro (cerca de
> 11 e 10 minutos). Feito pela tela, o tempo real da Carla entra na
> produtividade do Atendimento no ciclo 4 (decisão D48).

---

## Passo 3: fechar o ciclo 4

1. Saia e entre como **Renata**.
2. **Painel** → **Fechar ciclo aberto**.

**Resultado esperado:** no registro de ações:
- "Ciclo 4 fechado. Ciclo 5 aberto."
- "Alerta novo: Sabe usar, mas não se sente pronto – Atendimento (ALTA)"
- "Alerta mantido: Confiança alta sem senso crítico – Marketing"

📷 `03-painel-ciclo-4-fechado.png`

---

## Passo 4: visão geral e detalhe do Atendimento

1. Clique em **Visão geral**.
   **Esperado (Quadro 1):** ciclo 4 selecionado; faixa "Maior erro da empresa
   no ciclo 4: Verificação das respostas da IA"; um cartão por time com
   técnico, confiança, resistência e engajamento e a variação (↑ ↓ =); selo
   vermelho **"Sabe usar, mas não se sente pronto · Alta"** no Atendimento,
   selo âmbar **"Confiança alta sem senso crítico · Normal"** no Marketing e
   "Sem alertas" no Financeiro; destaque **"Financeiro: +19 pontos (34 →
   53)"**.
   📷 `04a-visao-geral.png`
2. No cartão do Atendimento, clique em **Ver detalhe do time**.
   **Esperado (Quadro 2):** caixa do alerta com a regra em linguagem simples
   ("Técnico subiu de 61 para 67 e 72; confiança caiu de 59 para 53 e 46") e
   "Engajamento caiu 13 pontos → prioridade alta"; gráfico de linhas dos
   quatro indicadores; **Erro por área**; **Dificuldades relatadas**
   (receio de substituição 5, ou 71% de 7 respostas); produtividade; aviso de
   amostra "7 de 8 check-ins respondidos (mínimo de 5)".
   📷 `04b-detalhe-atendimento.png`

---

## Passo 5: recomendação

1. Na caixa do alerta, clique em **Ver recomendação →**.
2. Abra **Por que esta recomendação?** no primeiro item.

**Esperado (Quadro 3):** título "Sabe usar, mas não se sente pronto –
Atendimento – ciclo 4"; dois itens:
1. Comunicação da liderança · receio de substituição · **5 de 7 respostas** ·
   selo **Ação humana**;
2. Trilha "Verificação de respostas" · não sei quando posso confiar ·
   **3 de 7 respostas** · selo **Plataforma**.

O expansor mostra o percentual (71%) e a regra ativa. No rodapé: "As
recomendações apoiam a decisão da gestão e não a substituem."

📷 `05-recomendacao.png`

3. Clique em **Aceitar e ajustar escopo**.

---

## Passo 6: escopo e agendamento

**Esperado:** escopo atual (5A e 5B) ao lado do proposto, com a troca
**5A → Trilha verificação – situações do Atendimento** já marcada. Aparece a
frase "a troca vale já no ciclo aberto", porque ninguém começou o ciclo 5.

📷 `06a-escopo.png`

1. Clique em **Salvar escopo**. **Esperado:** volta ao detalhe do time com
   "Escopo salvo. O alerta está em acompanhamento."
2. Volte à tela de escopo (no detalhe do time, **Ver recomendação** → "Ver
   escopo e agendamentos"). Em **Intervenções humanas**, escolha uma data para
   a comunicação da liderança e clique em **Marcar como agendada**.
   **Esperado:** "Intervenção agendada" e o selo com a data.
   📷 `06b-escopo-salvo-e-agendado.png`
3. Abra o detalhe do Atendimento. **Esperado:** alerta **Em acompanhamento**
   e a linha tracejada de **intervenção** no ciclo 5 do gráfico.
   📷 `06c-detalhe-em-acompanhamento.png`

---

## Passo 7: avançar os ciclos 5 e 6

1. **Painel** → **Avançar um ciclo** → **Avançar um ciclo** (duas vezes).

**Esperado:** o registro mostra os ciclos 5 e 6 gerados e fechados, o alerta
do Marketing mantido e, no ciclo 6, **"Alerta resolvido: Sabe usar, mas não
se sente pronto – Atendimento"**. O painel diz "Nenhum ciclo aberto".

📷 `07-painel-ciclos-5-e-6.png`

---

## Passo 8: comparar antes e depois

1. **Visão geral** → detalhe do **Atendimento**. No seletor, escolha
   **ciclos 1 a 6**.

**Esperado:** alerta **Resolvido** ("resolvido no fechamento do ciclo 6");
gráfico dos seis ciclos com o marcador em C5, a confiança subindo depois da
intervenção (46 → 50 → 56); tabela **Comparar antes e depois** com C4 (antes),
C5 e C6.

📷 `08-atendimento-ciclos-1-a-6.png`

---

## Passo 9: Financeiro e Marketing

1. **Visão geral** → **Ver detalhe do time** do **Financeiro**.
   **Esperado:** nenhum alerta; confiança subindo de forma consistente.
   📷 `09a-financeiro.png`
2. **Visão geral** → **Ver detalhe do time** do **Marketing**.
   **Esperado:** alerta **"Confiança alta sem senso crítico"** aberto desde o
   ciclo 2; verificação é a área com mais erro.
   📷 `09b-marketing.png`

---

## Gerar os prints automaticamente

Com o sistema no ar:

```powershell
cd P:\ferramentas\evidencias
npm install
node gravar-evidencias.js
```

O script executa exatamente este roteiro no Chrome (ou no Edge) da máquina e
salva os 20 prints em `docs/evidencias/`. Ele **restaura o cenário** no
passo 1, apagando o que estiver no banco. Para outra porta:
`node gravar-evidencias.js http://localhost:8081`.
