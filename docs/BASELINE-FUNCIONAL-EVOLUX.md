# Baseline funcional do Evolux

Esta versão representa a **base funcional protegida** do aplicativo Evolux para Android TV e celular.

## Estado que deve ser preservado

A base inclui autenticação por MAC lógico, polling de autorização, carregamento de playlists, suporte M3U e Xtream, separação de canais/filmes/séries, cache local, capas oficiais de séries, carregamento de temporadas e episódios sob demanda, player interno Media3/ExoPlayer, navegação por D-pad/toque e comportamento de voltar em camadas.

A fonte Xtream é usada para criar uma entidade por série com capa oficial e categoria. Ao abrir uma série, o aplicativo consulta os episódios e as temporadas sob demanda. Canais e filmes continuam separados da árvore de séries.

## Regra para futuras alterações visuais

Quando um novo modelo de primeira tela for fornecido, a alteração deve ficar limitada à camada visual da Home: composição, ordem dos blocos, espaçamento, tipografia, cores, fundo, logo, ícones, dimensões e aparência dos cards.

Não devem ser alterados sem instrução explícita os repositórios de autenticação, `PlaylistRepository`, `XtreamRepository`, `CatalogoCache`, modelos de catálogo, `PlayerScreen`, chamadas `get_series`, `get_series_info`, regras de classificação ou callbacks de navegação.

A implementação visual deve ser feita em uma branch própria ou commit separado. Antes de cada alteração visual, deve ser possível retornar ao commit/tag deste baseline sem perda da lógica funcional.

## Identificação

- Pacote: `com.evolux.tv`
- Build base: `Evolux-xtream-series-covers-debug.apk`
- Commit funcional: `6d547ce`
- APK SHA-256: `ed1f547438de20e32bf6d54c6ef19e78903e2ebbdb1fdf79dcde4086a4f52b9e`

## Fluxo recomendado

1. Criar uma branch para o novo visual.
2. Alterar somente `HomeScreen`, `MediaRow`, `TopNavBar`, componentes visuais e recursos de identidade.
3. Compilar o APK.
4. Verificar que `PlaylistRepository`, `XtreamRepository`, `CatalogoCache`, `MainActivity` e `PlayerScreen` não sofreram alterações funcionais.
5. Se o visual não for aprovado, retornar à tag do baseline.
