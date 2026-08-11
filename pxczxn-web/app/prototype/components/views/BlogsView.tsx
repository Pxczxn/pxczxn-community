/**
 * 星语社区 (pxczxn-community V2.1) - 专栏博客 Portal (Blogs View)
 */

import React, { useMemo, useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  Globe,
  BookOpen,
  Users,
  Search,
  ExternalLink,
  Plus,
  Compass,
  ArrowRight,
  TrendingUp,
  LayoutGrid,
} from 'lucide-react';

export const BlogsView: React.FC = () => {
  const { articles, teams, navigateTo, isCompactViewport } = useApp();

  const [searchFilter, setSearchFilter] = useState('');

  const blogsList = useMemo(() => {
    const personalBlogs = new Map<string, {
      id: string; slug: string; title: string; avatar: string; description: string;
      articlesCount: number; followersCount: number; isTeam: boolean; tags: string[];
    }>();
    for (const article of articles) {
      const slug = article.author.blogSlug;
      if (!slug) continue;
      const existing = personalBlogs.get(slug);
      if (existing) {
        existing.articlesCount += 1;
        existing.tags = [...new Set([...existing.tags, ...article.tags])].slice(0, 4);
      } else {
        personalBlogs.set(slug, {
          id: `personal-${slug}`,
          slug,
          title: article.author.blogName || `${article.author.displayName} 的专栏`,
          avatar: article.author.avatar,
          description: article.author.bio,
          articlesCount: 1,
          followersCount: article.author.followersCount,
          isTeam: false,
          tags: article.tags.slice(0, 4),
        });
      }
    }
    const teamBlogs = teams.map((team) => ({
      id: team.id,
      slug: team.slug,
      title: team.name,
      avatar: team.avatar,
      description: team.description,
      articlesCount: team.articlesCount,
      followersCount: team.followersCount,
      isTeam: true,
      tags: team.category ? [team.category] : [],
    }));
    return [...personalBlogs.values(), ...teamBlogs];
  }, [articles, teams]);

  const filteredBlogs = blogsList.filter(
    (b) =>
      b.title.toLowerCase().includes(searchFilter.toLowerCase()) ||
      b.description.toLowerCase().includes(searchFilter.toLowerCase()) ||
      b.tags.some((t) => t.toLowerCase().includes(searchFilter.toLowerCase()))
  );

  return (
    <div className={`max-w-7xl mx-auto px-3 sm:px-4 lg:px-6 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Hero Bar */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs mb-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
            <Globe className="w-5 h-5 text-indigo-500" />
            <span>独立博客与技术专栏 Portal</span>
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            汇聚社区开发者与团队的个性化知识专栏，支持独立 Slug 域名绑定与内容订阅
          </p>
        </div>

        <button
          onClick={() => navigateTo('/creator')}
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-3.5 py-1.5 rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center space-x-1 self-start sm:self-center"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>开通我的独立博客</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="mb-4 relative">
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
        <input
          type="text"
          placeholder="搜索博客专栏、标签或创作者..."
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
          className="w-full pl-9 pr-4 py-2 text-xs bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl focus:outline-hidden focus:border-indigo-500"
        />
      </div>

      {/* Grid of Blogs */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
        {filteredBlogs.map((blog) => (
          <div
            key={blog.id}
            className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs hover:border-indigo-300 dark:hover:border-indigo-800 transition-all flex flex-col justify-between space-y-3"
          >
            <div>
              <div className="flex items-start justify-between">
                <div className="flex items-center space-x-3">
                  <img src={blog.avatar} alt="" className="w-10 h-10 rounded-xl object-cover border border-slate-200 dark:border-slate-700" />
                  <div>
                    <div className="flex items-center space-x-1.5">
                      <h2 className="text-xs sm:text-sm font-bold text-slate-900 dark:text-white line-clamp-1">
                        {blog.title}
                      </h2>
                      {blog.isTeam && (
                        <span className="text-[10px] bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 px-1.5 py-0.2 rounded font-bold shrink-0">
                          团队
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-slate-400">@{blog.slug}</p>
                  </div>
                </div>
              </div>

              <p className="text-xs text-slate-600 dark:text-slate-300 mt-2.5 line-clamp-2">
                {blog.description}
              </p>

              <div className="flex flex-wrap gap-1 mt-2.5">
                {blog.tags.map((t) => (
                  <span key={t} className="text-[10px] bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 px-2 py-0.5 rounded-lg">
                    #{t}
                  </span>
                ))}
              </div>
            </div>

            <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs">
              <span className="text-[11px] text-slate-400">
                {blog.articlesCount} 篇文章 · {blog.followersCount} 人订阅
              </span>

              <button
                onClick={() => blog.isTeam ? navigateTo('/teams/:slug', { slug: blog.slug }) : navigateTo('/articles')}
                className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline flex items-center space-x-1"
              >
                <span>访问专栏</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>

    </div>
  );
};
