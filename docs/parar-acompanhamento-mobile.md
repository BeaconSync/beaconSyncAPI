# Parar de acompanhar

`DELETE /api/v1/pessoas/{pessoaId}/vinculos/me` com JWT Java de RESPONSAVEL ou
VOLUNTARIO. A conta removida vem da sessão; não há parâmetro de usuário alvo.

- 204: vínculo removido ou já ausente. Também retorna 204 para pessoa ausente,
  evitando revelar a existência do cadastro a quem não tem vínculo.
- 409: último vínculo ativo; aprovar outro responsável antes de sair.
- 403: vínculo pendente/recusado ou papel sem permissão.
- 400: identificador não positivo (ou formato inválido).

Remove somente a linha do próprio vínculo. Pessoa, endereços, outros vínculos e
registros de auditoria permanecem. Registra REMOVER_PROPRIO_VINCULO. O usuário
perde acesso às consultas protegidas por vínculo ativo; pode solicitar novo vínculo
pelo fluxo existente. Não confundir com DELETE /pessoas/{id}, que exclui o cadastro.
A consulta Minhas solicitações lista vínculos existentes: o vínculo removido deixa
de aparecer nela, mas os eventos de auditoria continuam registrados.

A transação bloqueia a linha da pessoa antes de consultar o vínculo e contar os
ativos. Duas saídas simultâneas pela nova operação são serializadas. Solicitações
pendentes não contam como responsáveis ativos. Nenhuma migração necessária.

Validação: 10 testes direcionados passaram (6 regras do serviço, 1 integração H2
com duas transações concorrentes e repetição, 3 permissões). Oracle e fluxo real no
celular devem ser validados após merge/deploy. Mobile deve exibir mensagem do 409,
confirmar antes de remover e atualizar Pessoas após sucesso.
