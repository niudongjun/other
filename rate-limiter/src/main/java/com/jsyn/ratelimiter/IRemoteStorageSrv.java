package com.jsyn.ratelimiter;

/**
 * description: 远程存储接口
 *
 * @author : niudongjun
 * @date : 2022/6/22 17:26
 */
public interface IRemoteStorageSrv {

    PermitsStorage getPermits();

    void setPermits(PermitsStorage permits);
}
