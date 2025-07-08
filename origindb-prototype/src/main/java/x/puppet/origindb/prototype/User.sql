-- 表
CREATE TABLE `user`
(
    `id`   INT PRIMARY KEY COMMENT 'id',
    `age`  TINYINT COMMENT '年龄',
    `city` VARCHAR(50) COMMENT '城市'
) COMMENT='用户表';

-- 数据
INSERT INTO user (id, age, city)
VALUES (1, 10, 'beijing'),  -- 10岁，北京
       (2, 20, 'shanghai'), -- 20岁，上海
       (3, 30, 'beijing'),  -- 30岁，北京
       (4, 40, 'shanghai'); -- 40岁，上海

-- 主键查询
SELECT id, age, city
FROM user
WHERE id = 1;

-- 复合查询
SELECT id, age, city
FROM user
WHERE age BETWEEN 20 AND 30
  AND address = 'beijing';