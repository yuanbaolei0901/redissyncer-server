package syncer.syncerplusredis.dao;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;
import syncer.syncerplusredis.model.AbandonCommandModel;
import syncer.syncerplusredis.model.BigKeyModel;

import java.util.List;

/**
 * @author zhanenqiang
 * @Description 描述
 * @Date 2020/7/6
 */

@Component
@Mapper
public interface BigKeyMapper {
    @Select("SELECT * FROM t_big_key WHERE taskId =#{taskId}")
    List<BigKeyModel> findBigKeyCommandListByTaskId(@Param("taskId") String taskId)throws Exception;

    @Insert("INSERT INTO t_big_key(taskId,command,command_type) VALUES(#{taskId},#{command},#{command_type})")
    boolean insertBigKeyCommandModel(BigKeyModel bigKeyModel)throws Exception;

    @Delete("DELETE FROM t_big_key WHERE id=#{id}")
    void deleteBigKeyCommandModelById(@Param("id")String id)throws Exception;

    @Delete("DELETE FROM t_big_key WHERE taskId=#{taskId}")
    void deleteBigKeyCommandModelByTaskId(@Param("taskId")String taskId)throws Exception;

}