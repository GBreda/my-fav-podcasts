import { z } from "zod";

export const registerSchema = z.object({
  displayName: z.string().trim().min(2).max(40),
  email: z.string().trim().toLowerCase().email(),
  password: z.string().min(8).max(72),
});

export const loginSchema = z.object({
  email: z.string().trim().toLowerCase().email(),
  password: z.string().min(1).max(72),
});

export const feedSchema = z.object({
  feedUrl: z.string().trim().url(),
});

export const passwordSchema = z.object({
  currentPassword: z.string().min(1).max(72),
  nextPassword: z.string().min(8).max(72),
});

export const forgotSchema = z.object({
  email: z.string().trim().toLowerCase().email(),
});

export const resetSchema = z
  .object({
    password: z.string().min(8).max(72),
    confirm: z.string().min(8).max(72),
  })
  .refine((value) => value.password === value.confirm, { path: ["confirm"] });

export function fieldErrors(error: z.ZodError): Record<string, true> {
  const fields: Record<string, true> = {};
  for (const issue of error.issues) {
    const key = issue.path[0];
    if (typeof key === "string") {
      fields[key] = true;
    }
  }
  return fields;
}
