"use client";

import { Check, ChevronDown, Loader2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Avatar } from "../components/prototype-ui";
import { publicFileUrl, type MyTeam } from "../lib/community-api";
import { ROLE_LABELS, readLastTeamSelection, saveLastTeamSelection } from "./team-labels";

/**
 * 工作台团队切换器：展示当前团队与我的角色，可下拉切换。
 * 默认选择规则：上次访问团队 → 我拥有的团队 → 最近加入（列表第一项）。
 */
export function TeamSwitcher({
  teams,
  selected,
  onSelect,
  disabled,
}: {
  teams: MyTeam[];
  selected: string;
  onSelect: (teamId: string) => void;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  const current = teams.find((team) => team.teamId === selected) ?? teams[0];

  useEffect(() => {
    if (!open) return;
    function closeOnOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", closeOnOutside);
    return () => document.removeEventListener("mousedown", closeOnOutside);
  }, [open]);

  if (!current) return null;

  function choose(teamId: string) {
    saveLastTeamSelection(teamId);
    onSelect(teamId);
    setOpen(false);
  }

  return (
    <div className="team-switcher" ref={rootRef}>
      <button
        type="button"
        className="team-switcher__current"
        onClick={() => !disabled && setOpen((value) => !value)}
        disabled={disabled || teams.length <= 1}
        aria-haspopup="listbox"
        aria-expanded={open}
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
        {teams.length > 1 && <ChevronDown className="team-switcher__chevron" size={16} />}
      </button>

      {open && teams.length > 1 && (
        <div className="team-switcher__menu surface-lg" role="listbox">
          <div className="team-switcher__menu-label">切换团队</div>
          {teams.map((team) => (
            <button
              type="button"
              key={team.teamId}
              role="option"
              aria-selected={team.teamId === current.teamId}
              className="team-switcher__option"
              onClick={() => choose(team.teamId)}
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
          {disabled && (
            <div className="team-switcher__loading">
              <Loader2 className="animate-spin" size={14} /> 正在加载团队…
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/** 按默认规则计算团队切换器的初始选择。 */
export function defaultTeamSelection(teams: MyTeam[]): string | null {
  if (teams.length === 0) return null;
  const saved = readLastTeamSelection();
  if (saved && teams.some((team) => team.teamId === saved)) return saved;
  const owned = teams.find((team) => team.viewerRole === "OWNER");
  return (owned ?? teams[0]).teamId;
}
