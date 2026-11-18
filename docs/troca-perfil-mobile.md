# Perfil e acompanhamento no mobile

`RESPONSAVEL` e `VOLUNTARIO` podem cadastrar e acompanhar pessoas, gerenciar endereços e vínculos e responder aos alertas. As verificações de vínculo ativo, autorização e associação ao alerta continuam obrigatórias. Ser voluntário não dá acesso aos dados de todas as pessoas.

## Editar o próprio perfil

O endpoint existente `PUT /api/v1/usuarios/me` aceita o campo opcional `papel`:

```json
{
  "nome": "Nome da pessoa",
  "telefone": "11999999999",
  "papel": "RESPONSAVEL"
}
```

- Omitir `papel` preserva o papel atual e mantém compatibilidade com o mobile existente.
- Somente contas `RESPONSAVEL`/`VOLUNTARIO` podem alternar entre esses dois valores.
- Não é possível obter ou remover papéis institucionais por esse campo.
- A resposta `UsuarioResponseDTO` contém o papel atualizado.
- Pessoas acompanhadas, vínculos e históricos são preservados.
- Ao entrar no voluntariado pela primeira vez, o perfil de resgatista é criado indisponível. O cadastro complementar do voluntariado continua no .NET.
- Ao sair ou retornar ao voluntariado, um cadastro existente fica indisponível; o usuário deve ativar a disponibilidade novamente.
- A mudança para responsável é recusada enquanto houver participação em ocorrência SOLICITADA ou EM_ANDAMENTO.
- Com `NET_INTEGRACAO=true`, a disponibilidade do cadastro existente também é pausada no .NET. Falha da integração impede confirmar a troca. O cadastro não é excluído.

## Ajuste ainda necessário na interface mobile

1. Em Editar perfil, oferecer Responsável e Voluntário somente para essas contas.
2. Enviar a seleção no PUT acima; aguardar a confirmação do servidor antes de alterar o perfil local.
3. Após uma troca confirmada, usar `POST /api/v1/auth/refresh` com o refresh token da sessão e armazenar os novos tokens. Se a renovação falhar, solicitar novo login; a troca de perfil já foi salva.
4. Recarregar o perfil e atualizar a navegação/entrada do voluntariado.

No Java, permissões usam o papel atual do banco em cada autenticação, mantendo a validação da assinatura e validade do JWT. Tokens antigos não conservam o papel anterior no Java. O .NET possui sua própria validação: o mobile deve renovar o token antes de chamá-lo. Revogação imediata de tokens antigos também no .NET exigiria alinhamento adicional nessa API; este PR não altera o .NET.

A pausa no .NET e a transação Oracle não são uma transação distribuída: se o .NET confirmar a pausa e o commit Oracle falhar, o usuário pode continuar voluntário, porém indisponível. Consultar o perfil antes de repetir a operação.

## Validação

Com JDK 25 e as dependências do projeto disponíveis:

```bash
./gradlew test --tests '*UsuarioServiceTest' --tests '*AutoridadesUsuarioAtualTest' --tests '*PermissoesAcompanhamentoTest' --tests '*DisponibilidadeServiceTest'
```

Esses testes não precisam de Oracle, RabbitMQ, Render nem credenciais reais. O teste de contexto completo já existente no projeto depende da configuração dos serviços externos.
