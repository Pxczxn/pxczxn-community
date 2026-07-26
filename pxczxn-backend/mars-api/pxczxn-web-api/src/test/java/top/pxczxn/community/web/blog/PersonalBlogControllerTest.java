package top.pxczxn.community.web.blog;

import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.application.BlogSettingsView;
import top.pxczxn.community.blog.application.PersonalBlogProfile;
import top.pxczxn.community.blog.application.PersonalBlogService;
import top.pxczxn.community.blog.application.PublicBlogProfile;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonalBlogControllerTest {

    @Test
    void mineUsesStringsForBigintIdentifiers() {
        PersonalBlogService service = mock(PersonalBlogService.class);
        when(service.getMine()).thenReturn(new PersonalBlogProfile(
                9223372036854775000L,
                "Alice 的博客",
                "alice",
                "简介",
                9223372036854774000L,
                null,
                "ACTIVE",
                12,
                36,
                new BlogSettingsView(
                        "ALL_LOGGED_IN",
                        "PUBLIC",
                        "ALLOW",
                        "light",
                        null,
                        null
                )
        ));

        PersonalBlogResponse response =
                new PersonalBlogController(service).getMine().getData();

        assertThat(response.blogId()).isEqualTo("9223372036854775000");
        assertThat(response.avatarFileId()).isEqualTo("9223372036854774000");
        assertThat(response.settings().themeKey()).isEqualTo("light");
    }

    @Test
    void updateDelegatesAndNormalizesBooleanFlags() {
        PersonalBlogService service = mock(PersonalBlogService.class);
        PersonalBlogProfile profile = new PersonalBlogProfile(
                200L,
                "New name",
                "alice",
                null,
                null,
                null,
                "ACTIVE",
                0,
                0,
                new BlogSettingsView(
                        "ALL_LOGGED_IN", "PUBLIC", "ALLOW", "dark", null, null
                )
        );
        when(service.updateMine(any())).thenReturn(profile);

        PersonalBlogResponse response = new PersonalBlogController(service).updateMine(
                new UpdatePersonalBlogRequest(
                        "New name", null, null, null, true, null, null
                )
        ).getData();

        assertThat(response.name()).isEqualTo("New name");
    }

    @Test
    void publicResponseContractContainsNoPrivateAccountFields() {
        PersonalBlogService service = mock(PersonalBlogService.class);
        when(service.getPublicBySlug("alice")).thenReturn(new PublicBlogProfile(
                200L,
                "PERSONAL",
                "Alice 的博客",
                "alice",
                "简介",
                null,
                null,
                12,
                36,
                "alice",
                "Alice",
                "写作者",
                null,
                "starry",
                "{\"accent\":\"blue\"}",
                "Alice 的博客",
                "写作者"
        ));

        PublicBlogResponse response =
                new PublicBlogController(service).getBySlug("alice").getData();

        assertThat(response.ownerUsername()).isEqualTo("alice");
        assertThat(response.themeKey()).isEqualTo("starry");
        assertThat(Arrays.stream(PublicBlogResponse.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase()))
                .doesNotContain(
                        "email",
                        "status",
                        "verificationstatus",
                        "personalblogid"
                );
    }
}
