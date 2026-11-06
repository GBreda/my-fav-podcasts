import { describe, expect, it } from "vitest";
import { formatClock, formatDurationLabel, formatRelative, greetingKey } from "./format";

describe("formatClock", () => {
  it("renders hours, minutes and seconds", () => {
    expect(formatClock(3723)).toBe("1:02:03");
    expect(formatClock(90)).toBe("1:30");
    expect(formatClock(0)).toBe("0:00");
  });
});

describe("formatDurationLabel", () => {
  it("uses the caller's unit words", () => {
    expect(formatDurationLabel(3723, { hour: "h", minute: "min" })).toBe("1 h 2 min");
    expect(formatDurationLabel(90, { hour: "h", minute: "min" })).toBe("2 min");
    expect(formatDurationLabel(null, { hour: "h", minute: "min" })).toBe("");
  });
});

describe("formatRelative", () => {
  it("uses the active locale", () => {
    const now = Date.parse("2026-11-04T15:00:00Z");
    expect(formatRelative("2026-11-04T13:00:00Z", "pt-BR", now)).toMatch(/hora/i);
    expect(formatRelative("2026-11-04T13:00:00Z", "en-US", now)).toMatch(/hour/i);
  });
});

describe("greetingKey", () => {
  it("splits the day into three greetings", () => {
    expect(greetingKey(8)).toBe("morning");
    expect(greetingKey(13)).toBe("afternoon");
    expect(greetingKey(20)).toBe("evening");
  });
});
