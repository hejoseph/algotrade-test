# Algorithmic Trading Engine Simulator

[![Maven](https://img.shields.io/badge/Build-Maven-blue.svg)](https://maven.org)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org)

This is a **high-frequency algorithmic trading simulator** built in Java. It's designed for Java developers (especially full-stack web devs) to learn core concepts of trading systems **without risking real money**. If you've built REST APIs (request → controller → service → DB), think of this as an **event-driven pipeline**: live/simulated market data → strategy decisions → risk checks → simulated order execution.

## Why Useful for You (Full-Stack Java Dev)?
Trading systems are **extreme concurrency/low-latency apps**:
- **1000s events/sec** (vs web's 100 req/min).
- **Microsecond latencies** (vs web's 100ms).
- **Immutable data + no shared mutation** (vs DB transactions).
- **Event-driven** (like Kafka/SSE, but synchronous pipeline).

**Learn**:
- Multithreading with `ExecutorService`/`Future` (safer than raw Threads).
- Deterministic pipelines (single-thread per stage → no race conditions).
- Simulated order matching (price-time priority, partial fills).
- Metrics (PnL, latency histograms).
- Real-time feeds (WebSocket → parse → process).

Extend to **real trading** (Binance API) or **HFT research**.

## Quick Start
```bash
git clone <repo>
cd algorithmic-trading-engine
mvn clean compile  # Downloads deps (OkHttp for WS)
```

### Run Backtest (Simulated Data, 60s)
```bash
mvn exec:java -Dexec.mainClass="com.algotrade.simulator.Backtester"
```
- Generates random BTC/USD prices.
- Runs mean-reversion strategy.
- Outputs: PnL ~$X, Fill Ratio 0.XX, Position Y BTC.

### Run Real-Time (Live Binance Data)
```bash
mvn exec:java -Dexec.mainClass="com.algotrade.RealTimeTrader"
```
- Live BTCUSDT bookTicker (bid/ask every ~1s).
- Ctrl+C → final metrics.
- No real trades (paper trading on simulated exchange).

### Tests
```bash
mvn test  # 27 tests: order matching, throttling, risk, strategy, parsing.
mvn test -Dtest=LiveMarketDataProviderTest  # JSON parse from Binance.
```

## Architecture: The Trading Pipeline
**Core Flow** (like MVC but for speed):
```
MarketData (bid/ask) ─→ Strategy (generate Orders) ─→ Risk (approve/reject) ─→ Throttler ─→ Exchange (match → Trades)
                                                                 ↓ Metrics (PnL, latency)
```

### 1. **Models** (`model/`)
- `MarketData`: Bid/ask price + qty + timestamp (immutable, like DTO).
- `Order`: Limit/market, BUY/SELL, qty, price (UUID id).
- `Trade`: Matched order fill (partial possible).

**Why immutable?** Thread-safe, no locks.

### 2. **Exchange** (`exchange/`)
- `OrderBook`: Per-symbol bids/asks (TreeMap price → priority queue time/qty).
- Matching: Best price first, time priority, partial fills.
- **Like?** In-memory DB with sorted indexes.

### 3. **Pipeline** (`pipeline/TradingPipeline`)
Implements `MarketDataProcessor` (functional interface).
```java
void processMarketData(MarketData data) {
  Future<List<Order>> orders = strategyExecutor.submit(() -> strategy.process(data));
  for (Order o : orders.get()) {
    if (riskExecutor.submit(() -> risk.check(o)).get()) {
      executionExecutor.submit(() -> executor.execute(o));
    }
  }
}
```
- **4 SingleThreadExecutors**: Market → Strategy → Risk → Exec.
- **Why single-thread/stage?** Sequential per stage (no parallelism needed), zero contention (web: thread-per-request chaos).
- `Future.get()`: Block for result (micro-batch sync).
- **Low-latency**: Dedicated threads, no context-switch hell.

### 4. **Strategy** (`strategy/MeanReversionStrategy`)
Tracks price history (deque, fixed lookback=50).
- Mid = (bid+ask)/2.
- MA = avg last N mids.
- BUY if mid < MA*(1-threshold); SELL if > MA*(1+threshold).
- **Why?** Prices revert to mean (stat arb).
- **Extend**: Add breakout (high > recent max).

### 5. **Risk** (`risk/MaxPositionRiskManager`)
- Tracks net position (PositionManager).
- Reject if |new pos| > max (e.g. 10 BTC).
- **Why first?** Cheap check before expensive exec.

### 6. **Execution** (`pipeline/`)
- `ExecutionThrottler`: Semaphore(5/sec) → prevents API bans.
- `ExchangeOrderExecutor`: Sends to simulated exchange.

### 7. **Metrics** (`metrics/`)
- `TradeMetrics`: PnL = sum(sell - buy prices * qty), fill ratio.
- `LatencyMetrics`: Histogram order create → exec time.

### 8. **Simulators** (`simulator/`)
- `Backtester`: Fixed-duration sim (random walk prices).
- `LiveMarketDataProvider`: Binance WS `@bookTicker` → parse JSON bid/ask → MarketData.
  - OkHttp WebSocket + manual JSON parse (fast, no Gson overhead).

## Multithreading Deep Dive (Your New Superpower)
**Web analogy**: Tomcat thread-per-request → shared DB locks → deadlocks.
**Here**:
- **No shared mutable state**: Each stage immutable inputs/outputs.
- **SingleThreadExecutor**: FIFO queue, unbounded, shutdown graceful.
- **Why not parallelStream()?** Order matters (data1 → strat → risk before data2).
- **Coordination**: `Future.get()` = await promise (CompletableFuture for async).
- **Low-latency tips**: Pin threads (no GC pauses), off-heap, but sim keeps simple.

**Perf**: ~1μs/order in sim (real HFT: nanoseconds).

## Customization
1. **New Strategy**:
   ```java
   class BreakoutStrategy implements StrategyProcessor {
     // Track highs/lows → BUY on new high.
   }
   ```
2. **Real Trading**: Replace `ExchangeOrderExecutor` → Binance API (sign orders).
3. **Multi-symbol**: Loop addSymbol/strategies.
4. **Data**: Add Kafka for historical ticks.

## Troubleshooting
- No trades? Lower threshold=0.0001, lookback=10, run 5min+.
- WS fails? Binance rate limits (1 conn/symbol).

## Next Steps
- Run backtest → tweak strategy → see PnL change.
- Add logging (SLF4J).
- Profile (JMH) pipeline latency