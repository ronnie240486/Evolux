# Análise da M3U real fornecida pelo usuário

Fonte analisada: endpoint `http://onixspeed.shop/get.php?...` com usuário e senha omitidos deste relatório.

A resposta contém **40.502 entradas** e **91 grupos**. Nenhuma entrada possui `tvg-type`; portanto, o campo `group-title` e o caminho/extensão da URL são os sinais reais de separação. A resposta contém `#EXTM3U` e `#EXT-X-SESSION-DATA`, seguido por blocos `#EXTINF` e uma URL de stream.

## Totais observados

| Medida | Resultado |
|---|---:|
| Entradas | 40.502 |
| Grupos distintos | 91 |
| Tamanho analisado | 9.958.931 bytes |
| Entradas com caminho `/movie/` | 21.405 (o path sozinho não decide a família) |
| Entradas com caminho `/series/` | 16.336 |
| Entradas sem esses marcadores de caminho | 2.761 |
| `tvg-type` ausente | 40.502 |

## Grupos reais mais importantes

| Grupo | Quantidade | Interpretação baseada nos registros |
|---|---:|---|
| `Series | Dorama` | 13.203 | Séries e episódios, nomes com `S01E01` |
| `Filmes | Legendados¹` | 8.324 | Filmes VOD, URLs `.mp4` |
| `Filmes | Drama¹` | 2.840 | Filmes VOD, URLs `.mp4` |
| `Series | Novelas` | 2.474 | Séries e episódios |
| `Filmes | Comedia¹` | 2.084 | Filmes VOD |
| `Filmes | Acao¹` | 1.917 | Filmes VOD |
| `Filmes | Terror¹` | 1.385 | Filmes VOD |
| `Filmes | Animacao¹` | 897 | Filmes VOD |
| `24/7 FILMES` | 114 | Canais lineares; as URLs terminam em `.ts` |
| `FILMES E SÉRIES` | 105 | Canais lineares; exemplos AMC e AXN terminam em `.ts` |
| `VARIEDADES`, `DOCUMENTÁRIOS`, `BAND`, `GLOBO...`, `ESPN`, `SPORTV` | vários | Canais lineares |

## Regra decisiva

O texto `filme` no nome do grupo **não é suficiente** para classificar VOD. Os grupos `24/7 FILMES` e `FILMES E SÉRIES` são canais ao vivo e devem permanecer em Canais. A separação correta nesta lista é feita primeiro pela família do grupo e pelo formato/path da URL:

- `Series | ...` e URL contendo `/series/` são séries.
- `Filmes | ...` com URL `.mp4`/`/movie/` são filmes.
- `24/7 ...`, `FILMES E SÉRIES` e grupos de emissoras, esportes, rádio, notícias e variedades são canais, mesmo quando o nome do grupo contém `filmes`.
- Os episódios usam nomes explícitos como `Jogada de Risco S01E08`, `Lucky S01E07`, `Reacher S04E04`, `Os Sete Pecados Capitais S05E24` e URLs `.mp4` dentro de `/series/`.

## Correção necessária no Evolux

O parser não pode tratar todo grupo que contém `filme` como VOD. Deve classificar por uma tabela de famílias de grupo com precedência: `Series |` → série; `Filmes |` → filme; grupos `24/7`, `FILMES E SÉRIES`, emissoras e canais → canal. O path da URL funciona como confirmação, não como substituto da família do grupo. A classificação deve preservar exatamente o `group-title` original para as categorias da interface.

A organização de séries deve criar a chave da série removendo apenas o marcador do episódio (`SxxEyy`) e mantendo a categoria `Series | ...`. Depois deve ordenar por temporada e episódio, sem limitar a 50 mil por grupo. A M3U real tem **13.203 episódios somente em `Series | Dorama`**, portanto o limite anterior por categoria explicava parte das séries desaparecidas.

## Conflitos grupo × URL encontrados

Foram encontrados **10 conflitos relevantes** em que o path da URL sugere VOD, mas o grupo original não é `Filmes |`:

| Grupo | Conteúdo | Classificação correta |
|---|---|---|
| `Reels Shorts` | dez entradas com nomes de shorts e conteúdo vertical | Canal/linha especial, não Filme |

A entrada `The Hunt (2026)` possui uma capa TMDB cujo valor contém vírgulas. Depois de corrigir a leitura da primeira vírgula fora das aspas, ela é corretamente lida como `Filmes | Drama¹` e permanece em Filmes.

Não há conflito equivalente para `Series |`: os 16.336 registros desse grupo estão no caminho de séries. Portanto, a regra final é **grupo primeiro; URL somente como confirmação**. Isso evita exatamente o erro de colocar `24/7 FILMES`, `FILMES E SÉRIES` ou `Reels Shorts` dentro de Filmes.

Após corrigir a leitura da primeira vírgula fora das aspas — necessária porque algumas capas TMDB têm vírgulas no valor de `tvg-logo` — os totais de referência pela família do grupo são: **16.336 séries, 21.395 filmes e 2.771 canais**. A soma é 40.502 entradas.
