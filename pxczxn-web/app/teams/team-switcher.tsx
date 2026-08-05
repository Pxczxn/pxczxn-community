"use client";

import { Check, ChevronDown, Loader2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Avatar } from "../components/prototype-ui";
import { publicFileUrl, type MyTeam } from "../lib/community-api";
import { ROLE_LABELS, saveLastTeamSelection } from "./team-labels";

/**
 * 团队工作台顶部切换器：展示当前团队与我的角色，下拉选择其他团队后
 * 导航到目标团队工作台，并把选择写入本地存储（最近访问团队）。
 */
export function TeamSwitcher({
  teams,
  currentTeamId,
  onNavigate,
  loading,
}: {
  teams: MyTeam[];
  currentTeamId: string;
  onNavigate: (team: MyTeam) => void;
  loading?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  const current = teams.find((team) => team.teamId === currentTeamId) ?? teams[0];
  const switchable = teams.length > 1;

  useEffect(() => {
    if (!open) return;
    function closeOnOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", closeOnOutside);
    return () => document.removeEventListener("mousedown", closeOnOutside);
  }, [open]);

  if (!current) return null;

  function choose(team: MyTeam) {
    saveLastTeamSelection(team.teamId);
    setOpen(false);
    onNavigate(team);
  }

  return (
    <div className="team-switcher" ref={rootRef}>
      <button
        type="button"
        className="team-switcher__current"
        onClick={() => switchable && setOpen((value) => !value)}
        disabled={!switchable || loading}
        aria-haspopup="listbox"
        aria-expanded={open}
        title={switchable ? "切换到其他团队" : "当前团队"}
      >
        <Avatar
          alt={`${current.name}头像`}
          label={current.name.slice(0, 1)}
          size="sm"
          src={publicFileUrl(current.avatarFileId)}
        />
        <span className="team-switcher__copy">
          <strong>{current.name}</strong>
          <span>
            @{current.slug} · {ROLE_LABELS[current.viewerRole] || current.viewerRole}
          </span>
        </span>
        {switchable && <ChevronDown className="team-switcher__chevron" size={16} />}
      </button>

      {open && (
        <div className="team-switcher__menu surface-lg" role="listbox">
          <div className="team-switcher__menu-label">切换团队（将进入对应工作台）</div>
          {teams.map((team) => (
            <button
              type="button"
              key={team.teamId}
              role="option"
              aria-selected={team.teamId === current.teamId}
              className="team-switcher__option"
              onClick={() => choose(team)}
            >
              <Avatar
                alt={`${team.name}头像`}
                label={team.name.slice(0, 1)}
                size="sm"
                src={publicFileUrl(team.avatarFileId)}
              />
              <span className="team-switcher__option-copy">
                <strong>{team.name}</strong>
                <span>
                  @{team.slug} · {ROLE_LABELS[team.viewerRole] || team.viewerRole}
                </span>
              </span>
              {team.teamId === current.teamId && <Check size={16} className="team-switcher__check" />}
            </button>
          ))}
          {loading && (
            <div className="team-switcher__loading">
              <Loader2 className="animate-spin" size={14} /> 正在加载团队…
            </div>
          )}
        </div>
      )}
    </div>
  );
}
