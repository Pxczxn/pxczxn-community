package top.pxczxn.community.team.application;

/**
 * Team portal settings shown on the public portal and in the workspace.
 * Identifier-free on purpose: it always rides along a {@link TeamPortalView}.
 */
public record TeamPortalSettingsView(
        String category,
        String contentDirection,
        String theme,
        String seoTitle,
        String seoDescription,
        Boolean publicMembers,
        Boolean allowSubmissions,
        String submissionGuideline,
        String contactInfo) {
}
