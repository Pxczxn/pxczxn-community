package top.pxczxn.community.series.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.series.model.SeriesReadingProgress;

import java.time.LocalDateTime;
import java.util.List;

public interface SeriesReadingProgressMapper extends BaseMapper<SeriesReadingProgress> {

    @Select("""
            SELECT *
            FROM series_reading_progress
            WHERE user_id = #{userId} AND series_id = #{seriesId}
            LIMIT 1
            """)
    SeriesReadingProgress findByUserAndSeries(
            @Param("userId") Long userId,
            @Param("seriesId") Long seriesId
    );

    /** Moves the "last read" pointer; the furthest chapter only ever grows. */
    @Update("""
            UPDATE series_reading_progress
            SET last_article_id = #{articleId},
                last_chapter_order = #{chapterOrder},
                max_chapter_order = GREATEST(max_chapter_order, #{chapterOrder}),
                updated_at = #{now}
            WHERE user_id = #{userId} AND series_id = #{seriesId}
            """)
    int advance(
            @Param("userId") Long userId,
            @Param("seriesId") Long seriesId,
            @Param("articleId") Long articleId,
            @Param("chapterOrder") Integer chapterOrder,
            @Param("now") LocalDateTime now
    );

    /** Recently read series of one reader, newest first; powers the "continue reading" shelf. */
    @Select("""
            SELECT *
            FROM series_reading_progress
            WHERE user_id = #{userId}
            ORDER BY updated_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<SeriesReadingProgress> findRecentByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );
}
