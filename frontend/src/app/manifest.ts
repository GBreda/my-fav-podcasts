import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Fila — My Fav Podcasts",
    short_name: "Fila",
    description: "Uma fila só para os podcasts que você acompanha.",
    start_url: "/pt-BR/queue",
    scope: "/",
    display: "standalone",
    background_color: "#100e0c",
    theme_color: "#100e0c",
    lang: "pt-BR",
    icons: [
      {
        src: "/icon.svg",
        sizes: "any",
        type: "image/svg+xml",
        purpose: "any",
      },
    ],
  };
}
