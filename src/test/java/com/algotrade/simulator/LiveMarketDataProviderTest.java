package com.algotrade.simulator;

import com.algotrade.model.MarketData;
import com.algotrade.pipeline.MarketDataProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * Unit tests for LiveMarketDataProvider.
 */
class LiveMarketDataProviderTest {
    @Test
    void testExtractJsonField() {
        String json = "{\"u\":123456789,\"s\":\"BTCUSDT\",\"b\":\"90970.58\",\"B\":\"1.40355\",\"a\":\"90970.59\",\"A\":\"6.40697\"}";
        assertEquals("90970.58", LiveMarketDataProvider.extractJsonField(json, "b"));
        assertEquals("1.40355", LiveMarketDataProvider.extractJsonField(json, "B"));
        assertEquals("90970.59", LiveMarketDataProvider.extractJsonField(json, "a"));
        assertEquals("6.40697", LiveMarketDataProvider.extractJsonField(json, "A"));
        assertNull(LiveMarketDataProvider.extractJsonField(json, "missing"));
    }
}