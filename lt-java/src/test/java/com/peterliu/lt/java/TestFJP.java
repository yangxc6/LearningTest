package com.peterliu.lt.java;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;

/**
 * 测试FJP框架
 * <p>
 * Created by liujun on 2018/1/22.
 */
public class TestFJP {

    @Test
    public void testLambda() {
        CompletableFuture.supplyAsync(() -> "hello world!").thenAccept((s) -> System.out.println(s));
    }

    @Test
    public void testAdd() {
        System.out.println(((ForkJoinPool) Executors.newWorkStealingPool()).submit(new Add(1, 40)).join());
    }

    /**
     * 递归分治性能其实不如循环
     */
    @Test
    public void testFibnacci() {
        Long result = ((ForkJoinPool) Executors.newWorkStealingPool()).submit(new Fibnacci(500)).join();
        Assert.assertEquals(new Long(2171430676560690477L), result);
    }

    // 分治求取1+2+3+4的和
    public static class Add extends RecursiveTask<Integer> {
        //父类ForkJoinTask实现了Serializable接口
        private static final long serialVersionUID = 1L;
        @Getter
        private int start;
        @Getter
        private int end;
        // 确定分治的粒度
        private static final int MAX_RECURSIVE_SIZE = 3;

        public Add(int start, int end) {
            this.end = end;
            this.start = start;
        }


        @Override
        protected Integer compute() {
            int sum = 0;
            if (end - start < MAX_RECURSIVE_SIZE) {
                for (int i = start; i <= end; i++) {
                    sum += i;
                }
            } else {
                int middle = (start + end) / 2;
                Add left = new Add(start, middle);
                Add right = new Add(middle + 1, end);
                left.fork();
                right.fork();
                sum += left.join();
                sum += right.join();
            }
            return sum;
        }
    }

    // 分治求取Fibnacci序列值，给定序号，求取当前序位的Fibnacci值
    public static class Fibnacci extends RecursiveTask<Long> {
        //父类ForkJoinTask实现了Serializable接口
        private static final long serialVersionUID = 1L;
        // 存储结果
        @Getter
        private volatile long ret = 0;
        // 初始化序列
        private int seq;
        // 确定分治的粒度：如果问题的大小是小于既定的大小，你直接在任务中解决这问题。
        public static int DIVIDE_AND_CONQUER_SIZE = 499;

        public Fibnacci(int seq) {
            if (seq < 0) {
                throw new IllegalArgumentException("seq cannot be negative");
            }
            this.seq = seq;
        }

        // 采用多线程FJ框架分治
        @Override
        protected Long compute() {
            if (seq <= DIVIDE_AND_CONQUER_SIZE) {
                ret += runFib2(seq);
                return ret;
            } else {
                Fibnacci left = new Fibnacci(seq - 1);
                Fibnacci right = new Fibnacci(seq - 2);
                // 方式一
                invokeAll(left, right);
                ret = left.getRet() + right.getRet();
                // 方式二
//                left.fork();
//                right.fork();
//                result += left.join();
//                result += right.join();
                return ret;
            }
        }

        // 采用单线程递归
        private long runFib(long seq) {
            if (seq <= 1) {
                return seq;
            } else {
                return runFib(seq - 1) + runFib(seq - 2);
            }
        }

        // 采用循环
        private long runFib2(long seq) {
            long left = 0;
            long right = 1;
            long temp;
            for (long i = 2; i <= seq; i++) {
                temp = right;
                right = left + right;
                left = temp;
            }
            return right;
        }
    }

    //《 Java 7 Concurrency Cookbook 》 第五章案例自实现
    @AllArgsConstructor
    public static class PriceModified extends RecursiveAction {

        //父类ForkJoinTask实现了Serializable接口
        private static final long serialVersionUID = 1L;

        private List<Product> products;
        private int first;
        private int last;
        // 存储价格的增长
        private double increment;

        @Override
        protected void compute() {

        }

        // 产品类
        @Data
        @AllArgsConstructor
        public static class Product {
            private String name;
            private double price;
        }

        // 产品制造类
        public static class ProductListGenerator {
            public List<Product> generate(int size) {
                List<Product> ret = Lists.newArrayList();
                for (int i = 0; i < size; i++) {
                    ret.add(new Product("Product" + i, 10));
                }
                return ret;
            }
        }
    }
}