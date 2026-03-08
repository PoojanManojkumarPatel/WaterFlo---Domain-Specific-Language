# WaterFlo 🌊

A custom domain-specific language (DSL) for simulating river flow networks.

## What it does
WaterFlo lets you model water systems using a simple, readable syntax.
Define rivers, connect them, add dams with flow control algorithms, and simulate output over multiple days.

## Example
```wflo
days 5;
let rain_today = rain(50mm);
dam d1 = reduce(0.8);
rain_today -> d1;
output d1;
```

Output:

rain_today(50) -> d1(40)

System Output (d1) per day = 40

For 5 days total = 200
