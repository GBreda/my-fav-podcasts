import Link from "next/link";

export default function NotFound() {
  return (
    <div className="empty">
      <h2>Essa página não existe.</h2>
      <Link className="primary" href="/">Voltar à fila</Link>
    </div>
  );
}
