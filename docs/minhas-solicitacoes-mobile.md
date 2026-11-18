# Minhas solicitações de vínculo

GET `/api/v1/vinculos/minhas-solicitacoes?page=0&size=50`, autenticado com JWT Java.
Disponível para RESPONSAVEL e VOLUNTARIO. A conta vem exclusivamente da sessão.
`page` deve ser >= 0; `size`, entre 1 e 100. Resposta paginada Spring Data.

Cada item contém `id` (vínculo), `parentesco`, `status` (PENDENTE, ATIVO ou RECUSADO),
`criadaEm` e `decididaEm` (pode ser null). ATIVO corresponde a aprovado no mobile.
Não expõe nome, documento, endereço ou identificador da pessoa. Mensagem não é
armazenada pelo fluxo atual e não está presente no retorno.

A consulta usa os eventos existentes SOLICITAR_VINCULO para distinguir pedidos de
vínculos automáticos do cadastro de pessoas. Retorna o pedido mais recente por
vínculo ainda existente, ordenado pela data decrescente e pelo ID do vínculo.
Reenvios após recusa substituem a tentativa anterior nesta visão. Não é um arquivo
completo de todas as tentativas. Vínculos excluídos e registros antigos sem evento
de solicitação não aparecem. Datas de decisão ausentes não são inventadas.
Nenhuma migração ou alteração nos fluxos de escrita é necessária.

## Validação

11 testes direcionados passaram: isolamento por conta, exclusão de vínculos
automáticos, decisões, reenvios, paginação, validação e permissões. A consulta nativa
foi executada em H2; a execução em Oracle e no Render precisa ser validada após o
deploy. H2 é dependência exclusiva de testes.

Após merge e deploy: conta A cadastra uma pessoa; conta B solicita vínculo;
B consulta e vê pendente; A aprova; B atualiza e vê aprovado. Testar também recusa
e conta sem solicitações. Cadastro de pessoa por si só não deve gerar item.

Este recurso pertence ao Java. Cadastro detalhado de voluntariado e disponibilidade
continuam no .NET. O mobile usa o mesmo JWT Java para as duas APIs.
