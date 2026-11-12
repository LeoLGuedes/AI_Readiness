-- =====================================================================
-- V1 - esquema inicial
--
-- Estrutura completa da secao 4 da especificacao. Os nomes de tabela e
-- coluna vem da especificacao e nao foram alterados. Tipos, indices e
-- restricoes de apoio estao registrados em docs/decisoes.md.
--
-- Convencoes adotadas:
--   * enums do dominio viram varchar + CHECK (nao usamos ENUM do Postgres,
--     para que um valor novo seja uma migration de texto e nao um tipo novo);
--   * indicadores sao numeric(4,1) porque a secao 5.1 manda guardar os
--     valores com uma casa decimal;
--   * "time" e escrito entre aspas por ser palavra-chave do SQL.
-- =====================================================================


-- ---------------------------------------------------------------------
-- Estrutura
-- ---------------------------------------------------------------------

create table usuario (
    id         bigserial    primary key,
    nome       varchar(120) not null,
    email      varchar(160) not null,
    senha_hash varchar(80)  not null,
    papel      varchar(20)  not null,
    time_id    bigint,
    constraint uq_usuario_email unique (email),
    constraint ck_usuario_papel check (papel in ('COLABORADOR', 'GESTOR')),
    -- Secao 4: se o papel for COLABORADOR, time_id e obrigatorio.
    constraint ck_usuario_colaborador_tem_time
        check (papel <> 'COLABORADOR' or time_id is not null)
);

create table "time" (
    id        bigserial   primary key,
    nome      varchar(80) not null,
    gestor_id bigint      not null,
    constraint uq_time_nome unique (nome)
);

-- As duas tabelas se referenciam, entao as chaves estrangeiras vem depois.
alter table usuario
    add constraint fk_usuario_time foreign key (time_id) references "time" (id);
alter table "time"
    add constraint fk_time_gestor foreign key (gestor_id) references usuario (id);

create index ix_usuario_time on usuario (time_id);

create table ciclo (
    id     bigserial   primary key,
    numero integer     not null,
    inicio date        not null,
    fim    date        not null,
    status varchar(10) not null,
    constraint uq_ciclo_numero unique (numero),
    constraint ck_ciclo_status check (status in ('ABERTO', 'FECHADO')),
    constraint ck_ciclo_periodo check (fim >= inicio)
);

-- Secao 4: no maximo um ciclo aberto (indice unico parcial).
create unique index uq_ciclo_unico_aberto on ciclo ((1)) where status = 'ABERTO';


-- ---------------------------------------------------------------------
-- Percepcao
-- ---------------------------------------------------------------------

-- O catalogo de intervencoes vem antes porque dificuldade aponta para ele.
create table intervencao (
    id        bigserial   primary key,
    codigo    varchar(60) not null,
    descricao text        not null,
    executor  varchar(80) not null,
    tipo      varchar(12) not null,
    constraint uq_intervencao_codigo unique (codigo),
    constraint ck_intervencao_tipo check (tipo in ('HUMANA', 'PLATAFORMA'))
);

create table dificuldade (
    id             bigserial   primary key,
    codigo         varchar(60) not null,
    descricao      text        not null,
    fator          varchar(80) not null,
    intervencao_id bigint      not null,
    constraint uq_dificuldade_codigo unique (codigo),
    constraint fk_dificuldade_intervencao
        foreign key (intervencao_id) references intervencao (id)
);
-- A "ordem do catalogo" que desempata a secao 5.3 e a ordem de insercao,
-- ou seja, a ordem crescente de id. Nao ha coluna de ordem separada.

create table checkin (
    id                  bigserial   primary key,
    usuario_id          bigint      not null,
    ciclo_id            bigint      not null,
    confianca           smallint    not null,
    receio_substituicao smallint    not null,
    respondido_em       timestamptz not null,
    constraint uq_checkin_usuario_ciclo unique (usuario_id, ciclo_id),
    constraint ck_checkin_confianca check (confianca between 1 and 5),
    constraint ck_checkin_receio check (receio_substituicao between 1 and 5),
    constraint fk_checkin_usuario foreign key (usuario_id) references usuario (id),
    constraint fk_checkin_ciclo foreign key (ciclo_id) references ciclo (id)
);

