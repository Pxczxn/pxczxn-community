/**
 * 星语社区 (pxczxn-community V2.1) - 页脚组件 (Footer)
 * 具备高度压缩的紧凑排版，适配 600-700px 视口
 */

import React from 'react';
import { useApp } from '../../context/AppContext';
import { Sparkles, Shield, HelpCircle, Lock } from 'lucide-react';

export const Footer: React.FC = () => {
  const { isCompactViewport, navigateTo } = useApp();

  return (
    <footer className={`bg-white dark:bg-slate-900 border-t border-slate-200/80 dark:border-slate-800 text-slate-500 dark:text-slate-400 text-xs transition-all ${
      isCompactViewport ? 'py-3' : 'py-6'
    }`}>
      <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2 sm:gap-4">

        {/* Left branding */}
        <div className="flex items-center space-x-2">
          <div className="w-5 h-5 rounded-md bg-gradient-to-tr from-indigo-600 to-purple-600 flex items-center justify-center text-white">
            <Sparkles className="w-3 h-3" />
          </div>
          <span className="font-bold text-slate-800 dark:text-slate-200">星语社区</span>
        </div>

        {/* Quick Links */}
        <div className="flex items-center space-x-4 text-[11px]">
          <button onClick={() => navigateTo('/governance')} className="hover:text-indigo-600 dark:hover:text-indigo-400 flex items-center gap-1">
            <Shield className="w-3 h-3" />
            <span>社区规范</span>
          </button>
          <button onClick={() => navigateTo('/settings')} className="hover:text-indigo-600 dark:hover:text-indigo-400 flex items-center gap-1">
            <Lock className="w-3 h-3" />
            <span>隐私策略</span>
          </button>
          <button onClick={() => navigateTo('/governance')} className="hover:text-indigo-600 dark:hover:text-indigo-400 flex items-center gap-1">
            <HelpCircle className="w-3 h-3" />
            <span>申诉与反馈</span>
          </button>
        </div>
      </div>
    </footer>
  );
};
