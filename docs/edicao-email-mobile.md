# Edição do e-mail da conta

`PUT /api/v1/usuarios/me` aceita `email` opcional junto a nome, telefone e papel.
Omitir email ou enviar null mantém o valor atual. Enviar vazio ou inválido falha na validação.
O valor é normalizado com trim e letras minúsculas; limite de 160 caracteres.
E-mail ocupado por outra conta retorna 409 antes de iniciar a mudança de papel.
A restrição única do banco continua protegendo contra gravações concorrentes.

O usuário mantém o ID, a senha, a foto e os vínculos. O login passa a usar o novo e-mail.
O JWT identifica a conta por ID (`sub`), mas contém uma claim de e-mail: após confirmar
o salvamento, o mobile deve renovar o token e recarregar o perfil, inclusive quando
somente o e-mail mudar. Se a renovação falhar, orientar novo login sem dizer que o
salvamento falhou. Não enviar senha ou token em logs.

Esta mudança altera o e-mail de login do Java, não edita automaticamente os dados
de contato já armazenados no cadastro de voluntariado .NET. Este usa o ID Java
para associação; seus dados de contato podem ser atualizados na edição do voluntariado.
Não implementa confirmação de propriedade por link de e-mail, que é um fluxo separado.

## Publicação

Após integrar o código, publicar a revisão correta no Render. Conferir em `/v3/api-docs`
se `AtualizarPerfilDTO` contém `papel` e `email`. Código no GitHub sem deploy não altera
a API usada pelo aplicativo.

Validar com uma conta de teste: salvar o novo e-mail, recarregar perfil, renovar a sessão,
sair e entrar com o novo e-mail; tentar um e-mail já ocupado e confirmar 409 sem alterações.
Testes unitários não substituem essa validação com Oracle e o serviço publicado.
