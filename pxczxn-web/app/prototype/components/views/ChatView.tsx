/**
 * 星语社区 (pxczxn-community V2.1) - 协同私信与团队频道 (Chat / Messaging View)
 * 实时即时消息、自动回复模拟与多会话切换
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  MessageSquare,
  Send,
  User,
  Users,
  Search,
  CheckCheck,
  Sparkles,
  PhoneCall,
  MoreVertical,
  ChevronLeft,
} from 'lucide-react';

export const ChatView: React.FC = () => {
  const { conversations, activeConversationId, selectConversation, chatMessages, sendChatMessage, user, isCompactViewport } = useApp();

  const [activePeerId, setActivePeerId] = useState<string>(activeConversationId);
  const [inputText, setInputText] = useState<string>('');
  const [showMobileChat, setShowMobileChat] = useState<boolean>(false);

  const activeConv = conversations.find((c) => c.peerUser.id === activePeerId) || conversations[0];
  const messages = chatMessages.filter(
    (m) => (m.senderId === user?.id && m.receiverId === activePeerId) || (m.senderId === activePeerId && m.receiverId === user?.id)
  );

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;
    sendChatMessage(activePeerId, inputText.trim());
    setInputText('');
  };

  const handleSelectPeer = (peerId: string) => {
    setActivePeerId(peerId);
    selectConversation(peerId);
    setShowMobileChat(true);
  };

  return (
    <div className={`max-w-6xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-xs overflow-hidden flex flex-col md:flex-row min-h-[520px]">

        {/* Left Conversation List (100% on mobile when chat not active, 28% on md+) */}
        <div className={`w-full md:w-1/3 xl:w-1/4 border-b md:border-b-0 md:border-r border-slate-200 dark:border-slate-800 p-3 space-y-3 bg-slate-50/50 dark:bg-slate-900/50 shrink-0 ${
          showMobileChat ? 'hidden md:block' : 'block'
        }`}>
          <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
            <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <MessageSquare className="w-4 h-4 text-indigo-500" />
              <span>协同私信与讨论</span>
            </h2>
          </div>

          <div className="space-y-1.5">
            {conversations.map((conv) => (
              <div
                key={conv.id}
                onClick={() => handleSelectPeer(conv.peerUser.id)}
                className={`p-3 rounded-xl transition-all cursor-pointer flex items-center space-x-3 text-xs ${
                  activePeerId === conv.peerUser.id
                    ? 'bg-white dark:bg-slate-800 shadow-xs border border-indigo-200 dark:border-indigo-800'
                    : 'hover:bg-white/60 dark:hover:bg-slate-800/60'
                }`}
              >
                <div className="relative shrink-0">
                  <img src={conv.peerUser.avatar} alt="" className="w-10 h-10 rounded-full object-cover" />
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-white dark:border-slate-900 absolute bottom-0 right-0"></span>
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-slate-900 dark:text-white line-clamp-1">{conv.peerUser.displayName}</h3>
                    <span className="text-[10px] text-slate-400 shrink-0">{conv.lastTime}</span>
                  </div>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-1 mt-0.5">{conv.lastMessage}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Active Chat Panel (100% on mobile when chat active, flex-1 on md+) */}
        <div className={`w-full md:w-2/3 xl:w-3/4 flex-1 flex flex-col justify-between bg-white dark:bg-slate-900 ${
          showMobileChat ? 'block' : 'hidden md:flex'
        }`}>

          {/* Active Peer Header */}
          <div className="p-3.5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-900/50">
            <div className="flex items-center space-x-2 sm:space-x-3">
              {/* Mobile Back Button */}
              <button
                onClick={() => setShowMobileChat(false)}
                className="md:hidden p-1.5 -ml-1 text-indigo-600 dark:text-indigo-400 hover:bg-slate-200 dark:hover:bg-slate-800 rounded-lg flex items-center gap-0.5 text-xs font-semibold cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" />
                <span>列表</span>
              </button>

              <img src={activeConv?.peerUser.avatar} alt="" className="w-8 h-8 rounded-full object-cover shrink-0" />
              <div>
                <h3 className="text-xs sm:text-sm font-bold text-slate-900 dark:text-white">{activeConv?.peerUser.displayName}</h3>
                <p className="text-[10px] text-emerald-500 font-semibold">在线 · 星语 WebSockets 协同</p>
              </div>
            </div>
          </div>

          {/* Messages Feed */}
          <div className="p-3 sm:p-4 space-y-3 overflow-y-auto max-h-[460px] min-h-[320px]">
            {messages.length === 0 ? (
              <div className="text-center text-xs text-slate-400 py-12">
                还没有聊天记录，发送第一条消息开启讨论吧！
              </div>
            ) : (
              messages.map((m) => (
                <div
                  key={m.id}
                  className={`flex flex-col ${m.isSelf ? 'items-end' : 'items-start'}`}
                >
                  <div
                    className={`max-w-[85%] sm:max-w-md p-3 rounded-2xl text-xs leading-relaxed ${
                      m.isSelf
                        ? 'bg-indigo-600 text-white rounded-br-xs shadow-xs'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 rounded-bl-xs'
                    }`}
                  >
                    {m.text}
                  </div>
                  <span className="text-[9px] text-slate-400 mt-0.5 px-1">{m.timestamp}</span>
                </div>
              ))
            )}
          </div>

          {/* Chat Input */}
          <form onSubmit={handleSend} className="p-3 border-t border-slate-200 dark:border-slate-800 flex gap-2 bg-slate-50/50 dark:bg-slate-900/50">
            <input
              type="text"
              placeholder="输入消息内容..."
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              className="flex-1 px-3 py-2 text-xs bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-hidden focus:border-indigo-500"
            />
            <button
              type="submit"
              disabled={!inputText.trim()}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors disabled:opacity-50 flex items-center space-x-1 cursor-pointer"
            >
              <Send className="w-3.5 h-3.5" />
              <span>发送</span>
            </button>
          </form>

        </div>

      </div>

    </div>
  );
};
