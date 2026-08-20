# Fontes dos escudos dos clubes

A fonte principal escolhida para os escudos é o TheSportsDB, que oferece uma API pública de esportes com dados de equipes, eventos e artwork. A documentação consultada indica que a API retorna artwork de equipes, incluindo imagens de badges e logos, e que o aplicativo já usa campos de badge para partidas vindas da agenda de futebol.

A fonte secundária de referência é a categoria pública de logos de clubes de futebol no Wikimedia Commons. Ela serve como fallback de pesquisa para clubes que não tenham badge disponível na API; o aplicativo não deve baixar imagens aleatórias de resultados de busca.

Para os jogos da playlist, o aplicativo continuará priorizando os canais reais do grupo `JOGOS DO DIA`. O escudo da API será usado quando o confronto puder ser associado aos nomes dos times; caso contrário, o app exibirá iniciais do clube ou a logo do canal como fallback.

Fontes consultadas:

- TheSportsDB API Documentation: https://www.thesportsdb.com/docs_api_guide
- Wikimedia Commons, Logos of association football clubs: https://commons.wikimedia.org/wiki/Category:Logos_of_association_football_clubs
- Wikimedia Commons, Association football logos: https://commons.wikimedia.org/wiki/Category:Association_football_logos

## Resultado da busca de páginas de clubes

A busca nas páginas da Wikipedia encontrou títulos utilizáveis para alguns clubes: `CR Vasco da Gama`, `SC Corinthians Paulista`, `Botafogo FR`, `Cienciano`, `LDU Quito`, `Mirassol Futebol Clube` e `Santos FC`. Para `Olimpia`, o resultado mais próximo foi `C.D. Olimpia`, que pode ser o clube hondurenho e precisa ser validado antes do uso. A busca por `Rosario Central` não retornou a página oficial diretamente, portanto o aplicativo deve manter o fallback por iniciais até encontrar uma fonte específica confiável.

O teste da agenda TheSportsDB consultada para a data atual retornou partidas de outras ligas, sem os confrontos da playlist `Olimpia x Vasco`, `Corinthians x Rosario Central` e `Botafogo x Cienciano`. Por isso, os escudos da playlist não devem ser associados por posição ou por nome aproximado de outra partida. A estratégia segura é usar badges de páginas de clube confirmadas, ou iniciais, e manter a logo do canal como último fallback.

Consulta de busca Wikipedia API: https://en.wikipedia.org/w/api.php
Documentação TheSportsDB: https://www.thesportsdb.com/docs_api_guide
Wikimedia Commons logos: https://commons.wikimedia.org/wiki/Category:Logos_of_association_football_clubs
