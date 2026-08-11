/**
 * 星语社区 (pxczxn-community V2.1) - 社区自治与规范治理 (Governance View)
 * 包含社区准则、违规举报流程、内容审核机制与透明自治日志
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import { communityApi, type CommunityReport } from '../../../lib/community-api';
import {
  ShieldAlert,
  ShieldCheck,
  FileText,
  AlertCircle,
  CheckCircle2,
  Lock,
  Scale,
  Send,
  Users,
} from 'lucide-react';

export const GovernanceView: React.FC = () => {
  const { isCompactViewport } = useApp();

  const [reportType, setReportType] = useState('SPAM');
  const [reportTargetType, setReportTargetType] = useState<CommunityReport['targetType']>('ARTICLE');
  const [reportTarget, setReportTarget] = useState('');
  const [reportDetail, setReportDetail] = useState('');
  const [submittedReport, setSubmittedReport] = useState(false);

  const handleSubmitReport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reportTarget.trim()) return;
    try {
      await communityApi.createReport({
        targetType: reportTargetType,
        targetId: reportTarget.trim(),
        reasonCode: reportType,
        description: reportDetail.trim() || undefined,
      });
      setSubmittedReport(true);
      setReportTarget('');
      setReportDetail('');
    } catch {
      setSubmittedReport(false);
    }
  };

  return (
    <div className={`max-w-4xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Hero Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs mb-4 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="p-2.5 bg-emerald-50 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-300 rounded-xl">
            <Scale className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-extrabold text-slate-900 dark:text-white">
              星语社区公约与自治治理
            </h1>
            <p className="text-xs text-slate-400">营造专业、包容、尊重知识产权的技术交流生态</p>
          </div>
        </div>
      </div>

      {/* Grid Rules */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5 mb-4 text-xs">
        <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 space-y-2">
          <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
            <span>鼓励的行为 (Encouraged)</span>
          </h2>
          <ul className="list-disc list-inside space-y-1 text-slate-600 dark:text-slate-300">
            <li>深度技术原创与实战演练总结</li>
            <li>明确标注引用与共创作者署名</li>
            <li>友善、建设性的代码 Review 讨论</li>
            <li>组建或加入技术团队共建专栏</li>
          </ul>
        </div>

        <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 space-y-2">
          <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
            <ShieldAlert className="w-4 h-4 text-rose-500" />
            <span>禁止的行为 (Prohibited)</span>
          </h2>
          <ul className="list-disc list-inside space-y-1 text-slate-600 dark:text-slate-300">
            <li>抄袭洗稿与未经授权滥用他人成果</li>
            <li>发布恶意广告、垃圾链接或违规代码</li>
            <li>人身攻击、语言骚扰或撕裂社区氛围</li>
            <li>恶意刷赞、自动化脚本操纵数据</li>
          </ul>
        </div>
      </div>

      {/* Report Form */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs space-y-3 text-xs">
        <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
          <AlertCircle className="w-4 h-4 text-rose-500" />
          <span>违规举报与自治提交 (Submit Violation)</span>
        </h2>

        {submittedReport ? (
          <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl font-bold">
            🎉 举报审核请求已提交至社区治理委员会，我们将于 24 小时内完成复核并通知处理结果。
          </div>
        ) : (
          <form onSubmit={handleSubmitReport} className="space-y-3 max-w-lg">
            <div>
              <label className="block text-slate-500 mb-1">举报对象类型</label>
              <select
                value={reportTargetType}
                onChange={(e) => setReportTargetType(e.target.value as CommunityReport['targetType'])}
                className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
              >
                <option value="ARTICLE">文章</option>
                <option value="MOMENT">动态</option>
                <option value="COMMENT">评论</option>
                <option value="BLOG">博客</option>
                <option value="USER">用户</option>
                <option value="TEAM">团队</option>
                <option value="CHAT">私信</option>
              </select>
            </div>

            <div>
              <label className="block text-slate-500 mb-1">举报类型</label>
              <select
                value={reportType}
                onChange={(e) => setReportType(e.target.value)}
                className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
              >
                <option value="SPAM">抄袭洗稿 / 侵权</option>
                <option value="AD">恶意广告 / 垃圾信息</option>
                <option value="ABUSE">不当言论 / 人身攻击</option>
                <option value="OTHER">其他违规事项</option>
              </select>
            </div>

            <div>
              <label className="block text-slate-500 mb-1">涉嫌违规的内容链接或标题/用户</label>
              <input
                type="text"
                placeholder="例如：文章 ID 或用户 Username..."
                value={reportTarget}
                onChange={(e) => setReportTarget(e.target.value)}
                className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
              />
            </div>

            <div>
              <label className="block text-slate-500 mb-1">详细违规说明</label>
              <textarea
                rows={2}
                placeholder="附上原作者链接或具体违规文字描述..."
                value={reportDetail}
                onChange={(e) => setReportDetail(e.target.value)}
                className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
              />
            </div>

            <button type="submit" disabled={!reportTarget.trim()} className="px-4 py-1.5 bg-rose-600 text-white font-bold rounded-xl disabled:opacity-50">
              提交社区审核
            </button>
          </form>
        )}
      </div>

    </div>
  );
};
