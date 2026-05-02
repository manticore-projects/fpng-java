package com.manticore.benchmark

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Produces a Pareto-frontier report joining JMH timings with file sizes from
 * EncoderBenchmark.csv. For each (image, channels) group, plots all
 * (encoder, compression-level) points in (time, size) space, marks
 * Pareto-optimal points, and identifies the "knee" of each encoder's
 * frontier as the recommended compression level.
 *
 * Input expectations:
 *   - resultsFile: standard JMH results.json. Each entry must include
 *     params.imageName, params.channels, params.compressionLevel.
 *   - sizesFile:  EncoderBenchmark.csv with semicolon-separated rows of
 *     date;encoder;image;channels;level;sizeBytes (last row per key wins).
 *
 * Output: a self-contained HTML report with one card per (image, channels)
 * group, scatter chart and per-encoder breakdown tables.
 */
abstract class JmhParetoReportTask extends DefaultTask {

    @InputFile
    abstract RegularFileProperty getResultsFile()

    @InputFile
    abstract RegularFileProperty getSizesFile()

    @OutputFile
    abstract RegularFileProperty getReportFile()

    // Mirror the labels used in JmhReportTask so the two reports speak the
    // same language. Keep in sync if the encoder roster changes.
    private static final Map<String, String> ENCODER_LABELS = [
            'FPNGE23Benchmark'              : 'FPNGE (Java 23 FFM)',
            'FPNGEBenchmark'                : 'FPNGE (JNI)',
            'FPNGEncoder23Benchmark'        : 'FPNG Encoder (Java 23)',
            'FPNGEncoderBenchmark'          : 'FPNG Encoder (JNI)',
            'ImageIOEncoderBenchmark'       : 'ImageIO',
            'ObjectPlanetPNGEncoderBenchmark': 'ObjectPlanet PNG',
            'PNGEncoderBenchmark'           : 'PNGEncoder',
    ]

    // Per-encoder colour assignments. Cycles if there are more encoders than
    // colours; first two slots match the existing JmhReportTask palette.
    private static final List<String> ENCODER_COLOURS = [
            '#030146',  // navy   — primary
            '#FF420E',  // orange — accent
            '#1d8348',  // green
            '#7d3c98',  // purple
            '#aab7b8',  // grey
    ]

    @TaskAction
    void generateReport() {
        def json = new JsonSlurper().parse(resultsFile.asFile.get())
        def sizes = parseSizes(sizesFile.asFile.get())
        def groups = groupAndAnalyze(json as List, sizes)

        if (groups.isEmpty()) {
            def jmhSample = (json as List).take(3).collect { entry ->
                def cls = (entry.benchmark as String).tokenize('.')[-2]
                def p = entry.params
                "${cls}|${p?.imageName}|${p?.channels}|${p?.compressionLevel}"
            }
            def csvSample = sizes.keySet().take(3).toList()
            logger.warn("Pareto report is empty: 0 of ${(json as List).size()} JMH " +
                    "entries joined against ${sizes.size()} CSV size rows.")
            logger.warn("  JMH keys (sample): ${jmhSample}")
            logger.warn("  CSV keys (sample): ${csvSample}")
            logger.warn("  Check that encoder class names, channels and " +
                    "compressionLevel @Param values match on both sides.")
        }

        def html = renderHtml(groups)
        reportFile.asFile.get().text = html
        logger.lifecycle("JMH Pareto report written to ${reportFile.asFile.get()}")
    }

    // ── data model ──────────────────────────────────────────────────────

    static class DataPoint {
        String encoderClass   // raw JMH class name, used for size CSV joins
        String encoder        // human-readable label
        int level
        double timeMs
        double timeError
        long sizeBytes
        boolean pareto = false        // non-dominated within this encoder
        boolean knee = false          // knee of this encoder's frontier
        boolean globalPareto = false  // non-dominated across all encoders in the group
        boolean globalKnee = false    // knee of the global frontier — the overall recommendation
    }

    // JMH compiles each @Benchmark class into a synthetic <Name>_jmhType
    // subclass (with _jmhType_B1/B2/B3 blackhole variants). If the CSV writer
    // captured the class name via getClass().getSimpleName() it will carry
    // that suffix, while results.json reports the user-facing class. Strip
    // the suffix on read so the two sides join regardless.
    private static final java.util.regex.Pattern JMH_SUFFIX = ~/_jmhType.*$/

