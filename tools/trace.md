# tools/trace.py 和 tools/trace_report.py

在电视上录桌面的 Perfetto trace，然后报告时间花在哪。只要总数（按键延迟、卡顿、冷启动、PSS）的话用 `tools/bench.py`，它不用额外装东西。

## 准备

```bash
tools/venv/bin/pip install -r tools/requirements-perf.txt
```

录制本身只需要 `tools/requirements.txt` 和一台 Android 9 以上的电视。报告第一次运行会下载 Perfetto 的查询引擎（约 13 MB，带 SHA-256 校验）到 `~/.local/share/perfetto`。不想下载就自己下 `trace_processor_shell`，用 `--engine PATH` 指过去。

## 录制

```bash
TV_HOST=<电视 IP> tools/venv/bin/python -I tools/trace.py home --runs 5 --out traces/before
```

| 场景 | 录什么 | 用来查 |
|---|---|---|
| `home` | 别的应用在前台时按 HOME | 大多数桌面改动 |
| `cold` | force-stop 之后 `am start -W`，另一个应用在前台 | 启动阶段（`onCreate`、配置、字体） |
| `coldsys` | 桌面在前台时 `am force-stop`，让 Android 自己重启它 | 因内存被杀之后的重启 |
| `nav` | 24 次方向键，间隔 0.35 秒 | 焦点、滚动、绘制 |
| `custom` | `--setup CMD` 之后跑 `--step CMD`（可重复） | 其他场景；`sleep N` 控制时长 |

- **输出**：每次运行写一个 `<场景>-<n>.pftrace`（10～20 MB）和一个 `.json`，里面是执行的命令和电视的型号、build。`traces/` 已加进 gitignore。
- **另一个应用**：`--app 包名/Activity`，默认用电视的设置应用。

## 报告

```bash
tools/venv/bin/python tools/trace_report.py traces/before --vs traces/after --markdown
```

| 场景 | 数字 |
|---|---|
| 启动类（`home`、`cold`、`coldsys`、`custom`） | 从开始到画出第一帧；中间各步（`bindApplication`、`onCreate`、`onStart`、`onResume`、首次布局和绘制）；随后 1.5 秒内画了多少帧、掉了多少帧 |
| `nav` | 帧时间（中位数、90 分位）；RenderThread 绘制；GPU 等待；掉帧 |

带 `--vs` 时会多一张表，列每个中位数的变化。超过中位数两倍的运行会标星。

## 规则

- **多跑几次**。同一个 build、同一台电视、同样的配置主题壁纸，每次录 5 遍，比中位数。
- **标星的运行要留着**。通常是 1 GB 电视的内存压力。报出来，别丢。
- **桌面在前台时不要 force-stop 之后又 `am start`**。Android 已经把桌面重启了，`onResume` 会出现两次。用 `cold` 或 `coldsys`。
- **录完检查遥控器**。force-stop 会关掉无障碍辅助服务。`trace.py` 在最后一次运行后会写回列表，但个别 TCL 机型写回之后也绑不上（见 `docs/hisense/故障排查.md`）。
- **GPU 的收益要看对地方**。过度绘制减少体现在 `nav` 的 GPU 等待上，不体现在主线程时间上。
- **挪到后台线程不会降低延迟**。它减少的是工作量，不是到第一帧的时间。
- **`.pftrace` 永远不要提交**，里面有电视的进程列表。
