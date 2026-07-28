import type { MetadataRoute } from "next"; import { siteUrl } from "./lib/seo";
export default function robots(): MetadataRoute.Robots { return { rules:{userAgent:"*",allow:"/",disallow:["/api/","/editor","/workspace","/me","/settings","/login"]},sitemap:`${siteUrl()}/sitemap.xml`}; }
