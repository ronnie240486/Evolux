# Arquitetura confirmada do Ouro Pro

## Pacote e tecnologia

O APK analisado é `com.ouropro.player.debug`, versão 6.4, com atividades distintas para celular e TV (`MainActivity` e `MainTVActivity`). O pacote inclui Realm, Glide, Volley/OkHttp, Leanback e um parser M3U próprio (`iptv.m3u.parser`).

## Cache da M3U

`LoadM3UItemsCommand` usa `getExternalFilesDir(null)` e grava a playlist em um arquivo `.m3u` por usuário. A validade é determinada por dois elementos: o arquivo da playlist e um marcador separado `.complete`. A playlist é baixada para `.part`, o temporário é renomeado para o arquivo final e somente depois o marcador `.complete` é criado. Se a operação falhar, o temporário é apagado e o cache anterior não é usado como arquivo parcialmente escrito.

O cache é considerado válido por data armazenada nas preferências e pela presença do marcador completo. Quando válido, o Ouro Pro não baixa a M3U novamente; carrega o arquivo local com `M3UToolSet.load(file.getPath()).getItems()`.

## Separação e persistência

O aplicativo mantém listas separadas de `M3UItem` para canais, vídeos e séries em `LTVApp`. Em seguida, carregadores independentes transformam os registros em `EPGChannel`, `MovieModel` e `EpisodeModel`. O `RealmController` persiste e consulta esses modelos em `MTV.realm`.

As consultas internas são feitas diretamente no Realm por categoria, busca, favoritos e ordenação. Para filmes e séries há consultas separadas por `category_name`/`category_id`; para séries há também consultas separadas por nome e por temporada. A tela não precisa reprocessar a M3U inteira ao trocar de categoria.

## Séries

O Ouro Pro representa séries como `SeriesModel` e episódios como `EpisodeModel`. O agrupamento de episódios por temporada é feito quando necessário, e a categoria/quantidade é consultada no Realm. O card da série usa o modelo da série; imagens de episódios ficam nos modelos de episódio.

## Implicação para o Evolux

O Evolux atual mantém o catálogo inteiro em listas Kotlin observadas pela composição e reprocessa agrupamentos/filtros em telas. A arquitetura equivalente ao Ouro Pro deve persistir os registros lógicos em um banco local, consultar somente a categoria/página atual e manter o cache M3U com marcador de conclusão. O caminho crítico da abertura não deve gravar o catálogo detalhado duas vezes nem passar listas completas por callbacks de UI.

## Confirmação do fluxo de atualização

A rotina `BaseActivity.reloadM3UData` usa uma flag estática `busy` para impedir duas sincronizações simultâneas. Após a playlist ser carregada, `prepareData` separa os `M3UItem` em três listas — canais, vídeos e séries — e os carregadores transformam os itens em modelos próprios. Cada conjunto é persistido no Realm em uma transação, substituindo os registros antigos somente quando a quantidade diverge.

A ordem do Ouro Pro é: buscar/carregar a M3U, separar os itens, persistir canais, construir categorias de canais, persistir filmes, construir categorias de filmes, persistir episódios e construir os modelos de séries. As atividades de Home, Filmes e Séries consultam o Realm por categoria e ordenação; elas não reparseiam a M3U nem agrupam toda a playlist a cada abertura.

A existência do arquivo `.complete` e da data da última M3U é usada para evitar novo download. A atividade de TV é separada da atividade móvel, e os controles são views/foco do Leanback, com botões de Home, TV ao Vivo, Filmes, Séries, configurações, recarregar e sair.

## Decisão para o Evolux

A correção estrutural necessária é substituir o fluxo atual de listas completas observadas pelo Compose por uma camada persistente local com três tabelas/coleções lógicas, contagem e consulta por categoria/página. O parser pode continuar lendo a M3U uma vez na primeira sincronização, mas deve escrever registros em lotes no armazenamento local e nunca copiar o catálogo acumulado para callbacks de UI. As telas devem consultar apenas a página atual e usar o total persistido para exibir 1/N.
