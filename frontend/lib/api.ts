export class ApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ApiError";
  }
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const base = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/$/, "");
  let response: Response;
  try {
    response = await fetch(`${base}${path}`, {
      ...init,
      headers: {
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...init?.headers,
      },
    });
  } catch {
    throw new ApiError("Não consegui falar com o servidor.");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  let body: { message?: string } | null = null;
  const text = await response.text();
  if (text) {
    try {
      body = JSON.parse(text) as { message?: string };
    } catch {
      body = null;
    }
  }

  if (!response.ok) {
    throw new ApiError(body?.message || "Algo deu errado.");
  }

  return body as T;
}
