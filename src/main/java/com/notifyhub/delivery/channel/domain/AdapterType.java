package com.notifyhub.delivery.channel.domain;

/**
 * 送出通道类型。用户触达和归档只保留类型名，本批不提供厂商客户端。
 */
public enum AdapterType {
    HTTP,
    KAFKA,
    ROCKETMQ,
    PUSH,
    SMS,
    WECHAT,
    EMAIL,
    ARCHIVE
}
