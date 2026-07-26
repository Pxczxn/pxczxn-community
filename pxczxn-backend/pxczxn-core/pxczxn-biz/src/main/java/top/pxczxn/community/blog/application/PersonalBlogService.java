package top.pxczxn.community.blog.application;

public interface PersonalBlogService {

    PersonalBlogProfile getMine();

    PersonalBlogProfile updateMine(UpdatePersonalBlogCommand command);

    BlogSettingsView updateMySettings(UpdateBlogSettingsCommand command);

    PublicBlogProfile getPublicBySlug(String blogSlug);
}
