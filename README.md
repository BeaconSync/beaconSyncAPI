# BeaconSync

API da CitizenTech **BeaconSync** (Global Solution, Java Advanced – FIAP).

Em uma enchente, quem não consegue sair de casa sozinho é quem mais corre risco, e é justamente quem as equipes de resgate não sabem onde está. O BeaconSync é um cadastro prévio de pessoas vulneráveis (idosos acamados, pessoas com deficiência, bebês, dependentes de aparelhos médicos) que, no momento do alerta, vira um mapa por distrito de quem precisa ser resgatado primeiro e de onde a pessoa está de verdade.

> **Status:** etapa 3 de 5 (alertas por distrito, mapa de risco e eventos no RabbitMQ). As seções marcadas com _(etapa N)_ serão preenchidas conforme o projeto avança.

## Tecnologias

- Java 25, Spring Boot 4.1, Gradle
- Spring Data JPA + Oracle Database + Flyway
- Spring Security + JWT (HS256)
- RabbitMQ: opção **A** do requisito 5
- Spring AI com OpenAI _(etapa 4)_
- springdoc-openapi (Swagger)
- Docker Compose (Oracle Free 23ai + RabbitMQ)

## Como rodar

### Pré-requisitos

- Java 25 (JDK)
- Docker Desktop (com Docker Compose)

### 1. Configure as variáveis de ambiente

Nenhuma senha está no código. Tudo fica num arquivo `.env`, que **não** vai para o Git.

```bash
cp .env.example .env        # Windows (PowerShell): copy .env.example .env
```

Abra o `.env` e troque as senhas de exemplo. Use só letras e números.

### 2. Suba o banco e o RabbitMQ

```bash
docker compose up -d
```

Na primeira vez, o Oracle leva de 1 a 2 minutos para ficar pronto. Acompanhe com `docker compose ps` até aparecer `healthy`.

### 3. Rode a API

```bash
./gradlew bootRun           # Windows: gradlew.bat bootRun
```

Na subida, o Flyway cria as tabelas e carrega os 5 órgãos e os 96 distritos de São Paulo.

### 4. Teste

- Swagger: http://localhost:8080/swagger-ui.html
- Distritos: http://localhost:8080/api/v1/distritos
- Saúde da API: http://localhost:8080/actuator/health
- Painel do RabbitMQ: http://localhost:15672 (usuário e senha do seu `.env`)

### Alternativa: tudo em Docker (sem instalar Java)

```bash
docker compose --profile app up -d --build
```

### Alternativa: Oracle da FIAP

No `.env`, troque as três linhas do banco pelos seus dados da FIAP:

```properties
DB_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
DB_USERNAME=rmXXXXXX
DB_PASSWORD=sua_senha
```

Depois suba só o RabbitMQ (`docker compose up -d rabbitmq`) e rode a API normalmente. Todas as tabelas usam o prefixo `BS_`, para não colidir com tabelas de outros projetos no mesmo schema.

### Problemas comuns

| Sintoma | Solução |
| --- | --- |
| `Could not resolve placeholder 'DB_PASSWORD'` | O `.env` não existe ou falta a variável. Refaça o passo 1. |
| `ORA-01017: invalid username/password` depois de trocar a senha no `.env` | O usuário do Oracle é criado só na primeira subida. Rode `docker compose down -v` e suba de novo (apaga os dados locais). |
| `Connection refused` na porta 1521 | O Oracle ainda está iniciando. Espere o `healthy` em `docker compose ps`. |

## Segredos e dados sensíveis

- Senhas, chaves e segredo do JWT existem apenas no `.env` (ignorado pelo Git) ou em variáveis de ambiente.
- O `application.properties` só referencia as variáveis (`${DB_PASSWORD}`) e não tem valor padrão para nada sensível. Se faltar alguma, a API não sobe e o erro mostra o nome da variável.
- O `compose.yaml` também lê tudo do `.env`.
- O JWT é assinado com `JWT_SECRET` (mínimo de 32 caracteres; a API não sobe com menos). Não há chave `.pem` no projeto.
- As senhas dos usuários são guardadas só como hash BCrypt, e a dos usuários de teste vem de `SEED_PASSWORD`.
- O CPF sai mascarado nas respostas (`***.982.247-**`).
- `.env.example` é o modelo versionado, só com valores de exemplo.

## Usuários de teste

