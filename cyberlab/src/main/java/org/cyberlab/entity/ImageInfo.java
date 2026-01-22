package org.cyberlab.entity;

import lombok.Data;

/**
 * Docker镜像信息实体类
 * 用于存储docker images命令获取的镜像数据
 */
public class ImageInfo {
    private String repository;   // 镜像仓库名称，如 nginx
    private String tag;          // 镜像标签，如 alpine
    private String imageId;      // 镜像ID
    private String created;      // 创建时间
    private String size;         // 镜像大小

    // 主机节点相关字段
    private Long hostNodeId;     // 所在主机节点ID
    private String hostNodeName; // 所在主机节点名称
    private String hostNodeIp;   // 所在主机节点IP

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Long getHostNodeId() {
        return hostNodeId;
    }

    public void setHostNodeId(Long hostNodeId) {
        this.hostNodeId = hostNodeId;
    }

    public String getHostNodeName() {
        return hostNodeName;
    }

    public void setHostNodeName(String hostNodeName) {
        this.hostNodeName = hostNodeName;
    }

    public String getHostNodeIp() {
        return hostNodeIp;
    }

    public void setHostNodeIp(String hostNodeIp) {
        this.hostNodeIp = hostNodeIp;
    }

    /**
     * 获取完整的镜像名称（repository:tag）
     */
    public String getFullName() {
        if (tag == null || tag.isEmpty() || "<none>".equals(tag)) {
            return repository;
        }
        return repository + ":" + tag;
    }

    /**
     * 判断是否为有效镜像（过滤<none>:<none>）
     */
    public boolean isValid() {
        return repository != null
            && !"<none>".equals(repository)
            && tag != null
            && !"<none>".equals(tag);
    }
}
