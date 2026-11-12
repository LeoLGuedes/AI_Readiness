# Prontidão IA — cadastro e perfil de colaborador (melhoria CRUD)

## 1. Escopo exato

Base inspecionada: `artefato(6).zip` (Java 21, Spring Boot 3.3.5, Gradle 8.10.2, Thymeleaf, PostgreSQL 16, Flyway, Spring Security). Protótipo de referência: `ai_readiness.zip` (HTML/CSS/JavaScript sem backend). A base continha a suíte PBL7 + PBL8, com **102 testes já aprovados no computador do usuário antes desta melhoria**.

A melhoria foi **preparada e compilada**, mas os testes com JUnit/PostgreSQL não foram executados neste ambiente. O resultado de 102 testes **não pode ser atribuído à versão alterada** até executar a suíte novamente.

### Funcionalidades adicionadas

| Operação | URL | Acesso | Efeito |
|---|---|---|---|
| Create | GET/POST `/cadastro` | Público | Cria somente papel `COLABORADOR`, exigindo nome, e-mail, senha, confirmação e departamento |
| Read | GET `/perfil` | COLABORADOR autenticado | Exibe somente os dados da própria sessão |
| Update | POST `/perfil` | COLABORADOR de autocadastro | Altera nome, e-mail, senha opcional e departamento antes de haver participação; exige senha atual; encerra a sessão |
| Delete | POST `/perfil/excluir` | COLABORADOR de autocadastro | Exige senha e confirmação `EXCLUIR`; desativa a conta e substitui nome/e-mail; encerra a sessão; mantém vínculos históricos por ID |

O formulário **não possui campo de papel** e o serviço usa `Usuario.novoColaborador`, que fixa `Papel.COLABORADOR`; novas contas de gestão não podem ser criadas. Departamentos são os três times pré-existentes: Atendimento, Financeiro e Marketing. Os IDs são validados no servidor.

**Proteção intencional:** Renata, Carla e os demais usuários fixos do cenário acadêmico **podem continuar utilizando a demonstração normalmente**, mas seus cadastros estão bloqueados para edição e exclusão pelo perfil. O fluxo CRUD completo é testado com **uma nova conta criada no formulário**. Isso evita estragar a semente fixa, os testes do PBL7/PBL8 e a demonstração do PBL6.

**Movimentação de departamento:** a mudança é permitida antes de o novo colaborador ter enviado um check-in ou iniciado uma atividade. Depois disso, ela é bloqueada para evitar atribuir retroativamente check-ins a outro time (as consultas existentes consultam `usuario.time` para agregar percepções).

**Exclusão:** é lógica com desidentificação parcial; o registro `usuario` permanece com `ativo = false`, nome genérico e endereço sintético, sem acesso por login. IDs e informações históricas permanecem para integridade referencial. Não equivale a uma rotina completa de eliminação de todos os dados pessoais exigida em produção.

## 2. O que foi examinado em cada ZIP

### `artefato(6).zip`

- Projeto Spring Boot real com classes de domínio, controladores, serviços, repositórios, recursos Thymeleaf e migrations SQL.
- Autenticação existente via `/login`, senha BCrypt, papéis `COLABORADOR` e `GESTOR`.
- Tabela `usuario` com `id`, `nome`, `email`, `senha_hash`, `papel`, `time_id`, sem suporte anterior a autocadastro e desativação.
- Os departamentos já existem na tabela `time` ligada a `usuario.time_id`; **não foi criada outra tabela departamento**.
- `CenarioInicial` mantém os usuários existentes ao restaurar e `GeradorDeterministico` usa as metas do elenco de oito colaboradores por equipe.
- Contém classes de teste JUnit anteriores e documentação de PBL7 e PBL8.
- Também contém diretórios de compilação/cache e Git. **Não devem ser copiados sobre outro projeto.**

### `ai_readiness.zip`

