# BeaconSync

API da CitizenTech **BeaconSync** (Global Solution, Java Advanced – FIAP).

Em uma enchente, quem não consegue sair de casa sozinho é quem mais corre risco, e é justamente quem as equipes de resgate não sabem onde está. O BeaconSync é um cadastro prévio de pessoas vulneráveis (idosos acamados, pessoas com deficiência, bebês, dependentes de aparelhos médicos) que, no momento do alerta, vira um mapa por distrito de quem precisa ser resgatado primeiro e de onde a pessoa está de verdade.

> **Vídeo pitch:** _cole aqui o link do YouTube (não listado)_

## Onde está cada requisito

| Requisito | Onde está no projeto |
| --- | --- |
| 01. API REST do domínio | 14 entidades relacionadas, CRUD com Spring Data JPA e paginação ([Endpoints](#endpoints-principais), [Modelagem](#modelagem)) |
| 02. Validação e erros | Bean Validation nos DTOs e `GlobalExceptionHandler` com respostas padronizadas ([Formato de erro](#formato-padrão-de-erro)) |
| 03. Segurança | Spring Security + JWT com 4 papéis e endpoints protegidos por papel ([Autenticação](#autenticação-jwt)) |
| 04. Spring AI | Triagem do pedido de ajuda e boletim do alerta, integrados ao fluxo do resgate ([Spring AI](#funcionalidade-com-spring-ai)) |
| 05. Arquitetura distribuída | **Opção A: RabbitMQ**, com eventos publicados e consumidos de forma assíncrona ([RabbitMQ](#arquitetura-distribuída-opção-a-rabbitmq)) |
| 06. Documentação e integração | Swagger, este README e a API consumida pelo app em React Native / Expo ([Mobile](#integração-com-o-app-mobile-react-native--expo)) |

## Tecnologias

- Java 25, Spring Boot 4.1, Gradle
- Spring Data JPA + Oracle Database + Flyway
- Spring Security + JWT (HS256)
- RabbitMQ: opção **A** do requisito 5
- Spring AI 2.0 com OpenAI (triagem dos pedidos de ajuda e boletim do órgão)
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

A IA é opcional. Para ligar, use `AI_PROVIDER=openai` e coloque sua chave em `OPENAI_API_KEY`. Com `AI_PROVIDER=none` (padrão), tudo funciona com a triagem por regra e o boletim por modelo de texto.

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
| `OpenAI API key must be set` | `OPENAI_API_KEY` está vazio no `.env`. Sem IA, use `OPENAI_API_KEY=sem-chave`. |
| Log `IA indisponível na triagem` | A chave é inválida ou a OpenAI não respondeu. A triagem segue pela regra base. |

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
| voluntario@beaconsync.com | VOLUNTARIO | | Disponibilidade e participação em resgates |
| bombeira@beaconsync.com | PROFISSIONAL | Corpo de Bombeiros | Participação em resgates e mudança de etapa |
| agente@beaconsync.com | AGENTE_ORGAO | Defesa Civil | Alertas, mapa, boletim, fila, equipe e etapas do resgate |
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

## Endpoints principais

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
| GET | `/api/v1/alertas/{id}/boletim` | AGENTE_ORGAO | Boletim do alerta gerado por IA, com os números brutos |
| GET | `/api/v1/ocorrencias` | AGENTE_ORGAO, PROFISSIONAL | Fila do órgão na ordem de prioridade (`?status=`, `?alertaId=`) |
| GET | `/api/v1/ocorrencias/{id}` | AGENTE_ORGAO, PROFISSIONAL, VOLUNTARIO | Detalhe do resgate (voluntário só se escalado) |
| PATCH | `/api/v1/ocorrencias/{id}/prioridade` | AGENTE_ORGAO | Muda a ordem sugerida, com justificativa auditada |
| POST | `/api/v1/ocorrencias/{id}/equipe` | AGENTE_ORGAO | Escala profissional ou voluntário disponível |
| PATCH | `/api/v1/ocorrencias/{id}/status` | AGENTE_ORGAO, PROFISSIONAL | EM_ANDAMENTO, CONCLUIDA ou CANCELADA |
| GET | `/api/v1/ocorrencias/minhas` | PROFISSIONAL, VOLUNTARIO | Resgates em que você foi escalado |
| GET | `/api/v1/resgatistas/me` | PROFISSIONAL, VOLUNTARIO | Seu perfil de resgatista |
| PATCH | `/api/v1/resgatistas/me/disponibilidade` | PROFISSIONAL, VOLUNTARIO | Informa se está disponível e em qual distrito |
| GET | `/api/v1/resgatistas/disponiveis` | AGENTE_ORGAO | Quem pode ser escalado (`?distritoId=`) |

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

## Funcionalidade com Spring AI

A IA entra em dois pontos do fluxo do resgate, e nenhum deles é um chat solto. Nos dois casos a IA sugere e o agente decide.

### 1. Triagem do pedido de ajuda

Quando a família responde "precisa de ajuda" ou "não consegui contato", ela pode escrever um relato. Exemplo: "a água entrou no quarto, ela usa oxigênio e o cilindro dura umas 4 horas".

1. A resposta publica `resposta.registrada`, e o `TriagemListener` consome a mensagem.
2. O `TriagemService` lê os dados numa transação curta e chama o LLM **fora da transação**, para não segurar conexão do banco durante a IA.
3. O `TriagemIaService` usa o `ChatClient` do Spring AI com **saída estruturada**: `.entity(TriagemResultado, spec -> spec.validateSchema())` devolve gravidade (1 a 5), risco imediato, necessidades e um resumo para a equipe.
4. O resultado é gravado na situação, a ocorrência é aberta e a prioridade sugerida é calculada.

**Cuidados:**
- **Privacidade:** o LLM recebe só tipo de vulnerabilidade, idade, necessidades, situação, tempo desde o alerta e o relato. Nome, CPF e endereço nunca vão para a IA.
- **Segurança da decisão:** a IA pode subir a gravidade da regra base, nunca baixar. Uma pessoa dependente de oxigênio continua com gravidade 5, mesmo que o modelo erre.
- **Injeção de prompt:** o relato vai entre aspas triplas, e o prompt de sistema manda ignorar instruções dentro dele.
- **Fallback:** sem chave, com timeout ou com resposta inválida, vale a regra base (`origemTriagem: REGRA`). Com IA, `origemTriagem: IA`.

### 2. Boletim do alerta

`GET /api/v1/alertas/{id}/boletim` (AGENTE_ORGAO) calcula os números por distrito no banco: sem resposta, pedem ajuda, sem contato, acamados ou dependentes de aparelho, resgates em andamento e concluídos. Só esses números vão para o LLM, que devolve um boletim curto, por exemplo: "Lapa (vermelho): 14 sem resposta, 3 acamadas, 2 resgates em andamento". A resposta traz o texto e os números brutos lado a lado, para o agente conferir. Sem IA, o mesmo boletim sai por um modelo de texto (`origem: MODELO`).

## Resgate

Cada pedido de ajuda vira uma ocorrência do órgão que emitiu o alerta.

- **Fila:** `GET /ocorrencias` ordena pela prioridade manual do agente (1 = primeiro) e depois pelo score sugerido, recalculado na hora (gravidade, tempo em aberto e cor do distrito).
- **Equipe:** cada resgate pode ter vários resgatistas, e o mesmo resgatista pode atuar em vários resgates. A equipe junta profissionais (de qualquer órgão) e voluntários disponíveis, e quem é escalado recebe um aviso.
- **Etapas:** `SOLICITADA → EM_ANDAMENTO → CONCLUIDA`, ou `CANCELADA`. Só inicia com equipe. Cada mudança grava histórico e avisa a família pelo RabbitMQ. `CONCLUIDA` marca a pessoa como resgatada, e o distrito muda de cor no mapa.
- **Privacidade:** o voluntário vê só o resgate em que foi escalado. Toda consulta de detalhe fica no `AuditLog`.

### Como testar o resgate (continuação do fluxo do alerta)

1. Como `voluntario@` e depois como `bombeira@`, `PATCH /api/v1/resgatistas/me/disponibilidade` com `{"disponivel": true, "distritoBaseId": 48}`.
2. Como `agente@`, `GET /api/v1/ocorrencias`: os pedidos de ajuda aparecem na ordem de prioridade, com o resumo da triagem.
3. `GET /api/v1/resgatistas/disponiveis` lista os dois. Use `POST /api/v1/ocorrencias/{id}/equipe` com `{"resgatistaId": ..., "funcao": "condutor do barco"}` para o voluntário e repita para a bombeira.
4. `PATCH /api/v1/ocorrencias/{id}/status` com `{"status": "EM_ANDAMENTO"}` e depois `{"status": "CONCLUIDA"}`.
5. Veja o mapa (a contagem cai), o boletim (`GET /alertas/{id}/boletim`) e, como `demo@`, as notificações de cada etapa.

## Integração com o app Mobile (React Native / Expo)

### Endereço da API no app

| Onde o app roda | URL base |
| --- | --- |
| Celular com Expo Go | `http://IP_DO_COMPUTADOR:8080/api/v1` (o celular e o computador na mesma rede Wi-Fi) |
| Emulador Android | `http://10.0.2.2:8080/api/v1` |
| Expo web (navegador) | `http://localhost:8080/api/v1` |

No celular, `localhost` aponta para o próprio celular, por isso é preciso usar o IP do computador. No Windows, o IP aparece em `ipconfig` (linha "Endereço IPv4"). Se o celular não conectar, libere a porta 8080 no Firewall do Windows. O CORS já vem liberado para o Expo web (`CORS_ALLOWED_ORIGINS`).

### Como o app conversa com a API

```js
const API = "http://192.168.0.10:8080/api/v1";

// Login: guarde o token (ex.: expo-secure-store)
const resposta = await fetch(`${API}/auth/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email, senha }),
});
const { token, usuario } = await resposta.json();

// Demais chamadas: envie o token
const pessoas = await fetch(`${API}/pessoas?page=0&size=10`, {
  headers: { Authorization: `Bearer ${token}` },
}).then((r) => r.json());
// Listas paginadas vêm como { content: [...], page: { number, size, totalElements, totalPages } }
```

- Use `usuario.papel` do login para decidir quais telas mostrar.
- Em erro, mostre o campo `mensagem`. Em erro 400, use também a lista `campos` para marcar os campos inválidos do formulário.
- As notificações são lidas por consulta: durante um alerta, chame `GET /notificacoes` a cada 30 segundos, por exemplo.
- O mapa (`GET /alertas/{id}/mapa`) traz o nome, o código e a cor de cada distrito. O app pinta o GeoJSON dos distritos de São Paulo casando pelo nome.

### Telas e rotas

| Perfil | Tela | Rotas |
| --- | --- | --- |
| Todos | Login e cadastro | `POST /auth/login`, `POST /auth/registro`, `GET /usuarios/me` |
| Todos | Avisos | `GET /notificacoes`, `PATCH /notificacoes/{id}/lida` |
| Responsável | Minhas pessoas e cadastro | `GET /pessoas`, `POST /pessoas`, `PUT /pessoas/{id}`, `GET /distritos` |
| Responsável | Endereços e "onde está agora" | `GET/POST /pessoas/{id}/enderecos`, `PUT /pessoas/{id}/endereco-atual` |
| Responsável | Pedidos de vínculo | `POST /pessoas/{id}/vinculos`, `GET /vinculos/pendentes`, `PATCH /vinculos/{id}` |
| Responsável | Alerta ativo: responder | `GET /alertas/ativos/meus`, `POST /alertas/{id}/respostas` |
| Agente do órgão | Alertas e mapa | `POST /alertas`, `GET /alertas?status=ATIVO`, `GET /alertas/{id}/mapa`, `PATCH /alertas/{id}/encerrar` |
| Agente do órgão | Distrito, fila e boletim | `GET /alertas/{id}/distritos/{d}/pessoas`, `GET /ocorrencias`, `GET /alertas/{id}/boletim` |
| Agente do órgão | Resgate | `GET /ocorrencias/{id}`, `POST /ocorrencias/{id}/equipe`, `PATCH /ocorrencias/{id}/prioridade`, `PATCH /ocorrencias/{id}/status` |
| Voluntário e profissional | Disponibilidade e meus resgates | `PATCH /resgatistas/me/disponibilidade`, `GET /ocorrencias/minhas`, `GET /ocorrencias/{id}` |
| Profissional | Etapa do resgate | `PATCH /ocorrencias/{id}/status` |

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

**No desenvolvimento.** O grupo usou o Claude (Anthropic) como assistente para planejar a arquitetura, sugerir a modelagem e gerar partes do código, da documentação e das mensagens de commit. Todo o conteúdo gerado foi lido, testado e ajustado pelos integrantes, que entendem e respondem pelo que foi entregue.

**No produto.** A API usa o Spring AI com modelos da OpenAI em duas funcionalidades: a triagem dos pedidos de ajuda e o boletim do alerta. Nome, CPF e endereço das pessoas vulneráveis não são enviados ao modelo, e a decisão final é sempre de um agente humano.