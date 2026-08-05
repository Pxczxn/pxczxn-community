"use client";

import { useEffect } from "react";
import { communityApi } from "../../lib/community-api";

export default function PersonalCenterPage() {
  useEffect(() => {
    void communityApi.myBlog().then((blog) => {
      window.location.replace(`/${encodeURIComponent(blog.slug)}`);
    }).catch(() => window.location.replace("/login"));
  }, []);

  return null;
}
