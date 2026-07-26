"use client";

import { useEffect } from "react";

export type ThemeMode = "light" | "dark" | "starry";

export function ThemeBootstrap() {
  useEffect(() => {
    const saved = window.localStorage.getItem("pxczxn-theme") as ThemeMode | null;
    const theme = saved ?? "light";
    document.documentElement.dataset.theme = theme;
  }, []);
  return null;
}

export function setTheme(theme: ThemeMode) {
  document.documentElement.dataset.theme = theme;
  window.localStorage.setItem("pxczxn-theme", theme);
}
