# Contrato dos ajustes Mobile

Todas as rotas abaixo usam a base Java e `Authorization: Bearer <token>`, exceto login, registro, refresh e logout. O mobile também mantém uma base separada para .NET.

## Conta e foto

| Método | Rota | Corpo/resposta |
|---|---|---|
| GET | `/api/v1/usuarios/me` | Dados atuais da conta, incluindo `fotoUrl` ou null |
| PUT | `/api/v1/usuarios/me` | `{"nome":"João","telefone":"11999999999"}`; devolve a conta atualizada |
| PUT | `/api/v1/usuarios/me/foto` | multipart/form-data, campo `arquivo`; devolve conta com `fotoUrl` |
| GET | `/api/v1/usuarios/me/foto` | JPEG privado; requer Bearer, sem cache |
| DELETE | `/api/v1/usuarios/me/foto` | 204; repetição também é aceita |

PUT do perfil exige os dois campos. Nome não pode ser vazio (máximo 120); telefone exige DDD e 10/11 dígitos. E-mail, papel, órgão e ID não são editáveis por esse formulário. O mobile deve usar a resposta ou consultar `/me` novamente; o nome dentro de um JWT antigo só muda no próximo token emitido.

A foto aceita JPEG/PNG até 2 MiB e 12 milhões de pixels. O servidor valida os bytes, reduz o lado maior para até 1024 pixels e grava JPEG novo sem EXIF. No mobile, normalize orientação/converta HEIC para JPEG e reduza o tamanho antes do envio. Ao usar FormData, deixe o cliente HTTP preencher o Content-Type e o boundary.

`fotoUrl` é relativa à base Java, por exemplo `/api/v1/usuarios/me/foto?v=<versao>`. Envie o Bearer também ao carregar a imagem; a rota sempre representa a conta do token. Não é uma URL pública para compartilhar. Sem foto: 404; arquivo inválido: 400; grande: 413; sem token: 401.

## Voluntariado e disponibilidade

O comportamento depende de `NET_INTEGRACAO` no Java. Com `false` (padrão), a disponibilidade do voluntário fica só no Java: o app usa o PATCH abaixo e nada é consultado no .NET. Com `true`, valem as regras desta seção.

- Login/conta: Java; cadastro de voluntariado: .NET `/api/v1/voluntarios/me`, com o mesmo token Java.
- Use uma conta Java com papel VOLUNTARIO para as rotas de resgatista/IA do voluntário. Cadastrar o perfil no .NET não troca o papel Java.
- GET Java `/api/v1/resgatistas/me` consulta o .NET para preencher `disponivel`; `habilidades` e `distritoBaseId` continuam no Java.
- PATCH Java `/api/v1/resgatistas/me/disponibilidade` recebe `{"disponivel":true,"distritoBaseId":48,"habilidades":"Transporte"}`. Para voluntários encaminha `disponivelParaAjuda` ao .NET; para profissionais persiste localmente.
- O mobile pode alterar diretamente no .NET. O Java consultará esse valor no próximo GET/listagem/escala. Não precisa enviar a mesma alteração às duas APIs.
- Cadastro .NET ausente: disponibilidade lida como false e PATCH Java retorna 422 pedindo o cadastro. Falha de comunicação, resposta inválida ou erro de configuração: 503. Nenhum valor antigo é usado como substituto.
- A criação/edição de outros campos do voluntariado continua diretamente no .NET. Nome/telefone da conta e contato do voluntariado são campos distintos; não há sincronização automática desses contatos.

## Mapa e análise

GET `/api/v1/alertas/{id}/mapa` mantém o contrato existente. POST `/api/v1/alertas/{alertaId}/distritos/{distritoId}/analise` aceita AGENTE_ORGAO ou VOLUNTARIO e corpo `{"pergunta":"Qual é a situação deste distrito?"}` (5–500 caracteres).

O Java consulta as contagens do distrito pertencente ao alerta. O mobile não envia contagens nem a chave Python. A resposta mantém os campos do contrato Python (`id_analise`, `analise`, `boletim_dados`, `status`, `modo` etc.). Os dados pessoais das pessoas do distrito continuam restritos a AGENTE_ORGAO; o voluntário não ganha acesso a essa rota.

401/403 são da autenticação/autorização Java. Erros do Python são traduzidos para 502/503/504; não devem disparar renovação do token do usuário. O mapa deve continuar utilizável quando só a análise falhar.
