"use client";

import Link from "next/link";
import { AlertCircle, ArrowRight, Inbox, Loader2, Sparkles, Users } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import {
  communityApi,
  readSession,
  SESSION_EVENT,
  type MyTeam,
  type TeamApplication,
  type TeamInvitation,
  type TeamSummary,
} from "../lib/community-api";
import { DiscoverTeamsTab } from "./discover-tab";
import { MyTeamsTab } from "./my-teams-tab";
import { RequestsTab } from "./requests-tab";

type TabKey = "mine" | "discover" | "requests";
const TAB_KEYS: TabKey[] = ["mine", "discover", "requests"];

function tabFromUrl(): TabKey | null {
  if (typeof window === "undefined") return null;
  const value = new URLSearchParams(window.location.search).get("tab");
  return TAB_KEYS.includes(value as TabKey) ? (value as TabKey) : null;
}

export default function TeamsPage() {
  const [session, setSession] = useState(() => readSession());
  const [explicitTab, setExplicitTab] = useState<TabKey | null>(null);
  const [teams, setTeams] = useState<TeamSummary[]>([]);
  const [teamsLoading, setTeamsLoading] = useState(true);
  const [teamsError, setTeamsError] = useState("");
  const [mine, setMine] = useState<MyTeam[] | null>(null);
  const [mineLoading, setMineLoading] = useState(false);
  const [mineError, setMineError] = useState("");
  const [invitations, setInvitations] = useState<TeamInvitation[]>([]);
  const [requestsLoading, setRequestsLoading] = useState(false);
  const [requestsError, setRequestsError] = useState("");
  const [application, setApplication] = useState<TeamApplication | null>(null);

  const refreshSession = useCallback(() => setSession(readSession()), []);
  useEffect(() => {
    window.addEventListener(SESSION_EVENT, refreshSession);
    window.addEventListener("storage", refreshSession);
    return () => {
      window.removeEventListener(SESSION_EVENT, refreshSession);
      window.removeEventListener("storage", refreshSession);
    };
  }, [refreshSession]);

  // URL ?tab= 显式参数优先于自动判断；刷新后仍能恢复当前页签。
  useEffect(() => {
    const timer = window.setTimeout(() => setExplicitTab(tabFromUrl()), 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    let active = true;
    communityApi.teams()
      .then((value) => { if (active) setTeams(value); })
      .catch((reason) => { if (active) setTeamsError(reason instanceof Error ? reason.message : "团队列表加载失败"); })
      .finally(() => { if (active) setTeamsLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      if (!session) {
        setMine(null);
        setInvitations([]);
        setApplication(null);
        return;
      }
      setMineLoading(true);
      setMineError("");
      communityApi.myTeams()
        .then((value) => { if (active) setMine(value); })
        .catch((reason) => { if (active) setMineError(reason instanceof Error ? reason.message : "我的团队加载失败"); })
        .finally(() => { if (active) setMineLoading(false); });

      setRequestsLoading(true);
      setRequestsError("");
      void Promise.all([
        communityApi.myTeamInvitations(),
        communityApi.myTeamApplication(),
      ])
        .then(([invites, app]) => {
          if (active) {
            setInvitations(invites);
            setApplication(app);
          }
        })
        .catch((reason) => {
          if (active) setRequestsError(reason instanceof Error ? reason.message : "邀请与申请加载失败");
        })
        .finally(() => { if (active) setRequestsLoading(false); });
    }, 0);
    return () => { active = false; window.clearTimeout(timer); };
  }, [session]);

  const reloadInvitations = useCallback(() => {
    if (!session) return;
    communityApi.myTeamInvitations()
      .then(setInvitations)
      .catch(() => setRequestsError("邀请列表刷新失败，请稍后重试"));
    communityApi.myTeamApplication()
      .then(setApplication)
      .catch(() => setRequestsError("申请状态刷新失败，请稍后重试"));
    // 接受邀请会建立成员关系：同步刷新“我的团队”，新团队无需刷新页面即可出现。
    communityApi.myTeams()
      .then(setMine)
      .catch(() => setMineError("我的团队刷新失败，请稍后重试"));
  }, [session]);

  const reloadMine = useCallback(() => {
    if (!session) return;
    communityApi.myTeams()
      .then(setMine)
      .catch(() => setMineError("我的团队刷新失败，请稍后重试"));
  }, [session]);

  function selectTab(key: TabKey) {
    setExplicitTab(key);
    window.history.replaceState(null, "", `/teams?tab=${key}`);
  }

  function requireLogin(event?: React.MouseEvent<HTMLElement>) {
    if (readSession()) return;
    event?.preventDefault();
    const returnTo = typeof window === "undefined" ? "/teams" : `${window.location.pathname}${window.location.search}`;
    window.location.assign(`/login?returnTo=${encodeURIComponent(returnTo)}`);
  }

  const loggedIn = Boolean(session);
  const hasTeams = Boolean(mine && mine.length > 0);
  const pendingInvitationCount = invitations.filter((invitation) => invitation.status === "PENDING").length;
  const pendingRequestCount = pendingInvitationCount + (application?.status === "PENDING" ? 1 : 0);

  // 默认 Tab 规则：有团队成员关系 → 我的团队；无团队 / 未登录 → 发现团队。
  const autoTab: TabKey = !loggedIn || !hasTeams ? "discover" : "mine";
  const activeTab = explicitTab ?? autoTab;

  return (
    <>
      <UserTopbar title="团队" />
      <main className="page-shell teams-hub">
        <header className="teams-hub__header surface-lg">
          <div className="teams-hub__header-copy">
            <span className="eyebrow"><Users size={15} /> 团队空间</span>
            <h1>团队</h1>
            <p>管理你参与的团队、内容投稿与协作事项。</p>
          </div>
          <div className="teams-hub__header-actions">
            <Link href="/team-applications" className="primary-button" onClick={requireLogin}>
              <Sparkles size={15} /> 申请建立团队
            </Link>
            <Link href="/team-invitations" className="ghost-button" onClick={requireLogin}>
              <Inbox size={15} /> 处理邀请
              {pendingInvitationCount > 0 && <span className="badge badge--warn">{pendingInvitationCount}</span>}
            </Link>
          </div>
        </header>

        {!loggedIn && (
          <section className="surface teams-hub__notice">
            <div>
              <strong>登录后参与团队协作</strong>
              <p>可以申请建立团队、处理邀请并进入团队工作台；未登录时只能浏览公开团队。</p>
            </div>
            <Link href="/login?returnTo=%2Fteams" className="secondary-button">
              登录 / 注册 <ArrowRight size={15} />
            </Link>
          </section>
        )}
        {loggedIn && !mineLoading && !mineError && mine?.length === 0 && (
          <section className="surface teams-hub__notice">
            <div>
              <strong>你还没有加入团队</strong>
              <p>可以浏览公开团队，或提交团队建立申请。{pendingRequestCount > 0 ? `你还有 ${pendingRequestCount} 条邀请或申请待处理。` : ""}</p>
            </div>
            <Link href="/teams?tab=requests" className="secondary-button" onClick={() => selectTab("requests")}>
              {pendingRequestCount > 0 ? `查看待办（${pendingRequestCount}）` : "查看邀请与申请"} <ArrowRight size={15} />
            </Link>
          </section>
        )}

        <nav className="teams-hub__tabs" role="tablist" aria-label="团队模块页签">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "mine"}
            className={activeTab === "mine" ? "active" : ""}
            onClick={() => selectTab("mine")}
          >
            我的团队
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "discover"}
            className={activeTab === "discover" ? "active" : ""}
            onClick={() => selectTab("discover")}
          >
            发现团队
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "requests"}
            className={activeTab === "requests" ? "active" : ""}
            onClick={() => selectTab("requests")}
          >
            邀请与申请
            {pendingRequestCount > 0 && <span className="badge badge--warn">{pendingRequestCount}</span>}
          </button>
        </nav>

        {activeTab === "mine" && !loggedIn && (
          <section className="surface inline-feedback" role="alert">
            <AlertCircle size={20} />
            <div>
              <strong>请先登录</strong>
              <p>登录后可以查看你参与的团队并进入工作台。</p>
              <Link href="/login?returnTo=%2Fteams" className="secondary-button">登录 / 注册</Link>
            </div>
          </section>
        )}
        {activeTab === "mine" && loggedIn && mineLoading && (
          <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载你的团队…</div>
        )}
        {activeTab === "mine" && loggedIn && !mineLoading && mineError && (
          <section className="surface inline-feedback error" role="alert">
            <AlertCircle size={20} />
            <div>
              <strong>我的团队暂时无法加载</strong>
              <p>{mineError}</p>
              <button type="button" className="secondary-button" onClick={reloadMine}>重试</button>
            </div>
          </section>
        )}
        {activeTab === "mine" && loggedIn && !mineLoading && !mineError && mine && mine.length > 0 && (
          <MyTeamsTab teams={mine} />
        )}

        {activeTab === "discover" && (
          <DiscoverTeamsTab
            teams={teams}
            mine={mine}
            loading={teamsLoading}
            error={teamsError}
            onRequireLogin={requireLogin}
          />
        )}

        {activeTab === "requests" && !loggedIn && (
          <section className="surface inline-feedback" role="alert">
            <AlertCircle size={20} />
            <div>
              <strong>请先登录</strong>
              <p>登录后可以处理收到的团队邀请并查看团队建立申请状态。</p>
              <Link href="/login?returnTo=%2Fteams%3Ftab%3Drequests" className="secondary-button">登录 / 注册</Link>
            </div>
          </section>
        )}
        {activeTab === "requests" && loggedIn && (
          <RequestsTab
            invitations={invitations}
            application={application}
            teams={teams}
            mine={mine}
            loading={requestsLoading}
            error={requestsError}
            onRequireLogin={requireLogin}
            onInvitationChanged={reloadInvitations}
            onApplicationChanged={reloadInvitations}
          />
        )}
      </main>
    </>
  );
}
