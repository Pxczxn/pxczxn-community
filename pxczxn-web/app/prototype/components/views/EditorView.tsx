/**
 * 星语社区 (pxczxn-community V2.1) - 深度创作编辑器 (Editor Canvas)
 * 支持 Markdown 双栏实时预览、系列关联、共创署名、团队归属与版本快照
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { communityApi } from '../../../lib/community-api';
import {
  FileText,
  Eye,
  Edit3,
  BookOpen,
  Users,
  UserPlus,
  Save,
  Send,
  ArrowLeft,
  Sparkles,
  Layers,
  Clock,
  Check,
} from 'lucide-react';

export const EditorView: React.FC = () => {
  const { seriesList, teams, addArticle, saveArticleDraft, navigateTo, isCompactViewport } = useApp();

  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [content, setContent] = useState(
    `# 架构设计：高并发微服务下的事件驱动模型

## 背景与痛点
在传统的 HTTP 同步 RPC 调用体系中，随着下游业务链路加长，系统面临三大核心瓶颈：
1. 响应延迟级联叠加
2. 下游故障强耦合
3. 无法平滑应对突发流量峰值

## 解决方案
引入 **事件驱动架构 (EDA)**，通过消息中间件实现生产者与消费者的彻底解耦...`
  );

  const [selectedSeriesId, setSelectedSeriesId] = useState('');
  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [tagsText, setTagsText] = useState('系统设计, React 19, 微服务');
  const [coAuthorName, setCoAuthorName] = useState('');
  const [coAuthorsList, setCoAuthorsList] = useState<string[]>([]);
  const [editorMode, setEditorMode] = useState<'SPLIT' | 'EDIT' | 'PREVIEW'>('SPLIT');
  const [saveStatus, setSaveStatus] = useState<'IDLE' | 'SAVED'>('IDLE');
  const [draftState, setDraftState] = useState<{ articleId: string; slug: string; lockVersion: number } | undefined>();

  const handleAddCoAuthor = () => {
    if (coAuthorName.trim() && !coAuthorsList.includes(coAuthorName.trim())) {
      setCoAuthorsList([...coAuthorsList, coAuthorName.trim()]);
      setCoAuthorName('');
    }
  };

  const handlePublish = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    const articleInput = {
      title: title.trim(),
      summary: summary.trim() || content.slice(0, 100),
      content: content.trim(),
      tags: tagsText.split(',').map((t) => t.trim()).filter(Boolean),
    };

    let articleId: string | null;
    if (draftState) {
      try {
        const saved = await communityApi.saveArticle(draftState.articleId, {
          title: articleInput.title,
          slug: draftState.slug,
          summary: articleInput.summary,
          contentMode: 'MARKDOWN',
          markdownContent: articleInput.content,
          visibility: 'PUBLIC',
          publishMethod: 'PLATFORM_REVIEW',
          tagIds: [],
          contentFileIds: [],
          expectedLockVersion: draftState.lockVersion,
        });
        await communityApi.submitReview(saved.articleId, saved.lockVersion);
        articleId = saved.articleId;
      } catch {
        articleId = null;
      }
    } else {
      articleId = await addArticle(articleInput);
    }

    if (articleId) navigateTo('/articles/:id', { id: articleId });
  };

  const handleSaveSnapshot = async () => {
    const savedDraft = await saveArticleDraft({
      title: title.trim(),
      summary: summary.trim() || content.slice(0, 100),
      content: content.trim(),
      tags: tagsText.split(',').map((tag) => tag.trim()).filter(Boolean),
    }, draftState);
    if (!savedDraft) return;
    setDraftState(savedDraft);
    setSaveStatus('SAVED');
  };

  return (
    <div className={`max-w-7xl mx-auto px-3 sm:px-4 lg:px-6 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Editor Top Control Bar */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3 sm:p-4 shadow-xs mb-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center space-x-3">
          <button
            onClick={() => navigateTo('/creator')}
            className="p-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200"
            title="返回创作者中心"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <h1 className="text-xs sm:text-sm font-extrabold text-slate-900 dark:text-white flex items-center gap-1.5">
              <Edit3 className="w-4 h-4 text-indigo-500" />
              <span>星语深度创作工作台</span>
            </h1>
            <p className="text-[10px] text-slate-400 mt-0.5">自动版本快照 · Markdown 双栏实时预览 · 协同共创</p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          {/* View Mode Toggle */}
          <div className="bg-slate-100 dark:bg-slate-800 p-1 rounded-xl flex items-center text-xs font-semibold">
            <button
              onClick={() => setEditorMode('EDIT')}
              className={`px-2.5 py-1 rounded-lg transition-colors ${editorMode === 'EDIT' ? 'bg-white dark:bg-slate-700 text-indigo-600 shadow-xs' : 'text-slate-500'}`}
            >
              编辑
            </button>
            <button
              onClick={() => setEditorMode('SPLIT')}
              className={`hidden md:block px-2.5 py-1 rounded-lg transition-colors ${editorMode === 'SPLIT' ? 'bg-white dark:bg-slate-700 text-indigo-600 shadow-xs' : 'text-slate-500'}`}
            >
              双栏
            </button>
            <button
              onClick={() => setEditorMode('PREVIEW')}
              className={`px-2.5 py-1 rounded-lg transition-colors ${editorMode === 'PREVIEW' ? 'bg-white dark:bg-slate-700 text-indigo-600 shadow-xs' : 'text-slate-500'}`}
            >
              预览
            </button>
          </div>

          <button
            onClick={handleSaveSnapshot}
            className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-medium hover:bg-slate-200 flex items-center space-x-1"
          >
            {saveStatus === 'SAVED' ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Save className="w-3.5 h-3.5" />}
            <span>{saveStatus === 'SAVED' ? '快照已保存' : '存草稿'}</span>
          </button>

          <button
            onClick={handlePublish}
            disabled={!title.trim() || !content.trim()}
            className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors disabled:opacity-50 flex items-center space-x-1"
          >
            <Send className="w-3.5 h-3.5" />
            <span>发布文章</span>
          </button>
        </div>
      </div>

      {/* Editor Body Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">

        {/* Main Canvas (3 Cols) */}
        <div className="lg:col-span-3 space-y-4">

          {/* Article Title & Summary Inputs */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs space-y-3">
            <input
              type="text"
              placeholder="请输入清晰有吸引力的文章标题..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full text-base sm:text-lg font-black bg-transparent border-b border-slate-100 dark:border-slate-800 pb-2 text-slate-900 dark:text-white focus:outline-hidden focus:border-indigo-500"
            />
            <input
              type="text"
              placeholder="一句话摘要/前言 (生成卡片预览时显示)..."
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              className="w-full text-xs text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50 p-2.5 rounded-xl border border-slate-100 dark:border-slate-800 focus:outline-hidden"
            />
          </div>

          {/* Code/Markdown Canvas Area */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs">
            <div className={`grid gap-4 ${editorMode === 'SPLIT' ? 'grid-cols-1 md:grid-cols-2' : 'grid-cols-1'}`}>

              {/* Left Markdown Textarea */}
              {(editorMode === 'EDIT' || editorMode === 'SPLIT') && (
                <div className="space-y-1">
                  <div className="text-[10px] font-mono text-slate-400 uppercase tracking-wider mb-1">Markdown Content</div>
                  <textarea
                    rows={16}
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="开始撰写高质量文章 (支持 Markdown 语法)..."
                    className="w-full p-3 font-mono text-xs bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-slate-800 dark:text-slate-200 focus:outline-hidden focus:border-indigo-500 leading-relaxed resize-y"
                  />
                </div>
              )}

              {/* Right Live Preview */}
              {(editorMode === 'PREVIEW' || editorMode === 'SPLIT') && (
                <div className="space-y-1 border-t md:border-t-0 md:border-l border-slate-100 dark:border-slate-800 pt-3 md:pt-0 md:pl-4">
                  <div className="text-[10px] font-mono text-slate-400 uppercase tracking-wider mb-1">Live Render Preview</div>
                  <div className="p-3 bg-white dark:bg-slate-900 rounded-xl text-xs space-y-2 text-slate-800 dark:text-slate-200 whitespace-pre-wrap font-sans leading-relaxed min-h-[300px]">
                    <h1 className="text-sm font-black border-b pb-1 mb-2">{title || '（文章标题预览）'}</h1>
                    {content}
                  </div>
                </div>
              )}

            </div>
          </div>
        </div>

        {/* Sidebar Settings (1 Col) */}
        <div className="space-y-4">

          {/* Series & Team Metadata Settings */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs space-y-3 text-xs">
            <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <Layers className="w-4 h-4 text-indigo-500" />
              <span>专栏与归属配置</span>
            </h3>

            <div>
              <label className="block text-slate-500 text-[11px] mb-1">归入专栏系列 (Series)</label>
              <select
                value={selectedSeriesId}
                onChange={(e) => setSelectedSeriesId(e.target.value)}
                className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
              >
                <option value="">不归入系列 (独立文章)</option>
                {seriesList.map((s) => (
                  <option key={s.id} value={s.id}>{s.title}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-slate-500 text-[11px] mb-1">关联团队 (Team)</label>
              <select
                value={selectedTeamId}
                onChange={(e) => setSelectedTeamId(e.target.value)}
                className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
              >
                <option value="">个人独立发布</option>
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-slate-500 text-[11px] mb-1">文章分类标签 (逗号分隔)</label>
              <input
                type="text"
                value={tagsText}
                onChange={(e) => setTagsText(e.target.value)}
                className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
              />
            </div>
          </div>

          {/* Co-Authors Attribution */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs space-y-3 text-xs">
            <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <Users className="w-4 h-4 text-purple-500" />
              <span>共创成员署名 (Co-Authors)</span>
            </h3>

            <div className="flex gap-1.5">
              <input
                type="text"
                placeholder="填写共创者 Username..."
                value={coAuthorName}
                onChange={(e) => setCoAuthorName(e.target.value)}
                className="flex-1 p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl text-xs"
              />
              <button onClick={handleAddCoAuthor} className="px-3 py-1.5 bg-purple-600 text-white font-bold rounded-xl text-xs">
                添加
              </button>
            </div>

            <div className="flex flex-wrap gap-1 pt-1">
              {coAuthorsList.map((ca) => (
                <span key={ca} className="text-[10px] bg-purple-50 dark:bg-purple-950 text-purple-700 dark:text-purple-300 px-2 py-0.5 rounded-lg font-semibold">
                  @{ca}
                </span>
              ))}
            </div>
          </div>

        </div>

      </div>

    </div>
  );
};
