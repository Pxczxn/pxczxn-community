package top.pxczxn.platform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.pxczxn.platform.file.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息 Mapper
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
}
