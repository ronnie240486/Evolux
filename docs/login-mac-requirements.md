# Requisitos do login MAC do Evolux

## Endpoint de configuração

O aplicativo deve gerar um MAC lógico novo na primeira instalação, exibi-lo na tela de acesso e permitir que o usuário o copie para cadastro no painel. Depois do cadastro, o APK deve consultar por `GET` a rota abaixo usando sempre o mesmo valor persistido:

```text
https://renciaapp.manus.space/api/v5/apps/evolux/config?mac=AA:BB:CC:DD:EE:FF
```

A resposta esperada é JSON. Os campos principais são `registered`, `allowed`, `mac`, `app_id`, `app_name` e `playlist_urls`.

## Regras de segurança e robustez

O APK não pode executar `Enum.valueOf(...)` diretamente com valores vindos do JSON. Se algum enum for adicionado ao contrato, o valor deverá ser validado, normalizado e receber um padrão quando estiver ausente ou for inválido.

A ausência de `name`, `logo`, `banner`, `background` ou ícones não deve travar o aplicativo. A interface deve usar a identidade visual padrão do Evolux quando esses campos estiverem vazios.

A primeira URL válida de `playlist_urls` deve ser escolhida. Respostas HTTP malsucedidas, HTML ou `Content-Type: text/html` não são playlists válidas e devem resultar na mensagem `Lista indisponível ou credenciais inválidas`, sem tentar interpretar o corpo como M3U.

## Fluxo esperado

1. Na primeira instalação, o Evolux gera um MAC lógico aleatório, localmente administrado, no formato `AA:BB:CC:DD:EE:FF`.
2. A tela exibe esse MAC e oferece o botão `COPIAR MAC` para cadastro no painel.
3. O valor permanece salvo nesta instalação; o APK não depende do MAC físico da rede.
4. Depois do cadastro, o usuário aciona `VALIDAR APARELHO` e o Evolux consulta o endpoint de configuração.
5. Se `registered` ou `allowed` forem falsos, o acesso é negado sem travamento.
6. Se a resposta for válida, a primeira playlist válida é carregada somente após validar status HTTP, conteúdo HTML e URL válida.

O MAC `8C:97:31:CD:31:8A` informado no PDF continua sendo o aparelho de teste do endpoint; o APK de distribuição gera um MAC lógico próprio para cada instalação, que deve ser cadastrado no painel antes da validação.

## Observação do endpoint real

A resposta real de teste retornou JSON válido com `registered`, `allowed`, `mac`, `app_id`, `app_name` e `playlist_urls`. Os assets usam os nomes `logo_url`, `banner_url` e `background_url`, por isso o parser aceita esses nomes e também os nomes simplificados do contrato inicial.

As URLs de playlist podem conter credenciais de acesso. Elas não devem ser gravadas em documentação, logs ou commits; o aplicativo apenas as mantém em memória ou no armazenamento local necessário para a sessão autorizada.
