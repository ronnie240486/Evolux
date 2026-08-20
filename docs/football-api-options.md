# Opções para Jogos do Dia

## TheSportsDB

A documentação oficial informa uma API v1 gratuita com a chave pública `123`, base `https://www.thesportsdb.com/api/v1/json/123`, endpoints de agenda e dados de partidas. A API retorna imagens de eventos e escudos/logos de equipes. A documentação informa limite gratuito de aproximadamente 30 requisições por minuto. A agenda pode ser consultada por data e a camada de imagens oferece tamanhos menores para cards.

Fonte: https://www.thesportsdb.com/docs_api_guide

## Football-Data.org

A documentação oficial descreve uma API REST de partidas agendadas, com filtro de data e campos de competição e equipes. A autenticação usa o cabeçalho `X-Auth-Token`, portanto exige cadastro de uma chave e não é ideal para embutir diretamente em um APK distribuído. A cobertura também varia por competição e plano.

Fonte: https://www.football-data.org/documentation/api

## Escolha para o primeiro painel

A primeira implementação deve usar TheSportsDB v1 como fonte opcional, porque permite consultar partidas do dia sem colocar uma chave privada no APK. O aplicativo deve armazenar somente os dados retornados em memória por um período curto, não inventar jogos e exibir estado vazio quando a API não responder. A arquitetura deve manter um provedor abstrato para permitir trocar por Football-Data.org ou por uma API própria do painel no futuro.
