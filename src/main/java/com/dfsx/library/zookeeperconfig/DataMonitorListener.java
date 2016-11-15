package com.dfsx.library.zookeeperconfig;

import java.util.List;

/**
 * Created by ifpelset on 11/7/16.
 */

public interface DataMonitorListener {
    void updateExistsMap(String path, boolean exist);
    void updateValueMap(String path, byte[] data);
    void updateChildrenMap(String path, List<String> children);
}
