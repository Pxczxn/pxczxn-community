import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";
import { ThemeBootstrap } from "./components/theme-bootstrap";

export async function generateMetadata(): Promise<Metadata> {
  const requestHeaders = await headers();
  const forwardedHost = requestHeaders.get("x-forwarded-host");
  const requestHost = forwardedHost || requestHeaders.get("host") || "";
  const safeHost = /^[a-zA-Z0-9.-]+(?::\d+)?$/.test(requestHost)
    ? requestHost
    : "localhost:8847";
  const forwardedProto = requestHeaders.get("x-forwarded-proto");
  const protocol = forwardedProto === "http" || forwardedProto === "https"
    ? forwardedProto
    : safeHost.startsWith("localhost") || safeHost.startsWith("127.0.0.1")
      ? "http"
      : "https";
  const origin = `${protocol}://${safeHost}`;
  const description = "发现有价值的内容，与有趣的人一起创作。";
  const shareImage = `${origin}/og.png`;

  return {
    metadataBase: new URL(origin),
    title: {
      default: "星语社区",
      template: "%s · 星语社区",
    },
    description,
    openGraph: {
      type: "website",
      locale: "zh_CN",
      siteName: "星语社区",
      title: "星语社区",
      description,
      images: [{ url: shareImage, width: 1734, height: 909, alt: "星语社区" }],
    },
    twitter: {
      card: "summary_large_image",
      title: "星语社区",
      description,
      images: [shareImage],
    },
  };
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body>
        <ThemeBootstrap />
        {children}
      </body>
    </html>
  );
}
