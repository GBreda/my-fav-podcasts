import { Roboto } from "next/font/google";

const roboto = Roboto({ subsets: ["latin"], weight: ["400", "700"] });

export default function GlobalNotFound() {
  return (
    <html lang="pt-BR">
      <body className={roboto.className} style={{ background: "#100e0c", color: "#f6f1e8" }}>
        <main style={{ maxWidth: 420, margin: "20vh auto", padding: 24 }}>
          <p>Fila</p>
          <h1>Essa página não existe.</h1>
          <a href="/pt-BR" style={{ color: "#a83264" }}>
            Voltar
          </a>
        </main>
      </body>
    </html>
  );
}
