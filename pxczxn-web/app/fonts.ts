/**
 * Star Whisper Design System - Font Configuration
 * 
 * Provides optimized Google Fonts with CJK fallbacks for the Stitch design system.
 * Based on Stitch v2.1_15 design language.
 */
import { Hanken_Grotesk, Inter, Source_Serif_4, JetBrains_Mono } from 'next/font/google';

/**
 * Hanken Grotesk - Display font for headings and UI elements
 * Weights: 600 (SemiBold), 700 (Bold), 800 (ExtraBold)
 * Used for: Logo, section titles, navigation
 */
const hankenGrotesk = Hanken_Grotesk({ 
  subsets: ['latin'], 
  weight: ['600', '700', '800'],
  display: 'swap',
  variable: '--font-heading',
});

/**
 * Inter - UI font for body text and interface elements
 * Default weight
 * Used for: Body text, buttons, labels, general UI
 */
const inter = Inter({ 
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-sans',
});

/**
 * Source Serif 4 - Editorial font for article content
 * Weight: 400 (Regular)
 * Used for: Article titles, long-form reading content
 */
const sourceSerif4 = Source_Serif_4({ 
  subsets: ['latin'],
  weight: ['400'],
  display: 'swap',
  variable: '--font-serif',
});

/**
 * JetBrains Mono - Monospace font for code blocks
 * Default weight
 * Used for: Code snippets, technical content
 */
const jetbrainsMono = JetBrains_Mono({ 
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-mono',
});

/**
 * Font configuration object with CSS variable names
 * Import this in layout.tsx and spread into the html element's className
 */
export const swFonts = {
  heading: hankenGrotesk.variable,
  sans: inter.variable,
  serif: sourceSerif4.variable,
  mono: jetbrainsMono.variable,
};

/**
 * CJK fallback font stacks
 * Applied via CSS @layer base rules in globals.css
 */
export const cjkFallbacks = {
  heading: '"Hanken Grotesk", "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif',
  sans: '"Inter", "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif',
  serif: '"Source Serif 4", "Noto Serif SC", "PingFang SC", "Microsoft YaHei", serif',
  mono: '"JetBrains Mono", "Fira Code", "Source Han Mono", monospace',
};