    /**
     * Parse EncoderBenchmark.csv. Last row per key wins, allowing reruns to
     * overwrite stale measurements without manually clearing the CSV.
     * Returns Map keyed as "encoderClass|imageName|channels|level".
     */
    private static Map<String, Long> parseSizes(File csv) {
        def sizes = [:] as LinkedHashMap
        if (!csv.exists()) return sizes
        csv.eachLine { line ->
            def parts = line.split(';')
            if (parts.length < 6) return
            def (_date, rawEncoderClass, imageName, channels, level, sizeStr) =
            [parts[0], parts[1], parts[2], parts[3], parts[4], parts[5].trim()]
            def encoderClass = rawEncoderClass.replaceAll(JMH_SUFFIX, '')
            try {
                long bytes = sizeStr as long
                sizes["${encoderClass}|${imageName}|${channels}|${level}".toString()] = bytes
            } catch (NumberFormatException ignored) {
                // header row or malformed line — skip silently
            }
        }
        return sizes
    }

    /**
     * Joins JMH entries with sizes, groups by (image, channels), and
     * computes Pareto frontier + knee per encoder within each group.
     * Returns Map<groupKey, List<DataPoint>>.
     */
    private static Map<String, List<DataPoint>> groupAndAnalyze(
            List json, Map<String, Long> sizes) {
        def grouped = [:] as LinkedHashMap

        json.each { entry ->
            def fqn = entry.benchmark as String
            def className = fqn.tokenize('.')[-2]
            def label = ENCODER_LABELS.getOrDefault(className, className)

            def imageName = entry.params.imageName as String
            def channels = entry.params.channels as String
            def levelStr = entry.params.compressionLevel as String
            if (levelStr == null) {
                // Benchmark didn't sweep compressionLevel — can't place on Pareto.
                return
            }

            def sizeKey = "${className}|${imageName}|${channels}|${levelStr}".toString()
            def sizeBytes = sizes[sizeKey]
            if (sizeBytes == null) {
                // No matching CSV row — skip this entry rather than guess.
                return
            }

            def groupKey = "${imageName} | ${channels} ch".toString()
            grouped.computeIfAbsent(groupKey) { [] } << new DataPoint(
                    encoderClass: className,
                    encoder: label,
                    level: levelStr as int,
                    timeMs: entry.primaryMetric.score as double,
                    timeError: entry.primaryMetric.scoreError as double,
                    sizeBytes: sizeBytes,
            )
        }

        // Compute Pareto + knee per encoder within each group, then a second
        // pass on the combined point set to find the overall winner across
        // all (encoder, level) candidates.
        grouped.each { _, points ->
            points.groupBy { it.encoder }.each { _enc, pts ->
                computeParetoAndKnee(pts)
            }
            computeGlobalParetoAndKnee(points)
        }

        return grouped
    }

    /**
     * Marks p.pareto for non-dominated points and p.knee for the maximum-
     * curvature Pareto-optimal point (perpendicular-distance heuristic on
     * normalized axes). Mutates the points in place.
     */
    private static void computeParetoAndKnee(List<DataPoint> points) {
        def (pareto, knee) = computeFrontierAndKnee(points)
        pareto.each { it.pareto = true }
        if (knee != null) (knee as DataPoint).knee = true
    }

    /**
     * Same calculation as computeParetoAndKnee, but writes to the global
     * flags. Run on the full point set of an (image, channels) group to
     * find the single best (encoder, level) combination — the global knee.
     */
    private static void computeGlobalParetoAndKnee(List<DataPoint> points) {
        def (pareto, knee) = computeFrontierAndKnee(points)
        pareto.each { it.globalPareto = true }
        if (knee != null) (knee as DataPoint).globalKnee = true
    }

