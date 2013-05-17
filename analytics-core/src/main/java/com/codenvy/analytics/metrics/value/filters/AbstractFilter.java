/*
 *    Copyright (C) 2013 Codenvy.
 *
 */
package com.codenvy.analytics.metrics.value.filters;

import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.value.ListListStringValueData;
import com.codenvy.analytics.metrics.value.ListStringValueData;
import com.codenvy.analytics.metrics.value.MapStringLongValueData;
import com.codenvy.analytics.metrics.value.SetStringValueData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
public abstract class AbstractFilter implements Filter {

    protected final ListListStringValueData valueData;

    public AbstractFilter(ListListStringValueData valueData) {
        this.valueData = valueData;
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return valueData.getAll().size();
    }

    /** {@inheritDoc} */
    @Override
    public ListListStringValueData apply(MetricFilter key, String value) throws IllegalArgumentException {
        return apply(getIndex(key), value);
    }

    /** {@inheritDoc} */
    @Override
    public MapStringLongValueData sizeOfGroups(MetricFilter key) throws IllegalArgumentException {
        return sizeOfGroups(getIndex(key));
    }


    /** {@inheritDoc} */
    @Override
    public SetStringValueData getAvailable(MetricFilter key) throws IllegalArgumentException {
        return getAvailable(getIndex(key));
    }

    /** {@inheritDoc} */
    @Override
    public int size(MetricFilter key, String value) throws IllegalArgumentException {
        return size(getIndex(key), value);
    }

    private int size(int index, String value) {
        int result = 0;

        for (ListStringValueData item : valueData.getAll()) {
            if (item.getAll().get(index).equals(value)) {
                result++;
            }
        }

        return result;
    }

    private SetStringValueData getAvailable(int index) {
        Set<String> result = new HashSet<String>();

        for (ListStringValueData item : valueData.getAll()) {
            result.add(item.getAll().get(index));
        }

        return new SetStringValueData(result);
    }


    protected MapStringLongValueData sizeOfGroups(int index) throws IllegalArgumentException {
        Map<String, Long> map = new HashMap<String, Long>();

        for (ListStringValueData item : valueData.getAll()) {
            String key = item.getAll().get(index);

            long prevValue = map.containsKey(key) ? map.get(key) : 0;
            map.put(key, prevValue + 1);
        }

        return new MapStringLongValueData(map);
    }

    private ListListStringValueData apply(int index, String value) {
        List<ListStringValueData> list = new ArrayList<ListStringValueData>();

        for (ListStringValueData item : valueData.getAll()) {
            if (item.getAll().get(index).equals(value)) {
                list.add(item);
            }
        }

        return new ListListStringValueData(list);
    }

    /**
     * Returns filter index for the {@link #valueData}. Throws {@link IllegalArgumentException} if filter is not supported.
     */
    protected abstract int getIndex(MetricFilter key) throws IllegalArgumentException;
}
