# Fila

Uma fila só para os podcasts que você acompanha. Os episódios dos programas assinados entram juntos, **na ordem de lançamento**. O padrão é do mais recente para o mais antigo; em Ajustes da própria fila dá para inverter e ver também o que já foi ouvido.

O app é mobile first. Nesta v1 ele abre no navegador, com tema escuro, português e inglês, cadastro e um player de áudio.

## Como os podcasts entram

Não existe uma API oficial e gratuita que entregue o áudio de todos os podcasts. O que existe, e é o que os agregadores usam por baixo, é isto:

1. A [iTunes Search API](https://performance-partners.apple.com/search-api) é pública e não pede chave. A busca devolve o nome, a capa e a URL do feed RSS.
2. O RSS do programa traz os episódios e o arquivo de áudio. A Fila lê esse feed, como qualquer podcatcher.

Também dá para colar a URL do feed direto, na busca.

Feeds em `localhost`, rede privada ou outros alvos internos são recusados. O servidor não baixa um endereço que o usuário apontar para a própria máquina.

## Rodar na sua máquina

Pré-requisitos: Java 21 e Node 22.

```bash
cd backend
./mvnw spring-boot:run
```

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:3000`. A API sobe em `http://localhost:8080`. Sem Docker, o banco local é um arquivo H2 em `backend/data`.

Testes:

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

## Docker

O `docker-compose.yml` sobe Postgres, a API, o Next.js e um nginx. A aplicação inteira responde em `http://localhost:8080`.

```bash
cp .env.example .env
docker compose up --build
```

Troque `JWT_SECRET` e `POSTGRES_PASSWORD` antes de publicar. O segredo do JWT precisa ter pelo menos 32 bytes.

## Arquitetura

É um monólito modular: um repositório, um processo Spring Boot e uma interface. Não há microserviços.

O backend separa três contextos:

- **identity** — conta, senha e sessão JWT
- **catalog** — busca na iTunes, leitura de RSS, podcasts e episódios
- **listening** — assinaturas, fila por data de lançamento e progresso do player

A fila não é uma tabela. Ela é calculada: episódios dos podcasts assinados, ordenados pela data de publicação. A regra mora em `ReleaseQueue` e tem teste de unidade. O fluxo de cadastro, assinatura e ordenação tem teste de integração com o Spring.

A interface é Next.js, TypeScript, Tailwind e Zod. O texto da tela está em pt-BR e en-US. O tema escuro é o padrão; também existem claro e sistema.

## O que a v1 já deixa pronto

A tela é mobile first, com área segura, toque grande e `playsInline` no áudio. O manifesto permite instalar como PWA. A URL da API é `NEXT_PUBLIC_API_BASE`: vazia quando o nginx serve tudo na mesma origem, e absoluta quando um app nativo precisar falar com o servidor.

Ainda não há vídeo, download offline, recuperação de senha por e-mail nem publicação nas lojas.

## Publicar a versão web pelo menor custo

A forma que continua saindo de graça para este tipo de app (JVM + Postgres + um processo Node) é uma máquina virtual do **Oracle Cloud Always Free** e o `docker compose` nela. A conta pede um cartão na verificação. Dentro do Always Free, a VM Ampere não gera cobrança. A capacidade ARM às vezes esgota na região; vale tentar São Paulo ou Ashburn.

Se a VM grátis não estiver disponível, o caminho mais simples é um host de container com um Postgres pequeno (Fly.io ou Render). Os dois deixaram de ser “sempre grátis” para esse formato; espere um custo baixo mensal, não zero. A Vercel serve bem o Next.js, mas o Spring e o Postgres precisariam de outro lugar, então o compose numa VM única continua mais barato.

O app não foi pensado para Cloudflare Workers: o backend é Java.

## Publicar no iPhone e no Android

O caminho mais barato, e o que esta v1 já comporta, é um app híbrido com [Capacitor](https://capacitorjs.com/). O WebView abre a mesma interface. Duas variantes:

- O app aponta para a URL publicada. Correção de tela não passa por review da loja.
- O app embute um export estático e chama a API publicada. Aí cada mudança de interface pede uma versão nova.

Passos, quando for a hora:

```bash
cd frontend
npm install @capacitor/core @capacitor/cli @capacitor/ios @capacitor/android
npx cap init Fila app.myfavpodcasts --web-dir=out
```

Para a variante hospedada, `capacitor.config.ts` usa `server.url` com o endereço público. Para a variante embutida, o Next precisa de `output: "export"` e a API continua em `NEXT_PUBLIC_API_BASE`.

Lojas:

- **Android:** conta do Google Play, taxa única de US$ 25. O build é um AAB gerado pelo Android Studio ou por um CI. Dá para testar com o Play Console em faixa interna antes de abrir para todo mundo.
- **iPhone:** Apple Developer Program, US$ 99 por ano. O build precisa de macOS (um Mac ou um runner pago). O TestFlight distribui para testadores. A App Store faz review.
- **Sem loja:** no celular, “Adicionar à Tela de Início” usa o manifesto que já está no app. Não passa por review e não cobra taxa. No iOS o áudio em segundo plano fica mais limitado do que num app nativo.

Áudio em WebView só começa depois de um toque. Os botões de play desta v1 já disparam o `play()` no clique.
