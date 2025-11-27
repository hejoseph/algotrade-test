package com.algotrade.simulator;

import com.algotrade.model.MarketData;
import com.algotrade.pipeline.MarketDataProcessor;
import okhttp3.*;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Live market data provider using Binance WebSocket ticker stream (&commat;ticker).
 * Runnable that connects WS and feeds MarketData to given pipeline.
 */
public class LiveMarketDataProvider implements Runnable {
    private final String symbol;
    private final String streamName;
    private final MarketDataProcessor pipeline;
    private WebSocket webSocket;
    private final OkHttpClient client;

    public LiveMarketDataProvider(String symbol, MarketDataProcessor pipeline) {
        this.symbol = symbol;
        this.streamName = symbol.toLowerCase().replace("/", "");
        this.pipeline = pipeline;
        this.client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void run() {
        Request request = new Request.Builder()
                .url("wss://stream.binance.com:9443/ws/" + streamName + "@ticker")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("Connected to Binance WS: " + streamName + "@ticker");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    String bidPriceStr = extractJsonField(text, "b");
                    String bidQtyStr = extractJsonField(text, "B");
                    String askPriceStr = extractJsonField(text, "a");
                    String askQtyStr = extractJsonField(text, "A");
                    if (bidPriceStr != null && bidQtyStr != null && askPriceStr != null && askQtyStr != null) {
                        double bid = Double.parseDouble(bidPriceStr);
                        long bidQty = Math.round(Double.parseDouble(bidQtyStr));
                        double ask = Double.parseDouble(askPriceStr);
                        long askQty = Math.round(Double.parseDouble(askQtyStr));
                        MarketData data = new MarketData(symbol, bid, ask, bidQty, askQty);
                        pipeline.processMarketData(data);
                        System.out.println("Live: " + data);
                    }
                } catch (Exception e) {
                    System.err.println("Parse error: " + e.getMessage() + " | Sample: " + text.substring(0, Math.min(100, text.length())));
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("WS failure: " + t.getMessage());
                // Reconnect logic could be added here
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("WS closed: " + code + " " + reason);
            }
        });

        // Block until interrupted
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            shutdown();
        }
    }

    public void shutdown() {
        if (webSocket != null) {
            webSocket.close(1000, "Shutdown");
        }
        client.dispatcher().executorService().shutdown();
    }

    private static String extractJsonField(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }
}