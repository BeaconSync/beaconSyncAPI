# Integração Java, .NET e IA

## Tokens do aplicativo

Java emite HS256 com `sub` numérico positivo, `iss`, `iat`, `exp`, `jti`, `role`, nome e e-mail. A API .NET valida a mesma chave e emissor. O contrato de acesso do mobile não emite `aud`: mantenha a validação de audience desabilitada no .NET, como na main revisada.

O mobile recebe apenas access/refresh tokens e nunca a chave de assinatura. A renovação é descrita em `RENOVACAO_AUTOMATICA.md`.

## Disponibilidade: consulta ao .NET

A consulta só acontece com `NET_INTEGRACAO=true`. Com `false` (padrão), a disponibilidade dos voluntários fica no Java e nenhuma chamada ao .NET é feita: é o modo do roteiro do README e do vídeo de Java. Com `true`, o `JWT_SECRET` e o `JWT_ISSUER` do Java precisam ser os mesmos do .NET publicado, e cada voluntário precisa ter se cadastrado antes em `/api/v1/voluntarios/me` no .NET. Trocar o valor exige reiniciar a API. Profissionais mantêm a disponibilidade no Java nos dois modos.

O Java já é o emissor confiável dos JWTs. Para consultar o perfil de um candidato à equipe sem depender do token guardado no celular, ele emite internamente um JWT de 60 segundos com o subject do usuário consultado, audience `beaconsync-net` e purpose `voluntariado-interno`. Não inclui `role`; o `TokenClaimsValidator` Java rejeita esse token nas rotas Java.

Essa credencial só é usada por `VoluntariadoClient`, nos caminhos fixos GET `/api/v1/voluntarios/me` e PATCH `/api/v1/voluntarios/me/disponibilidade`. Nunca é retornada em DTO, armazenada no banco ou entregue ao mobile. O GET é usado para consultas/escala autorizadas; o PATCH é usado para a alteração do próprio perfil autenticado.

O .NET atual valida assinatura, emissor, expiração e subject, mas **não restringe operações pelo claim purpose ou audience**. Portanto, o token interno não é uma credencial criptograficamente limitada a leitura: deve permanecer exclusivamente entre os servidores. Essa solução preserva o contrato existente sem habilitar o CRUD legado. Uma futura separação de confiança pode adicionar endpoints/policies específicos de serviço no .NET.

O cliente valida a identidade e o emissor da resposta, usa HTTPS (HTTP apenas localhost), não segue redirects e aplica timeouts. 404 significa perfil ausente; os demais erros impedem confirmar disponibilidade. Nenhum cadastro .NET é criado silenciosamente.

Não existe cache persistente nem tarefa de sincronização: listar candidatos consulta o .NET por voluntário e escalar consulta novamente. Se o .NET não responder na listagem, o voluntário fica fora da lista e os profissionais continuam aparecendo; na escala, a resposta é 503. Com a integração ligada, o Java não grava a disponibilidade do voluntário: ao desligar, volta a valer o último valor gravado no Java. Para bases grandes, adote consulta em lote/paginação com endpoint de serviço no .NET. A disponibilidade pode mudar depois da consulta; a escala não é uma reserva distribuída. O PATCH pode concluir remotamente antes de uma falha local; ao receber erro, o mobile deve consultar o estado. Reenviar o mesmo booleano é uma operação idempotente.

## IA

Somente AGENTE_ORGAO e VOLUNTARIO podem pedir a análise agregada. Java coleta contagens; Python recebe esses números, IDs do alerta/distrito, nome do distrito, horário da consulta e pergunta. Não são enviados nomes/CPFs/endereços dos cidadãos. Não inclua dados pessoais no texto livre da pergunta.

`IA_SERVICE_API_KEY` vai no header `X-API-Key` da chamada Java → Python. Ela não é a chave do Gemini nem o segredo JWT. A resposta é conferida contra os IDs, contagens e horário enviados. Erro 401/403 do Python vira 502 no Java, para não confundir o mobile com expiração da sessão.

A conexão real com Python e .NET depende das variáveis descritas no README e no `.env.example`.

## Checklist para a demonstração integrada (NET_INTEGRACAO=true)

1. `JWT_SECRET` e `JWT_ISSUER` iguais aos do .NET publicado, só nas variáveis do serviço ou no `.env` de quem apresenta.
2. `NET_READ_TIMEOUT=30s` se o .NET estiver no Render gratuito, e abrir o `/actuator/health` do Java e uma rota do .NET alguns minutos antes, para acordar os serviços.
3. Cadastrar o `voluntario@beaconsync.com` em `/api/v1/voluntarios/me` no .NET usando o token do Java.
4. Alternar a disponibilidade no .NET e conferir em `GET /api/v1/resgatistas/me` no Java; depois escalar o voluntário como agente.
5. Com o .NET fora do ar, a escala do voluntário responde 503 sem usar valor antigo.
