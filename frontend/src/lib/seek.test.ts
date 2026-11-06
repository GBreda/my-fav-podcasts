import { describe, expect, it } from "vitest";
import { clampSeek } from "./seek";

describe("clampSeek", () => {
  it("moves backward and forward by the requested amount", () => {
    expect(clampSeek(40, -15, 100)).toBe(25);
    expect(clampSeek(40, 15, 100)).toBe(55);
  });

  it("stops at the start of the track", () => {
    expect(clampSeek(10, -15, 100)).toBe(0);
    expect(clampSeek(0, -15, 100)).toBe(0);
  });

  it("stops at the end of the track", () => {
    expect(clampSeek(90, 15, 100)).toBe(100);
    expect(clampSeek(100, 15, 100)).toBe(100);
  });

  it("does not invent an end when duration is still unknown", () => {
    expect(clampSeek(5, 15, 0)).toBe(20);
    expect(clampSeek(5, -15, Number.NaN)).toBe(0);
  });
});
