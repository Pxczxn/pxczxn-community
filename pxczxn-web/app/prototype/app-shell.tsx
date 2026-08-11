"use client";

/**
 * 星语社区 (pxczxn-community V2.1) - 主入口 (App Main Component)
 * 具备 600-700px 视口优化、全局响应式导航、三阶主题样式与 15+ 视图流切换
 */

import { motion, AnimatePresence } from 'motion/react';
import { AppProvider, useApp } from './context/AppContext';
import { Navbar } from './components/layout/Navbar';
import { BottomNav } from './components/layout/BottomNav';
import { Footer } from './components/layout/Footer';

// Views
import { HomeView } from './components/views/HomeView';
import { DiscoverView } from './components/views/DiscoverView';
import { ArticlesView } from './components/views/ArticlesView';
import { ArticleDetailView } from './components/views/ArticleDetailView';
import { MomentsView } from './components/views/MomentsView';
import { SeriesView } from './components/views/SeriesView';
import { SeriesDetailView } from './components/views/SeriesDetailView';
import { BlogsView } from './components/views/BlogsView';
import { TeamsView } from './components/views/TeamsView';
import { TeamDetailView } from './components/views/TeamDetailView';
import { TeamWorkspaceView } from './components/views/TeamWorkspaceView';
import { ProfileView } from './components/views/ProfileView';
import { PersonalSpaceView } from './components/views/PersonalSpaceView';
import { CreatorStudioView } from './components/views/CreatorStudioView';
import { EditorView } from './components/views/EditorView';
import { NotificationsView } from './components/views/NotificationsView';
import { ChatView } from './components/views/ChatView';
import { GovernanceView } from './components/views/GovernanceView';
import { SearchView } from './components/views/SearchView';
import { SettingsView } from './components/views/SettingsView';

function AppContent() {
  const { currentRoute } = useApp();

  const renderCurrentView = () => {
    switch (currentRoute) {
      case '/':
        return <HomeView />;
      case '/discover':
        return <DiscoverView />;
      case '/articles':
        return <ArticlesView />;
      case '/articles/:id':
        return <ArticleDetailView />;
      case '/moments':
      case '/moments/:id':
        return <MomentsView />;
      case '/series':
        return <SeriesView />;
      case '/series/:id':
        return <SeriesDetailView />;
      case '/blogs':
        return <BlogsView />;
      case '/teams':
        return <TeamsView />;
      case '/teams/:slug':
        return <TeamDetailView />;
      case '/teams/:slug/workspace':
        return <TeamWorkspaceView />;
      case '/profile':
      case '/profile/:username':
      case '/profile/:slug':
      case '/users/:username':
        return <ProfileView />;
      case '/me':
      case '/space':
        return <PersonalSpaceView />;
      case '/creator':
        return <CreatorStudioView />;
      case '/editor/new':
      case '/editor/:id':
        return <EditorView />;
      case '/notifications':
        return <NotificationsView />;
      case '/chat':
        return <ChatView />;
      case '/governance':
        return <GovernanceView />;
      case '/search':
        return <SearchView />;
      case '/settings':
        return <SettingsView />;
      default:
        return <HomeView />;
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans antialiased selection:bg-indigo-500 selection:text-white transition-colors duration-200">
      <Navbar />
      <main className="flex-1 w-full relative pb-16 md:pb-0">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentRoute}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="w-full"
          >
            {renderCurrentView()}
          </motion.div>
        </AnimatePresence>
      </main>
      <Footer />
      <BottomNav />
    </div>
  );
};

export default function PrototypeCommunityApp() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  );
}
