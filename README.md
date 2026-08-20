# Evolux — App Android TV (Kotlin nativo)

Projeto Android TV nativo usando **Jetpack Compose for TV** (`androidx.tv`), no
mesmo estilo visual do print enviado: fundo azul-marinho, detalhes dourados,
barra de navegação no topo e telas: **Início, TV ao Vivo, Filmes, Séries,
Jogos do Dia, Favoritos e Configurações**.

## Como abrir
1. Abra a pasta `Evolux` no **Android Studio** (versão Koala/2024.1+).
2. Deixe o Gradle sincronizar (baixa as dependências do Compose for TV e Coil).
3. Rode em um emulador de Android TV ou em uma TV Box com depuração USB/ADB
   (`adb connect <ip_da_tv_box>`).

Para gerar o APK Debug pelo GitHub, abra a aba **Actions**, execute o workflow **Build Evolux APK** e baixe o artefato `evolux-debug-apk`. O APK também pode ser compilado localmente pelo Android Studio com **Build > Build APK(s)**.

## O que já funciona
- Navegação completa entre as 7 telas pelo D-pad (setas + OK), com destaque
  dourado no item focado — igual ao "INÍCIO" destacado no print.
- **Banner de destaque que troca sozinho pela lista** (`FeaturedBanner.kt`):
  ele recebe `SampleData.destaques` e a cada 8 segundos (configurável)
  avança para o próximo item da lista, com transição suave e bolinhas de
  posição. Para o filme mudar, **basta adicionar/editar itens na lista**
  `SampleData.destaques` — não precisa mexer na lógica do componente.
- Grades e fileiras de filmes/séries com foco visual (borda dourada + zoom
  leve no item focado), painel de "Jogos do Dia" e grade de canais ao vivo.
- Tema de cores centralizado em `ui/theme/Theme.kt`.
- Favoritos funcionais nas fileiras e grades: o usuário pode adicionar ou remover títulos pelo D-pad, e a seleção é persistida localmente entre sessões.
- Permissão de internet declarada no manifesto para carregar as imagens HTTPS usadas pelos dados de demonstração.
- Login por MAC lógico: o app gera um valor novo na primeira instalação, exibe-o, permite copiá-lo para cadastro no painel e o mantém persistido durante a instalação.
- Parser tolerante a campos nulos, incluindo `app_name`, e verificação da primeira playlist antes de liberar o acesso.
- Identidade visual futurista do Evolux integrada ao login, ao banner do Android TV e ao ícone do launcher.
- Tela de login responsiva para celular, com rolagem vertical, MAC visível e botões empilhados para `COPIAR MAC` e `VALIDAR APARELHO`.
- Diagnóstico seguro na tela: o usuário pode ver HTTP 403, HTML, JSON inválido, timeout, lista vazia ou falha de rede sem que URLs com credenciais sejam exibidas.
- Redesign responsivo com fundo futurista do Evolux, navegação horizontal rolável no celular, grades adaptativas e cards com feedback de foco dourado.
- Cards, linhas de catálogo, canais e destaques agora possuem ações reais: quando há stream configurado, o app abre o player interno Media3; quando não há URL, informa o motivo em vez de parecer um botão quebrado.
- O catálogo de canais, filmes, séries e sugestões é derivado exclusivamente da playlist autorizada pelo MAC. A Home não usa mais itens fictícios.

## Acessibilidade (TalkBack + D-pad)
- **Foco inicial automático**: ao abrir o app, a aba selecionada na
  `TopNavBar` já recebe foco sozinha (`FocusRequester` + `LaunchedEffect`),
  então o D-pad funciona desde o primeiro frame — sem isso, numa TV, nada
  responde até o usuário "acordar" o foco.
