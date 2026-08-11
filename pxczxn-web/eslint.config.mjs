import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    // Superseded by the source-prototype route adapters.
    "app/articles/articles-page.tsx",
    "app/home/home-page.tsx",
    "app/search/search-page.tsx",
    "app/submissions/ai-agent/submission-detail-panel.tsx",
    "components/ui/community-ui.tsx",
  ]),
  {
    files: ["app/prototype/**/*.tsx"],
    rules: {
      "@typescript-eslint/no-explicit-any": "off",
    },
  },
]);

export default eslintConfig;
