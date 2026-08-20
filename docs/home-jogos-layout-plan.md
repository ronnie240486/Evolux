# Plano visual da Home Evolux com Jogos do Dia

A Home terá uma composição em duas colunas no modo TV: à esquerda ficará o destaque principal com imagem, título, sinopse e ações; à direita ficará o painel compacto de Jogos do Dia. Em telas estreitas, o painel será movido para baixo do destaque, sem reduzir o conteúdo a ponto de perder legibilidade.

A barra superior seguirá o padrão visual do Evolux, com ícones Material elegantes para Início, TV ao Vivo, Filmes, Séries, Jogos do Dia, Favoritos e Configurações. O botão de Jogos do Dia continuará abrindo a tela própria; o painel lateral será uma entrada rápida para a mesma área.

O painel lateral terá título dourado, ícone de futebol, cards individuais com escudo e abreviação de cada equipe, horário, campeonato e foco destacado. O botão final abrirá a tela de TV ao Vivo, preservando o comportamento funcional já existente. Quando a API não retornar partidas futuras, o painel mostrará uma mensagem neutra e não criará dados fictícios.

As fileiras inferiores continuarão usando o catálogo real do Evolux: sugestões derivadas dos filmes e séries autorizados, filmes do catálogo e séries do catálogo. Nenhum dado de exemplo será usado no fluxo principal.

A fonte de jogos será TheSportsDB v1, consultada por data. O app filtrará eventos de futebol da data local, descartará partidas encerradas ou adiadas para o painel de próximos jogos e limitará a quantidade exibida para preservar a fluidez da Home. As URLs de escudos serão usadas diretamente pelo carregador de imagens, com fallback visual para o ícone de futebol.

A implementação visual ficará separada da autenticação, parser M3U/Xtream, cache, player, modelos de catálogo e navegação interna protegidos pelo baseline `evolux-baseline-native-xtream-v1`.
