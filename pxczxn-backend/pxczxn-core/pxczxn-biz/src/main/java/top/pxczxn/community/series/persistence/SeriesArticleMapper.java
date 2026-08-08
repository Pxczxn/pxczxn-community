package top.pxczxn.community.series.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.series.model.SeriesArticle;

import java.util.List;

@Mapper
public interface SeriesArticleMapper extends BaseMapper<SeriesArticle> {
    @Select("SELECT * FROM series_article WHERE series_id = #{seriesId} ORDER BY chapter_order ASC")
    List<SeriesArticle> findBySeries(@Param("seriesId") Long seriesId);

    @Select("SELECT * FROM series_article WHERE article_id = #{articleId} LIMIT 1")
    SeriesArticle findByArticle(@Param("articleId") Long articleId);

    /** Series membership of many articles, for the workspace / portal content lists. */
    @Select("<script>"
            + "SELECT article_id, series_id FROM series_article WHERE article_id IN "
            + "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<SeriesArticle> findByArticles(@Param("articleIds") List<Long> articleIds);

    @Delete("DELETE FROM series_article WHERE series_id = #{seriesId}")
    int deleteBySeries(@Param("seriesId") Long seriesId);
}
