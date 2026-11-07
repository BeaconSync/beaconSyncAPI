# BeaconSync

API da CitizenTech **BeaconSync** (Global Solution, Java Advanced – FIAP).

Em uma enchente, quem não consegue sair de casa sozinho é quem mais corre risco, e é justamente quem as equipes de resgate não sabem onde está. O BeaconSync é um cadastro prévio de pessoas vulneráveis (idosos acamados, pessoas com deficiência, bebês, dependentes de aparelhos médicos) que, no momento do alerta, vira um mapa por distrito de quem precisa ser resgatado primeiro e de onde a pessoa está de verdade.

> **Status:** etapa 1 de 5 (base do projeto, banco e modelagem). As seções marcadas com _(etapa N)_ serão preenchidas conforme o projeto avança.

## Tecnologias

- Java 17, Spring Boot 4.1, Gradle
- Spring Data JPA + Oracle Database + Flyway
- Spring Security + JWT _(etapa 2)_
- RabbitMQ: opção **A** do requisito 5 _(etapa 3)_
- Spring AI com OpenAI _(etapa 4)_
- springdoc-openapi (Swagger)
- Docker Compose (Oracle Free 23ai + RabbitMQ)

## Como rodar

### Pré-requisitos

- Java 17 (JDK)
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
- O `application.yml` só referencia as variáveis (`${DB_PASSWORD}`) e não tem valor padrão para nada sensível. Se faltar alguma, a API não sobe e o erro mostra o nome da variável.
- O `compose.yaml` também lê tudo do `.env`.
- `.env.example` é o modelo versionado, só com valores de exemplo.

## Usuários de teste _(etapa 2)_

## Endpoints principais _(etapas 2 a 4)_

| Método | Rota | Papel | Descrição |
| --- | --- | --- | --- |
| GET | `/api/v1/distritos` | público (etapa 1) | Lista paginada dos 96 distritos |

## Arquitetura distribuída: opção A (RabbitMQ) _(etapa 3)_

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