    /**
     * Pure function: given a set of (time, size) points, returns
     * [paretoPoints, kneePoint-or-null] without mutating any flag fields.
     * Both wrappers above just decide which boolean to flip.
     */
    private static List computeFrontierAndKnee(List<DataPoint> points) {
        // A point is dominated if some other point is <= on both axes
        // and strictly < on at least one.
        def pareto = points.findAll { p ->
            !points.any { q ->
                !q.is(p) &&
                        q.timeMs <= p.timeMs && q.sizeBytes <= p.sizeBytes &&
                        (q.timeMs < p.timeMs || q.sizeBytes < p.sizeBytes)
            }
        }

        def frontier = pareto.toSorted { it.timeMs }
        if (frontier.size() < 3) return [pareto, null]

        double tMin = frontier.first().timeMs
        double tMax = frontier.last().timeMs
        double sMin = frontier.last().sizeBytes
        double sMax = frontier.first().sizeBytes
        if (tMax == tMin || sMax == sMin) return [pareto, null]

        int kneeIdx = 0
        double kneeDist = -1.0d
        frontier.eachWithIndex { p, i ->
            double x = (p.timeMs - tMin) / (tMax - tMin)
            double y = (p.sizeBytes - sMin) / (sMax - sMin)
            // Line through (0,1) and (1,0): x + y - 1 = 0.
            // Distance from point to that line is |x+y-1|/sqrt(2); we only
            // need the relative ordering, so the constant divisor drops.
            double d = Math.abs(x + y - 1.0d)
            if (d > kneeDist) {
                kneeDist = d
                kneeIdx = i
            }
        }
        return [pareto, frontier[kneeIdx]]
    }

    // ── HTML rendering ──────────────────────────────────────────────────

    private static String renderHtml(Map<String, List<DataPoint>> groups) {
        def cells = new StringBuilder()
        int chartIdx = 0
        groups.each { groupKey, points ->
            cells.append(renderGroup(groupKey, points, chartIdx++))
        }

        return """\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>JMH PNG Encoder Pareto Report</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@3"></script>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Fira+Sans:wght@300;400;600;700&display=swap');

  :root {
    --navy:   #030146;
    --orange: #FF420E;
    --gold:   #f1c40f;
    --bg:     #f7f7fb;
    --card:   #ffffff;
    --muted:  #6c6c8a;
    --border: #e2e2ef;
    --pareto-bg:    rgba(3, 1, 70, 0.05);
    --knee-bg:      rgba(241, 196, 15, 0.18);
    --winner-bg:    rgba(241, 196, 15, 0.45);
    --dominated-bg: rgba(170, 183, 184, 0.12);
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
  @media (max-width: 1100px) {
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
    min-height: 320px;
    position: relative;
    overflow: hidden;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.78rem;
    margin-top: 0.5rem;
  }
  thead th {
    text-align: left;
    padding: 0.4rem 0.5rem;
    border-bottom: 2px solid var(--navy);
    font-weight: 600;
    font-size: 0.7rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--muted);
  }
  thead th:nth-child(n+3) { text-align: right; }
  tbody td {
    padding: 0.3rem 0.5rem;
    border-bottom: 1px solid var(--border);
  }
  tbody td:nth-child(n+3) { text-align: right; font-variant-numeric: tabular-nums; }

  tbody tr.knee td      { background: var(--knee-bg); font-weight: 600; }
  tbody tr.knee td:first-child::before { content: '\\1F3C6\\00a0'; }
  tbody tr.winner td    { background: var(--winner-bg); font-weight: 700; color: var(--navy); }
  tbody tr.winner td:first-child::before { content: '\\1F451\\00a0'; }
  tbody tr.pareto td    { background: var(--pareto-bg); }
  tbody tr.dominated td { color: var(--muted); }

  .flag {
    display: inline-block;
    font-size: 0.66rem;
    font-weight: 600;
    padding: 0.05rem 0.35rem;
    border-radius: 3px;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }
  .flag-winner    { background: var(--navy); color: var(--gold); border: 1px solid var(--gold); }
  .flag-knee      { background: var(--gold); color: var(--navy); }
  .flag-pareto    { background: var(--navy); color: white; }
  .flag-dominated { background: transparent; color: var(--muted); }

  .winner-banner {
    background: var(--winner-bg);
    color: var(--navy);
    border: 1px solid var(--gold);
    border-radius: 6px;
    padding: 0.5rem 0.75rem;
    margin-bottom: 0.75rem;
    font-size: 0.85rem;
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .winner-banner::before { content: '\\1F451'; font-size: 1.05rem; }
  .winner-banner .label {
    text-transform: uppercase;
    font-size: 0.65rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: var(--muted);
    margin-right: 0.25rem;
  }
  .winner-banner .none {
    font-style: italic;
    font-weight: 400;
    color: var(--muted);
  }

  .encoder-section { margin-top: 0.5rem; }
  .encoder-section h3 {
    font-size: 0.85rem;
    font-weight: 600;
    margin-bottom: 0.25rem;
    color: var(--navy);
  }

  .legend {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    margin: 0.5rem 0 0.75rem;
    font-size: 0.75rem;
    color: var(--muted);
  }
  .legend-item { display: flex; align-items: center; gap: 0.3rem; }
  .legend-marker {
    width: 12px; height: 12px;
    border-radius: 50%;
    border: 1px solid var(--navy);
  }
  .legend-knee { background: var(--gold); }
  .legend-winner { background: var(--gold); border-width: 2.5px; box-shadow: 0 0 0 2px var(--navy) inset; }
  .legend-pareto { background: var(--navy); }
  .legend-dominated { background: transparent; }

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
    <h1>PNG Encoder Pareto Report</h1>
    <div class="sub">Generated by Manticore Projects &middot; encode time vs. file size &middot; lower-left is better</div>
  </div>
</header>

<div class="grid">
${cells}
</div>

<footer>
  Manticore Projects &copy; ${java.time.Year.now()} &middot;
  Pareto frontier marks non-dominated (encoder, level) combinations &middot;
  knee = perpendicular-distance maximum on normalized axes
</footer>

</body>
</html>
"""
    }