- Protótipo `ai_readiness_prototype/ai_readiness_prototype/` com `index.html`, `app.js`, `styles.css`, `README.md`, além de `.idea/`.
- `app.js` exibe botão **Criar conta** e rota `/signup`, mas o botão só mostra mensagem de conta simulada, sem gravar no PostgreSQL.
- A tela `/profile` do protótipo exibe campos de nome, papel e e-mail, mas o botão **Salvar alterações** só apresenta uma mensagem: não há update persistente.
- Autenticação e progresso demonstrativos via `localStorage`; não há backend, Spring Security, BCrypt ou banco no protótipo.
- A interface é inspiração visual. **Não foram copiados seus dados de login de demonstração, seu JavaScript ou seu armazenamento local**.
- Porta `8000` do protótipo é independente da porta `8080` da aplicação real.

## 3. Arquivos alterados e por quê

### Novos

- `db/migration/V3__autocadastro_perfil.sql`: adiciona `usuario.ativo`, `usuario.autocadastro` e índice, sem excluir ou recriar tabelas.
- `usuario/CadastroForm.java`: campos/validação de registro público, sem papel ou IDs de terceiros.
- `usuario/PerfilForm.java`: campos de edição e senha atual.
- `usuario/ContaColaboradorService.java`: cadastro, leitura, atualização e exclusão; valida departamentos, senhas, duplicidades e vínculo histórico.
- `usuario/ContaColaboradorController.java`: rotas HTTP e tratamento de formulários; logout após alteração/exclusão.
- `templates/cadastro.html`: nova tela de cadastro no estilo do artefato.
- `templates/colaborador/perfil.html`: perfil e formulário de edição/exclusão.
- `src/test/.../usuario/CadastroPerfilCrudTest.java`: 6 testes de integração novos a executar no seu Windows.

### Modificados, preservando as regras anteriores

- `config/SecurityConfig.java`: `/cadastro` público e `/perfil/**` somente colaborador; CSRF existente preservado.
- `web/LoginController.java` e `templates/login.html`: botão **Criar nova conta** e mensagens de sucesso.
- `templates/layout/base.html`: item **Meu perfil** no menu do colaborador.
- `static/css/app.css`: estilos incrementais para formulários, respeitando as cores originais.
- `usuario/Usuario.java`: propriedades `ativo`, `autocadastro` e métodos de edição/exclusão protegidos.
- `usuario/UsuarioRepository.java`: busca por e-mail e contagem de membros ativos.
- `usuario/UsuarioAutenticado.java` e `usuario/UsuarioDetailsService.java`: `isEnabled()` respeita o estado da conta, e e-mail de login é normalizado.
- `checkin/CheckinRepository.java` e `atividade/TentativaRepository.java`: consulta sobre existência de participação antes de trocar de departamento.
- `simulacao/GeradorDeterministico.java`: não gera respostas automáticas para contas novas; mantém os oito participantes originais por time na geração automática.
- `simulacao/PainelDemo.java`, `gestao/consulta/DetalheTimeQuery.java` e `diagnostico/fechamento/FechamentoCicloService.java`: denominadores da equipe consideram apenas membros ativos.
- `diagnostico/indicador/ColetaDoCiclo.java` e consultas de `PainelDemo.java`: no fechamento do ciclo aberto, as respostas de contas excluídas não são computadas como se elas ainda fossem membros ativos. As fotografias dos ciclos já fechados continuam no banco.

Não alterados: `build.gradle`, Gradle Wrapper, `V1__esquema.sql`, `V2__catalogos.sql`, regras A/B, calculadora de indicadores, testes do PBL7/PBL8, passwords dos usuários de demonstração e rotas de gestão existentes.

## 4. Preparar backup no Windows (antes de instalar)

Abra PowerShell ou terminal do IntelliJ. Garanta que o `bootRun` foi interrompido normalmente com `Ctrl+C`. Mantenha o Docker Desktop acessível.

