import { ProfileEntry } from "./profile-entry";

export default async function UserProfileRoute({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  return <ProfileEntry slug={slug} />;
}