Criados automaticamente na subida da API, se ainda não existirem. A senha de todos é o valor de `SEED_PASSWORD` no seu `.env` (no `.env.example`: `Teste123`).

| E-mail | Papel | Órgão | Para testar |
| --- | --- | --- | --- |
| responsavel@beaconsync.com | RESPONSAVEL | | Cadastro de pessoas, endereços e vínculos |
| responsavel2@beaconsync.com | RESPONSAVEL | | Pedido de vínculo para uma pessoa já cadastrada |
| voluntario@beaconsync.com | VOLUNTARIO | | Disponibilidade e resgates _(etapa 4)_ |
| bombeira@beaconsync.com | PROFISSIONAL | Corpo de Bombeiros | Ocorrências e resgates _(etapa 4)_ |
| agente@beaconsync.com | AGENTE_ORGAO | Defesa Civil | Alertas, mapa e lista por distrito |
| demo@beaconsync.com | RESPONSAVEL | | Responsável pelas 49 pessoas de demonstração |

Com `SEED_DEMO=true` (padrão), a API também cria 49 pessoas de demonstração ligadas a `demo@beaconsync.com`: 32 na Lapa, 12 na Barra Funda e 5 na Vila Leopoldina. Assim o mapa mostra todas as cores.

## Autenticação (JWT)

1. `POST /api/v1/auth/login` com `{"email": "...", "senha": "..."}`. A senha é conferida com o hash BCrypt.
2. A API devolve um token assinado em HS256 com o segredo `JWT_SECRET`, válido por 2 horas. O token leva `sub` (id do usuário), `role` (papel) e `orgaoId`.
3. O app envia `Authorization: Bearer <token>` em toda chamada. No Swagger, use o botão **Authorize**.
4. O resource server do Spring Security valida assinatura, validade e emissor, e transforma `role` em `ROLE_<PAPEL>`.
5. `@PreAuthorize` checa o papel em cada controller. O `AuthorizationService` checa o dono do dado (ex.: só responsáveis vinculados veem a pessoa).
6. Token ausente, inválido ou sem permissão devolve 401 ou 403 no mesmo formato de erro da API.

### Formato padrão de erro

```json
{
  "timestamp": "2026-11-09T14:03:00",
  "status": 422,
  "erro": "Regra de Negócio",
  "mensagem": "O endereço informado não pertence a esta pessoa.",
  "path": "/api/v1/pessoas/1/endereco-atual"
}
```

Erros de validação (400) trazem a lista `campos` com `campo` e `mensagem`. Conflitos (409) trazem `recursoId`: por exemplo, ao cadastrar um CPF que já existe, vem o id da pessoa para pedir vínculo.

## Endpoints principais _(etapas 2 a 4)_

Listagens paginadas aceitam `?page=0&size=10&sort=nome,asc`.

