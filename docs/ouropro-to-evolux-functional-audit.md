# Auditoria funcional: OuroPro para Evolux

## Estado atual do Evolux

O Evolux já autentica por MAC lógico, consulta o endpoint de configuração, faz polling periódico, carrega uma primeira playlist válida e usa cache binário por fingerprint das URLs. O parser aceita M3U e alguns formatos JSON, extrai capas, sinopses, notas e popularidade, e separa itens em canais, filmes e séries.

A classificação atual é baseada em `stream_type`/`tvg-type`/`type`/`kind` quando disponíveis. Sem tipo explícito, ela usa o nome do grupo: grupos com termos de séries viram séries; grupos com termos de filmes/VOD viram filmes; o restante cai em canais. A heurística ainda trata qualquer item ambíguo como canal e não possui um modelo explícito de múltiplas fontes com nome, tipo e posição ativa.

A apresentação atual da home e dos catálogos já possui categorias simples, cards e uma tela hierárquica de séries que agrupa episódios por `serieId`/nome, temporada e episódio. Filmes e canais ainda não têm busca, ordenação persistida, categorias ocultas, retomada de posição ou favoritos separados por seção. O SettingsScreen atual só executa a troca de MAC; as demais opções são estáticas.

## Comportamento observado no OuroPro

O OuroPro mantém uma lista local de playlists deduplicadas vindas do painel, persiste a posição ativa e sincroniza novamente as URLs do painel. Ao selecionar uma URL, identifica o modo antes de carregar: links Xtream com `get.php`, `type=m3u` ou `output=mpegts` seguem para login Xtream; links reconhecidos como XUI seguem para login XUI; os demais são tratados como M3U puro.

No modo Xtream, o OuroPro consulta endpoints separados para categorias e streams live, VOD e séries, além de informações detalhadas de filmes e séries. No modo M3U, ele grava os modelos localmente e consulta por `category_name`. A navegação padrão é categoria → busca/ordenação → grade de itens; para séries, categoria → série → temporadas → episódios. O estado é persistido em SharedPreferences e Realm, incluindo lista ativa, modo M3U, URL/credenciais, datas de atualização, ordenação por seção, categorias ocultas, favoritos, recentes, progresso e formato de stream ao vivo.

As configurações funcionais observadas incluem adicionar/trocar playlist, ocultar categorias de live/VOD/séries, ordenar conteúdo por seção, limpar histórico, formato live TS/M3U8, player externo, atualização automática, formato de hora, legendas, tipo de dispositivo e atualização manual.

## Direção de implementação

O primeiro ciclo deve trazer ao Evolux a arquitetura comportamental, não a aparência do OuroPro: modelo de fontes de playlist, detecção explícita M3U/Xtream, sincronização de múltiplas URLs, seleção de lista ativa, categorias por seção, busca e ordenação locais, categorias ocultas, cache por fonte e hierarquia de séries. O visual Compose/Evolux será mantido.

A integração Xtream precisa ser adicionada separadamente do parser M3U, pois o OuroPro não tenta interpretar todos os endpoints Xtream como uma lista M3U única. Para não guardar credenciais no APK ou no repositório, os dados devem permanecer em memória/cache local protegido apenas pelo mecanismo já usado para a configuração autorizada, sem registrar tokens no código.

## Regras específicas extraídas do OuroPro

Na entrada M3U, o OuroPro usa também a URL como sinal de tipo. Ele classifica como filme quando o stream contém `movie/`, `movies/`, `vod/`, `video/`, `=movie` ou `==movie`; classifica como série quando contém `series/`; e trata o restante como canal. Essa regra vem antes de abrir o item, e deve ser combinada com `stream_type`, `group-title` e `category` no Evolux para evitar que canais de filmes sejam exibidos na seção Filmes.

Para o catálogo, o OuroPro cria categorias reais e ainda acrescenta categorias especiais de recentes, todos e favoritos. A lista de cada seção é consultada por categoria, texto de busca e posição de ordenação. As categorias ocultas são filtradas antes de montar os adapters. Em séries, o catálogo de episódios é agrupado por série e depois transformado em temporadas; a posição da temporada e do episódio é preservada para retomada.

O período de atualização do OuroPro compara a hora atual com a última data de playlist salva e só refaz a rede quando o período expirou, mantendo o Realm local no restante do tempo. O Evolux já possui fingerprint de URLs, mas precisa evoluir para fingerprint da fonte ativa, modo de playlist e metadados retornados pelo painel, além de guardar preferências de categoria/ordem/ocultação.
