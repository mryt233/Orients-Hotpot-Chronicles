package cn.blockforge.orient.framework.loader;

/** 网络协议策略：必要协议不匹配拒绝连接，可选协议降级。作者：mryt233 */
public enum ProtocolPolicy {
    REQUIRED,
    OPTIONAL
}