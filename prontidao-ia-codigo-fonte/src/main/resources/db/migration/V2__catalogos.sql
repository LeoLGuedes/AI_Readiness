-- =====================================================================
-- Catalogos fixos (secoes 4 e 5.3 da especificacao).
--
-- Ficam numa migration, e nao no seed de demonstracao, porque as regras de
-- negocio dependem dos codigos (por exemplo, TRILHA_VERIFICACAO_CHECKLIST na
-- recomendacao do alerta B). Sao dados de referencia, nao dados simulados.
-- A ordem de insercao e a "ordem do catalogo" que desempata a secao 5.3 (D10).
-- =====================================================================

insert into area (codigo, nome) values
    ('FORMULACAO',  'Formulação de pedidos à IA'),
    ('VERIFICACAO', 'Verificação das respostas da IA'),
    ('DADOS',       'Uso seguro de dados'),
    ('APLICACAO',   'Aplicação em tarefas do setor');

insert into intervencao (codigo, descricao, executor, tipo) values
    ('COMUNICACAO_LIDERANCA',
     'Comunicação da liderança, em reunião de time, sobre a IA como apoio ao trabalho, e não como substituição',
     'Liderança', 'HUMANA'),
    ('TRILHA_VERIFICACAO',
     'Trilha “Verificação de respostas”: atividades práticas de baixo risco, com situações do próprio setor e feedback comentado',
     'Plataforma', 'PLATAFORMA'),
    ('PRATICA_E_SUPORTE',
     'Horário reservado para prática e ponto focal de suporte no time',
     'Gestor', 'HUMANA'),
    ('OFICINA_CASOS_DE_USO',
     'Oficina de casos de uso do setor',
     'Liderança e TI', 'HUMANA'),
    ('LIDERANCA_USA_E_COMPARTILHA',
     'Liderança passa a usar o assistente e a compartilhar exemplos de uso',
     'Liderança', 'HUMANA'),
    ('TRILHA_VERIFICACAO_CHECKLIST',
     'Trilha “Verificação de respostas” e checklist de revisão das respostas da IA',
     'Plataforma', 'PLATAFORMA');

insert into dificuldade (codigo, descricao, fator, intervencao_id) values
    ('RECEIO_SUBSTITUICAO',
     'Receio de substituição',
     'deslocamento',
     (select id from intervencao where codigo = 'COMUNICACAO_LIDERANCA')),
    ('NAO_SEI_QUANDO_CONFIAR',
     'Não sei quando posso confiar na resposta',
     'confiança',
     (select id from intervencao where codigo = 'TRILHA_VERIFICACAO')),
    ('FALTA_TEMPO_SUPORTE',
     'Falta de tempo ou suporte para praticar',
     'condições facilitadoras',
     (select id from intervencao where codigo = 'PRATICA_E_SUPORTE')),
    ('NAO_SE_ENCAIXA',
     'O assistente não se encaixa nas minhas tarefas',
     'compatibilidade',
     (select id from intervencao where codigo = 'OFICINA_CASOS_DE_USO')),
    ('LIDERANCA_NAO_USA',
     'Meu time ou minha liderança não usa',
     'normas subjetivas',
     (select id from intervencao where codigo = 'LIDERANCA_USA_E_COMPARTILHA'));
