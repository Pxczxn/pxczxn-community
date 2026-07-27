package top.pxczxn.community.series.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.series.model.TeamSeriesArticle;

import java.util.List;

@Mapper
public interface TeamSeriesArticleMapper extends BaseMapper<TeamSeriesArticle> {
    @Select("SELECT * FROM team_series_article WHERE series_id = #{seriesId} ORDER BY chapter_order ASC")
    List<TeamSeriesArticle> findBySeries(@Param("seriesId") Long seriesId);

    @Select("SELECT * FROM team_series_article WHERE article_id = #{articleId} LIMIT 1")
    TeamSeriesArticle findByArticle(@Param("articleId") Long articleId);

    @Delete("DELETE FROM team_series_article WHERE series_id = #{seriesId}")
    int deleteBySeries(@Param("seriesId") Long seriesId);
}
