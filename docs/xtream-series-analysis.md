# Análise Xtream da conta real

As consultas foram feitas nos endpoints do mesmo servidor fornecido pelo usuário, com usuário e senha omitidos deste documento:

- `http://onixspeed.shop/player_api.php?...&action=get_series_categories`
- `http://onixspeed.shop/player_api.php?...&action=get_series`

## Categorias retornadas

O endpoint de categorias retornou famílias que não aparecem completas na resposta M3U analisada, incluindo `Series | AMC Plus`, `Series | Brasil Paralelo`, `Series | Claro video`, `Series | Crunchyroll`, `Series | Discovery Plus`, `Series | Disney Plus`, `Series | DirecTV`, `Series | Dorama`, `Series | Funimation Now`, `Series | Globoplay`, `Series | Lionsgate`, `Series | Max`, `Series | Mexicanas`, `Series | Netflix`, `Series | Paramount`, `Series | PlutoTV`, `Series | Reelshort`, `Series | SBT`, `Series | Star Plus`, `Series | Novelas`, `Series | Turcas` e `Series | Outras Produtoras`.

## Modelo de série retornado pelo Xtream

A resposta `get_series` retorna uma entrada por série, não uma entrada por episódio. Os campos observados incluem `series_id`, `name`, `title`, `year`, `stream_type=series`, `cover`, `plot`, `cast`, `director`, `genre`, `release_date`, `rating`, `backdrop_path`, `youtube_trailer`, `episode_run_time`, `category_id` e `category_ids`.

Exemplos observados:

| Série | ID | Categoria | Capa |
|---|---:|---|---|
| `Quem Ama Cuida (2026)` | 40519 | `Series | Novelas` | `image.tmdb.org/.../p97unAJ9n9gpNrICCwEKuZdrb1t.jpg` |
| `Jogada de Risco (2026)` | 32499 | `Series | Globoplay` | `image.tmdb.org/.../iEcju8TBN5jmZswIpQXV5YTJB9L.jpg` |
| `Lucky (2026)` | 32484 | `Series | Apple TV Plus` | `image.tmdb.org/.../xsrkiXg8EuNNtbPtbmvCxg95gK7.jpg` |
| `Ted Lasso` | 8713 | `Series | Apple TV Plus` | `image.tmdb.org/.../5fhZdwP1DVJ0FyVH6vrFdHwpXIn.jpg` |
| `Reacher` | 13627 | `Series | Prime Video` | `image.tmdb.org/.../c9JwFbaBWarL9fwo1NSqsiTj7Zh.jpg` |
| `Os Sete Pecados Capitais (2014)` | 40540 | `Series | Netflix` | `image.tmdb.org/.../lfpXaHTiwV63RzOheV1GFKdpikL.jpg` |

## Conclusão técnica

A M3U fornece episódios com `S01E01` e `tvg-logo`, mas a fonte Xtream fornece a entidade série consolidada com `series_id`, capa oficial, sinopse e categoria, além de categorias adicionais. Para reproduzir o comportamento do OuroPro, o Evolux precisa reconhecer a URL Xtream, carregar `get_series` para os cards de séries e chamar `get_series_info&series_id=...` somente quando o usuário abrir uma série para obter temporadas e episódios. O M3U pode continuar como fallback para streams explícitos.

## Formato confirmado de `get_series_info`

Para `series_id=13627` (`Reacher`), a API retornou `seasons` como uma lista com `season_number`, `episode_count`, `name`, `overview`, `cover` e `cover_big`. O objeto `episodes` veio como um objeto JSON cujas chaves são os números das temporadas (`"1"`, `"2"`, `"3"`, `"4"`), e cada chave contém uma lista de episódios.

Cada episódio possui `id`, `episode_num`, `title`, `container_extension`, `season` e um objeto `info` com `plot`, `movie_image`, `rating`, `season` e `cover_big`. A URL de reprodução Xtream é construída como `/series/{username}/{password}/{id}.{container_extension}`. Esse formato coincide com o parser implementado no Evolux.
