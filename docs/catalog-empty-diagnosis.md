# Diagnóstico do catálogo vazio

## Sintoma

A Home abre com `TV ao Vivo 0`, `Filmes 0` e `Séries 0`.

## Causa observada

A rota de playlist testada durante o diagnóstico respondeu `HTTP 403 Forbidden` com `Content-Type: text/html`, e não com uma playlist M3U. A API Xtream do mesmo host também respondeu `HTTP 403 Forbidden` no ambiente de teste.

## Defeito de apresentação no app

Em `MainActivity.kt`, após a autorização, o app atribuía `PlaylistCatalog(emptyList(), emptyList(), emptyList())` antes de terminar o carregamento em segundo plano. Quando a playlist falhava, o erro era mostrado apenas em Toast e a Home permanecia visível com contadores zerados, mascarando a falha.

## Correção planejada

O catálogo vazio não será mais publicado como catálogo válido. Enquanto a playlist é carregada, a tela exibirá o carregamento; se houver falha, exibirá a causa e um botão para tentar novamente. Um cache só será aceito quando contiver pelo menos um item. O app continuará aceitando URLs `http` e `https`, mas não pode transformar uma resposta externa HTTP 403 em conteúdo válido; nesse caso, será necessário atualizar a URL autorizada no painel/provedor.

Nenhuma credencial de playlist é registrada neste documento.