```powershell
cd C:\dev\artefato
Get-Location
git status --short
$instante = Get-Date -Format 'yyyyMMdd_HHmmss'
$arquivoBackup = "C:\dev\backup_artefato_crud_$instante.zip"
Compress-Archive -Path .\src,.\docs,.\ferramentas,.\gradle,.\gradlew,.\gradlew.bat,.\build.gradle,.\settings.gradle,.\docker-compose.yml -DestinationPath $arquivoBackup -CompressionLevel Optimal
Get-Item $arquivoBackup
```

**Backup adicional do PostgreSQL — importante porque o Flyway executará V3.** O banco é do cenário acadêmico. Não use `docker compose down -v`.

```powershell
cd C:\dev\artefato
docker compose up -d --wait
$dump = "prontidao_pre_crud_$(Get-Date -Format 'yyyyMMdd_HHmmss').dump"
docker exec prontidao-ia-postgres pg_dump -U prontidao -d prontidao -Fc -f "/tmp/$dump"
docker cp "prontidao-ia-postgres:/tmp/$dump" "C:\dev\$dump"
Get-Item "C:\dev\$dump"
docker exec prontidao-ia-postgres rm "/tmp/$dump"
```

Esse processo produz um dump binário sem depender do redirecionamento `>` do PowerShell 5.1 (que pode alterar fluxos binários). Se o `pg_dump` falhar, não prossiga com migrações até entender o motivo.

## 5. Instalar com verificação de conflitos

1. Extraia `Kit_Integracao_CRUD_ProntidaoIA.zip` em **`C:\dev\kit-crud-prontidao`**, sem colocá-lo dentro de `C:\dev\artefato`.
2. Confirme que existem `MANIFESTO_SHA256.json`, `arquivos/` e `INSTALAR_CRUD_SEM_SOBRESCREVER_DIVERGENCIAS.ps1`.
3. Se desejar, abra o script `.ps1` no editor antes de executar.
4. Execute no PowerShell:

```powershell
cd C:\dev\kit-crud-prontidao
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\INSTALAR_CRUD_SEM_SOBRESCREVER_DIVERGENCIAS.ps1" -Projeto "C:\dev\artefato"
```

O instalador faz um **pré-check de hash SHA-256 de TODOS os arquivos alterados antes de copiar qualquer um deles**. Se seu projeto tiver diferenças em relação ao ZIP enviado, ele **interrompe sem sobrescrever essas diferenças**. Também cria um backup local de cada arquivo modificado antes da aplicação. Não toca no banco nem executa `git checkout`, `git reset`, `docker compose down` ou qualquer limpeza de volumes.

Após aplicação:

```powershell
cd C:\dev\artefato
git status --short
Test-Path .\src\main\resources\db\migration\V3__autocadastro_perfil.sql
Test-Path .\src\main\resources\templates\cadastro.html
Test-Path .\src\main\resources\templates\colaborador\perfil.html
```

## 6. Testar e executar

**Java e Docker**

```powershell
cd C:\dev\artefato
$env:JAVA_HOME = "C:\Users\jcpad\.jdks\temurin-21.0.12.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\gradlew.bat --version
docker desktop status
docker compose up -d --wait
```

**Testes novos:**

```powershell
.\gradlew.bat test --tests "br.com.prontidaoia.usuario.CadastroPerfilCrudTest" --rerun-tasks --console=plain
```

**Regressão completa:**

```powershell
.\gradlew.bat test --rerun-tasks --console=plain
Start-Process .\build\reports\tests\test\index.html
```

São **102 testes anteriores + 6 testes novos = 108 casos esperados**. Isso é uma meta, não um resultado confirmado nesta máquina até que os 108 testes passem. Se houver falhas, preserve a saída completa antes de alterar algo.

**Subir o site:**

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

O Flyway deve registrar a migração para versão **3** e o Tomcat iniciar na porta 8080. O Gradle pode continuar em `80% EXECUTING` normalmente. Não encerre o terminal durante a navegação.

## 7. Teste manual de ponta a ponta

