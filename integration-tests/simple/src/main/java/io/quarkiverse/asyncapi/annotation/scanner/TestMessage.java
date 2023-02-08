
package io.quarkiverse.asyncapi.annotation.scanner;

import java.util.List;

public class TestMessage<T> {

    private String x;
    private String y;
    private Integer sum;
    private T data;
    private T[] dataArray;
    private String[] stringArray;
    private List<T> dataList;
    private List<String> stringList;

}
