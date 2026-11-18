# Renovação da sessão no Mobile

O Java já oferece login, refresh com rotação e logout. O mobile precisa chamar essas rotas; a API não renova sozinha um token guardado no celular.

1. POST `/api/v1/auth/login` com e-mail e senha retorna `token`, `tipo`, `expiraEmSegundos`, `usuario`, `refreshToken` e `refreshExpiraEmSegundos`.
2. Guarde o par de tokens em armazenamento seguro do dispositivo. Nunca inclua `JWT_SECRET` no aplicativo.
3. Use o access token atual nas chamadas Java e .NET. Quando expirar ou ocorrer um 401 elegível, execute **uma única renovação compartilhada** pelas requisições concorrentes.
4. POST `/api/v1/auth/refresh` com `{"refreshToken":"<atual>"}`, **sem header Authorization**. Troque e persista os dois tokens recebidos juntos antes de liberar as requisições em espera. Repita cada requisição original no máximo uma vez.
5. Um refresh antigo já usado provoca revogação de toda a sessão. Não dispare vários refresh em paralelo nem repita automaticamente um refresh cujo resultado se perdeu. Se a renovação retornar 401, limpe a sessão e peça login.
6. Logout: POST `/api/v1/auth/logout`, sem Bearer, com o refresh atual; depois limpe a sessão local. Se estiver sem rede, limpe o dispositivo e informe que a revogação remota não foi confirmada.

O prazo padrão do access token é 2 horas; a sessão de renovação dura no máximo 7 dias desde o login. São configuráveis no servidor. Renovar não prolonga esse prazo absoluto. Usuário inativo não renova.

Não renove por 403 (permissão), 422 (regra de negócio) ou 502/503/504 de .NET/IA. Não faça login de novo automaticamente com senha armazenada. Logout revoga o refresh; um access token já emitido continua válido até expirar, inclusive no .NET.