| Método | Rota | Papel | Descrição |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/registro` | público | Autocadastro de RESPONSAVEL ou VOLUNTARIO |
| POST | `/api/v1/auth/login` | público | Devolve o JWT |
| GET | `/api/v1/usuarios/me` | logado | Perfil do usuário do token |
| GET | `/api/v1/distritos` | público | Lista paginada dos 96 distritos |
| POST | `/api/v1/pessoas` | RESPONSAVEL | Cadastra a pessoa com o primeiro endereço (409 se o CPF já existir) |
| GET | `/api/v1/pessoas` | RESPONSAVEL | Lista paginada das pessoas vinculadas a você |
| GET | `/api/v1/pessoas/{id}` | RESPONSAVEL | Detalhe (só com vínculo ativo) |
| PUT | `/api/v1/pessoas/{id}` | RESPONSAVEL | Atualiza os dados (o CPF não muda) |
| DELETE | `/api/v1/pessoas/{id}` | RESPONSAVEL | Remove o cadastro (revoga o consentimento) |
| GET | `/api/v1/pessoas/{id}/enderecos` | RESPONSAVEL | Lista os endereços |
| POST | `/api/v1/pessoas/{id}/enderecos` | RESPONSAVEL | Adiciona endereço |
| PUT | `/api/v1/pessoas/{id}/endereco-atual` | RESPONSAVEL | Define onde a pessoa está agora |
| DELETE | `/api/v1/pessoas/{id}/enderecos/{enderecoId}` | RESPONSAVEL | Remove endereço (não pode ser o atual) |
| POST | `/api/v1/pessoas/{id}/vinculos` | RESPONSAVEL | Pede vínculo com pessoa já cadastrada |
| GET | `/api/v1/pessoas/{id}/vinculos` | RESPONSAVEL | Responsáveis e pedidos da pessoa |
| GET | `/api/v1/vinculos/pendentes` | RESPONSAVEL | Pedidos que você pode aprovar |
| PATCH | `/api/v1/vinculos/{id}` | RESPONSAVEL | Aprova ou recusa um pedido |
| POST | `/api/v1/alertas` | AGENTE_ORGAO | Emite alerta para uma lista de distritos |
| GET | `/api/v1/alertas` | logado | Lista paginada (filtro `?status=ATIVO`) |
| GET | `/api/v1/alertas/{id}` | logado | Detalhe do alerta |
| PATCH | `/api/v1/alertas/{id}/encerrar` | AGENTE_ORGAO | Encerra o alerta (só o órgão que emitiu) |
| GET | `/api/v1/alertas/{id}/mapa` | logado | Contagem e cor por distrito, sem nomes |
| GET | `/api/v1/alertas/{id}/distritos/{distritoId}/pessoas` | AGENTE_ORGAO | Pessoas do distrito por prioridade (só com alerta ativo) |
| GET | `/api/v1/alertas/ativos/meus` | RESPONSAVEL | Suas pessoas em áreas com alerta ativo |
| POST | `/api/v1/alertas/{id}/respostas` | RESPONSAVEL | SEGURA, PRECISA_AJUDA ou SEM_CONTATO |
| GET | `/api/v1/notificacoes` | logado | Seus avisos (alerta e andamento do resgate) |
| PATCH | `/api/v1/notificacoes/{id}/lida` | logado | Marca o aviso como lido |

### Exemplo: cadastrar uma pessoa

```json
POST /api/v1/pessoas
{
  "nome": "Maria Aparecida Souza",
  "dataNascimento": "1941-03-12",
  "cpf": "52998224725",
  "tipoVulnerabilidade": "DEPENDENTE_APARELHO_MEDICO",
  "necessidadesRemocao": "maca e cilindro de oxigênio",
  "parentesco": "filha",
  "consentimento": true,
  "endereco": {
    "apelido": "Casa da filha",
    "logradouro": "Rua das Flores",
    "numero": "120",
    "cep": "04101-000",
    "distritoId": 92
  }
}
```

O `distritoId` vem de `GET /api/v1/distritos`. Para testar o registro único, faça login com `responsavel2@beaconsync.com` e envie o mesmo CPF: a resposta é 409 com o `recursoId` da pessoa.

## Arquitetura distribuída: opção A (RabbitMQ)

O que é lento ou não precisa ser imediato sai da requisição e vai para filas. O app recebe a resposta na hora, e a triagem e os avisos acontecem segundos depois.

| Evento (routing key) | Publicado em | Fila | O que o consumidor faz |
| --- | --- | --- | --- |
| `alerta.emitido` | `POST /alertas` | `beaconsync.notificacao.q` | Grava um aviso para cada responsável das pessoas na área |
| `resposta.registrada` | `POST /alertas/{id}/respostas` (pedido de ajuda ou sem contato) | `beaconsync.triagem.q` | Faz a triagem, abre a ocorrência e calcula a prioridade sugerida |
| `ocorrencia.status-alterado` | Abertura ou mudança de etapa do resgate | `beaconsync.notificacao.q` | Avisa a família da nova etapa |

- **Exchange:** `beaconsync.events` (topic). As filas são duráveis.
- **Publicação depois do commit:** os services publicam um evento interno, e o `EventoRabbitPublisher` só envia ao RabbitMQ com `@TransactionalEventListener(AFTER_COMMIT)`. O consumidor nunca recebe o id de algo que ainda não foi gravado.
- **Mensagem leva só o id:** o consumidor lê o estado atual no banco, então nunca age sobre dado velho.
- **Idempotência:** a triagem ignora mensagem repetida da mesma resposta e ignora pedido que virou "segura".
- **DLQ:** mensagem que falha no consumidor vai para `beaconsync.triagem.dlq` ou `beaconsync.notificacao.dlq` (via `beaconsync.events.dlx`), visível no painel do RabbitMQ.

### Como testar o fluxo do alerta

1. Login com `agente@beaconsync.com`. Em `GET /api/v1/distritos?size=100`, anote os ids de Lapa, Barra Funda, Vila Leopoldina e Jaguaré (numa base nova: 48, 6, 90 e 40).
2. `POST /api/v1/alertas` com `{"titulo": "Enchente no Tietê", "distritoIds": [...]}`. A resposta mostra `pessoasAfetadas: 49`.
3. `GET /api/v1/alertas/{id}/mapa`: Lapa vermelho, Barra Funda laranja, Vila Leopoldina amarelo, Jaguaré azul.
4. Login com `demo@beaconsync.com`. `GET /api/v1/alertas/ativos/meus` lista as 49 situações. Responda algumas com `PRECISA_AJUDA` e outras com `SEGURA`.
5. Veja no log as linhas `Triagem recebida` e no painel do RabbitMQ (http://localhost:15672) as mensagens passando.
6. Como agente, chame o mapa de novo (a contagem cai com as respostas `SEGURA`) e `GET /alertas/{id}/distritos/{lapaId}/pessoas`: quem pediu ajuda aparece primeiro, com gravidade e resumo da triagem.
7. Como `demo@`, `GET /api/v1/notificacoes` mostra o aviso do alerta e o do pedido de resgate registrado.

### Mapa de risco

A cor do distrito considera só quem ainda não está seguro (pendente, precisa de ajuda ou sem contato) no endereço atual. A contagem é feita a cada consulta, então o distrito muda de cor sozinho. As faixas ficam no `application.properties`:

| Cor | Pessoas sem segurança | Propriedade |
| --- | --- | --- |
| Azul | 0 | |
| Amarelo | 1 a 9 | `beaconsync.mapa.faixa-amarelo=1` |
| Laranja | 10 a 29 | `beaconsync.mapa.faixa-laranja=10` |
| Vermelho | 30 ou mais | `beaconsync.mapa.faixa-vermelho=30` |

A prioridade sugerida de cada pedido soma gravidade (40), tempo em aberto (30, até 120 minutos) e cor do distrito (30). Os pesos também ficam no `application.properties`. O sistema sugere a ordem, mas o agente sempre tem a palavra final.

## Funcionalidade com Spring AI _(etapa 4)_

## Modelagem

14 entidades (15 tabelas, contando a ligação alerta–distrito) no pacote `br.com.fiap.beaconsync`, com o schema versionado pelo Flyway em `src/main/resources/db/migration`.

| Entidade | Tabela | Papel no domínio |
| --- | --- | --- |
| Orgao | BS_ORGAO | Bombeiros, PM, Defesa Civil, SAMU, GCM |
| Usuario | BS_USUARIO | Responsáveis, voluntários, profissionais e agentes |
| Distrito | BS_DISTRITO | Os 96 distritos oficiais de SP |
| PessoaVulneravel | BS_PESSOA_VULNERAVEL | Registro único por CPF |
| Endereco | BS_ENDERECO | Vários por pessoa; um deles é o endereço atual |
| VinculoResponsavel | BS_VINCULO_RESPONSAVEL | Responsável x pessoa (N:N com status) |
| Alerta | BS_ALERTA (+ BS_ALERTA_DISTRITO) | Alerta emitido para uma lista de distritos |
| SituacaoNoAlerta | BS_SITUACAO_ALERTA | Uma linha por (alerta, pessoa), com a triagem |
| Ocorrencia | BS_OCORRENCIA | Pedido de resgate com prioridade |
| HistoricoOcorrencia | BS_HISTORICO_OCORRENCIA | Cada mudança de etapa do resgate |
| Resgatista | BS_RESGATISTA | Perfil profissional ou voluntário |
| ParticipacaoResgate | BS_PARTICIPACAO_RESGATE | Equipe do resgate (N:N com função) |
| Notificacao / AuditLog | BS_NOTIFICACAO / BS_AUDIT_LOG | Avisos para o app e trilha de auditoria |

## Deploy

Não há deploy público até o momento.

## Integrantes

| Nome                             | RM     |
|----------------------------------|--------|
| Felipe Ribeiro Salles de Camargo | 565224 |
| João Pedro Pereira Camilo        | 562005 |
| Lucas Matsubara                  | 565020 |
| Pamella Christiny Chaves Brito   | 565206 |

## Declaração de uso de IA

_Rascunho, revisar antes da entrega:_ o grupo usou o Claude (Anthropic) como apoio no planejamento da arquitetura, na modelagem e na geração de parte do código. Todo o código foi revisado, testado e ajustado pelos integrantes, que respondem pelo conteúdo entregue.