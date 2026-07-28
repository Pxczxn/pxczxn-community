package top.pxczxn.community.rss.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.application.*;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.series.model.TeamSeries;
import top.pxczxn.community.series.model.TeamSeriesArticle;
import top.pxczxn.community.series.persistence.TeamSeriesArticleMapper;
import top.pxczxn.community.series.persistence.TeamSeriesMapper;
import top.pxczxn.community.taxonomy.model.ArticleTag;
import top.pxczxn.community.taxonomy.model.PlatformTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.taxonomy.persistence.PlatformTagMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class PublicRssService {
    private final PublicArticleService articleService; private final ArticleMapper articleMapper; private final BlogMapper blogMapper;
    private final PlatformTagMapper tagMapper; private final ArticleTagMapper articleTagMapper; private final TeamSeriesMapper seriesMapper; private final TeamSeriesArticleMapper chapterMapper;
    @Transactional(readOnly=true) public RssDocument site(String origin){return document("星语社区 - 最新文章", "全站公开文章", origin+"/articles", articleService.discover(new PublicArticleQuery(null,1,30)).records());}
    @Transactional(readOnly=true) public RssDocument blog(String origin,String slug){Blog blog=blogMapper.selectOne(Wrappers.<Blog>lambdaQuery().eq(Blog::getSlug,slug).eq(Blog::getStatus,"ACTIVE").isNull(Blog::getDeletedAt).last("LIMIT 1"));if(blog==null)throw new BusinessException(404,"博客不存在");return document(blog.getName()+" - RSS", Optional.ofNullable(blog.getSummary()).orElse("公开文章订阅"),origin+"/blogs/"+blog.getSlug(),articleService.page(slug,new PublicArticleQuery(null,1,30)).records());}
    @Transactional(readOnly=true) public RssDocument tag(String origin,String slug){PlatformTag tag=tagMapper.selectOne(Wrappers.<PlatformTag>lambdaQuery().eq(PlatformTag::getSlug,slug).eq(PlatformTag::getStatus,"ACTIVE").isNull(PlatformTag::getMergedToTagId).last("LIMIT 1"));if(tag==null)throw new BusinessException(404,"标签不存在");List<Long> ids=articleTagMapper.selectList(Wrappers.<ArticleTag>lambdaQuery().eq(ArticleTag::getTagId,tag.getId()).orderByDesc(ArticleTag::getArticleId)).stream().map(ArticleTag::getArticleId).distinct().limit(30).toList();List<PublicArticleSummaryView> records=new ArrayList<>();for(Long id:ids){try{var d=articleService.detail(id);records.add(summary(d));}catch(BusinessException ignored){}}return document("#"+tag.getName()+" - RSS",Optional.ofNullable(tag.getDescription()).orElse("标签公开文章订阅"),origin+"/tags?tag="+tag.getSlug(),records);}
    @Transactional(readOnly=true) public RssDocument series(String origin,Long seriesId){TeamSeries series=seriesMapper.findPublicById(seriesId);if(series==null)throw new BusinessException(404,"系列不存在");List<PublicArticleSummaryView> records=new ArrayList<>();for(TeamSeriesArticle chapter:chapterMapper.findBySeries(seriesId)){try{var d=articleService.detail(chapter.getArticleId());records.add(summary(d));}catch(BusinessException ignored){}}return document(series.getTitle()+" - RSS",Optional.ofNullable(series.getSummary()).orElse("系列公开章节订阅"),origin+"/series/"+seriesId,records);}
    private RssDocument document(String title,String description,String link,List<PublicArticleSummaryView> rows){List<RssItem> items=rows.stream().map(article->new RssItem(article.title(),Optional.ofNullable(article.summary()).orElse(""),linkFor(link,article.canonicalPath()),article.publishedAt(),article.canonicalPath())).toList();String xml=render(title,description,link,items);String etag=hash(xml);return new RssDocument(xml,etag,items.stream().map(RssItem::publishedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));}
    private static PublicArticleSummaryView summary(PublicArticleDetailView d){return new PublicArticleSummaryView(d.articleId(),d.title(),d.slug(),d.summary(),d.coverFileId(),d.contentMode(),d.author(),d.category(),d.tags(),d.publishedAt(),d.updatedAt(),d.wordCount(),d.readingTimeMinutes(),d.viewCount(),d.likeCount(),d.favoriteCount(),d.commentCount(),d.canonicalPath());}
    private static String linkFor(String origin,String path){int marker=origin.indexOf('/',origin.indexOf("//")+2);String base=marker<0?origin:origin.substring(0,marker);return base+path;}
    private static String render(String title,String description,String link,List<RssItem> items){StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel>");tag(b,"title",title);tag(b,"description",description);tag(b,"link",link);tag(b,"lastBuildDate",DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));for(RssItem i:items){b.append("<item>");tag(b,"title",i.title());tag(b,"description",i.description());tag(b,"link",i.link());tag(b,"guid",i.guid());if(i.publishedAt()!=null)tag(b,"pubDate",DateTimeFormatter.RFC_1123_DATE_TIME.format(i.publishedAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)));b.append("</item>");}return b.append("</channel></rss>").toString();}
    private static void tag(StringBuilder b,String name,String value){b.append('<').append(name).append('>').append(escape(value)).append("</").append(name).append('>');} private static String escape(String v){return (v==null?"":v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");} private static String hash(String value){try{return "\""+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)))+"\"";}catch(Exception e){throw new IllegalStateException(e);}}
    public record RssDocument(String xml,String etag,LocalDateTime lastModified){} private record RssItem(String title,String description,String link,LocalDateTime publishedAt,String guid){}
}
