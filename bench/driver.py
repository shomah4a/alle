#!/usr/bin/env python3
"""
pty経由でエディタを起動し、初期化完了後に asprof collect -d でプロファイリングする。

使い方:
    python3 driver.py <fat_jar> <profiler_lib> <asprof_bin> <testdata> <keystroke_file> <jfr_output>
"""
import fcntl
import os
import pty
import select
import struct
import subprocess
import sys
import termios
import threading
import time


def child_setup(slave_fd: int) -> None:
    """子プロセスで制御端末を設定する"""
    os.setsid()
    fcntl.ioctl(slave_fd, termios.TIOCSCTTY, 0)


def drain_pty(master_fd: int, stop_event: threading.Event) -> None:
    """ptyの出力を読み捨てるスレッド（バッファ溢れ防止）"""
    while not stop_event.is_set():
        readable, _, _ = select.select([master_fd], [], [], 0.5)
        if readable:
            try:
                os.read(master_fd, 65536)
            except OSError:
                break


def main() -> None:
    if len(sys.argv) != 7:
        print(
            "Usage: driver.py <fat_jar> <profiler_lib> <asprof_bin> <testdata> <keystroke_file> <jfr_output>",
            file=sys.stderr,
        )
        sys.exit(1)

    fat_jar = sys.argv[1]
    profiler_lib = sys.argv[2]
    asprof_bin = sys.argv[3]
    testdata = sys.argv[4]
    keystroke_file = sys.argv[5]
    jfr_output = sys.argv[6]

    with open(keystroke_file, "rb") as f:
        keystrokes = f.read()

    # ptyを割り当て
    master_fd, slave_fd = pty.openpty()

    # ターミナルサイズを設定 (80x24)
    winsize = struct.pack("HHHH", 24, 80, 0, 0)
    fcntl.ioctl(master_fd, termios.TIOCSWINSZ, winsize)

    env = os.environ.copy()
    env["TERM"] = "xterm-256color"

    # async-profiler をロード（開始しない）
    agent_opts = f"{profiler_lib}=event=cpu"

    proc = subprocess.Popen(
        [
            "java",
            f"-agentpath:{agent_opts}",
            "-jar",
            fat_jar,
            testdata,
        ],
        stdin=slave_fd,
        stdout=slave_fd,
        stderr=slave_fd,
        env=env,
        close_fds=True,
        preexec_fn=lambda: child_setup(slave_fd),
    )
    os.close(slave_fd)

    pid = proc.pid
    print(f"Editor PID: {pid}", file=sys.stderr)

    # --- 初期化完了を待つ ---
    print("Waiting for initialization...", file=sys.stderr)
    init_wait = 60
    start_time = time.time()
    output_buf = b""
    while time.time() - start_time < init_wait:
        ret = proc.poll()
        if ret is not None:
            readable, _, _ = select.select([master_fd], [], [], 0.5)
            if readable:
                try:
                    output_buf += os.read(master_fd, 65536)
                except OSError:
                    pass
            print(f"Editor exited early with code {ret}", file=sys.stderr)
            print(f"Output:\n{output_buf[-2000:].decode('utf-8', errors='replace')}", file=sys.stderr)
            os.close(master_fd)
            sys.exit(1)

        readable, _, _ = select.select([master_fd], [], [], 0.5)
        if readable:
            try:
                data = os.read(master_fd, 4096)
                if data:
                    output_buf += data
            except OSError:
                break

        if len(output_buf) > 1000:
            print(f"Screen output detected ({len(output_buf)} bytes).", file=sys.stderr)
            time.sleep(2)
            break

    elapsed = time.time() - start_time
    print(f"Init wait: {elapsed:.1f}s", file=sys.stderr)

    # --- pty出力読み捨てスレッドを起動 ---
    drain_stop = threading.Event()
    drain_thread = threading.Thread(target=drain_pty, args=(master_fd, drain_stop), daemon=True)
    drain_thread.start()

    # --- キーストローク送信量から必要時間を見積もり ---
    chunk_size = 32
    sleep_per_chunk = 0.005
    num_chunks = (len(keystrokes) + chunk_size - 1) // chunk_size
    estimated_send_sec = num_chunks * sleep_per_chunk
    # 送信時間 + 処理の余裕 + asprof自身のオーバーヘッド
    profile_duration = int(estimated_send_sec + 15)

    # --- asprof collect をバックグラウンドで開始 ---
    print(f"Starting asprof collect -d {profile_duration} ...", file=sys.stderr)
    asprof_proc = subprocess.Popen(
        [asprof_bin, "collect", "-d", str(profile_duration), "-o", "jfr", "-f", jfr_output, str(pid)],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )

    # asprof が attach してプロファイリングを開始するまで少し待つ
    time.sleep(1)

    # --- キーストロークを送信 ---
    print(f"Sending {len(keystrokes)} bytes of keystrokes...", file=sys.stderr)
    for i in range(0, len(keystrokes), chunk_size):
        chunk = keystrokes[i : i + chunk_size]
        try:
            os.write(master_fd, chunk)
        except OSError as e:
            print(f"Write error at offset {i}: {e}", file=sys.stderr)
            break
        time.sleep(sleep_per_chunk)

    # --- asprof の完了を待つ ---
    print("Waiting for asprof collect to finish...", file=sys.stderr)
    asprof_stdout, asprof_stderr = asprof_proc.communicate(timeout=profile_duration + 30)
    print(f"asprof exit={asprof_proc.returncode}", file=sys.stderr)
    if asprof_stdout.strip():
        print(f"  stdout: {asprof_stdout.decode('utf-8', errors='replace').strip()}", file=sys.stderr)
    if asprof_stderr.strip():
        print(f"  stderr: {asprof_stderr.decode('utf-8', errors='replace').strip()}", file=sys.stderr)

    # --- 後片付け ---
    drain_stop.set()
    drain_thread.join(timeout=2)

    ret = proc.poll()
    if ret is None:
        print("Killing editor...", file=sys.stderr)
        proc.kill()
        proc.wait(timeout=5)

    try:
        os.close(master_fd)
    except OSError:
        pass

    print(f"Editor exit code: {proc.returncode}", file=sys.stderr)

    if os.path.exists(jfr_output):
        size = os.path.getsize(jfr_output)
        print(f"JFR output: {jfr_output} ({size} bytes)", file=sys.stderr)
    else:
        print(f"ERROR: JFR file not found: {jfr_output}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