create index ix_checkin_ciclo on checkin (ciclo_id);

create table checkin_dificuldade (
    checkin_id     bigint not null,
    dificuldade_id bigint not null,
    constraint pk_checkin_dificuldade primary key (checkin_id, dificuldade_id),
    constraint fk_checkin_dificuldade_checkin
        foreign key (checkin_id) references checkin (id) on delete cascade,
    constraint fk_checkin_dificuldade_dificuldade
        foreign key (dificuldade_id) references dificuldade (id)
);


-- ---------------------------------------------------------------------
-- Desempenho
-- ---------------------------------------------------------------------

create table area (
    id     bigserial   primary key,
    codigo varchar(20) not null,
    nome   varchar(80) not null,
    constraint uq_area_codigo unique (codigo),
    constraint ck_area_codigo
        check (codigo in ('FORMULACAO', 'VERIFICACAO', 'DADOS', 'APLICACAO'))
);

create table atividade (
    id        bigserial    primary key,
    codigo    varchar(40)  not null,
    titulo    varchar(160) not null,
    nivel     integer      not null,
    descricao text,
    constraint uq_atividade_codigo unique (codigo),
    constraint ck_atividade_nivel check (nivel >= 1)
);

create table questao (
    id             bigserial primary key,
    atividade_id   bigint    not null,
    area_id        bigint    not null,
    ordem          integer   not null,
    enunciado      text      not null,
    alternativas   jsonb     not null,
    indice_correto integer   not null,
    comentario     text      not null,
    constraint uq_questao_atividade_ordem unique (atividade_id, ordem),
    constraint ck_questao_indice_correto check (indice_correto >= 0),
    constraint fk_questao_atividade foreign key (atividade_id) references atividade (id),
    constraint fk_questao_area foreign key (area_id) references area (id)
);

-- Escopo do time no ciclo.
create table atividade_liberada (
    id              bigserial primary key,
    time_id         bigint    not null,
    ciclo_id        bigint    not null,
    atividade_id    bigint    not null,
    recomendacao_id bigint,
    constraint uq_atividade_liberada unique (time_id, ciclo_id, atividade_id),
    constraint fk_atividade_liberada_time foreign key (time_id) references "time" (id),
    constraint fk_atividade_liberada_ciclo foreign key (ciclo_id) references ciclo (id),
    constraint fk_atividade_liberada_atividade
        foreign key (atividade_id) references atividade (id)
);

create index ix_atividade_liberada_ciclo on atividade_liberada (ciclo_id);

create table tentativa (
    id                    bigserial   primary key,
    usuario_id            bigint      not null,
    atividade_liberada_id bigint      not null,
    iniciada_em           timestamptz not null,
    finalizada_em         timestamptz,
    constraint uq_tentativa unique (usuario_id, atividade_liberada_id),
    constraint ck_tentativa_periodo
        check (finalizada_em is null or finalizada_em >= iniciada_em),
    constraint fk_tentativa_usuario foreign key (usuario_id) references usuario (id),
    constraint fk_tentativa_atividade_liberada
        foreign key (atividade_liberada_id) references atividade_liberada (id)
);

create index ix_tentativa_atividade_liberada on tentativa (atividade_liberada_id);

create table resposta (
    id                    bigserial primary key,
    tentativa_id          bigint    not null,
    questao_id            bigint    not null,
    alternativa_escolhida integer   not null,
    correta               boolean   not null,
    tempo_segundos        integer   not null,
    constraint uq_resposta unique (tentativa_id, questao_id),
    constraint ck_resposta_tempo check (tempo_segundos >= 0),
    constraint fk_resposta_tentativa
        foreign key (tentativa_id) references tentativa (id) on delete cascade,
    constraint fk_resposta_questao foreign key (questao_id) references questao (id)
);


