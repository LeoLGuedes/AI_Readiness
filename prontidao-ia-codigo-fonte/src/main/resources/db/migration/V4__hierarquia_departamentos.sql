-- =====================================================================
-- V4 - hierarquia de departamentos
--
--   * novo papel LIDER: lider de um departamento (time). Ve apenas o proprio
--     departamento e cria colaboradores somente nele;
--   * GESTOR passa a significar a Gerente de RH: ve todos os departamentos;
--   * o lider se liga ao departamento pela coluna "time".gestor_id, que ja
--     existia. Ele NAO e membro do time (time_id nulo), para que nao entre no
--     calculo dos indicadores nem na contagem de membros;
--   * criado_por_id registra quem (lider ou RH) criou a conta do colaborador.
--
-- Migration aditiva: nao altera nenhum dado existente.
-- =====================================================================

alter table usuario drop constraint ck_usuario_papel;
alter table usuario add constraint ck_usuario_papel
    check (papel in ('COLABORADOR', 'LIDER', 'GESTOR'));

-- O lider responde por um departamento, mas nao e contado como membro dele.
alter table usuario add constraint ck_usuario_lider_sem_time
    check (papel <> 'LIDER' or time_id is null);

alter table usuario add column criado_por_id bigint;
alter table usuario
    add constraint fk_usuario_criado_por foreign key (criado_por_id) references usuario (id);

create index ix_time_gestor on "time" (gestor_id);
