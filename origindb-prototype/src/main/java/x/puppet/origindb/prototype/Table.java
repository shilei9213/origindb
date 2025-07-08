package x.puppet.origindb.prototype;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;


/**
 * 数据库
 *
 * @author sl
 * @date 2025/6/25
 */
public class Table<T extends Supplier<Integer>> {

    /**
     * 主键索引
     */
    private final Map<Integer, T> mainIndex = new HashMap<>();

    /**
     * 二级索引：key 字段名，subkey 字段值，value：符合的对象列表
     */
    private final Map<String, Map<Object, Set<T>>> secondaryIndex = new HashMap<>();

    @SneakyThrows
    public final void insert(T... objs) {
        for (T obj : objs) {
            // 插入数据
            mainIndex.put(obj.get(), obj);

            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);

                Object fieldValue = field.get(obj);
                // 插入二级索引
                secondaryIndex
                        .computeIfAbsent(field.getName(),
                                fieldName -> {
                                    if (fieldValue instanceof Comparable) {
                                        return new TreeMap<>();
                                    } else {
                                        return new HashMap<>();
                                    }
                                })
                        .computeIfAbsent(fieldValue, fieldName -> new HashSet<>())
                        .add(obj);
            }
        }

    }

    // 查询 ===============================

    public T getById(Integer id) {
        return mainIndex.get(id);
    }

    public Collection<T> search(String indexName, Object value) {
        Map<Object, Set<T>> values = secondaryIndex.get(indexName);

        if (MapUtils.isEmpty(values)) {
            return Collections.emptySet();
        }

        return secondaryIndex.get(indexName).get(value);
    }

    public Collection<T> rangeSearch(String indexName, Object from, boolean fromInclusive, Object to, boolean toInclusive) {
        Map<Object, Set<T>> values = secondaryIndex.get(indexName);
        if (!(values instanceof NavigableMap)) {
            throw new UnsupportedOperationException("unsupported range search!");
        }
        if (MapUtils.isEmpty(values)) {
            return Collections.emptySet();
        }
        Collection<Set<T>> result = ((NavigableMap<Object, Set<T>>) values)
                .subMap(from, fromInclusive, to, toInclusive).values();

        return or(result);
    }

    // 工具方法 =============================
    public static <T> Collection<T> or(Collection<Set<T>> results) {
        if (CollectionUtils.isEmpty(results)) {
            return Collections.emptySet();
        }

        if (results.size() == 1) {
            return results.iterator().next();
        }

        Collection<T> finalResult = new HashSet<>();
        for (Set<T> resultItem : results) {
            finalResult = CollectionUtils.union(finalResult, resultItem);
        }
        return finalResult;
    }


    public static void main(String[] args) {
        // 测试类
        @ToString
        @AllArgsConstructor
        class User implements Supplier<Integer> {
            private Integer id;
            private Integer age;
            private String address;

            @Override
            public Integer get() {
                return id;
            }
        }

        // 1.建表
        Table<User> table = new Table<>();

        // 2.插入数据 并建立索引 =========================

        table.insert(
                // age = 10， address = beijing
                new User(1, 10, "beijing"),
                // age = 20， address = shanghai
                new User(2, 20, "shanghai"),
                // age = 30， address = beijing
                new User(3, 30, "beijing"),
                // age = 40， address = shanghai
                new User(4, 40, "shanghai")
        );


        //3 查询
        // 主键
        System.out.println("SQL：where id = 1 ：");
        System.out.println(table.getById(1));
        System.out.println("======================================");

        // 多字段查询
        System.out.println("SQL: where (age BETWEEN 20 AND 30) AND address = 'beijing': ");
        Collection<User> ageAddressResult = CollectionUtils.intersection(
                table.rangeSearch("age", 20, true, 30, true),
                table.search("address", "beijing"));

        ageAddressResult.forEach(System.out::println);
        System.out.println("======================================");
    }
}