-- ---------------------------------------------------------------------
-- Diagnostico
-- ---------------------------------------------------------------------

-- Fotografia gravada no fechamento do ciclo; nunca recalculada depois.
create table indicador_time_ciclo (
    id              bigserial    primary key,
    time_id         bigint       not null,
    ciclo_id        bigint       not null,
    tecnico         numeric(4,1) not null,
    confianca       numeric(4,1),
    resistencia     numeric(4,1),
    engajamento     numeric(4,1) not null,
    tempo_medio_min numeric(5,1) not null,
    n_checkins      integer      not null,
    n_membros       integer      not null,
    calculado_em    timestamptz  not null,
    constraint uq_indicador_time_ciclo unique (time_id, ciclo_id),
    constraint fk_indicador_time foreign key (time_id) references "time" (id),
    constraint fk_indicador_ciclo foreign key (ciclo_id) references ciclo (id)
);

create table alerta (
    id                 bigserial   primary key,
    time_id            bigint      not null,
    ciclo_id           bigint      not null,
    tipo               varchar(40) not null,
    prioridade         varchar(10) not null,
    status             varchar(20) not null,
    ciclo_resolucao_id bigint,
    detalhe            jsonb       not null,
    constraint ck_alerta_tipo
        check (tipo in ('DIVERGENCIA_TECNICO_CONFIANCA', 'CONFIANCA_SEM_VERIFICACAO')),
    constraint ck_alerta_prioridade check (prioridade in ('NORMAL', 'ALTA')),
    constraint ck_alerta_status
        check (status in ('ABERTO', 'EM_ACOMPANHAMENTO', 'RESOLVIDO')),
    constraint ck_alerta_resolucao
        check (status <> 'RESOLVIDO' or ciclo_resolucao_id is not null),
    constraint fk_alerta_time foreign key (time_id) references "time" (id),
    constraint fk_alerta_ciclo foreign key (ciclo_id) references ciclo (id),
    constraint fk_alerta_ciclo_resolucao foreign key (ciclo_resolucao_id) references ciclo (id)
);

-- Secao 5.2 "sem duplicatas": um time so pode ter um alerta nao resolvido
-- de cada tipo. Quando a regra volta a valer, atualizamos o detalhe.
create unique index uq_alerta_nao_resolvido
    on alerta (time_id, tipo) where status <> 'RESOLVIDO';

create table recomendacao (
    id                 bigserial   primary key,
    alerta_id          bigint      not null,
    intervencao_id     bigint      not null,
    dificuldade_id     bigint,
    n_respostas        integer     not null,
    total_respostas    integer     not null,
    status             varchar(12) not null,
    descricao_ajustada text,
    data_prevista      date,
    ciclo_aplicacao_id bigint,
    decidido_por       bigint,
    decidido_em        timestamptz,
    constraint ck_recomendacao_status
        check (status in ('SUGERIDA', 'ACEITA', 'AJUSTADA', 'ADIADA')),
    constraint ck_recomendacao_contagem
        check (n_respostas >= 0 and total_respostas >= n_respostas),
    constraint fk_recomendacao_alerta foreign key (alerta_id) references alerta (id),
    constraint fk_recomendacao_intervencao
        foreign key (intervencao_id) references intervencao (id),
    constraint fk_recomendacao_dificuldade
        foreign key (dificuldade_id) references dificuldade (id),
    constraint fk_recomendacao_ciclo_aplicacao
        foreign key (ciclo_aplicacao_id) references ciclo (id),
    constraint fk_recomendacao_decidido_por foreign key (decidido_por) references usuario (id)
);

create index ix_recomendacao_alerta on recomendacao (alerta_id);

-- Fecha o ciclo de referencias: a atividade liberada por uma troca de escopo
-- guarda a recomendacao que a originou.
alter table atividade_liberada
    add constraint fk_atividade_liberada_recomendacao
    foreign key (recomendacao_id) references recomendacao (id);
