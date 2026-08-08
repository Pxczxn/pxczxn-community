import type { Metadata } from "next";
import { MySeriesPage } from "./my-series-page";

export const metadata: Metadata = {
  title: "我的系列",
  description: "把个人博客的文章编排成连载系列，并管理审核与公开状态。",
};

export default function Page() {
  return <MySeriesPage />;
}
