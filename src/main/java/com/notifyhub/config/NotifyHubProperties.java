package com.notifyhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * 通知中枢运行时配置，绑定 application.yml 中 notify-hub 前缀。
 */
@ConfigurationProperties(prefix = "notify-hub")
public class NotifyHubProperties {

    private Dispatcher dispatcher = new Dispatcher();
    /** 重试退避间隔（分钟），列表长度即最大重试次数 */
    private List<Integer> retryIntervalsMinutes = List.of(1, 5, 30, 60, 360);
    /** 各渠道 baseUrl，key 为 crm / ad / inventory */
    private Map<String, ChannelConfig> channels = Map.of();

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public List<Integer> getRetryIntervalsMinutes() {
        return retryIntervalsMinutes;
    }

    public void setRetryIntervalsMinutes(List<Integer> retryIntervalsMinutes) {
        this.retryIntervalsMinutes = retryIntervalsMinutes;
    }

    public Map<String, ChannelConfig> getChannels() {
        return channels;
    }

    public void setChannels(Map<String, ChannelConfig> channels) {
        this.channels = channels;
    }

    /** 调度器相关配置 */
    public static class Dispatcher {
        /** 两轮调度之间的间隔（毫秒） */
        private long intervalMs = 10_000;
        /** 每批拉取的最大任务数 */
        private int batchSize = 50;

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    /** 单个外部渠道的连接配置 */
    public static class ChannelConfig {
        /** 供应商 API 根地址 */
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
