import { describe, expect, it } from "vitest";
import { feedSchema, loginSchema, registerSchema } from "./schemas";

describe("registerSchema", () => {
  it("accepts a trimmed account", () => {
    const parsed = registerSchema.safeParse({
      displayName: "  Ana  ",
      email: "  Ana@Example.com ",
      password: "senha-forte",
    });
    expect(parsed.success).toBe(true);
    if (parsed.success) {
      expect(parsed.data.displayName).toBe("Ana");
      expect(parsed.data.email).toBe("ana@example.com");
    }
  });

  it("rejects a short name and a short password", () => {
    const parsed = registerSchema.safeParse({
      displayName: "A",
      email: "ana@example.com",
      password: "curta",
    });
    expect(parsed.success).toBe(false);
  });
});

describe("loginSchema", () => {
  it("rejects an empty password", () => {
    expect(loginSchema.safeParse({ email: "ana@example.com", password: "" }).success).toBe(false);
  });
});

describe("feedSchema", () => {
  it("accepts an https feed and rejects a bare word", () => {
    expect(feedSchema.safeParse({ feedUrl: "https://example.com/feed.xml" }).success).toBe(true);
    expect(feedSchema.safeParse({ feedUrl: "nerdcast" }).success).toBe(false);
  });
});
