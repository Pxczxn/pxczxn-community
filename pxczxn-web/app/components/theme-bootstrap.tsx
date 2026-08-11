"use client";

import { useEffect } from "react";

export type ThemeMode = "light" | "dark" | "starry";
export const THEME_EVENT = "pxczxn-theme-change";

export function ThemeBootstrap() {
  useEffect(() => {
    const saved = window.localStorage.getItem("pxczxn-theme") as ThemeMode | null;
    const theme = saved ?? "light";
    const root = document.documentElement;
    root.dataset.theme = theme;
    root.classList.remove("light", "dark", "starlight");
    root.classList.add(theme === "starry" ? "starlight" : theme);
    if (theme !== "light") root.classList.add("dark");
  }, []);
  return null;
}

export function setTheme(theme: ThemeMode) {
  const root = document.documentElement;
  root.dataset.theme = theme;
  root.classList.remove("light", "dark", "starlight");
  root.classList.add(theme === "starry" ? "starlight" : theme);
  if (theme !== "light") root.classList.add("dark");
  window.localStorage.setItem("pxczxn-theme", theme);
  window.dispatchEvent(new CustomEvent<ThemeMode>(THEME_EVENT, { detail: theme }));
}
