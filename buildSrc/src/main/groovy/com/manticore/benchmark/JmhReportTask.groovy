package com.manticore.benchmark

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class JmhReportTask extends DefaultTask {

    @InputFile
    abstract RegularFileProperty getResultsFile()

    @OutputFile
    abstract RegularFileProperty getReportFile()

    // Human-readable encoder labels, derived from the JMH benchmark class name
    private static final Map<String, String> ENCODER_LABELS = [
            'FPNGE23Benchmark'              : 'FPNGE (FFM)',
            'FPNGEBenchmark'                : 'FPNGE (JNA)',
            'FPNGEncoder23Benchmark'        : 'FPNG (FFM)',
            'FPNGEncoderBenchmark'          : 'FPNG (JNA)',
            'ZPNG23Benchmark'               : 'ZPNG (FFM)',
            'ZPNGBenchmark'                 : 'ZPNG (JNA)',
            'ImageIOEncoderBenchmark'       : 'ImageIO',
            'ObjectPlanetPNGEncoderBenchmark': 'ObjectPlanet PNG',
            'PNGEncoderBenchmark'           : 'PNGEncoder',
            'PNGEncoderFastestBenchmark'    : 'PNGEncoder (FAST)',
    ]

    // Method-level overrides for benchmarks that expose multiple methods
    private static final Map<String, String> METHOD_LABELS = [
            'encodeFastest': ' (fastest)',
    ]

    @TaskAction
    void generateReport() {
        def json = new JsonSlurper().parse(resultsFile.asFile.get())
        def groups = groupBenchmarks(json)
        def html = renderHtml(groups)
        reportFile.asFile.get().text = html
        logger.lifecycle("JMH HTML report written to ${reportFile.asFile.get()}")
    }

    // ── data model ──────────────────────────────────────────────────────

    static class BenchmarkEntry {
        String encoder
        double score
        double scoreError
        String scoreUnit
    }

    /**
     * Groups entries by (imageName, channels).
     * Returns Map<String, List<BenchmarkEntry>> keyed as "imageName | channels ch"
     */
    private static Map<String, List<BenchmarkEntry>> groupBenchmarks(List json) {
        def grouped = [:] as LinkedHashMap

        json.each { entry ->
            def fqn = entry.benchmark as String
            def className = fqn.tokenize('.')[-2]
            def methodName = fqn.tokenize('.')[-1]

            def label = ENCODER_LABELS.getOrDefault(className, className)
            def methodSuffix = METHOD_LABELS.getOrDefault(methodName, '')
            def encoder = label + methodSuffix

            def imageName = entry.params.imageName as String
            def channels = entry.params.channels as String
            def groupKey = "${imageName} | ${channels} ch"

            grouped.computeIfAbsent(groupKey) { [] } << new BenchmarkEntry(
                    encoder: encoder,
                    score: entry.primaryMetric.score as double,
                    scoreError: entry.primaryMetric.scoreError as double,
                    scoreUnit: entry.primaryMetric.scoreUnit as String,
            )
        }

        // Sort each group by score ascending (fastest first)
        grouped.each { _, entries -> entries.sort { it.score } }

        return grouped
    }

    // ── HTML rendering ──────────────────────────────────────────────────

    private static String renderHtml(Map<String, List<BenchmarkEntry>> groups) {
        def cells = new StringBuilder()
        int chartIdx = 0

        groups.each { groupKey, entries ->
            cells.append(renderGroup(groupKey, entries, chartIdx++))
        }

        return """\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>JMH PNG Encoder Benchmark Report</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@3"></script>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Fira+Sans:wght@300;400;600;700&display=swap');

  :root {
    --navy:   #030146;
    --orange: #FF420E;
    --bg:     #f7f7fb;
    --card:   #ffffff;
    --muted:  #6c6c8a;
    --border: #e2e2ef;
    --winner-bg: rgba(255, 66, 14, 0.08);
  }
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: 'Fira Sans', system-ui, sans-serif;
    background: var(--bg);
    color: var(--navy);
    line-height: 1.5;
    padding: 2rem;
  }

  header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 2.5rem;
    border-bottom: 3px solid var(--orange);
    padding-bottom: 1rem;
  }
  header .logo {
    width: 48px; height: 48px;
    background: var(--navy);
    border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    color: white; font-weight: 700; font-size: 1.3rem;
  }
  header h1 { font-size: 1.6rem; font-weight: 700; }
  header .sub { color: var(--muted); font-size: 0.9rem; font-weight: 300; }

  .grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
    margin-bottom: 2rem;
  }
  @media (max-width: 360px) {
    .grid { grid-template-columns: 1fr; }
  }

  .group {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 1.25rem 1.5rem;
    box-shadow: 0 1px 4px rgba(3,1,70,0.06);
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .group h2 {
    font-size: 1.05rem;
    font-weight: 600;
    margin-bottom: 0.75rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .group h2 .badge {
    display: inline-block;
    background: var(--navy);
    color: white;
    font-size: 0.65rem;
    font-weight: 600;
    padding: 0.12rem 0.5rem;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .chart-wrap {
    width: 100%;
    margin-bottom: 1rem;
    flex: 1;
    min-height: 280px;
    position: relative;
    overflow: hidden;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.82rem;
  }
  thead th {
    text-align: left;
    padding: 0.4rem 0.5rem;
    border-bottom: 2px solid var(--navy);
    font-weight: 600;
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--muted);
  }
  thead th:nth-child(n+2) { text-align: right; }
  tbody td {
    padding: 0.35rem 0.5rem;
    border-bottom: 1px solid var(--border);
  }
  tbody td:nth-child(n+2) { text-align: right; font-variant-numeric: tabular-nums; }
  tbody tr.winner td { background: var(--winner-bg); font-weight: 600; }
  tbody tr.winner td:first-child::before {
    content: '\\1F3C6\\00a0';
  }
  .pct { color: var(--orange); font-weight: 600; }
  .pct-zero { color: var(--navy); font-weight: 600; }

  footer {
    margin-top: 2rem;
    font-size: 0.78rem;
    color: var(--muted);
    text-align: center;
  }
</style>
</head>
<body>

<header>
  <div class="logo">M</div>
  <div>
    <h1>PNG Encoder Benchmark Report</h1>
    <div class="sub">Generated by Manticore Projects &middot; JMH ${(groups.values().first()?.first()?.scoreUnit) ?: 'ms/op'} &middot; lower is better</div>
  </div>
</header>

<div class="grid">
${cells}
</div>

<footer>
  Manticore Projects &copy; ${java.time.Year.now()} &middot; JMH benchmark report &middot;
  JDK 23 &middot; Serial GC &middot; 1&times;3 fork/iteration
</footer>

</body>
</html>
"""
    }

    /**
     * Renders one grid cell: heading, horizontal bar chart with annotation line
     * and percentage top axis, and a comparison table underneath.
     */
    private static String renderGroup(String groupKey, List<BenchmarkEntry> entries, int idx) {
        def (imageName, channelsLabel) = groupKey.split(' \\| ')
        def canvasId = "chart_${idx}"

        def winner = entries.first()
        double bestScore = winner.score
        double worstScore = entries.last().score

        // Chart data (bars: fastest → slowest, already sorted)
        def labels = entries.collect { "'${escJs(it.encoder)}'" }.join(', ')
        def data = entries.collect { String.format('%.3f', it.score) }.join(', ')
        def bgColors = entries.collect { it.is(winner) ? "'#FF420E'" : "'#030146'" }.join(', ')

        // Table rows
        def rows = new StringBuilder()
        entries.each { e ->
            def isWinner = e.is(winner)
            def pctSlower = isWinner ? 0d : ((e.score - bestScore) / bestScore * 100d)
            def pctClass = isWinner ? 'pct-zero' : 'pct'
            def pctText = isWinner ? 'baseline' : "+${String.format('%.1f', pctSlower)}%"
            def trClass = isWinner ? ' class="winner"' : ''
            rows.append("""      <tr${trClass}>
        <td>${esc(e.encoder)}</td>
        <td>${String.format('%.3f', e.score)}</td>
        <td>&plusmn;${String.format('%.3f', e.scoreError)}</td>
        <td class="${pctClass}">${pctText}</td>
      </tr>
""")
        }

        return """
<div class="group">
  <h2>
    <span class="badge">${esc(channelsLabel)}</span>
    ${esc(imageName as String)}
  </h2>

  <div class="chart-wrap">
    <canvas id="${canvasId}"></canvas>
  </div>

  <table>
    <thead>
      <tr>
        <th>Encoder</th>
        <th>Score (${esc(entries.first().scoreUnit)})</th>
        <th>Error</th>
        <th>vs Winner</th>
      </tr>
    </thead>
    <tbody>
${rows}    </tbody>
  </table>
</div>

<script>
(function() {
  const ctx = document.getElementById('${canvasId}');
  const bestScore = ${String.format('%.3f', bestScore)};
  const axisMax = ${String.format('%.3f', worstScore * 1.08)};
  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: [${labels}],
      datasets: [{
        data: [${data}],
        backgroundColor: [${bgColors}],
        borderRadius: 4,
        barPercentage: 0.7,
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      layout: { padding: { top: 4 } },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: function(c) {
              const pct = (c.raw / bestScore * 100).toFixed(0);
              return c.raw.toFixed(3) + ' ${escJs(entries.first().scoreUnit)}  (' + pct + '%)';
            }
          }
        },
        annotation: {
          annotations: {
            winnerLine: {
              type: 'line',
              xMin: bestScore,
              xMax: bestScore,
              borderColor: '#FF420E',
              borderWidth: 2,
              borderDash: [6, 3],
              label: {
                display: true,
                content: bestScore.toFixed(1) + ' ${escJs(entries.first().scoreUnit)}',
                position: 'start',
                backgroundColor: '#FF420E',
                color: '#fff',
                font: { family: 'Fira Sans', size: 10, weight: '600' },
                padding: { top: 2, bottom: 2, left: 5, right: 5 },
                borderRadius: 3,
              }
            }
          }
        }
      },
      scales: {
        x: {
          type: 'linear',
          position: 'bottom',
          title: { display: true, text: '${escJs(entries.first().scoreUnit)}', font: { family: 'Fira Sans', size: 11 } },
          grid: { color: '#e2e2ef' },
          ticks: { font: { family: 'Fira Sans', size: 10 } },
          min: 0,
          max: axisMax,
        },
        xPct: {
          type: 'linear',
          position: 'top',
          grid: { drawOnChartArea: false },
          ticks: {
            font: { family: 'Fira Sans', size: 10 },
            color: '#FF420E',
            callback: function(value) { return (value / bestScore * 100).toFixed(0) + '%'; }
          },
          min: 0,
          max: axisMax,
        },
        y: {
          grid: { display: false },
          ticks: { font: { family: 'Fira Sans', size: 11 } }
        }
      }
    }
  });
})();
</script>
"""
    }

    // ── escaping helpers ────────────────────────────────────────────────

    private static String esc(String s) {
        s.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;')
    }

    private static String escJs(String s) {
        s.replaceAll("'", "\\\\'").replaceAll('\n', '\\\\n')
    }
}