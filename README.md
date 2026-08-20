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
- Login por endereço MAC com detecção opcional do MAC de rede, entrada manual, validação do endpoint remoto e persistência apenas do MAC autorizado.
- Parser tolerante a campos nulos, incluindo `app_name`, e verificação da primeira playlist antes de liberar o acesso.
- Identidade visual futurista do Evolux integrada ao login, ao banner do Android TV e ao ícone do launcher.

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
Na abertura, o Evolux consulta a configuração em `https://renciaapp.manus.space/api/v5/apps/evolux/config?mac=...` somente depois que o MAC é informado ou detectado. O aplicativo exige `registered = true`, `allowed = true` e uma primeira URL HTTP/HTTPS de playlist que responda sem erro, sem HTML e sem `Content-Type: text/html`.

O MAC autorizado é salvo localmente para revalidação na próxima abertura. URLs de playlist não são persistidas, pois podem conter credenciais. Se o backend responder com dados nulos, HTML, HTTP 403 ou playlist vazia, o Evolux mostra `Lista indisponível ou credenciais inválidas` sem travar. Por compatibilidade com fontes legadas, o APK aceita playlist HTTP; para produção, prefira sempre HTTPS.

Os requisitos detalhados estão em [`docs/login-mac-requirements.md`](docs/login-mac-requirements.md).

## Próximas melhorias recomendadas
A próxima etapa natural é substituir os callbacks vazios por um player Media3/ExoPlayer e trocar o estado de demonstração por um `ViewModel` com `StateFlow`. Também vale migrar a persistência de favoritos para DataStore quando o contrato de dados estiver definido.

## Onde plugar conteúdo de verdade
Tudo hoje usa dados de exemplo em `data/SampleData.kt` (com imagens de
placeholder do picsum.photos e `streamUrl` vazia). Para ligar a fontes reais:

1. Crie um `Repository` (ex.: `data/ConteudoRepository.kt`) que busque os
   dados de uma API própria, um backend, ou uma lista M3U/EPG que você
   tenha os direitos de usar.
2. Troque as referências a `SampleData.*` nas telas (`HomeScreen`,
   `LiveTvScreen`, `MainActivity`, etc.) por chamadas ao seu repositório,
   idealmente através de um `ViewModel` com `StateFlow`.
3. Implemente o player de vídeo (recomendo **Media3/ExoPlayer**, que é o
   player oficial do Google para Android/Android TV — dá suporte a HLS,
   DASH, DRM etc.) e chame-o nos callbacks `aoAssistir`, `aoAbrirCanal`,
   `aoAbrirMidia`, que já estão nos lugares certos, só faltando a
   implementação do player em si.

## Aviso importante
Este projeto entrega **somente a interface e a navegação** do app. Ele não
inclui, buscar ou distribuir nenhum conteúdo de vídeo — os campos `streamUrl`
estão vazios de propósito. Ao conectar fontes de filmes, séries, canais e
esportes, garanta que você tem os direitos/licenças necessários para
transmitir esse conteúdo; distribuir streams de canais de TV, filmes ou
jogos sem autorização do detentor dos direitos é ilegal no Brasil e na
maioria dos países.

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
