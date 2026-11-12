-- V3 aditiva: preserva o elenco historico e as chaves estrangeiras existentes.
-- Contas preconfiguradas sao protegidas; as novas sao geridas pela propria pessoa.
alter table usuario add column ativo boolean not null default true;
alter table usuario add column autocadastro boolean not null default false;
create index ix_usuario_time_ativo on usuario (time_id) where ativo = true;