- **Um card = um anúncio**: cards de filme/série, canais e itens de
  configuração usam `Modifier.semantics(mergeDescendants = true)` com uma
  `contentDescription` só, juntando título + estado (ex.: "Interestelar,
  60 por cento assistido") em vez do TalkBack ler a imagem, o texto e a
  barra de progresso como três coisas separadas.
- **Imagens decorativas silenciadas**: pôsteres, escudos de time e o logo
  do app têm `contentDescription = null` (ou `clearAndSetSemantics {}`)
  porque o texto ao lado já descreve o conteúdo — evita leitura duplicada.
- **Abas com estado**: cada item da barra superior usa `role = Role.Tab`
  e `selected = ...`, então o TalkBack anuncia "Início, aba, selecionada".
- **Banner de destaque com `liveRegion`**: como ele troca de item sozinho
  (a cada 8s), o texto do card usa `liveRegion = LiveRegionMode.Polite`
  para o TalkBack avisar a troca automaticamente. Se achar muito
  falante, é só remover essa linha em `FeaturedBanner.kt`.
- **Contraste**: a paleta atual (dourado/branco sobre `#0A0E1A`) já passa
  em WCAG AA/AAA para texto — inclusive o cinza secundário (`TextoCinza`)
  dá ~7.6:1 de contraste. Não precisa trocar cores.

## Identidade visual
Os assets finais estão em `assets/evolux_logo_futurista_final.png` e `assets/evolux_app_icon_futurista_final.png`. As cópias usadas pelo APK ficam em `app/src/main/res/drawable/evolux_logo.png` e `app/src/main/res/drawable/evolux_icon.png`.

## Login por MAC
Na primeira abertura, o Evolux gera um MAC lógico novo no formato `AA:BB:CC:DD:EE:FF`, mostra o valor na tela e oferece `COPIAR MAC`. Depois de cadastrar esse MAC no painel, use `VALIDAR APARELHO`; então o app consulta `https://renciaapp.manus.space/api/v5/apps/evolux/config?mac=...`, exigindo `registered = true`, `allowed = true` e uma primeira URL HTTP/HTTPS de playlist que responda sem erro, sem HTML e sem `Content-Type: text/html`.

O MAC lógico é salvo localmente e permanece igual até a desinstalação do APK. URLs de playlist não são persistidas, pois podem conter credenciais. Se o backend responder com dados nulos, HTML, HTTP 403 ou playlist vazia, o Evolux mostra `Lista indisponível ou credenciais inválidas` e um detalhe seguro do motivo, sem travar. Por compatibilidade com fontes legadas, o APK aceita playlist HTTP; para produção, prefira sempre HTTPS.

Os requisitos detalhados estão em [`docs/login-mac-requirements.md`](docs/login-mac-requirements.md).

## Proteção contra playlists grandes
O APK não usa mais `readText()` para carregar a playlist inteira. M3U é processada linha a linha; respostas JSON têm limite de 8 MB e o catálogo suporta até 100.000 itens totais, com até 50.000 por categoria como proteção contra respostas malformadas. A playlist normal do usuário passa inteira sem o corte artificial de 10.000 filmes. O campo `group-title` é preservado em cada item e aparece como filtro real em Filmes, Séries e Favoritos.

## Próximas melhorias recomendadas
O fluxo real já está ligado ao catálogo autorizado e ao player interno Media3. As próximas melhorias naturais são migrar o estado de catálogo e favoritos para um `ViewModel` com `StateFlow`, adicionar EPG quando o backend fornecer os dados e incluir suporte a múltiplas playlists autorizadas.

## Fonte de conteúdo
O aplicativo não cria nem distribui conteúdo. A fonte deve ser uma playlist M3U/JSON autorizada pelo backend após o cadastro do MAC lógico. Canais, filmes, séries e sugestões usam os itens recebidos dessa playlist e preservam seus respectivos `streamUrl`, títulos, grupos e imagens.

## Aviso importante
O aplicativo reproduz somente as URLs recebidas da playlist autorizada do aparelho. Ele não cria, hospeda ou distribui conteúdo próprio. Garanta que você possui os direitos e as licenças necessários para transmitir as fontes conectadas; distribuir streams sem autorização do detentor dos direitos pode ser ilegal.

## Estrutura
```
app/src/main/java/com/evolux/tv/
├── MainActivity.kt              # ponto de entrada + navegação entre telas
├── data/
│   ├── Models.kt                 # Destaque, Midia, Canal, Jogo
│   └── SampleData.kt             # dados de exemplo (troque pela fonte real)
├── ui/theme/Theme.kt             # paleta dourado/marinho
├── ui/components/
│   ├── TopNavBar.kt               # barra de navegação superior
│   ├── FeaturedBanner.kt          # banner que roda pela lista de destaques
│   ├── MediaRow.kt                # fileira horizontal de cards
│   └── PainelJogos.kt             # painel "Jogos do Dia"
└── ui/screens/
    ├── HomeScreen.kt
    ├── LiveTvScreen.kt
    ├── GradeMidiaScreen.kt        # usada por Filmes, Séries e Favoritos
    ├── GamesScreen.kt
    └── SettingsScreen.kt
```

## Fluxo de navegação e sugestões

A navegação do Evolux mantém o visual próprio do aplicativo, mas segue o fluxo de referência do Maximus Player: o botão Voltar fecha o player antes de sair da categoria; em uma categoria, retorna à Home; somente na Home o Back pode encerrar a atividade. As categorias reais do `group-title` do M3U são preservadas nos itens e exibidas como filtros nas telas de Filmes, Séries e Favoritos.

A Home escolhe os cards de sugestão a partir do catálogo autorizado. Quando a fonte fornece `rating`, `vote_average`, `popularity`, `vote_count`, `views` ou `view_count`, esses campos são usados para priorizar o conteúdo em alta; sem esses metadados, o aplicativo não inventa ranking e exibe somente itens reais com stream e imagem válidos. A reprodução acontece no Media3/ExoPlayer interno do Evolux.

A análise estática do APK de referência está registrada em `reference_maximus/reference-findings.md` no ambiente de desenvolvimento; o projeto não depende de código proprietário do APK analisado.