    /**
     * Renders one card per (image, channels) group: a scatter chart with one
     * series per encoder, dashed Pareto frontier overlays, and per-encoder
     * breakdown tables underneath.
     */
    private static String renderGroup(String groupKey, List<DataPoint> points, int idx) {
        def (imageName, channelsLabel) = groupKey.split(' \\| ')
        def canvasId = "chart_${idx}"

        def byEncoder = points.groupBy { it.encoder }
        def encoders = byEncoder.keySet().toList().sort()

        // Build Chart.js datasets — two per encoder:
        //   1. all points, with per-point styling distinguishing
        //      knee / pareto / dominated.
        //   2. Pareto frontier line connecting non-dominated points sorted
        //      by time.
        def datasets = new StringBuilder()
        encoders.eachWithIndex { encoder, encIdx ->
            def colour = ENCODER_COLOURS[encIdx % ENCODER_COLOURS.size()]
            def encPoints = (byEncoder[encoder] as List<DataPoint>).sort { it.level }

            def pointsJs = encPoints.collect { p ->
                "{x:${String.format('%.3f', p.timeMs)},y:${p.sizeBytes},level:${p.level}}"
            }.join(',')

            def bgColours = encPoints.collect { p ->
                (p.globalKnee || p.knee) ? "'#f1c40f'" :
                        (p.pareto ? "'${colour}'" : "'rgba(170,183,184,0.4)'")
            }.join(',')

            def borderColours = encPoints.collect { p ->
                (p.globalKnee || p.knee) ? "'#030146'" : "'${colour}'"
            }.join(',')

            def radii = encPoints.collect { p ->
                p.globalKnee ? '14' : (p.knee ? '10' : (p.pareto ? '7' : '5'))
            }.join(',')

            def borderWidths = encPoints.collect { p ->
                p.globalKnee ? '4' : (p.knee ? '2.5' : '1.5')
            }.join(',')

            datasets.append("""        {
          type: 'scatter',
          label: '${escJs(encoder)}',
          data: [${pointsJs}],
          backgroundColor: [${bgColours}],
          borderColor: [${borderColours}],
          borderWidth: [${borderWidths}],
          pointRadius: [${radii}],
          pointHoverRadius: [${encPoints.collect { p -> p.globalKnee ? '16' : (p.knee ? '12' : '8') }.join(',')}],
        },
""")

            // Pareto frontier line — separate dataset for the dashed connector
            def frontier = encPoints.findAll { it.pareto }.sort { it.timeMs }
            if (frontier.size() >= 2) {
                def lineJs = frontier.collect { p ->
                    "{x:${String.format('%.3f', p.timeMs)},y:${p.sizeBytes}}"
                }.join(',')
                datasets.append("""        {
          type: 'line',
          label: '${escJs(encoder)} (frontier)',
          data: [${lineJs}],
          borderColor: '${colour}',
          borderWidth: 1.5,
          borderDash: [5, 4],
          pointRadius: 0,
          fill: false,
          showLine: true,
          tension: 0,
          order: 99,
        },
""")
            }
        }

        // Per-encoder breakdown tables
        def tables = new StringBuilder()
        encoders.each { encoder ->
            def encPoints = (byEncoder[encoder] as List<DataPoint>).sort { it.level }
            def maxSize = encPoints.collect { it.sizeBytes }.max() ?: 1L

            tables.append("""    <div class="encoder-section">
      <h3>${esc(encoder)}</h3>
      <table>
        <thead>
          <tr>
            <th>Lvl</th>
            <th>Status</th>
            <th>Time (ms)</th>
            <th>Size (KB)</th>
            <th>Ratio</th>
          </tr>
        </thead>
        <tbody>
""")
            encPoints.each { p ->
                String trClass = p.globalKnee ? 'winner' :
                        (p.knee ? 'knee' :
                                (p.pareto ? 'pareto' : 'dominated'))
                String flagClass = p.globalKnee ? 'flag-winner' :
                        (p.knee ? 'flag-knee' :
                                (p.pareto ? 'flag-pareto' : 'flag-dominated'))
                String flagText = p.globalKnee ? 'WINNER' :
                        (p.knee ? 'KNEE' :
                                (p.pareto ? 'PARETO' : 'dominated'))
                double ratioPct = 100.0d * p.sizeBytes / maxSize
                tables.append("""          <tr class="${trClass}">
            <td>${p.level}</td>
            <td><span class="flag ${flagClass}">${flagText}</span></td>
            <td>${String.format('%.2f', p.timeMs)} &plusmn;${String.format('%.2f', p.timeError)}</td>
            <td>${String.format('%.1f', p.sizeBytes / 1024.0d)}</td>
            <td>${String.format('%.1f', ratioPct)}%</td>
          </tr>
""")
            }
            tables.append("        </tbody>\n      </table>\n    </div>\n")
        }

        // Find the global knee (overall winner) so we can render a banner
        // and the expanded legend. May be null when the global frontier
        // has fewer than 3 points (knee calculation needs three to define
        // a meaningful elbow).
        def winner = points.find { it.globalKnee }
        def winnerBanner
        if (winner != null) {
            winnerBanner = """  <div class="winner-banner">
    <span class="label">Overall best</span>
    ${esc(winner.encoder)} at level ${winner.level}
    &mdash; ${String.format('%.2f', winner.timeMs)}&nbsp;ms,
    ${String.format('%.1f', winner.sizeBytes / 1024.0d)}&nbsp;KB
  </div>
"""
        } else {
            winnerBanner = """  <div class="winner-banner">
    <span class="label">Overall best</span>
    <span class="none">no compromise winner &mdash; global frontier has fewer than 3 points</span>
  </div>
"""
        }

        return """
<div class="group">
  <h2>
    <span class="badge">${esc(channelsLabel)}</span>
    ${esc(imageName as String)}
  </h2>

${winnerBanner}
  <div class="legend">
    <div class="legend-item"><span class="legend-marker legend-winner"></span>overall winner</div>
    <div class="legend-item"><span class="legend-marker legend-knee"></span>per-encoder knee</div>
    <div class="legend-item"><span class="legend-marker legend-pareto"></span>pareto-optimal</div>
    <div class="legend-item"><span class="legend-marker legend-dominated"></span>dominated</div>
  </div>

  <div class="chart-wrap">
    <canvas id="${canvasId}"></canvas>
  </div>

${tables}
</div>

<script>
(function() {
  const ctx = document.getElementById('${canvasId}');
  new Chart(ctx, {
    data: {
      datasets: [
${datasets}      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            font: { family: 'Fira Sans', size: 10 },
            filter: function(item) { return !item.text.includes('(frontier)'); }
          }
        },
        tooltip: {
          callbacks: {
            label: function(c) {
              const p = c.raw;
              const sizeKb = (p.y / 1024).toFixed(1);
              if (p.level !== undefined) {
                return c.dataset.label + ' L' + p.level +
                       ': ' + p.x.toFixed(2) + ' ms, ' + sizeKb + ' KB';
              }
              return null;
            }
          }
        }
      },
      scales: {
        x: {
          type: 'linear',
          position: 'bottom',
          title: { display: true, text: 'encode time (ms)', font: { family: 'Fira Sans', size: 11 } },
          grid: { color: '#e2e2ef' },
          ticks: { font: { family: 'Fira Sans', size: 10 } }
        },
        y: {
          type: 'linear',
          title: { display: true, text: 'output size (bytes)', font: { family: 'Fira Sans', size: 11 } },
          grid: { color: '#e2e2ef' },
          ticks: {
            font: { family: 'Fira Sans', size: 10 },
            callback: function(v) { return (v / 1024).toFixed(0) + ' KB'; }
          }
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