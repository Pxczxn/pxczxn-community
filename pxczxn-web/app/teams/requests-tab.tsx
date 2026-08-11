"use client";

import Link from "next/link";
import { Button, Card, Empty, Space, Spin, Tag, Typography } from "@/components/ui/community-ui";
import {
  AlertCircle,
  ArrowUpRight,
  Check,
  FileCheck2,
  Inbox,
  UserPlus,
  X,
} from "lucide-react";
import { useState } from "react";
import { Avatar } from "../components/prototype-ui";
import {
  communityApi,
  publicFileUrl,
  type MyTeam,
  type TeamApplication,
  type TeamInvitation,
} from "../lib/community-api";
import {
  APPLICATION_STATUS_LABELS,
  formatDateTime,
  ROLE_LABELS,
} from "./team-labels";

const { Title, Paragraph, Text } = Typography;

export function RequestsTab({
  invitations,
  application,
  mine,
  loading,
  error,
  onRequireLogin,
  onInvitationChanged,
  onApplicationChanged,
  onSwitchTab,
}: {
  invitations: TeamInvitation[];
  application: TeamApplication | null;
  mine: MyTeam[] | null;
  loading: boolean;
  error: string;
  onRequireLogin: () => void;
  onInvitationChanged: () => void;
  onApplicationChanged: () => void;
  onSwitchTab: (tab: "mine" | "discover" | "requests") => void;
}) {
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState("");
  const [lastAccepted, setLastAccepted] = useState<{ teamName: string; teamSlug: string | null } | null>(null);

  const myTeamByTeamId = new Map((mine ?? []).map((team) => [team.teamId, team]));

  async function respond(invitation: TeamInvitation, action: "accept" | "reject") {
    setBusy(invitation.id);
    setActionError("");
    try {
      if (action === "accept") {
        await communityApi.acceptTeamInvitation(invitation.id);
        setLastAccepted({
          teamName: invitation.teamName ?? `团队 #${invitation.teamId}`,
          teamSlug: invitation.teamSlug ?? null,
        });
      } else {
        await communityApi.rejectTeamInvitation(invitation.id);
      }
      onInvitationChanged();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "处理邀请失败，请稍后重试");
    } finally {
      setBusy(null);
    }
  }

  async function cancelApplication() {
    if (!application) return;
    setBusy(`application-${application.id}`);
    setActionError("");
    try {
      await communityApi.cancelTeamApplication(application.id);
      onApplicationChanged();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "撤回申请失败，请稍后重试");
    } finally {
      setBusy(null);
    }
  }

  const approvedTeam = application?.status === "APPROVED" && application.teamSlug
    ? myTeamByTeamId.get(
        (mine ?? []).find((team) => team.slug === application.teamSlug)?.teamId ?? "",
      )
    : undefined;

  return (
    <div className="requests-tab">
      {actionError && (
        <Card style={{ borderRadius: 12, borderColor: "#ff4d4f", marginBottom: 16 }}>
          <Text type="danger">{actionError}</Text>
        </Card>
      )}

      {lastAccepted && (
        <Card style={{ borderRadius: 12, borderColor: "#52c41a", marginBottom: 16 }}>
          <Space align="center" size={12}>
            <Check size={18} style={{ color: "#52c41a" }} />
            <div>
              <Text strong style={{ fontSize: 14 }}>你已加入 {lastAccepted.teamName}！</Text>
              <Text type="secondary" style={{ fontSize: 12, display: "block" }}>
                成员关系已建立。你可以进入工作台开始协作，或在“我的团队”中找到它。
              </Text>
            </div>
            {lastAccepted.teamSlug && (
              <Link href={`/teams/${lastAccepted.teamSlug}/workspace`}>
                <Button type="primary" size="small" icon={<ArrowUpRight size={14} />}>
                  进入工作台
                </Button>
              </Link>
            )}
          </Space>
        </Card>
      )}

      {loading && (
        <div style={{ textAlign: "center", padding: 60 }}>
          <Spin tip="正在读取待处理事项…" />
        </div>
      )}

      {!loading && error && (
        <Card style={{ borderRadius: 12, borderColor: "#ff4d4f", marginBottom: 16 }}>
          <Text type="danger">{error}</Text>
        </Card>
      )}

      {!loading && !error && (
        <Space direction="vertical" style={{ width: "100%" }} size={20}>
          {/* 收到的邀请 */}
          <Card
            title={
              <Space align="center">
                <UserPlus size={18} style={{ color: "var(--primary, #1677ff)" }} />
                <Text strong style={{ fontSize: 15 }}>收到的团队邀请 ({invitations.length})</Text>
              </Space>
            }
            style={{ borderRadius: 14 }}
          >
            {invitations.length === 0 ? (
              <Empty description="当前没有待处理的团队邀请。" style={{ margin: "20px 0" }} />
            ) : (
              <Space direction="vertical" style={{ width: "100%" }} size={12}>
                {invitations.map((invitation) => {
                  const isPending = invitation.status === "PENDING";
                  const isAccepted = invitation.status === "ACCEPTED";
                  const isRejected = invitation.status === "REJECTED";
                  const teamName = invitation.teamName ?? `团队 #${invitation.teamId}`;

                  return (
                    <Card size="small" key={invitation.id} style={{ borderRadius: 10 }}>
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
                        <Space size={12}>
                          <Avatar
                            alt={`${teamName}头像`}
                            label={teamName.slice(0, 1)}
                            size="md"
                            src={publicFileUrl(invitation.teamAvatarFileId)}
                          />
                          <div>
                            <Text strong style={{ fontSize: 14, display: "block" }}>{teamName}</Text>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              邀请你作为 <Tag color="blue" style={{ margin: "0 4px" }}>{ROLE_LABELS[invitation.roleCode] || invitation.roleCode}</Tag> 加入团队 · {formatDateTime(invitation.createdAt)}
                            </Text>
                          </div>
                        </Space>
                        {isPending && (
                          <Space size={8}>
                            <Button
                              type="primary"
                              size="small"
                              loading={busy === invitation.id}
                              icon={<Check size={13} />}
                              onClick={() => respond(invitation, "accept")}
                            >
                              接受
                            </Button>
                            <Button
                              size="small"
                              danger
                              loading={busy === invitation.id}
                              icon={<X size={13} />}
                              onClick={() => respond(invitation, "reject")}
                            >
                              拒绝
                            </Button>
                          </Space>
                        )}
                        {isAccepted && <Tag color="green">已接受</Tag>}
                        {isRejected && <Tag color="default">已拒绝</Tag>}
                      </div>
                    </Card>
                  );
                })}
              </Space>
            )}
          </Card>

          {/* 团队建立申请 */}
          <Card
            title={
              <Space align="center">
                <FileCheck2 size={18} style={{ color: "var(--primary, #1677ff)" }} />
                <Text strong style={{ fontSize: 15 }}>团队建立申请</Text>
              </Space>
            }
            extra={
              !application && (
                <Link href="/team-applications">
                  <Button type="primary" size="small" onClick={onRequireLogin}>
                    新建申请
                  </Button>
                </Link>
              )
            }
            style={{ borderRadius: 14 }}
          >
            {!application ? (
              <Empty description="你还没有提交团队建立申请。" style={{ margin: "20px 0" }}>
                <Link href="/team-applications">
                  <Button type="primary" size="small" onClick={onRequireLogin}>
                    申请建立团队
                  </Button>
                </Link>
              </Empty>
            ) : (
              <Card size="small" style={{ borderRadius: 10 }}>
                <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
                  <div>
                    <Space size={8} style={{ marginBottom: 6 }}>
                      <Title level={5} style={{ margin: 0, fontSize: 15 }}>{application.teamName}</Title>
                      <Text type="secondary" style={{ fontSize: 12 }}>@{application.teamSlug}</Text>
                      <Tag color={application.status === "PENDING" ? "warning" : application.status === "APPROVED" ? "success" : "error"}>
                        {APPLICATION_STATUS_LABELS[application.status] || application.status}
                      </Tag>
                    </Space>
                    <Paragraph type="secondary" style={{ fontSize: 13, margin: "0 0 8px" }}>
                      {application.description || "无申请描述"}
                    </Paragraph>
                    <Text type="secondary" style={{ fontSize: 11 }}>
                      提交时间：{formatDateTime(application.createdAt)}
                    </Text>
                  </div>
                  {application.status === "PENDING" && (
                    <Button
                      size="small"
                      danger
                      loading={busy === `application-${application.id}`}
                      onClick={cancelApplication}
                    >
                      撤回申请
                    </Button>
                  )}
                  {approvedTeam && (
                    <Link href={`/teams/${approvedTeam.slug}/workspace`}>
                      <Button type="primary" size="small" icon={<ArrowUpRight size={14} />}>
                        工作台
                      </Button>
                    </Link>
                  )}
                </div>
              </Card>
            )}
          </Card>
        </Space>
      )}
    </div>
  );
}
