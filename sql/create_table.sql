# 数据库初始化

-- 创建库
create database if not exists community;

-- 切换库
use community;

-- 用户表
create table if not exists user
(
    id             bigint auto_increment comment 'id' primary key,
    userAccount    varchar(256)                           not null comment '账号',
    userPassword   varchar(512)                           not null comment '密码',
    userName       varchar(256)                           null comment '用户昵称',
    userAvatar     varchar(1024)                          null comment '用户头像',
    userProfile    varchar(512)                           null comment '用户简介',
    email          varchar(128)                           null comment '邮箱',
    salt           int(11)                                null comment '盐值',
    activationCode varchar(255) default null comment '激活码',
    userRole       varchar(256) default 'unActive'            not null comment '用户角色：unActive/user/admin/ban',
    createTime     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint      default 0                 not null comment '是否删除',
    index idx_userName_userAccount (userName, userAccount)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 帖子表
create table if not exists post
(
    id            bigint auto_increment comment 'id' primary key,
    title         varchar(512)                       null comment '标题',
    content       text                               null comment '内容',
    tags          varchar(1024)                      null comment '标签列表（json 数组）',
    postStatus    int(11)  default 0                 null comment '0-正常 1-精华 2-拉黑',
    type          int(11)  default 0                 null comment '0-普通帖 1-置顶帖',
    thumbNum      int      default 0                 not null comment '点赞数',
    favourNum     int      default 0                 not null comment '收藏数',
    score         double                             null comment '分数，用于排名',
    commentCount int(11)  default null comment '冗余存储每个帖子评论数量',
    userId        bigint                             not null comment '创建用户 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '帖子' collate = utf8mb4_unicode_ci;

-- 评论表
create table if not exists comment
(
    id            bigint auto_increment comment 'id' primary key,
    userId        bigint                             not null comment '创建用户 id',
    entityType  int null default 0 comment '评论目标的类型 0-帖子 1-评论 ',
    entityId bigint null comment '评论目标的id',
    targetId int default 0 comment '对于有指向性的评论，如，针对某个人的评论进行评论',
    content       text                               null comment '内容',
    commentStatus    int(11)  default 0                 null comment '0-正常 1-隐藏',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '帖子' collate = utf8mb4_unicode_ci;

-- 消息表（硬删除）
create table if not exists message
(
    id         bigint auto_increment comment 'id' primary key,
    formId     bigint                             not null comment '发送人 id',
    toId     bigint                             not null comment '接收人 id',
    conversationId varchar(128)  null comment '会话id' ,
    messageStatus int not null default 0 comment '消息状态 0-已读 1-未读',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    index idx_postId (formId),
    index idx_userId (toId),
    index conversationId (conversationId)
) comment '消息';


-- 帖子点赞表（硬删除）
create table if not exists post_thumb
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子点赞';

-- 帖子收藏表（硬删除）
create table if not exists post_favour
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子收藏';
