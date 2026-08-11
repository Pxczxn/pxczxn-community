/**
 * AppNavbar - Stitch v2.1_15 Design Navigation Component
 * 
 * A pilot implementation of the new Stitch-based navigation bar.
 * Features:
 * - Fixed top positioning with backdrop blur
 * - Logo + Navigation links + Search + Actions layout
 * - Responsive mobile menu
 * - Design tokens integration
 * 
 * Based on Stitch v2.1_15 design language.
 */

"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { 
  Search, 
  Bell, 
  Menu, 
  X, 
  Compass, 
  BookOpen, 
  Users, 
  Sparkles,
  ChevronDown 
} from "lucide-react";

/* ─────────────────────────── Navigation Data ─────────────────────────── */

const NAV_ITEMS = [
  {
    label: "发现",
    href: "/discover",
    icon: Compass,
    accent: "article" as const,
  },
  {
    label: "专栏",
    href: "/articles",
    icon: BookOpen,
    accent: "article" as const,
  },
  {
    label: "系列",
    href: "/series",
    icon: Sparkles,
    accent: "series" as const,
  },
  {
    label: "团队",
    href: "/teams",
    icon: Users,
    accent: "team" as const,
  },
];

const ACCENT_COLORS = {
  article: "var(--sw-article-accent)",
  series: "var(--sw-series-accent)",
  team: "var(--sw-team-accent)",
} as const;

/* ─────────────────────────── Component ─────────────────────────── */

export function AppNavbar() {
  const pathname = usePathname();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);

  const isActive = (href: string) => {
    if (href === "/discover") {
      return pathname === "/" || pathname.startsWith("/discover");
    }
    return pathname.startsWith(href);
  };

  return (
    <>
      {/* ─────────────────────────────────────────────────────────────────
       * Desktop Navigation Bar
       * Fixed top, backdrop blur, height: 64px
       * ───────────────────────────────────────────────────────────────── */}
      <header
        className="fixed top-0 left-0 right-0 z-50 h-16 glass-panel"
        style={{
          background: "var(--sw-surface-container-lowest)",
          borderBottom: "1px solid var(--sw-outline-variant)",
        }}
      >
        <nav className="page-shell h-full flex items-center justify-between gap-4">
          {/* Logo */}
          <Link 
            href="/" 
            className="flex items-center gap-2 sw-focus-ring"
            style={{ textDecoration: "none" }}
          >
            {/* Star Icon */}
            <div
              className="w-9 h-9 rounded-lg flex items-center justify-center"
              style={{ background: "var(--sw-primary)" }}
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
                  fill="var(--sw-on-primary)"
                  stroke="var(--sw-on-primary)"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            {/* Logo Text */}
            <span
              className="sw-heading-md"
              style={{ 
                color: "var(--sw-primary)",
                fontFamily: "var(--font-heading), var(--sw-font-heading-fallback)",
              }}
            >
              星语
            </span>
          </Link>

          {/* Desktop Navigation Links */}
          <div className="hidden md:flex items-center gap-1">
            {NAV_ITEMS.map((item) => {
              const active = isActive(item.href);
              const Icon = item.icon;
              
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className="sw-focus-ring flex items-center gap-1.5 px-4 py-2 rounded-lg transition-all duration-200"
                  style={{
                    textDecoration: "none",
                    color: active ? "var(--sw-primary)" : "var(--sw-on-primary-container)",
                    background: active ? "var(--sw-surface-container)" : "transparent",
                    fontWeight: active ? 600 : 500,
                    fontSize: "var(--sw-text-sm)",
                  }}
                >
                  <Icon 
                    size={16} 
                    style={{ color: active ? ACCENT_COLORS[item.accent] : "inherit" }} 
                  />
                  {item.label}
                </Link>
              );
            })}
          </div>

          {/* Actions */}
          <div className="flex items-center gap-2">
            {/* Search Button */}
            <button
              onClick={() => setIsSearchOpen(!isSearchOpen)}
              className="sw-focus-ring p-2 rounded-lg transition-colors"
              style={{
                background: "transparent",
                color: "var(--sw-on-primary-container)",
              }}
              aria-label="搜索"
            >
              <Search size={20} />
            </button>

            {/* Notifications */}
            <button
              className="sw-focus-ring relative p-2 rounded-lg transition-colors"
              style={{
                background: "transparent",
                color: "var(--sw-on-primary-container)",
              }}
              aria-label="通知"
            >
              <Bell size={20} />
              {/* Notification Badge */}
              <span
                className="absolute top-1 right-1 w-2 h-2 rounded-full"
                style={{ background: "var(--sw-article-accent)" }}
              />
            </button>

            {/* User Avatar */}
            <button
              className="sw-focus-ring flex items-center gap-1 p-1 rounded-lg transition-colors"
              style={{
                background: "transparent",
              }}
              aria-label="用户菜单"
            >
              <div
                className="sw-avatar sw-avatar-sm"
                style={{
                  background: "var(--sw-secondary)",
                  color: "var(--sw-on-secondary)",
                }}
              >
                U
              </div>
              <ChevronDown size={14} style={{ color: "var(--sw-outline)" }} />
            </button>

            {/* Mobile Menu Toggle */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden sw-focus-ring p-2 rounded-lg"
              style={{ color: "var(--sw-primary)" }}
              aria-label="菜单"
            >
              {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </nav>

        {/* Search Bar (Expanded) */}
        {isSearchOpen && (
          <div
            className="absolute top-full left-0 right-0 p-4"
            style={{
              background: "var(--sw-surface-container-lowest)",
              borderBottom: "1px solid var(--sw-outline-variant)",
            }}
          >
            <div className="page-shell">
              <div
                className="sw-input flex items-center gap-2"
                style={{
                  background: "var(--sw-surface-container)",
                  cursor: "pointer",
                }}
              >
                <Search size={18} style={{ color: "var(--sw-outline)" }} />
                <span style={{ color: "var(--sw-outline)" }}>
                  搜索文章、系列、创作者...
                </span>
                <kbd
                  className="ml-auto px-2 py-0.5 rounded text-xs"
                  style={{
                    background: "var(--sw-surface-container-high)",
                    color: "var(--sw-outline)",
                  }}
                >
                  ⌘K
                </kbd>
              </div>
            </div>
          </div>
        )}
      </header>

      {/* ─────────────────────────────────────────────────────────────────
       * Mobile Navigation Menu
       * Slide-down menu for mobile devices
       * ───────────────────────────────────────────────────────────────── */}
      {isMobileMenuOpen && (
        <div
          className="fixed top-16 left-0 right-0 z-40 md:hidden"
          style={{
            background: "var(--sw-surface-container-lowest)",
            borderBottom: "1px solid var(--sw-outline-variant)",
            animation: "sw-fade-in 0.2s ease-out",
          }}
        >
          <nav className="page-shell py-4 flex flex-col gap-1">
            {NAV_ITEMS.map((item) => {
              const active = isActive(item.href);
              const Icon = item.icon;
              
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="sw-focus-ring flex items-center gap-3 px-4 py-3 rounded-lg"
                  style={{
                    textDecoration: "none",
                    color: active ? "var(--sw-primary)" : "var(--sw-on-primary-container)",
                    background: active ? "var(--sw-surface-container)" : "transparent",
                    fontWeight: 500,
                  }}
                >
                  <Icon 
                    size={20} 
                    style={{ color: active ? ACCENT_COLORS[item.accent] : "inherit" }} 
                  />
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>
      )}

      {/* Spacer for fixed header */}
      <div className="h-16" />
    </>
  );
}
