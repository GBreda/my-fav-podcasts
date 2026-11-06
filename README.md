# Fila

Uma fila só para os podcasts que você acompanha, em ordem de lançamento.

Você segue vários programas. Os episódios que ainda não ouviu entram juntos, do mais recente para o mais antigo (ou o contrário, se preferir colocar o atraso na frente). O player fica na web, pensado primeiro para o celular.

## O que a v1 faz

- Busca podcasts no catálogo da Apple e segue pelo feed RSS
- Monta uma fila única dos episódios não ouvidos
- Toca no navegador, com velocidade, voltar 15s, avançar 30s e “tocar o próximo”
- Guarda a posição e o que já foi ouvido
- Atualiza os feeds na hora e de 30 em 30 minutos

Ainda não há login: é uma biblioteca de uma pessoa, gravada num banco local. Antes de publicar a API na internet, coloque autenticação na frente.

## Como rodar

É preciso Java 21, Maven e Node 20+.

Backend, na pasta `backend`:

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Os dados ficam em `backend/data`.

Frontend, na pasta `frontend`:

```bash
npm install
npm run dev
```

O app abre em `http://localhost:3000` e encaminha `/api` para o backend.

Para seguir um feed direto:

```bash
curl -X POST http://localhost:8080/api/podcasts \
  -H 'Content-Type: application/json' \
  -d '{"feedUrl":"https://exemplo.com/feed.xml"}'
```

## API

| Método | Caminho | Uso |
| --- | --- | --- |
| `GET` | `/api/queue?sort=newest\|oldest` | Fila dos não ouvidos |
| `GET` | `/api/podcasts/search?q=` | Busca no catálogo |
| `POST` | `/api/podcasts` | Seguir (`feedUrl` ou `itunesId`) |
| `GET` | `/api/podcasts` | Biblioteca |
| `GET` | `/api/podcasts/{id}` | Programa e episódios |
| `POST` | `/api/podcasts/refresh` | Atualizar todos os feeds |
| `POST` | `/api/podcasts/{id}/refresh` | Atualizar um feed |
| `DELETE` | `/api/podcasts/{id}` | Deixar de seguir |
| `PATCH` | `/api/episodes/{id}` | Posição e ouvido (`positionSeconds`, `played`) |

## App híbrido

A interface já é um cliente da API, mobile-first: áreas seguras, alvos de toque grandes, manifest para “Adicionar à Tela de Início” e um `frontend/capacitor.config.json`. O próximo passo é embalar essa mesma UI com Capacitor, sem reescrever as telas.

No `frontend`, com o backend acessível por uma URL pública:

```bash
NEXT_PUBLIC_API_URL=https://sua-api.exemplo npm run build:static
npm install @capacitor/core @capacitor/ios @capacitor/android
npx cap add ios
npx cap add android
npx cap sync
```

`build:static` gera a pasta `out`, que é o `webDir` do Capacitor. A URL da API entra no build porque o app empacotado não usa o proxy do Next. O backend já aceita as origens `capacitor://localhost` e `https://localhost`. iOS e Android ainda pedem o Xcode e o Android SDK na máquina de cada um.
