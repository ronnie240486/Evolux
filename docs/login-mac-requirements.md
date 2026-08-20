# Requisitos do login MAC do Evolux

## Endpoint de configuração

O aplicativo deve consultar por `GET` a rota abaixo, substituindo o valor de `mac` pelo endereço informado ou detectado no aparelho:

```text
https://renciaapp.manus.space/api/v5/apps/evolux/config?mac=AA:BB:CC:DD:EE:FF
```

A resposta esperada é JSON. Os campos principais são `registered`, `allowed`, `mac`, `app_id`, `app_name` e `playlist_urls`.

## Regras de segurança e robustez

O APK não pode executar `Enum.valueOf(...)` diretamente com valores vindos do JSON. Se algum enum for adicionado ao contrato, o valor deverá ser validado, normalizado e receber um padrão quando estiver ausente ou for inválido.

A ausência de `name`, `logo`, `banner`, `background` ou ícones não deve travar o aplicativo. A interface deve usar a identidade visual padrão do Evolux quando esses campos estiverem vazios.

A primeira URL válida de `playlist_urls` deve ser escolhida. Respostas HTTP malsucedidas, HTML ou `Content-Type: text/html` não são playlists válidas e devem resultar na mensagem `Lista indisponível ou credenciais inválidas`, sem tentar interpretar o corpo como M3U.

## Fluxo esperado

1. O usuário informa o MAC quando o sistema não conseguir detectá-lo automaticamente.
2. O aplicativo normaliza o valor para o formato `AA:BB:CC:DD:EE:FF`.
3. O Evolux consulta o endpoint de configuração.
4. Se `registered` ou `allowed` forem falsos, o acesso é negado sem travamento.
5. Se a resposta for válida, o MAC é salvo localmente e o aplicativo pode continuar usando a aparência padrão quando os assets vierem vazios.
6. A playlist deverá ser carregada somente após validar status HTTP, conteúdo HTML e URL válida.

O MAC de teste informado no PDF foi `8C:97:31:CD:31:8A`. Ele não deve ser fixado no aplicativo.

## Observação do endpoint real

A resposta real de teste retornou JSON válido com `registered`, `allowed`, `mac`, `app_id`, `app_name` e `playlist_urls`. Os assets usam os nomes `logo_url`, `banner_url` e `background_url`, por isso o parser aceita esses nomes e também os nomes simplificados do contrato inicial.

As URLs de playlist podem conter credenciais de acesso. Elas não devem ser gravadas em documentação, logs ou commits; o aplicativo apenas as mantém em memória ou no armazenamento local necessário para a sessão autorizada.