1. Abra `http://localhost:8080/login` e confira o botão **Criar nova conta de colaborador**.
2. Clique no botão; abra `/cadastro`.
3. Preencha nome, e-mail novo, senha de 8 ou mais caracteres e escolha **Atendimento** (ou Financeiro/Marketing). Confirme a senha e crie a conta.
4. Deve voltar ao login com confirmação. Entre com o novo e-mail e senha. Deve abrir `/inicio`, nunca `/gestao`.
5. Abra **Meu perfil**. Confira nome, e-mail, papel imutável **Colaborador** e departamento.
6. Atualize o nome/e-mail; se ainda não participou de check-ins/atividades, altere o departamento. Informe senha atual. Salve.
7. Deve sair da sessão. Entre com o e-mail novo. Confira o perfil atualizado e as pendências do setor escolhido.
8. Teste e-mail já cadastrado e senha incorreta: devem ser recusados e a conta original preservada.
9. No perfil da conta nova, digite sua senha e `EXCLUIR`. Confirme. A sessão deve ser encerrada, e o login anterior não deve mais funcionar.
10. Entre com Carla/Renata para verificar que as telas e permissões preexistentes permanecem.
11. Para fins de apresentação, prefira uma conta de demonstração nova com dados fictícios. **Não clique em “Restaurar cenário” durante a coleta de evidências**, pois isso recria os dados de atividades e check-ins.

**Onde tirar prints:** login com botão, cadastro com dropdown de três departamentos, mensagem após cadastro, início com novo colaborador, perfil com papel fixo, edição, confirmação do logout, relatório Gradle. Não exponha senhas em capturas.

## 8. Limites que permanecem

- O protótipo e o novo fluxo são acadêmicos. Não há confirmação de e-mail, convite ou aprovação de RH. Como `/cadastro` é público, **não habilite esse autocadastro em produção sem controle de admissão, verificação e limitação de abuso**.
- O painel `/demo` continua compartilhado com perfis autenticados no ambiente `demo`, limitação preexistente.
- A exclusão preserva identificadores internos e histórico, embora remova nome/e-mail da conta desativada; um procedimento de eliminação completa de dados pessoais não está implementado. A agregação de ciclos fechados mantém suas fotografias; a coleta para ciclos futuros ignora contas inativas. A série técnica histórica usada para novas decisões também poderá mudar se uma conta com respostas antigas for excluída, razão pela qual um sistema de produção deveria registrar vínculo de equipe e participação por ciclo antes de permitir mutações no cadastro.
- Após alteração/exclusão, a sessão atual termina; **outras sessões abertas em dispositivos diferentes não são invalidadas centralmente**. Isso exigiria controle de sessões para produção.
- A mudança de departamento após participação é bloqueada, pois o modelo original não mantém histórico de vínculos por período.
- Novas contas podem participar de ciclos e alterar agregados/denominadores. O gerador de respostas automáticas do demo continua com o elenco original para evitar gerar dados fictícios para novos usuários. Com novos colaboradores ativos, os totais do cenário podem deixar de corresponder às metas exatas da turma original.
- Não foram adicionadas interface administrativa de listagem/edição de todas as pessoas, recuperação de senha, verificação de e-mail ou criação de gerentes, porque isso não integra o CRUD do próprio colaborador solicitado.

## 9. Verificações realizadas aqui

- `javac --release 21` de **todas as classes de produção**: compilação passou usando as bibliotecas do JAR original.
- Smoke test direto, com repositórios simulados: papel `COLABORADOR`, e-mail normalizado, departamento, hash BCrypt, leitura, atualização, troca de senha e exclusão lógica passaram.
- **Não foi possível executar Gradle/JUnit ou PostgreSQL nesta sessão**: o Gradle Wrapper tentou baixar a distribuição e recebeu `UnknownHostException` por indisponibilidade de rede. Por isso o pacote contém instruções e testes para validar no seu IntelliJ; nenhuma aprovação de 108 testes é declarada antecipadamente.
