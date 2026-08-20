# Plano de adaptação funcional OuroPro → Evolux

## Objetivo

Manter o visual e a navegação Compose do Evolux, incorporando o comportamento funcional que o OuroPro usa para carregar e organizar conteúdo.

## Ordem de implementação

Primeiro, o parser M3U será corrigido com precedência de classificação por URL, tipo explícito e grupo. As URLs contendo `movie/`, `movies/`, `vod/`, `video/`, `=movie` ou `==movie` serão tratadas como filmes; as contendo `series/` como séries; e apenas o restante será canal quando não houver sinal mais forte. Grupos como `24H`, `ao vivo`, `live` e `canal` não poderão promover um item de filme para a grade de filmes quando a URL indicar live.

Segundo, a camada de catálogo ganhará metadados de origem e helpers reais de categoria, busca e ordenação. Filmes e canais terão busca textual e ordenação por nome, nota, popularidade ou ordem original. As séries continuarão agrupadas por categoria e série, com temporadas e episódios ordenados internamente.

Terceiro, o carregador passará a reconhecer URLs Xtream no formato usado pelo OuroPro. Quando houver `username` e `password` em uma URL `get.php`/Xtream, o Evolux deverá consultar os endpoints separados de categorias e streams, em vez de tentar tratar o endpoint como uma M3U. O carregamento M3U continuará disponível para URLs puras.

Quarto, as preferências locais serão ampliadas para guardar fonte ativa, ordem por seção e categorias ocultas. A troca de MAC continuará sendo o mecanismo de conta e o polling do painel continuará reconhecendo mudanças de lista. O cache deverá ser invalidado pela combinação da URL ativa, modo da fonte e metadados relevantes, evitando recarregar uma lista que não mudou.

## Limite consciente

A busca e a classificação podem ser implementadas imediatamente com os modelos existentes. A obtenção preguiçosa de detalhes Xtream de cada série — chamada `get_series_info` para temporadas e episódios — deve ser adicionada de forma separada para não baixar milhares de respostas durante o login. Enquanto isso, M3U com episódios explícitos continuará funcionando pela hierarquia local já existente.
