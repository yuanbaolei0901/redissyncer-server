package com.i1314i.syncerplusservice.service.Impl;

import com.i1314i.syncerpluscommon.util.common.TemplateUtils;
import com.i1314i.syncerplusservice.constant.RedisVersion;
import com.i1314i.syncerplusservice.constant.TaskMsgConstant;
import com.i1314i.syncerplusservice.entity.dto.RedisSyncDataDto;
import com.i1314i.syncerplusservice.service.IRedisReplicatorService;
import com.i1314i.syncerplusservice.service.exception.TaskMsgException;
import com.i1314i.syncerplusservice.task.*;
import com.i1314i.syncerplusservice.util.RedisUrlUtils;
import com.i1314i.syncerplusservice.util.TaskMonitorUtils;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.CommandListener;
import com.moilioncircle.redis.replicator.io.RawByteListener;
import com.moilioncircle.redis.replicator.rdb.RdbListener;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;


import java.io.*;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


@Service("redisReplicatorService")
@Slf4j
public class IRedisReplicatorServiceImpl implements IRedisReplicatorService {
    @Autowired
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * AOF备份
     *
     * @param redisPath
     * @param aofPath
     * @throws Exception
     */
    @Override
    public void backupAof(String redisPath, String aofPath) {
        File file = new File(aofPath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.info("create new file fail because:{%s}", e.getMessage());
            }
        }

        try {
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            final RawByteListener rawByteListener = new RawByteListener() {
                @Override
                public void handle(byte... rawBytes) {
                    try {
                        out.write(rawBytes);
                    } catch (IOException ignore) {
                    }
                }
            };

            Replicator replicator = new RedisReplicator(redisPath);
            replicator.addRdbListener(new RdbListener() {
                @Override
                public void preFullSync(Replicator replicator) {
                }

                @Override
                public void handle(Replicator replicator, KeyValuePair<?> kv) {
                }

                @Override
                public void postFullSync(Replicator replicator, long checksum) {
                    replicator.addRawByteListener(rawByteListener);
                }
            });

            final AtomicInteger acc = new AtomicInteger(0);
            replicator.addCommandListener(new CommandListener() {
                @Override
                public void handle(Replicator replicator, Command command) {
                    if (acc.incrementAndGet() == 1000) {
                        try {
                            out.close();
                            replicator.close();
                        } catch (Exception e) {

                        }
                    }
                }
            });
            replicator.open();
        } catch (Exception e) {
            log.info("[backupAof run error and reason is {%s}]", e.getMessage());
        }

    }


    /**
     * 远程备份RDB文件
     *
     * @param redis://127.0.0.1:6379?authPassword=yourPassword
     * @param c://test.RDB
     * @throws Exception
     */
    @Override
    public void backUPRdb(String redisPath, String path) {
        Future<Integer> result = threadPoolTaskExecutor.submit(new BackUPRdbTask(redisPath,path));
        try {
            System.out.println("status:----"+result.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    /**
     * 单机redis数据迁移
     * @param redis://127.0.0.1:6379?authPassword=yourPassword
     * @param redis://127.0.0.1:6380?authPassword=yourPassword
     */
    @Override
    public void sync(String sourceUri, String targetUri) throws TaskMsgException {
        checkRedisUrl(sourceUri,"sourceUri");
        checkRedisUrl(targetUri,"targetUri");
        String threadName= TemplateUtils.uuid();
        if(TaskMonitorUtils.containsKeyAliveMap(threadName)){
            throw new TaskMsgException(TaskMsgConstant.Task_MSG_PARSE_ERROR_CODE);
        }
        threadPoolTaskExecutor.execute(new SyncTask(sourceUri,targetUri));
    }

    /**
     * 单机redis数据迁移
     * @param redis://127.0.0.1:6379?authPassword=yourPassword
     * @param redis://127.0.0.1:6380?authPassword=yourPassword
     * @param 任务名称
     */
    @Override
    public void sync(String sourceUri, String targetUri, String threadName) throws TaskMsgException {

        checkRedisUrl(sourceUri,"sourceUri");
        checkRedisUrl(targetUri,"targetUri");
        if(TaskMonitorUtils.containsKeyAliveMap(threadName)){
            throw new TaskMsgException(TaskMsgConstant.Task_MSG_PARSE_ERROR_CODE);
        }
        threadPoolTaskExecutor.execute(new SyncTask(sourceUri,targetUri,threadName));
    }

    @Override
    public void sync(RedisSyncDataDto syncDataDto) throws TaskMsgException {
        checkRedisUrl(syncDataDto.getSourceUri(),"sourceUri");
        checkRedisUrl(syncDataDto.getTargetUri(),"targetUri");
        RedisVersion redisVersion=null;
        try {
             redisVersion=RedisUrlUtils.selectSyncerVersion(syncDataDto.getSourceUri(),syncDataDto.getTargetUri());
        } catch (URISyntaxException e) {
            throw new TaskMsgException(e.getMessage());
        }
        if(TaskMonitorUtils.containsKeyAliveMap(syncDataDto.getThreadName())){
            throw new TaskMsgException(TaskMsgConstant.Task_MSG_PARSE_ERROR_CODE);
        }

        if(redisVersion.equals(RedisVersion.SAME)){
            threadPoolTaskExecutor.execute(new SyncSameTask(syncDataDto));
            log.info("同步同版本（>3.0）版本数据...");
        }else if(redisVersion.equals(RedisVersion.LOWER)){
            threadPoolTaskExecutor.execute(new SyncLowerTask(syncDataDto));
            log.info("同步同版本（<3.0）版本数据...");
        }else if(redisVersion.equals(RedisVersion.OTHER)){
            threadPoolTaskExecutor.execute(new SyncDiffTask(syncDataDto));
            log.info("同步不同版本（）版本数据...");
        }else {
            threadPoolTaskExecutor.execute(new SyncTask(syncDataDto));
        }


    }


    public static void main(String[] args) throws Exception {
        IRedisReplicatorService redisReplicatorService = new IRedisReplicatorServiceImpl();
        redisReplicatorService.backUPRdb("redis://114.67.81.232:6340?authPassword=redistest0102", "D:\\tests");
    }

    void checkRedisUrl(String url,String name) throws TaskMsgException {

        try {
            if(!RedisUrlUtils.checkRedisUrl(url)){
                throw new TaskMsgException("scheme must be [redis].");
            }
            if(!RedisUrlUtils.getRedisClientConnectState(url,name)){
                throw new TaskMsgException(name+" :连接redis失败");
            }
        } catch (URISyntaxException e) {
            throw new TaskMsgException(e.getMessage());
        }
    }
}